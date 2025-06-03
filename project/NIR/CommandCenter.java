package project.NIR;

import project.NIR.Models.Data.Data;
import project.NIR.Models.Drones.DroneData; // Import DroneData
import project.NIR.Models.Hundlers.ClientHandler;
import project.NIR.Models.Hundlers.DroneHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream; // Import ObjectOutputStream
import java.net.ServerSocket;
import java.net.Socket;

public class CommandCenter {
    private static final int PORT = 12345;

    public static void main(String[] args) throws IOException {
        // SharedData.clearAllMissions(); // Optional: Clear previous state on start
        App.run();
        startServer();
    }

    private static void startServer() {
        ServerSocket server = null;
        try {
            server = new ServerSocket(PORT);
            System.out.println("CommandCenter: Server started on port " + PORT);
        } catch (IOException e) {
            System.err.println("CommandCenter: Failed to start server on port " + PORT + ": " + e.getMessage());
            return; // Exit if server cannot start
        }

        System.out.println("CommandCenter: Waiting for connections...");
        while (true) {
            Socket clientSocket = null; // Initialize to null
            ObjectInputStream packageStream = null; // Initialize to null
            ObjectOutputStream outStreamToClient = null; // Initialize to null
            try {
                assert server != null;
                clientSocket = server.accept();
                System.out.println("\nCommandCenter: New connection from " + clientSocket.getRemoteSocketAddress());

                // Create OIS first, then OOS for the client socket
                packageStream = new ObjectInputStream(clientSocket.getInputStream());
                outStreamToClient = new ObjectOutputStream(clientSocket.getOutputStream());
                outStreamToClient.flush(); // Important: Flush OOS header

                Data receivedData = (Data) packageStream.readObject(); // Read the first object to determine type

                if ("DRONE".equals(receivedData.getType())) {
                    DroneData initialDroneData = (DroneData) receivedData;
                    System.out.println("CommandCenter: Connection type DRONE. ID: " + initialDroneData.getId() + ". Initial Coords: ["+initialDroneData.getLatitude()+","+initialDroneData.getLongitude()+"]");
                    // Pass the OIS, OOS, and the already-read initialDroneData
                    Thread thread = new Thread(new DroneHandler(clientSocket, packageStream, outStreamToClient, initialDroneData));
                    thread.start();
                } else if ("CLIENT".equals(receivedData.getType())) {
                    System.out.println("CommandCenter: Connection type CLIENT.");
                    // ClientHandler will use its own OOS. Pass the main OIS and the specific OOS.
                    // The ClientHandler constructor expects only OIS, it creates its own OOS from clientSocket.
                    // We pass packageStream (OIS) and outStreamToClient (OOS) for consistency, but ClientHandler only uses packageStream and re-creates OOS.
                    // This is slightly inconsistent. ClientHandler should ideally accept the OOS too.
                    // For now, matching ClientHandler's current constructor signature which takes (socket, packageStream)
                    // For this to work, ClientHandler MUST create its own ObjectOutputStream. Current one creates a new one.
                    Thread thread = new Thread(new ClientHandler(clientSocket, packageStream)); // ClientHandler creates its own OOS
                    thread.start();
                } else {
                    System.out.println("CommandCenter: Unknown client type: " + receivedData.getType() + ". Closing connection.");
                    if (outStreamToClient != null) outStreamToClient.close();
                    if (packageStream != null) packageStream.close();
                    if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("CommandCenter: Error handling connection: " + e.getMessage());
                // e.printStackTrace(); // For debugging
                // Ensure resources are closed if an error occurs after they are opened
                try {
                    if (outStreamToClient != null) outStreamToClient.close();
                    if (packageStream != null) packageStream.close();
                    if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                } catch (IOException ex) {
                    System.err.println("CommandCenter: Error closing resources after exception: " + ex.getMessage());
                }
            }
            // Loop continues for next connection
        }
        // ServerSocket should be closed in a real application, e.g. via shutdown hook
    }
} 