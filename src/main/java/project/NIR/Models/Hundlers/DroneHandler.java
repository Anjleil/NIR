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
    // private boolean initialResponseSentToDrone = false; // Can be removed or simplified

    public DroneHandler(Socket socket, ObjectInputStream packageStream, ObjectOutputStream packageStreamOut, DroneData initialDroneData) throws IOException {
        this.droneId = initialDroneData.getId(); 
        this.clientSocket = socket;
        this.packageStream = packageStream;
        this.packageStreamOut = packageStreamOut;
        System.out.println("DroneHandler initialized for connected drone ID: " + this.droneId + ". Remote: " + clientSocket.getRemoteSocketAddress());
        
        // Update SharedData with the connected drone's initial position, 
        // especially if this drone ID was pre-initialized as an idle drone.
        ActiveMission mission = SharedData.getActiveMissionByDroneId(this.droneId);
        if (mission != null) {
            // If drone was idle, its current pos is its warehouse pos. If it had a mission assigned while "offline", 
            // its current pos would be start of that path. Send its current known position.
            SharedData.updateDronePosition(this.droneId, 
                                           new GeoPosition(initialDroneData.getLatitude(), initialDroneData.getLongitude()), 
                                           mission.getCurrentSegmentTargetIndex()); 
            System.out.println("DroneHandler: Updated initial position in SharedData for drone " + this.droneId + " from its own report.");
        } else {
            // This case means a drone connected whose ID was NOT pre-initialized in SharedData.
            // This could be an error, or a policy to dynamically add new, unknown drones.
            // For now, let's assume drone IDs are known from initialization.
            System.err.println("DroneHandler: WARNING - Drone ID " + this.droneId + " connected but not found in SharedData. This drone might not be managed correctly.");
            // Optionally, create a new ActiveMission for it here if policy allows dynamic addition:
            // ActiveMission newDroneEntry = new ActiveMission(this.droneId, new GeoPosition(initialDroneData.getLatitude(), initialDroneData.getLongitude()));
            // SharedData.addOrUpdateDrone(newDroneEntry); // Would need a new method in SharedData
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
        } catch (IOException e) {
            System.err.println("DroneHandler for drone " + this.droneId + ": Error sending path: " + e.getMessage());
        }
    }

    private void sendDefaultCommand() {
        try {
            // Command 0 could be standby/no_new_instructions or a general ACK
            ServerData commandData = new ServerData(this.droneId, 0, null); 
            packageStreamOut.writeObject(commandData);
            packageStreamOut.flush();
        } catch (IOException e) {
            System.err.println("DroneHandler for drone " + this.droneId + ": Error sending default command: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // Immediately check if there's a mission assigned to this drone and send path if so.
        checkAndSendMissionPath();

        try {
            while (clientSocket.isConnected() && !clientSocket.isClosed()) {
                DroneData data = (DroneData) packageStream.readObject(); // Subsequent reads are drone updates
                
                ActiveMission mission = SharedData.getActiveMissionByDroneId(data.getId());
                GeoPosition currentReportedPosition = new GeoPosition(data.getLatitude(), data.getLongitude());
                int nextSegmentIndex = (mission != null && mission.isAssigned()) ? mission.getCurrentSegmentTargetIndex() : 0;
                
                SharedData.updateDronePosition(data.getId(), currentReportedPosition, nextSegmentIndex);

                // Periodically, the drone might need new instructions or re-assignment.
                // For now, after updates, just send a default command. 
                // A more advanced system might re-evaluate missions or allow new commands to be queued.
                sendDefaultCommand(); 
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
                 // e.printStackTrace(); // For debugging unexpected errors
            }
        } finally {
            System.out.println("DroneHandler for drone " + droneId + " closing connection and resources handled by CommandCenter.");
            // Streams (packageStream, packageStreamOut) and clientSocket are managed by CommandCenter's main loop,
            // so DroneHandler should not close them here.
        }
    }
}