package project.NIR.Models.Data;

import project.NIR.Models.Routes.GeoPath;
import project.NIR.Models.Routes.Path;
import project.NIR.Models.Warehouse;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Utils.GeoUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedData {
    // Stores all active missions and drone states.
    // Key: For client-submitted paths, it's a temporary mission ID (e.g., from missionIdCounter).
    //      For drones (idle or busy), it's the droneId.
    private static final Map<Integer, ActiveMission> activeMissions = new ConcurrentHashMap<>();
    private static final AtomicInteger tempMissionIdCounter = new AtomicInteger(1); // Start at 1 for positive temp IDs, will be made negative
    private static final AtomicInteger droneIdCounter = new AtomicInteger(1); // For generating unique drone IDs
    private static final List<Warehouse> warehouses = new ArrayList<>();
    private static final Object lock = new Object();

    public static void initializeWarehousesAndDrones(Map<String, GeoPosition> warehouseLocations, int dronesPerWarehouse) {
        synchronized (lock) {
            warehouses.clear(); // Clear any existing warehouses
            // activeMissions.clear(); // Optionally clear all missions, or just drone-related ones
            // tempMissionIdCounter.set(1); // Reset if clearing all missions
            
            for (Map.Entry<String, GeoPosition> entry : warehouseLocations.entrySet()) {
                Warehouse warehouse = new Warehouse(entry.getKey(), entry.getValue());
                warehouses.add(warehouse);
                System.out.println("SharedData: Initialized " + warehouse);

                for (int i = 0; i < dronesPerWarehouse; i++) {
                    int droneId = droneIdCounter.getAndIncrement(); // Positive IDs: 1, 2, 3...
                    warehouse.addDrone(droneId); // Register drone with warehouse
                    ActiveMission idleDroneMission = new ActiveMission(droneId, warehouse.getLocation());
                    activeMissions.put(droneId, idleDroneMission); // Keyed by positive droneId
                    System.out.println("SharedData: Initialized idle drone ID: " + droneId + " at warehouse " + warehouse.getName() + ". Total missions/drones: " + activeMissions.size());
                }
            }
        }
    }
    
    public static List<Warehouse> getWarehouses() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(warehouses));
        }
    }

    public static int addPendingClientMission(Path path) {
        synchronized (lock) {
            GeoPath geoPath = new GeoPath(path);
            if (geoPath.getPoints() == null || geoPath.getPoints().isEmpty()) {
                System.err.println("SharedData: Attempted to add pending mission with null or empty path.");
                return 0; // Indicate failure (0 is not a valid negative temp ID)
            }
            // This ActiveMission represents the path requested by a client, not yet assigned to a drone.
            // droneId is 0, assigned is false.
            ActiveMission clientRequestedMission = new ActiveMission(geoPath); 
            int positiveTempId = tempMissionIdCounter.getAndIncrement(); 
            int uniqueNegativeTempKey = -positiveTempId; // Make it negative and unique
            activeMissions.put(uniqueNegativeTempKey, clientRequestedMission); // Stored with a negative key
            System.out.println("SharedData: Added pending client mission with temp key: " + uniqueNegativeTempKey + " for path starting: " + geoPath.getPoints().get(0) + ". Missions map size: " + activeMissions.size());
            return uniqueNegativeTempKey; // Return the negative key
        }
    }

    // Renamed from assignPendingMissionToVirtualDrone and assignRealDroneToPendingMission
    public static boolean assignMissionToAvailableDrone(int tempMissionKey) { // Parameter is now the negative key
        synchronized (lock) {
            if (tempMissionKey >= 0) { // Defensive check: tempMissionKey should be negative
                System.err.println("SharedData: assignMissionToAvailableDrone called with invalid (non-negative) tempMissionKey: " + tempMissionKey);
                return false;
            }
            ActiveMission clientMission = activeMissions.get(tempMissionKey);
            if (clientMission == null || clientMission.isAssigned() || clientMission.getDroneId() != 0 || clientMission.getPath() == null) {
                System.err.println("SharedData: Client mission (key: " + tempMissionKey + ") not found, already assigned, not a pending client mission, or has no path.");
                return false;
            }

            GeoPosition missionStartPos = clientMission.getPathPoints().get(0);
            Warehouse closestWarehouse = findClosestWarehouse(missionStartPos);
            
            if (closestWarehouse == null) {
                System.err.println("SharedData: No warehouses found to assign mission (key: " + tempMissionKey + ")");
                return false;
            }

            System.out.println("SharedData: Closest warehouse to mission (key: " + tempMissionKey + ") is " + closestWarehouse.getName());

            // Iterate through drones registered at this warehouse
            for (Integer droneIdAtWarehouse : closestWarehouse.getResidentDroneIdsView()) { // droneIdAtWarehouse is positive
                ActiveMission drone = activeMissions.get(droneIdAtWarehouse);
                if (drone != null && drone.getDroneId() == droneIdAtWarehouse && !drone.isAssigned()) { // Ensure it's the correct drone and it's idle
                    System.out.println("SharedData: Found available idle drone ID: " + drone.getDroneId() + " at warehouse " + closestWarehouse.getName() + " for mission (key: " + tempMissionKey + ")");
                    
                    // Assign client mission path to this drone
                    drone.setPath(clientMission.getPath());
                    drone.setCurrentDronePosition(clientMission.getPathPoints().get(0)); // Set drone to start of new path
                    drone.setCurrentSegmentTargetIndex(1);
                    drone.setAssigned(true);
                    drone.setLastUpdateTime(System.currentTimeMillis());

                    activeMissions.remove(tempMissionKey); // Remove the original client mission entry using its negative key
                    // The drone's entry in activeMissions (keyed by its droneId) is now updated.
                    System.out.println("SharedData: Assigned client mission (original key: " + tempMissionKey + ") to drone " + drone.getDroneId() + ". Missions map size: " + activeMissions.size());
                    return true;
                }
            }
            System.out.println("SharedData: No available idle drone found at warehouse " + closestWarehouse.getName() + " for mission (key: " + tempMissionKey + ")");
            return false;
        }
    }

    private static Warehouse findClosestWarehouse(GeoPosition targetLocation) {
        Warehouse closest = null;
        double minDistance = Double.MAX_VALUE;
        for (Warehouse wh : warehouses) {
            double distance = GeoUtils.calculateDistance(wh.getLocation(), targetLocation);
            if (distance < minDistance) {
                minDistance = distance;
                closest = wh;
            }
        }
        return closest;
    }

    public static ActiveMission getActiveMissionByDroneId(int droneId) {
        synchronized (lock) {
            return activeMissions.get(droneId);
        }
    }
    
    @Deprecated // This was for temp IDs of unassigned missions before assignment to any drone type.
    public static ActiveMission getActiveMissionByTempId(int tempMissionKey) { // tempMissionKey should be negative
        synchronized (lock) {
            if (tempMissionKey >= 0) return null;
            ActiveMission mission = activeMissions.get(tempMissionKey);
            // Check if it's a truly pending client mission (droneId is 0, not yet assigned)
            if (mission != null && !mission.isAssigned() && mission.getDroneId() == 0) { 
                return mission;
            }
            return null; 
        }
    }

    public static void updateDronePosition(int droneId, GeoPosition newPosition, int nextSegmentTargetIndex) {
        synchronized (lock) {
            ActiveMission mission = activeMissions.get(droneId);
            if (mission != null) { // Drone could be idle or assigned
                mission.setCurrentDronePosition(newPosition);
                if (mission.isAssigned()) { // Only update segment index if on an active mission
                    mission.setCurrentSegmentTargetIndex(nextSegmentTargetIndex);
                }
                mission.setLastUpdateTime(System.currentTimeMillis());
            } else {
                // System.err.println("SharedData: Attempted to update position for non-existent drone ID: " + droneId);
            }
        }
    }

    public static Collection<ActiveMission> getAllActiveMissions() {
        synchronized (lock) {
            return new ArrayList<>(activeMissions.values());
        }
    }

    public static int findUnassignedClientMissionKey() { // Returns the negative key
        synchronized (lock) {
            for (Map.Entry<Integer, ActiveMission> entry : activeMissions.entrySet()) {
                Integer key = entry.getKey();
                ActiveMission mission = entry.getValue();
                // A client mission is stored with a negative key, has droneId 0, and is not yet assigned.
                if (key < 0 && mission.getDroneId() == 0 && !mission.isAssigned()) { 
                    System.out.println("SharedData: Found unassigned client mission with temp key: " + key);
                    return key; 
                }
            }
            System.out.println("SharedData: No unassigned client mission found.");
            return 0; // 0 indicates not found, as valid keys will be negative
        }
    }

    public static void clearAllMissionsAndWarehouses() {
        synchronized (lock) {
            activeMissions.clear();
            warehouses.clear();
            tempMissionIdCounter.set(1); 
            droneIdCounter.set(1); // Reset drone ID counter
            System.out.println("SharedData: All active missions and warehouses cleared.");
        }
    }

    // Deprecated methods - to be removed or updated if still necessary
    @Deprecated
    public static List<GeoPath> getPaths() { // This now needs to consider how paths are stored
        List<GeoPath> paths = new ArrayList<>();
        synchronized (lock) {
            for (ActiveMission mission : activeMissions.values()) {
                if (mission.getPath() != null && mission.isAssigned()) { // Only return paths of assigned missions
                    paths.add(mission.getPath());
                }
            }
        }
        return paths;
    }

    @Deprecated
    public static void addPath(Path path) {
        System.out.println("SharedData - Deprecated addPath(Path) called. Use addPendingClientMission instead.");
        addPendingClientMission(path);
    }

    @Deprecated
    public static void clearPaths() {
        // This is ambiguous now. Better to call clearAllMissionsAndWarehouses() for a full reset.
        System.out.println("SharedData - Deprecated clearPaths() called. Consider clearAllMissionsAndWarehouses().");
        // Replicating old behavior: clear only missions that look like client-submitted paths or drones with paths
        synchronized (lock) {
            activeMissions.entrySet().removeIf(entry -> 
                (entry.getKey() < 0 && entry.getValue().getDroneId() == 0 && !entry.getValue().isAssigned()) || // pending client mission (negative key)
                (entry.getKey() > 0 && entry.getValue().getPath() != null)    // drone with a path (positive key)
            );
             // This doesn't clear idle drones or reset counters, which might be desired for "clearPaths"
            System.out.println("SharedData: Deprecated clearPaths() executed. Some missions potentially cleared.");
        }
    }
}
