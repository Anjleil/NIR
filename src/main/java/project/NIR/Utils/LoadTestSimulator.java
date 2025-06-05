package project.NIR.Utils;

import org.jxmapviewer.viewer.GeoPosition;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import project.NIR.CommandCenter;
import project.NIR.Models.Data.ClientData;
import project.NIR.Models.Data.SharedData;
import project.NIR.Models.Warehouse;
import project.Pathfind.NoFlyZone;
import project.Pathfind.NoFlyZoneLoader;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class LoadTestSimulator {
    private static final int NUM_CLIENTS = 25;
    private static final int REPORT_DELAY_SECONDS = 30;
    private static final List<ResponseTimeDataPoint> responseTimes = new CopyOnWriteArrayList<>();
    private static final GeometryFactory factory = new GeometryFactory();
    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        System.out.println("LoadTestSimulator: Starting load test with " + NUM_CLIENTS + " clients.");
        System.out.println("LoadTestSimulator: Performance graph will be displayed in " + REPORT_DELAY_SECONDS + " seconds.");

        List<NoFlyZone> noFlyZones = new NoFlyZoneLoader(factory).loadNoFlyZones("src/main/resources/no_fly_zones_moscow.json");
        List<Warehouse> warehouses = SharedData.getWarehouses();
        if (warehouses.isEmpty()) {
            // This is a fallback in case CommandCenter hasn't been run and SharedData is empty.
            // In a real scenario, run CommandCenter first.
            warehouses = new ArrayList<>();
            warehouses.add(new Warehouse("Warehouse 1 (sim)", new GeoPosition(55.730899, 37.721991)));
            warehouses.add(new Warehouse("Warehouse 2 (sim)", new GeoPosition(55.773143, 37.530097)));
        }

        final List<Warehouse> finalWarehouses = warehouses;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> displayGraph(new ArrayList<>(responseTimes)), REPORT_DELAY_SECONDS, TimeUnit.SECONDS);

        ExecutorService clientExecutor = Executors.newFixedThreadPool(NUM_CLIENTS);
        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int clientNum = i + 1;
            clientExecutor.submit(() -> {
                try {
                    GeoPosition destination = generateRandomValidDestination(finalWarehouses, noFlyZones);
                    System.out.println("Client " + clientNum + ": Requesting delivery to " + destination);

                    long response = performClientRequest(destination);
                    if (response >= 0) {
                        responseTimes.add(new ResponseTimeDataPoint(System.currentTimeMillis(), response));
                    } else {
                        System.err.println("Client " + clientNum + ": Request failed.");
                    }
                } catch (Exception e) {
                    System.err.println("Client " + clientNum + ": Unhandled exception: " + e.getMessage());
                }
            });
        }
        
        clientExecutor.shutdown();
        // The scheduler will keep the JVM alive until the graph is displayed.
    }

    private static long performClientRequest(GeoPosition destination) {
        try (Socket socket = new Socket("localhost", CommandCenter.PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush(); // Handshake
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                ClientData data = new ClientData(destination);
                long startTime = System.currentTimeMillis();
                out.writeObject(data);
                out.flush();

                in.readObject(); // Wait for server's confirmation

                return System.currentTimeMillis() - startTime;
            }
        } catch (IOException | ClassNotFoundException e) {
            return -1;
        }
    }

    private static GeoPosition generateRandomValidDestination(List<Warehouse> warehouses, List<NoFlyZone> noFlyZones) {
        while (true) {
            Warehouse warehouse = warehouses.get(random.nextInt(warehouses.size()));
            GeoPosition warehouseLoc = warehouse.getLocation();

            double latOffset = (random.nextDouble() - 0.5) * 0.2; // Approx +/- 11km
            double lonOffset = (random.nextDouble() - 0.5) * 0.2;
            GeoPosition randomDest = new GeoPosition(warehouseLoc.getLatitude() + latOffset, warehouseLoc.getLongitude() + lonOffset);

            Point destPoint = factory.createPoint(new Coordinate(randomDest.getLongitude(), randomDest.getLatitude()));
            boolean inNoFlyZone = false;
            if (noFlyZones != null) {
                for (NoFlyZone zone : noFlyZones) {
                    if (zone.intersects(destPoint)) {
                        inNoFlyZone = true;
                        break;
                    }
                }
            }
            if (!inNoFlyZone) {
                return randomDest;
            }
        }
    }

    private static void displayGraph(List<ResponseTimeDataPoint> data) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Server Response Time");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            
            if (data.isEmpty()) {
                frame.add(new JLabel("No response data collected in time.", SwingConstants.CENTER));
            } else {
                 frame.add(new GraphPanel(data));
            }

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            System.out.println("LoadTestSimulator: Graph displayed. Total responses captured: " + data.size());
        });
    }
} 