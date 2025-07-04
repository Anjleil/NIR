package project.NIR.Models.Panes;

import lombok.Getter;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.viewer.WaypointPainter;
import org.locationtech.jts.geom.GeometryFactory;
import project.NIR.JXMapViewer.GeoapifyPositronTileFactoryInfo;
import project.NIR.JXMapViewer.PanMouseInputListener;
import project.NIR.JXMapViewer.PolygonPainter;
import project.NIR.JXMapViewer.Renderer.DroneWaypointPainter;
import project.NIR.Models.Data.ActiveMission;
import project.NIR.Models.MapModel;
import project.NIR.Models.NoFlyZoneMap;
import project.NIR.Models.Warehouse;
import project.NIR.Models.Waypoints.DroneWaypoint;
import project.Pathfind.NoFlyZoneLoader;
import project.NIR.JXMapViewer.Renderer.PointRenderer;
import project.NIR.Models.Data.SharedData;
import project.NIR.Utils.RoutePainter;
import project.NIR.Utils.GeoUtils;
import sample4_fancy.MyWaypoint;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MapViewer {
    private final JXMapViewer mapViewer;
    private List<Painter<JXMapViewer>> staticPainters = new ArrayList<>();
    private final GeometryFactory factory = new GeometryFactory();
    private final Map<Integer, DroneWaypoint> droneWaypoints = new ConcurrentHashMap<>();
    private final WaypointPainter<DroneWaypoint> droneWaypointPainter = new DroneWaypointPainter();

    public MapViewer() throws IOException {
        mapViewer = new JXMapViewer();

        NoFlyZoneLoader loader = new NoFlyZoneLoader(factory);
        List<NoFlyZoneMap> noFlyZones = loader.loadNoFlyZonesForMap("src/main/resources/no_fly_zones_moscow.json");

        for (NoFlyZoneMap zone : noFlyZones) {
            PolygonPainter polygonPainter = new PolygonPainter(zone.getGeoPositions(), new Color(255, 0, 0, 50), Color.RED);
            staticPainters.add(polygonPainter);
        }
        System.out.println("MapViewer: Initialized with " + staticPainters.size() + " static no-fly zone painters.");

        updateMapDisplay();

        GeoPosition Moscow = new GeoPosition(55.788845, 37.791609);
        addInteractions(Moscow);
    }

    public void updateMapDisplay() {
        List<Painter<JXMapViewer>> allPainters = new ArrayList<>(this.staticPainters);
        List<Painter<JXMapViewer>> dynamicPainters = new ArrayList<>();

        // Paint warehouses (using the old system)
        List<Warehouse> warehouses = SharedData.getWarehouses();
        if (warehouses != null) {
            for (Warehouse wh : warehouses) {
                if (wh.getLocation() != null) {
                    WaypointPainter<MyWaypoint> warehousePainter = setWarehousePainter(wh.getLocation(), wh.getName());
                    dynamicPainters.add(warehousePainter);
                }
            }
        }

        // --- New Drone Waypoint Logic ---
        Set<Integer> activeDroneIds = new HashSet<>();
        Collection<ActiveMission> missions = SharedData.getAllActiveMissions();
        Integer selectedDroneId = MapModel.getSelectedDroneId();

        if (missions != null) {
            for (ActiveMission mission : missions) {
                if (mission.isAssigned() && mission.getCurrentDronePosition() != null) {
                    int droneId = mission.getDroneId();
                    activeDroneIds.add(droneId);
                    
                    DroneWaypoint waypoint = droneWaypoints.get(droneId);
                    if (waypoint == null) {
                        waypoint = new DroneWaypoint(droneId, mission.getCurrentDronePosition());
                        droneWaypoints.put(droneId, waypoint);
                        mapViewer.add(waypoint.getButton());
                    } else {
                        waypoint.setPosition(mission.getCurrentDronePosition());
                    }

                    if (mission.getPathPoints() != null && mission.getCurrentSegmentTargetIndex() < mission.getPathPoints().size()) {
                        GeoPosition targetPos = mission.getPathPoints().get(mission.getCurrentSegmentTargetIndex());
                        double angle = GeoUtils.calculateBearing(mission.getCurrentDronePosition(), targetPos);
                        waypoint.setRotationAngle(angle);
                    }
                    
                    boolean isSelected = selectedDroneId != null && selectedDroneId.equals(droneId);
                    
                    if (mission.getPathPoints() != null && !mission.getPathPoints().isEmpty()) {
                        List<GeoPosition> route = mission.getPathPoints();
                        RoutePainter routePainter = new RoutePainter(route, mission.getCurrentDronePosition(), mission.getCurrentSegmentTargetIndex(), isSelected);
                        dynamicPainters.add(routePainter);

                        if (!route.isEmpty() && !mission.isReturning()) {
                            WaypointPainter<MyWaypoint> destinationPainter = setDestinationPainter(route.get(route.size() - 1));
                            dynamicPainters.add(destinationPainter);
                        }
                    }
                }
            }
        }
        
        Set<Integer> waypointsToRemove = new HashSet<>(droneWaypoints.keySet());
        waypointsToRemove.removeAll(activeDroneIds);
        for (Integer droneIdToRemove : waypointsToRemove) {
            DroneWaypoint waypoint = droneWaypoints.remove(droneIdToRemove);
            if (waypoint != null) {
                mapViewer.remove(waypoint.getButton());
            }
        }
        
        droneWaypointPainter.setWaypoints(new HashSet<>(droneWaypoints.values()));
        dynamicPainters.add(droneWaypointPainter);

        allPainters.addAll(dynamicPainters);
        
        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(allPainters);
        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.revalidate();
        mapViewer.repaint();
    }

    private WaypointPainter<MyWaypoint> setWarehousePainter(GeoPosition position, String name) {
        Set<MyWaypoint> warehouseWaypoints = new HashSet<>();
        warehouseWaypoints.add(new MyWaypoint(name, Color.DARK_GRAY, position));
        WaypointPainter<MyWaypoint> warehousePainter = new WaypointPainter<>();
        warehousePainter.setWaypoints(warehouseWaypoints);
        warehousePainter.setRenderer(new PointRenderer("/images/warehouse.png"));
        return warehousePainter;
    }

    private WaypointPainter<MyWaypoint> setDestinationPainter(GeoPosition destPos) {
        Set<MyWaypoint> destination = new HashSet<>(Set.of(
                new MyWaypoint("Пункт назначения", Color.BLUE, destPos)
        ));
        WaypointPainter<MyWaypoint> destinationPainter = new WaypointPainter<>();
        destinationPainter.setWaypoints(destination);
        destinationPainter.setRenderer(new PointRenderer("/images/home_black.png"));
        return destinationPainter;
    }

    public void addInteractions(GeoPosition startPos) {
        TileFactoryInfo info = new GeoapifyPositronTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);

        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));

        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON3) return;

                Integer selectedDroneId = MapModel.getSelectedDroneId();

                if (MapModel.isManualControlActive() && selectedDroneId != null) {
                    GeoPosition newDest = mapViewer.getTileFactory().pixelToGeo(e.getPoint(), mapViewer.getZoom());
                    SharedData.setManualWaypoint(selectedDroneId, newDest);
                } else {
                    MapModel.setSelectedDroneId(null);
                }
            }
        });

        tileFactory.setThreadPoolSize(32);
        mapViewer.setAddressLocation(startPos);
        mapViewer.setZoom(6);

        File cacheDir = new File(System.getProperty("user.home") + File.separator + ".jxmapviewer2");
        tileFactory.setLocalCache(new FileBasedLocalCache(cacheDir, false));
    }
}
