package project.NIR.Utils;

import project.NIR.Models.Drones.AirDrone;
// import org.jxmapviewer.viewer.GeoPosition; // Not strictly needed now
// import project.NIR.Models.Data.SharedData; // Not strictly needed now

import java.io.IOException;

public class TestConnection {
    public static void main(String[] args) throws IOException {
        System.out.println("TestConnection: Starting drone simulator...");

        // Launching drones with IDs 1 through 10.
        // These IDs should correspond to drones initialized in CommandCenter/SharedData.
        int numDronesToLaunch = 10; // Launching all 10 drones
        System.out.println("TestConnection: Attempting to launch " + numDronesToLaunch + " drones (IDs 1-10).");

        for(int i = 1; i <= numDronesToLaunch; i++){
            CreateDrone(i);
            try {
                Thread.sleep(200); // Stagger drone connections slightly to make server logs easier to follow
            } catch (InterruptedException e) {
                System.err.println("TestConnection: Sleep interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("TestConnection: " + numDronesToLaunch + " drone launch attempts initiated. Main thread will now wait indefinitely.");
        System.out.println("TestConnection: Check the console output of THIS TestConnection process for drone-specific logs (e.g., 'Drone X: Pos=...').");
        System.out.println("TestConnection: Check the CommandCenter console for server-side logs, including drone connection messages.");
        System.out.println("TestConnection: Press Ctrl+C in this console to terminate all drone threads and this simulator.");

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("TestConnection: Main simulator thread interrupted. Exiting.");
            Thread.currentThread().interrupt();
        }
    }

    private static void CreateDrone(int id) {
        Thread droneThread = new Thread(() -> {
            System.out.println("TestConnection (DroneRuntimeThread-" + id + "): Preparing to create and connect drone ID: " + id);
            try {
                AirDrone drone = new AirDrone(id); 
                drone.connectToServer("localhost", 12345); 
                 System.out.println("TestConnection (DroneRuntimeThread-" + id + "): drone.connectToServer call completed for drone ID: " + id);
            } catch (Exception e) { 
                System.err.println("TestConnection (DroneRuntimeThread-" + id + "): ERROR encountered for drone ID " + id + ". See stack trace below.");
                e.printStackTrace(); // ENABLED for detailed error diagnosis
            }
        });
        droneThread.setName("DroneRuntimeThread-" + id);
        droneThread.setDaemon(false); 
        droneThread.start();
    }
}
