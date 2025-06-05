package project.NIR.Models.Drones;

import lombok.*;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.Data.ServerData;
import project.NIR.Utils.GeoUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class Drone {
    private int id;
    private double currentLatitude;
    private double currentLongitude;
    private double altitude;
    private List<GeoPosition> assignedPathPoints;
    private int currentSegmentTargetIndex = 0;
    private GeoPosition currentPosition;

    private static final double SPEED_METERS_PER_SECOND = 25.0;
    private static final long UPDATE_INTERVAL_MS = 2000;

    public abstract void move(double newLatitude, double newLongitude, double newAltitude);

    private Socket droneSocket;
    private ObjectOutputStream out;
    private ObjectInput in;

    public void setAssignedPath(List<GeoPosition> pathPoints) {
        this.assignedPathPoints = pathPoints;
        if (pathPoints != null && !pathPoints.isEmpty()) {
            this.currentPosition = pathPoints.get(0);
            this.currentLatitude = this.currentPosition.getLatitude();
            this.currentLongitude = this.currentPosition.getLongitude();
            this.currentSegmentTargetIndex = 1;
            System.out.println("Drone " + id + " received path. Starting at: " + this.currentPosition + ". Next target index: " + this.currentSegmentTargetIndex + ". Path size: " + pathPoints.size() + (pathPoints.size() > 1 ? ", End: " + pathPoints.get(pathPoints.size()-1) : " (Single point path)"));
        } else {
            this.assignedPathPoints = null;
            this.currentSegmentTargetIndex = 0;
            System.out.println("Drone " + id + " received null or empty path. No target.");
        }
    }

    private void moveAlongPath() {
        if (assignedPathPoints == null || assignedPathPoints.isEmpty() || currentPosition == null ||
            currentSegmentTargetIndex <= 0 || currentSegmentTargetIndex >= assignedPathPoints.size()) {
            if (assignedPathPoints != null && !assignedPathPoints.isEmpty() && currentSegmentTargetIndex >= assignedPathPoints.size()) {
                System.out.println("Drone " + id + ": Path fully completed (top guard). Current position: " + currentPosition + ". Target index was: " + currentSegmentTargetIndex + ". Setting path to null, index to 0.");
                this.assignedPathPoints = null;
                this.currentSegmentTargetIndex = 0;
            }
            if (this.currentPosition != null) {
                 this.currentLatitude = this.currentPosition.getLatitude();
                 this.currentLongitude = this.currentPosition.getLongitude();
            }
            return;
        }

        GeoPosition targetWaypoint = assignedPathPoints.get(currentSegmentTargetIndex);
        System.out.println("Drone " + id + " moveAlongPath: CurrentPos=" + currentPosition + ", TargetSegIdx=" + currentSegmentTargetIndex + ", TargetWaypoint=" + targetWaypoint + " (Path size: " + assignedPathPoints.size() + ")");

        double distanceToTargetWaypoint = GeoUtils.calculateDistance(currentPosition, targetWaypoint);
        double distanceToTravelThisTick = SPEED_METERS_PER_SECOND * (UPDATE_INTERVAL_MS / 1000.0);

        if (distanceToTravelThisTick >= distanceToTargetWaypoint) {
            currentPosition = targetWaypoint;
            currentSegmentTargetIndex++;

            if (currentSegmentTargetIndex >= assignedPathPoints.size()) {
                System.out.println("Drone " + id + ": Reached final destination: " + currentPosition + ". Path marked complete.");
            } else {
                System.out.println("Drone " + id + ": Reached waypoint " + (currentSegmentTargetIndex - 1) + " at " + currentPosition + ". New target index: " + currentSegmentTargetIndex);
            }
        } else {
            double fraction = distanceToTravelThisTick / distanceToTargetWaypoint;
            if (distanceToTargetWaypoint > 0) {
                 currentPosition = GeoUtils.calculateIntermediatePoint(currentPosition, targetWaypoint, fraction);
            }
        }

        if (this.currentPosition != null) {
            this.currentLatitude = this.currentPosition.getLatitude();
            this.currentLongitude = this.currentPosition.getLongitude();
        }
    }

    @SneakyThrows
    public void connectToServer(String host, int port) {
        System.out.println("Drone " + id + ": Attempting socket connection to " + host + ":" + port);
        try {
            setDroneSocket(new Socket(host, port));
            System.out.println("Drone " + id + ": Socket connection established. Setting up streams.");
            setOut(new ObjectOutputStream(getDroneSocket().getOutputStream()));
            getOut().flush(); // Crucial: flush after creating ObjectOutputStream
            setIn(new ObjectInputStream(getDroneSocket().getInputStream()));
            System.out.println("Drone " + id + ": Streams established.");
            
            System.out.println("Drone " + id + ": Connected. Sending initial state.");
            sendMessage(); // Send initial state (pos, segmentIndex=0)
            System.out.println("Drone " + id + ": Initial state sent. Starting reader and movement threads.");

            // Thread for continuously reading from server
            Thread serverReadThread = new Thread(() -> {
                try {
                    while (getDroneSocket() != null && !getDroneSocket().isClosed() && getIn() != null) {
                        ServerData serverData = (ServerData) getIn().readObject();
                        System.out.println("Drone " + getId() + " received from server: " + serverData.toString());
                        if (serverData.getAssignedPath() != null && !serverData.getAssignedPath().isEmpty()) {
                            System.out.println("Drone " + getId() + " received new path assignment from server.");
                            setAssignedPath(serverData.getAssignedPath());
                            // Optionally, send an immediate acknowledgment or updated state if needed.
                            // For now, the regular timed sendMessage will cover it.
                        }
                    }
                } catch (IOException e) {
                    if (!getDroneSocket().isClosed()) {
                        System.out.println("Drone " + getId() + ": IO Error reading from server: " + e.getMessage());
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("Drone " + getId() + ": ClassNotFoundException reading from server: " + e.getMessage());
                } finally {
                    System.out.println("Drone " + getId() + ": Server read thread exiting.");
                    // closeSocket(); // Let the main timer thread or connectToServer catch block handle final closure
                }
            });
            serverReadThread.setDaemon(true);
            serverReadThread.setName("Drone-" + id + "-ReadThread");
            serverReadThread.start();

            // Timer for movement and sending updates to server
            Timer movementTimer = new Timer(true); // Use daemon thread for timer
            movementTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // System.out.println("Drone " + getId() + ": TimerTask tick."); // Optional: very verbose tick log
                        if (getDroneSocket() == null || getDroneSocket().isClosed()) {
                            System.out.println("Drone " + getId() + ": Socket closed or null in TimerTask. Cancelling timer.");
                            movementTimer.cancel();
                            serverReadThread.interrupt(); // Attempt to interrupt read thread if socket is closed
                            return;
                        }

                        moveAlongPath(); // Move first

                        if (currentPosition != null) {
                            currentLatitude = currentPosition.getLatitude();
                            currentLongitude = currentPosition.getLongitude();
                        }
                        
                        System.out.println("Drone " + getId() + ": Pos=[" + currentLatitude + "," + currentLongitude + "], Alt=" + altitude + ", TargetSegIdx=" + currentSegmentTargetIndex + ", PathExists=" + (assignedPathPoints != null && !assignedPathPoints.isEmpty()));

                        sendMessage(); // Then send updated state
                    } catch (Exception e) {
                        System.err.println("Drone " + getId() + ": Error in movement/sending TimerTask: " + e.getMessage());
                        // Consider cancelling timer and closing socket here too
                        movementTimer.cancel();
                        serverReadThread.interrupt();
                        closeSocket();
                    }
                }
            }, 0, UPDATE_INTERVAL_MS); // Start immediately (0 delay for first execution)

        } catch (IOException e) {
            System.out.println("Drone " + id + ": Could not connect to server " + host + ":" + port + ". " + e.getMessage());
            closeSocket(); // Ensure socket is closed on connection failure
        } 
        // No longer closing socket here by default, let threads manage or a main loop if this drone was a runnable part of a larger system
    }

    private void closeSocket() {
        try {
            // Interrupt read thread before closing streams
            // This is a bit tricky as the read thread might be blocked on readObject()
            // if (serverReadThread != null && serverReadThread.isAlive()) { // serverReadThread not a field
            //     serverReadThread.interrupt();
            // }
            if (droneSocket != null && !droneSocket.isClosed()) {
                System.out.println("Drone " + id + ": Closing socket.");
                droneSocket.close(); // Closing the socket should interrupt the blocking readObject in the read thread.
            }
            if (out != null) out.close();
            if (in != null) in.close(); // Close streams after socket to avoid issues
        } catch (IOException ex) {
            System.err.println("Drone " + id + ": Error closing socket resources: " + ex.getMessage());
        }
    }

    public DroneData createPackage(){
        return new DroneData(getId(), getCurrentLatitude(), getCurrentLongitude(), getAltitude(), 0, 0, getCurrentSegmentTargetIndex());
    }

    private void sendMessage(){
        try {
            if (getOut() != null && getDroneSocket() != null && !getDroneSocket().isClosed()) {
                DroneData data = createPackage();
                getOut().writeObject(data);
                getOut().flush();
            } else {
            }
        } catch (IOException e) {
            System.err.println("Drone " + id + ": Failed to send message: " + e.getMessage());
        }
    }

}
