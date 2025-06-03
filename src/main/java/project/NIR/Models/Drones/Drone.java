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
public abstract class Drone implements Runnable {
    private int id;
    private double currentLatitude;
    private double currentLongitude;
    private double altitude;
    private List<GeoPosition> assignedPathPoints;
    private int currentSegmentTargetIndex = 0;
    private GeoPosition currentPosition;

    private static final double SPEED_METERS_PER_SECOND = 20.0;
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
            System.out.println("Drone " + id + " received path. Static position set to: " + this.currentPosition);
        } else {
            this.currentSegmentTargetIndex = 0;
            System.out.println("Drone " + id + " received null or empty path. No static position set.");
        }
    }

    private void moveAlongPath() {
        if (this.currentPosition != null) {
            this.currentLatitude = this.currentPosition.getLatitude();
            this.currentLongitude = this.currentPosition.getLongitude();
        }
    }

    @SneakyThrows
    public void connectToServer(String host, int port) {
        try {
            setDroneSocket(new Socket(host, port));
            setOut(new ObjectOutputStream(getDroneSocket().getOutputStream()));
            getOut().flush();

            setIn(new ObjectInputStream(getDroneSocket().getInputStream()));
            
            System.out.println("Drone " + id + ": Sending initial empty/default data package.");
            sendMessage();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (currentPosition != null) {
                        currentLatitude = currentPosition.getLatitude();
                        currentLongitude = currentPosition.getLongitude();
                    }
                    
                    sendMessage();

                    try {
                        if (getIn() == null) {
                            System.err.println("Drone " + getId() + ": ObjectInputStream is null, cannot read from server.");
                            timer.cancel();
                            closeSocket();
                            return;
                        }
                        ServerData serverData = (ServerData) getIn().readObject();
                        System.out.println("Drone " + getId() + " received from server: " + serverData.toString());
                        if (serverData.getAssignedPath() != null && !serverData.getAssignedPath().isEmpty()) {
                            System.out.println("Drone " + getId() + " received new path assignment from server.");
                            setAssignedPath(serverData.getAssignedPath());
                            sendMessage();
                        }

                    } catch (IOException e) {
                        System.out.println("Drone " + getId() + ": IO Error reading from server or server disconnected: " + e.getMessage());
                        timer.cancel();
                        closeSocket();
                    } catch (ClassNotFoundException e) {
                        System.out.println("Drone " + getId() + ": ClassNotFoundException reading from server: " + e.getMessage());
                        timer.cancel();
                        closeSocket();
                    }
                }
            }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS);

        } catch (IOException e) {
            System.out.println("Drone " + id + ": Could not connect to server " + host + ":" + port + ". " + e.getMessage());
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (droneSocket != null && !droneSocket.isClosed()) droneSocket.close();
        } catch (IOException ex) {
            System.err.println("Drone " + id + ": Error closing socket resources: " + ex.getMessage());
        }
    }

    public DroneData createPackage(){
        return new DroneData(getId(), getCurrentLatitude(), getCurrentLongitude(), getAltitude(), 0, 0);
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
