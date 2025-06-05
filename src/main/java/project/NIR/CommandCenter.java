package project.NIR;

import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.Data.Data;
import project.NIR.Models.Drones.DroneData;
import project.NIR.Models.Data.SharedData;
import project.NIR.Models.Hundlers.ClientHandler;
import project.NIR.Models.Hundlers.DroneHandler;
import project.NIR.Models.Data.ClientData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class CommandCenter {
    public static final int PORT = 12345;
    private static ServerSocket serverSocketInstance = null;

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("CommandCenter: Shutdown hook triggered. Closing server socket...");
            if (serverSocketInstance != null && !serverSocketInstance.isClosed()) {
                try {
                    serverSocketInstance.close();
                    System.out.println("CommandCenter: Server socket closed.");
                } catch (IOException e) {
                    System.err.println("CommandCenter: Error closing server socket in shutdown hook: " + e.getMessage());
                }
            }
        }));

        SharedData.clearAllMissionsAndWarehouses();

        Map<String, GeoPosition> warehouseLocations = new HashMap<>();
        warehouseLocations.put("Warehouse 1", new GeoPosition(55.730899, 37.721991));
        warehouseLocations.put("Warehouse 2", new GeoPosition(55.773143, 37.530097));
        int dronesPerWarehouse = 5;
        SharedData.initializeWarehousesAndDrones(warehouseLocations, dronesPerWarehouse);

        App.run();
        startServer();
    }

    private static void startServer() {
        try {
            serverSocketInstance = new ServerSocket(PORT);
            System.out.println("CommandCenter: Server started on port " + PORT);
        } catch (IOException e) {
            System.err.println("CommandCenter: Failed to start server on port " + PORT + ": " + e.getMessage());
            return;
        }

        System.out.println("CommandCenter: Waiting for connections...");
        while (true) {
            Socket clientSocket = null;
            ObjectInputStream packageStream = null;
            ObjectOutputStream outStreamToClient = null;
            try {
                if (serverSocketInstance == null || serverSocketInstance.isClosed()) {
                    System.out.println("CommandCenter: Server socket is closed, stopping accept loop.");
                    break;
                }
                clientSocket = serverSocketInstance.accept();
                System.out.println("\nCommandCenter: New connection from " + clientSocket.getRemoteSocketAddress());

                packageStream = new ObjectInputStream(clientSocket.getInputStream());
                outStreamToClient = new ObjectOutputStream(clientSocket.getOutputStream());
                outStreamToClient.flush();

                Data receivedData = (Data) packageStream.readObject();

                if ("DRONE".equals(receivedData.getType())) {
                    DroneData initialDroneData = (DroneData) receivedData;
                    System.out.println("CommandCenter: Connection type DRONE. ID: " + initialDroneData.getId() + ". Initial Coords: ["+initialDroneData.getLatitude()+","+initialDroneData.getLongitude()+"]");
                    Thread thread = new Thread(new DroneHandler(clientSocket, packageStream, outStreamToClient, initialDroneData));
                    thread.start();
                } else if ("CLIENT".equals(receivedData.getType())) {
                    ClientData initialClientData = (ClientData) receivedData;
                    System.out.println("CommandCenter: Connection type CLIENT. Destination: [" + initialClientData.getDelivery().getLatitude() + "," + initialClientData.getDelivery().getLongitude() + "]");
                    Thread thread = new Thread(new ClientHandler(clientSocket, packageStream, outStreamToClient, initialClientData));
                    thread.start();
                } else {
                    System.out.println("CommandCenter: Unknown client type: " + receivedData.getType() + ". Closing connection.");
                    if (outStreamToClient != null) outStreamToClient.close();
                    if (packageStream != null) packageStream.close();
                    if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                }
            } catch (IOException | ClassNotFoundException e) {
                if (serverSocketInstance != null && serverSocketInstance.isClosed()) {
                    System.out.println("CommandCenter: Accept loop interrupted due to server socket closure.");
                    break;
                }
                System.err.println("CommandCenter: Error handling connection for " + (clientSocket != null ? clientSocket.getRemoteSocketAddress() : "UNKNOWN_CLIENT") + ": " + e.getMessage());
                try {
                    if (outStreamToClient != null) outStreamToClient.close();
                    if (packageStream != null) packageStream.close();
                    if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                } catch (IOException ex) {
                    System.err.println("CommandCenter: Error closing resources after exception: " + ex.getMessage());
                }
            }
        }
    }
}


