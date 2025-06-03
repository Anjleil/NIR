package project.NIR;

import org.jxmapviewer.viewer.GeoPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultiClientSimulator {

    private static final List<GeoPosition> deliveryDestinations = new ArrayList<>();

    static {
        deliveryDestinations.add(new GeoPosition(55.777851, 37.593145));
        deliveryDestinations.add(new GeoPosition(55.776119, 37.643531));
        deliveryDestinations.add(new GeoPosition(55.750281, 37.667896));
        deliveryDestinations.add(new GeoPosition(55.721883, 37.630421));
        deliveryDestinations.add(new GeoPosition(55.726406, 37.572113));
    }

    public static void main(String[] args) {
        System.out.println("MultiClientSimulator: Starting to simulate " + deliveryDestinations.size() + " client connections...");

        List<Thread> clientThreads = new ArrayList<>();

        for (int i = 0; i < deliveryDestinations.size(); i++) {
            GeoPosition destination = deliveryDestinations.get(i);
            final int clientNum = i + 1;

            Thread clientThread = new Thread(() -> {
                System.out.println("Client " + clientNum + " (Thread " + Thread.currentThread().getId() + "): Attempting to connect for destination " + destination);
                Client client = new Client(destination);
                try {
                    client.connectToServer("localhost", 12345);
                    System.out.println("Client " + clientNum + " (Thread " + Thread.currentThread().getId() + "): Connection process finished for destination " + destination);
                } catch (Exception e) { // Catching generic Exception for robustness in thread
                    System.err.println("Client " + clientNum + " (Thread " + Thread.currentThread().getId() + "): Error connecting for destination " + destination + " - " + e.getMessage());
                    // e.printStackTrace(); // Uncomment for detailed error diagnosis
                }
            });
            clientThreads.add(clientThread);
            clientThread.setName("ClientSimThread-" + clientNum);
            clientThread.start();

            // Optional: Add a small delay between starting each client thread to avoid overwhelming the server instantly
            // or to make observing logs easier, though for true concurrency test, no delay is fine.
            try {
                Thread.sleep(200); // e.g., 200ms delay
            } catch (InterruptedException e) {
                System.err.println("MultiClientSimulator: Sleep interrupted: " + e.getMessage());
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }

        System.out.println("MultiClientSimulator: All " + deliveryDestinations.size() + " client simulation threads launched.");

        // Optionally, wait for all client threads to complete if you need to ensure they all finish before main exits.
        // For a fire-and-forget simulation, this part can be omitted.
        /*
        for (Thread t : clientThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.err.println("MultiClientSimulator: Main thread interrupted while waiting for client thread " + t.getName() + ": " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("MultiClientSimulator: All client simulation threads have completed.");
        */
    }
} 