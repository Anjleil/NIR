package project.NIR.Models.Hundlers;

import lombok.Getter;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.Data.ActiveMission;
import project.NIR.Models.Drones.DroneData;
import project.NIR.Models.Data.ServerData;
import project.NIR.Models.Data.SharedData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

@Getter
public class DroneHandler implements Runnable {
    private final int droneId;
    private final Socket clientSocket;
    private final ObjectInputStream packageStream; // For subsequent messages from drone
    private final ObjectOutputStream packageStreamOut; // For sending messages to drone
    private List<GeoPosition> lastSentPathToDrone = null; // Stores the last path sent to this drone

    public DroneHandler(Socket socket, ObjectInputStream packageStream, ObjectOutputStream packageStreamOut, DroneData initialDroneData) throws IOException {
        this.droneId = initialDroneData.getId(); 
        this.clientSocket = socket;
        this.packageStream = packageStream;
        this.packageStreamOut = packageStreamOut;
        System.out.println("DroneHandler (" + Thread.currentThread().getName() + ") initialized for connected drone ID: " + this.droneId + ". Remote: " + clientSocket.getRemoteSocketAddress());
        
        ActiveMission missionInSharedData = SharedData.getActiveMissionByDroneId(this.droneId);
        if (missionInSharedData != null) {
            GeoPosition positionToSetInSharedData;
            int segmentIndexToSetInSharedData;

            if (missionInSharedData.isAssigned() && missionInSharedData.getPath() != null && missionInSharedData.getPathPoints() != null && !missionInSharedData.getPathPoints().isEmpty()) {
                // Case: Drone is connecting, and SharedData ALREADY has an assigned mission for it.
                // This happens when ClientHandler assigns a mission and then launches the drone simulation.
                // We must use the mission's starting position and segment index from SharedData,
                // not the drone's initial (likely 0,0, index 0) report.
                positionToSetInSharedData = missionInSharedData.getCurrentDronePosition(); // Should be the start of the path.
                segmentIndexToSetInSharedData = missionInSharedData.getCurrentSegmentTargetIndex(); // Should be 1 for an assigned path.
                System.out.println("DroneHandler: Drone " + this.droneId + " connecting for an existing assigned mission. Using mission's start pos from SharedData: " + positionToSetInSharedData + " and segIdx: " + segmentIndexToSetInSharedData + ". Drone's initial reported: lat=" + initialDroneData.getLatitude() + ",lon=" + initialDroneData.getLongitude()+",idx="+initialDroneData.getCurrentSegmentTargetIndex());
            } else {
                // Case: Drone is connecting and is idle in SharedData (e.g., pre-initialized and connecting)
                // or missionInSharedData exists but is not 'assigned' or has no path (e.g. an idle drone).
                // In this scenario, we trust the drone's reported initial state.
                positionToSetInSharedData = new GeoPosition(initialDroneData.getLatitude(), initialDroneData.getLongitude());
                segmentIndexToSetInSharedData = initialDroneData.getCurrentSegmentTargetIndex();
                System.out.println("DroneHandler: Drone " + this.droneId + " connecting as idle/new or for unassigned mission. Using drone's initial reported pos: " + positionToSetInSharedData + " and segIdx: " + segmentIndexToSetInSharedData);
            }
            SharedData.updateDronePosition(this.droneId, positionToSetInSharedData, segmentIndexToSetInSharedData);
            System.out.println("DroneHandler: Updated/Verified state in SharedData for drone " + this.droneId + ". Final Pos used: " + positionToSetInSharedData + ", Final SegIdx used: " + segmentIndexToSetInSharedData);
        } else {
            System.err.println("DroneHandler: CRITICAL - Drone ID " + this.droneId + " connected but NO ActiveMission object found in SharedData. This drone cannot be managed. Initial Data: lat=" + initialDroneData.getLatitude()+",lon="+initialDroneData.getLongitude()+",segIdx="+initialDroneData.getCurrentSegmentTargetIndex());
            // If this happens, the drone is connecting without being pre-initialized by CommandCenter or assigned by ClientHandler.
            // To prevent null issues later and at least register it, we can create a default idle mission entry:
            // GeoPosition reportedPos = new GeoPosition(initialDroneData.getLatitude(), initialDroneData.getLongitude());
            // ActiveMission newAm = new ActiveMission(this.droneId, reportedPos); // constructor for idle drone
            // SharedData.forceAddOrUpdateActiveMission(this.droneId, newAm); // Would need this new method in SharedData.
            // For now, this is an unhandled state that will likely lead to this drone not participating in missions.
        }
    }

    private void checkAndSendMissionPath() {
        ActiveMission mission = SharedData.getActiveMissionByDroneId(this.droneId);
        if (mission != null && mission.isAssigned() && mission.getPathPoints() != null && !mission.getPathPoints().isEmpty()) {
            System.out.println("DroneHandler for drone " + this.droneId + ": Found assigned mission. Sending path.");
            sendPathToDrone(mission.getPathPoints());
        } else {
            System.out.println("DroneHandler for drone " + this.droneId + ": No mission assigned or path is empty. Sending default command.");
            sendDefaultCommand(); 
        }
    }

    private void sendPathToDrone(List<GeoPosition> path) {
        try {
            ServerData pathData = new ServerData(this.droneId, 1, path); // Command 1: Path assignment
            System.out.println("DroneHandler for drone " + this.droneId + ": Sending path data: " + pathData);
            packageStreamOut.writeObject(pathData);
            packageStreamOut.flush();
            this.lastSentPathToDrone = path; // Update the last sent path
            System.out.println("DroneHandler for drone " + this.droneId + ": Sent path data. Last sent path updated.");
        } catch (IOException e) {
            System.err.println("DroneHandler for drone " + this.droneId + ": Error sending path: " + e.getMessage());
        }
    }

    private void sendDefaultCommand() {
        try {
            ServerData commandData = new ServerData(this.droneId, 0, null); 
            packageStreamOut.writeObject(commandData);
            packageStreamOut.flush();
            // Do not nullify lastSentPathToDrone here, it's managed based on SharedData state
        } catch (IOException e) {
            System.err.println("DroneHandler for drone " + this.droneId + ": Error sending default command: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        checkAndSendMissionPath(); // Initial path send if assigned on connection

        try {
            while (clientSocket.isConnected() && !clientSocket.isClosed()) {
                DroneData dataFromDrone = (DroneData) packageStream.readObject();
                
                GeoPosition currentReportedPosition = new GeoPosition(dataFromDrone.getLatitude(), dataFromDrone.getLongitude());
                int reportedSegmentIndexFromDrone = dataFromDrone.getCurrentSegmentTargetIndex();
                
                SharedData.updateDronePosition(dataFromDrone.getId(), currentReportedPosition, reportedSegmentIndexFromDrone);
                
                ActiveMission currentMissionStateInSharedData = SharedData.getActiveMissionByDroneId(dataFromDrone.getId());

                if (currentMissionStateInSharedData != null) {
                    if (reportedSegmentIndexFromDrone == 0 && currentMissionStateInSharedData.isAssigned()) {
                        // Drone signals path completion for the mission it was on.
                        System.out.println("DroneHandler: Drone " + dataFromDrone.getId() + " reported path completion (segment index 0). Marking as idle in SharedData.");
                        currentMissionStateInSharedData.setPath(null);
                        currentMissionStateInSharedData.setAssigned(false);
                        currentMissionStateInSharedData.setCurrentSegmentTargetIndex(0); // Reflect drone's state
                        this.lastSentPathToDrone = null; // Drone is now idle, no path is "active" for it
                        sendDefaultCommand(); // Acknowledge, drone is now idle.

                    } else if (currentMissionStateInSharedData.isAssigned() && 
                               currentMissionStateInSharedData.getPathPoints() != null && 
                               !currentMissionStateInSharedData.getPathPoints().isEmpty()) {
                        // Drone is (or should be) on a mission according to SharedData.
                        List<GeoPosition> pathInSharedData = currentMissionStateInSharedData.getPathPoints();
                        
                        // Check if the path in SharedData is different from the last one sent to this drone.
                        // This handles initial assignment (if checkAndSendMissionPath didn't catch it or if state changed rapidly)
                        // and re-assignment to a new path after becoming idle.
                        if (this.lastSentPathToDrone != pathInSharedData) { 
                            System.out.println("DroneHandler for drone " + this.droneId + ": Path in SharedData differs from last sent path (or first time for this handler). Sending/Re-sending path.");
                            sendPathToDrone(pathInSharedData); // This will update lastSentPathToDrone
                        } else {
                            // Path is the same as last sent, drone is presumably following it. Send default ack.
                            sendDefaultCommand();
                        }
                    } else { // Drone is idle in SharedData (not assigned, or path is null/empty)
                        if (this.lastSentPathToDrone != null) {
                            System.out.println("DroneHandler for drone " + this.droneId + ": Drone is now idle in SharedData, but was previously on a mission. Clearing last sent path state for handler.");
                            this.lastSentPathToDrone = null;
                        }
                        sendDefaultCommand();
                    }
                } else {
                    System.err.println("DroneHandler: No ActiveMission found for drone " + dataFromDrone.getId() + " after receiving update. Sending default command.");
                    sendDefaultCommand();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            String droneIdentifier = "drone " + droneId;
            if (clientSocket != null && clientSocket.getRemoteSocketAddress() != null) {
                droneIdentifier += " (" + clientSocket.getRemoteSocketAddress() + ")";
            }

            if (e instanceof java.io.EOFException) {
                 System.out.println("DroneHandler for " + droneIdentifier + ": Client disconnected (EOF).");
            } else if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("socket closed") || e.getMessage().toLowerCase().contains("connection reset"))) {
                 System.out.println("DroneHandler for " + droneIdentifier + ": Socket closed or connection reset.");
            } else {
                System.out.println("DroneHandler for " + droneIdentifier + ": Disconnected or error. " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } finally {
            System.out.println("DroneHandler for drone " + droneId + " closing connection and resources handled by CommandCenter.");
        }
    }
}