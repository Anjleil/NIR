package project.NIR.Models.Data;

import project.NIR.Models.Routes.GeoPath;
import project.NIR.Models.Routes.Path;
import project.NIR.Models.Warehouse;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Utils.GeoUtils;
import project.NIR.Utils.Pathfinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

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
    public static int assignMissionToAvailableDrone(int tempMissionKey) { // Parameter is now the negative key, returns droneId or 0
        synchronized (lock) {
            if (tempMissionKey >= 0) { // Defensive check: tempMissionKey should be negative
                System.err.println("SharedData: assignMissionToAvailableDrone called with invalid (non-negative) tempMissionKey: " + tempMissionKey);
                return 0; // Failure
            }
            ActiveMission clientMission = activeMissions.get(tempMissionKey);
            if (clientMission == null || clientMission.isAssigned() || clientMission.getDroneId() != 0 || clientMission.getPath() == null) {
                System.err.println("SharedData: Client mission (key: " + tempMissionKey + ") not found, already assigned, not a pending client mission, or has no path.");
                return 0; // Failure
            }

            GeoPosition missionStartPos = clientMission.getPathPoints().get(0);
            Warehouse closestWarehouse = findClosestWarehouse(missionStartPos);
            
            if (closestWarehouse == null) {
                System.err.println("SharedData: No warehouses found to assign mission (key: " + tempMissionKey + ")");
                return 0; // Failure
            }

            System.out.println("SharedData: Closest warehouse to mission (key: " + tempMissionKey + ") is " + closestWarehouse.getName());

            // Iterate through drones registered at this warehouse
            for (Integer droneIdAtWarehouse : closestWarehouse.getResidentDroneIdsView()) { // droneIdAtWarehouse is positive
                ActiveMission drone = activeMissions.get(droneIdAtWarehouse);
                if (drone != null && drone.getDroneId() == droneIdAtWarehouse && !drone.isAssigned()) { // Ensure it's the correct drone and it's idle
                    System.out.println("SharedData: Found available idle drone ID: " + drone.getDroneId() + " at warehouse " + closestWarehouse.getName() + " for mission (key: " + tempMissionKey + ")");
                    
                    // Assign client mission path to this drone
                    drone.setPath(clientMission.getPath());
                    drone.setOriginalPath(clientMission.getPath());
                    drone.setCurrentDronePosition(clientMission.getPathPoints().get(0)); // Set drone to start of new path
                    drone.setCurrentSegmentTargetIndex(1);
                    drone.setAssigned(true);
                    drone.setReturning(false); // It's a delivery mission
                    drone.setLastUpdateTime(System.currentTimeMillis());

                    activeMissions.remove(tempMissionKey); // Remove the original client mission entry using its negative key
                    // The drone's entry in activeMissions (keyed by its droneId) is now updated.
                    System.out.println("SharedData: Assigned client mission (original key: " + tempMissionKey + ") to drone " + drone.getDroneId() + ". Missions map size: " + activeMissions.size());
                    return drone.getDroneId(); // Return the assigned drone's ID
                }
            }
            System.out.println("SharedData: No available idle drone found at warehouse " + closestWarehouse.getName() + " for mission (key: " + tempMissionKey + ")");
            return 0; // Failure - no drone found
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

    public static Warehouse findWarehouseForDrone(int droneId) {
        synchronized (lock) {
            for (Warehouse warehouse : warehouses) {
                if (warehouse.getResidentDroneIdsView().contains(droneId)) {
                    return warehouse;
                }
            }
        }
        return null;
    }

    public static void recallDrone(int droneId, boolean forceReturn) {
        synchronized (lock) {
            ActiveMission mission = getActiveMissionByDroneId(droneId);
            if (mission == null) {
                System.err.println("SharedData: recallDrone - No mission found for drone ID: " + droneId);
                return;
            }

            if (mission.isReturning()) {
                System.out.println("SharedData: Drone " + droneId + " is already returning. No action needed.");
                return;
            }

            if (!mission.isAssigned() && !forceReturn) {
                System.out.println("SharedData: recallDrone - Drone " + droneId + " is already idle. No action needed.");
                return;
            }
            
            GeoPath originalPath = mission.getOriginalPath();
            if (originalPath == null || originalPath.getPoints() == null || originalPath.getPoints().isEmpty()) {
                 System.err.println("SharedData: recallDrone - No original path found for drone " + droneId + ". Cannot reverse path.");
                 return;
            }

            System.out.println("SharedData: Recalling drone " + droneId + " by reversing its path.");

            List<GeoPosition> originalPathPoints = originalPath.getPoints();
            int lastWaypointIndex = Math.max(0, mission.getCurrentSegmentTargetIndex() - 1);

            List<GeoPosition> returnPathPoints = new ArrayList<>();
            returnPathPoints.add(mission.getCurrentDronePosition());

            List<GeoPosition> visitedWaypoints = new ArrayList<>(originalPathPoints.subList(0, lastWaypointIndex + 1));
            Collections.reverse(visitedWaypoints);
            returnPathPoints.addAll(visitedWaypoints);

            Path returnPath = new Path();
            List<Point> jtsPoints = new ArrayList<>();
            for (GeoPosition geoPos : returnPathPoints) {
                jtsPoints.add(new GeometryFactory().createPoint(new Coordinate(geoPos.getLongitude(), geoPos.getLatitude())));
            }
            returnPath.setPoints(jtsPoints);

            mission.setPath(new GeoPath(returnPath));
            mission.setCurrentSegmentTargetIndex(1); 
            mission.setAssigned(true); 
            mission.setReturning(true);
            mission.setLastUpdateTime(System.currentTimeMillis());
            System.out.println("SharedData: Assigned new return path to drone " + droneId);
        }
    }

    public static void setManualWaypoint(int droneId, GeoPosition newDestination) {
        synchronized (lock) {
            ActiveMission mission = getActiveMissionByDroneId(droneId);
            if (mission == null || !mission.isAssigned()) {
                System.err.println("SharedData: setManualWaypoint - Drone " + droneId + " is not on an active mission.");
                return;
            }
            
            System.out.println("SharedData: Manual override for drone " + droneId + ". New destination: " + newDestination);

            GeometryFactory factory = new GeometryFactory();
            Pathfinder pathfinder = Pathfinder.getInstance();

            Point dronePoint = factory.createPoint(new Coordinate(mission.getCurrentDronePosition().getLongitude(), mission.getCurrentDronePosition().getLatitude()));
            Point newDestPoint = factory.createPoint(new Coordinate(newDestination.getLongitude(), newDestination.getLatitude()));

            Path newPath = pathfinder.createPath(dronePoint, newDestPoint);

            if (newPath != null && newPath.getPoints() != null && !newPath.getPoints().isEmpty()) {
                GeoPath newGeoPath = new GeoPath(newPath);
                mission.setPath(newGeoPath);
                // Do not overwrite the original path, a manual waypoint is a detour
                // mission.setOriginalPath(newGeoPath); 
                mission.setCurrentSegmentTargetIndex(1);
                mission.setAssigned(true);
                mission.setReturning(false); // A manual destination is a new delivery, not a return
                mission.setLastUpdateTime(System.currentTimeMillis());
                System.out.println("SharedData: Assigned new manual path to drone " + droneId);
            } else {
                System.err.println("SharedData: setManualWaypoint - Failed to create a manual path for drone " + droneId);
            }
        }
    }
}
