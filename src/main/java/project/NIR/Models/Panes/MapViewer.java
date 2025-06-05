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
import project.NIR.Models.Data.ActiveMission;
import project.NIR.Models.NoFlyZoneMap;
import project.NIR.Models.Warehouse;
import project.Pathfind.NoFlyZoneLoader;
import project.NIR.JXMapViewer.Renderer.PointRenderer;
import project.NIR.Models.Data.SharedData;
import project.NIR.Utils.RoutePainter;
import sample4_fancy.MyWaypoint;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class MapViewer {
    private final JXMapViewer mapViewer;
    private List<Painter<JXMapViewer>> staticPainters = new ArrayList<>();
    private final GeometryFactory factory = new GeometryFactory();

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

        List<Warehouse> warehouses = SharedData.getWarehouses();
        System.out.println("MapViewer: Updating display. Found " + (warehouses != null ? warehouses.size() : "null") + " warehouses in SharedData.");
        if (warehouses != null) {
            for (Warehouse wh : warehouses) {
                if (wh.getLocation() != null) {
                    WaypointPainter<MyWaypoint> warehousePainter = setWarehousePainter(wh.getLocation(), wh.getName());
                    dynamicPainters.add(warehousePainter);
                    System.out.println("MapViewer: Added warehouse painter for " + wh.getName() + " at " + wh.getLocation());
                }
            }
        }

        Collection<ActiveMission> missions = SharedData.getAllActiveMissions();
        System.out.println("MapViewer: Found " + (missions != null ? missions.size() : "null") + " total active missions/drone states in SharedData.");

        if (missions != null) {
            for (ActiveMission mission : missions) {
                if (mission.isAssigned() && mission.getCurrentDronePosition() != null) {
                    WaypointPainter<MyWaypoint> dronePainter = setDronePainter(mission.getCurrentDronePosition(), mission.getDroneId(), mission.isAssigned());
                    dynamicPainters.add(dronePainter);

                    if (mission.getPathPoints() != null && !mission.getPathPoints().isEmpty()) {
                        List<GeoPosition> route = mission.getPathPoints();

                        RoutePainter routePainter = new RoutePainter(route);
                        dynamicPainters.add(routePainter);

                        if (!route.isEmpty()) {
                            WaypointPainter<MyWaypoint> destinationPainter = setDestinationPainter(route.get(route.size() - 1));
                            dynamicPainters.add(destinationPainter);
                        }
                    }
                }
            }
        }
        
        allPainters.addAll(dynamicPainters);
        System.out.println("MapViewer: Total painters to set: " + allPainters.size() + " (Static: "+staticPainters.size()+", Dynamic: "+dynamicPainters.size()+")");

        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(allPainters);
        mapViewer.setOverlayPainter(compoundPainter);
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

    private WaypointPainter<MyWaypoint> setDronePainter(GeoPosition position, int droneID, boolean isAssigned) {
        Color droneColor = isAssigned ? Color.ORANGE : Color.GREEN;
        Set<MyWaypoint> drones = new HashSet<>(Set.of(
                new MyWaypoint("Дрон " + droneID + (isAssigned ? " (Занят)" : " (Свободен)"), droneColor, position)
        ));
        WaypointPainter<MyWaypoint> dronePainter = new WaypointPainter<>();
        dronePainter.setWaypoints(drones);
        dronePainter.setRenderer(new PointRenderer("/images/drones.png"));
        return dronePainter;
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
        tileFactory.setThreadPoolSize(32);
        mapViewer.setAddressLocation(startPos);
        mapViewer.setZoom(6);

        File cacheDir = new File(System.getProperty("user.home") + File.separator + ".jxmapviewer2");
        tileFactory.setLocalCache(new FileBasedLocalCache(cacheDir, false));
    }
}
