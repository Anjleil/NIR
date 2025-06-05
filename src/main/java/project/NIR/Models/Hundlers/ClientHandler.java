package project.NIR.Models.Hundlers;

import lombok.Getter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.Data.ClientData;
import project.NIR.Models.Data.ServerData;
import project.NIR.Models.Data.SharedData;
import project.NIR.Models.Routes.Path;
import project.NIR.Models.Warehouse;
import project.NIR.Utils.GeoUtils;
import project.NIR.Utils.Pathfinder;
import project.NIR.Models.Drones.AirDrone;
import project.NIR.CommandCenter; // For PORT access

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Getter
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectInputStream packageStream;
    private final ObjectOutputStream packageStreamOut;
    private final ClientData initialClientData;

    private static final ExecutorService pathfindingExecutor = Executors.newSingleThreadExecutor();

    public ClientHandler(Socket socket, ObjectInputStream packageStream, ObjectOutputStream packageStreamOut, ClientData initialClientData) throws IOException {
        this.clientSocket = socket;
        this.packageStream = packageStream;
        this.packageStreamOut = packageStreamOut;
        this.initialClientData = initialClientData;
        System.out.println("ClientHandler initialized for client at " + clientSocket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        GeometryFactory factory = new GeometryFactory();
        try {
            ClientData pathRequestData = this.initialClientData;
            GeoPosition clientDestinationGeoPos = pathRequestData.getDelivery();
            
            System.out.println("ClientHandler processing request for client destination: " + clientDestinationGeoPos);

            // 1. Find the closest warehouse to the client's destination
            List<Warehouse> warehouses = SharedData.getWarehouses();
            if (warehouses == null || warehouses.isEmpty()) {
                System.err.println("ClientHandler: No warehouses configured in SharedData.");
                sendResponse(false);
                return;
            }

            Warehouse closestWarehouse = null;
            double minDistance = Double.MAX_VALUE;
            for (Warehouse wh : warehouses) {
                double distance = GeoUtils.calculateDistance(wh.getLocation(), clientDestinationGeoPos);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestWarehouse = wh;
                }
            }

            if (closestWarehouse == null) { // Should not happen if warehouses list is not empty
                System.err.println("ClientHandler: Could not determine closest warehouse.");
                sendResponse(false);
                return;
            }
            System.out.println("ClientHandler: Closest warehouse is " + closestWarehouse.getName() + " at " + closestWarehouse.getLocation());

            // 2. Create JTS points for pathfinding
            Point warehousePoint = factory.createPoint(new Coordinate(closestWarehouse.getLocation().getLongitude(), closestWarehouse.getLocation().getLatitude()));
            Point clientDestinationPoint = factory.createPoint(new Coordinate(clientDestinationGeoPos.getLongitude(), clientDestinationGeoPos.getLatitude()));
            
            // 3. Submit pathfinding task to the queue and wait for the result
            System.out.println("ClientHandler for " + clientSocket.getRemoteSocketAddress() + " submitting pathfinding request to queue.");
            Future<Path> futurePath = pathfindingExecutor.submit(() ->
                Pathfinder.getInstance().createPath(warehousePoint, clientDestinationPoint)
            );
            Path path = futurePath.get(); // This blocks until the path is calculated by the single-threaded executor.
            
            if (path != null && path.getPoints() != null && !path.getPoints().isEmpty()) { // Check path.getPoints() for emptiness
                int tempMissionId = SharedData.addPendingClientMission(path);
                System.out.println("ClientHandler: Added new pending client mission with temp ID: " + tempMissionId + 
                                   " from warehouse " + closestWarehouse.getName() + 
                                   " to " + clientDestinationGeoPos + ". Path points: " + path.getPoints().size());
                
                int assignedDroneId = SharedData.assignMissionToAvailableDrone(tempMissionId);
                
                if (assignedDroneId > 0) { // Check if a drone was successfully assigned
                    System.out.println("ClientHandler: Successfully assigned pending mission " + tempMissionId + " to drone ID: " + assignedDroneId + ".");
                    
                    // Launch the simulated drone for this mission
                    AirDrone droneToLaunch = new AirDrone(assignedDroneId);
                    Thread droneLaunchThread = new Thread(() -> {
                        try {
                            System.out.println("ClientHandler (DroneLaunchThread-" + assignedDroneId + "): Launching simulated drone ID: " + assignedDroneId);
                            droneToLaunch.connectToServer("localhost", CommandCenter.PORT); // Use actual port from CommandCenter
                             System.out.println("ClientHandler (DroneLaunchThread-" + assignedDroneId + "): Simulated drone " + assignedDroneId + " connectToServer call completed.");
                        } catch (Exception e) {
                            System.err.println("ClientHandler (DroneLaunchThread-" + assignedDroneId + "): Error launching/connecting simulated drone " + assignedDroneId + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    droneLaunchThread.setName("SimDroneLaunchThread-" + assignedDroneId);
                    droneLaunchThread.setDaemon(false); // Consistent with TestConnection's drone threads
                    droneLaunchThread.start();

                    sendResponse(true); // Mission assignment to SharedData and drone launch initiated
                } else {
                    System.err.println("ClientHandler: Failed to assign pending mission " + tempMissionId + " to any available drone.");
                    sendResponse(false); 
                }
            } else {
                System.err.println("ClientHandler: Path could not be created from warehouse " + closestWarehouse.getName() + " to destination " + clientDestinationGeoPos);
                sendResponse(false); 
            }

        } catch (Exception e) { 
            System.err.println("ClientHandler Error processing client request: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
            sendResponse(false); 
        } finally {
             System.out.println("ClientHandler for " + clientSocket.getRemoteSocketAddress() + " finished processing.");
        }
    }

    private void sendResponse(boolean success){
        try {
            ServerData response = new ServerData(success ? 0 : -1); 
            packageStreamOut.writeObject(response);
            packageStreamOut.flush();
            System.out.println("ClientHandler sent response: " + response + " to " + clientSocket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.err.println("ClientHandler: Error sending response to client " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }
}
