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
import project.NIR.Models.NoFlyZoneMap;
import project.Pathfind.NoFlyZoneLoader;
import project.NIR.JXMapViewer.Renderer.PointRenderer;
import sample4_fancy.MyWaypoint;
import sample4_fancy.RoutePainter;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class MapViewer {
    private final JXMapViewer mapViewer;
    List<List<GeoPosition>> routes = new ArrayList<>();

    public MapViewer() throws IOException {
        mapViewer = new JXMapViewer();

        List<Painter<JXMapViewer>> painters = new ArrayList<>();

        // Пример маршрута
//        List<GeoPosition> route = List.of(
//                new GeoPosition(55.795099, 37.778590),
//                new GeoPosition(55.787885, 37.753699),
//                new GeoPosition(55.780895, 37.713494)
//        );

        List<GeoPosition> route = List.of(
                new GeoPosition(55.746619, 37.692969),new GeoPosition(55.74668669598512, 37.69316762840084),new GeoPosition(55.742190087955535, 37.67716642041813),new GeoPosition(55.73769347992594, 37.6611685325436),new GeoPosition(55.733196871896354, 37.64357747797361),new GeoPosition(55.73229755029044, 37.64038323746066),new GeoPosition(55.73049890707861, 37.627587440263895),new GeoPosition(55.729599585472684, 37.614811894481555),new GeoPosition(55.73139822868451, 37.605245819188426),new GeoPosition(55.73679415832002, 37.58928869181406),new GeoPosition(55.74129076634962, 37.581315375607396),new GeoPosition(55.751425, 37.564654));
        routes.add(route);
        route = List.of(
        new GeoPosition(55.776536, 37.686005),new GeoPosition(55.77338837437921, 37.69317223601698),new GeoPosition(55.77158973116738, 37.69474229847244),new GeoPosition(55.76529447992594, 37.69791048111024),new GeoPosition(55.74730804780758, 37.70578304261757),new GeoPosition(55.73112025890106, 37.71205160638933),new GeoPosition(55.726623650871474, 37.7136179409539),new GeoPosition(55.722127042841876, 37.71039361045224),new GeoPosition(55.714708, 37.699462));
        routes.add(route);
        //        List<GeoPosition> route = SharedData.getPaths().get(0).getPoints();

        GeometryFactory factory = new GeometryFactory();
        NoFlyZoneLoader loader = new NoFlyZoneLoader(factory);
        List<NoFlyZoneMap> noFlyZones = loader.loadNoFlyZonesForMap("src/main/resources/no_fly_zones_moscow.json");

        for (NoFlyZoneMap zone : noFlyZones){
            PolygonPainter polygonPainter = new PolygonPainter(zone.getGeoPositions(), new Color(255, 0, 0, 50), Color.RED);
            painters.add(polygonPainter);
        }

        // Добавляем painters в список

        int droneID = 1;
        for(List<GeoPosition> r : routes){
            RoutePainter routePainter = new RoutePainter(r);
            painters.add(routePainter);

            WaypointPainter<MyWaypoint> dronePainter = setDronePainter(r.get(0), droneID);
            painters.add(dronePainter);
            droneID++;

            WaypointPainter<MyWaypoint> destinationPainter = setDestinationPainter(r.get(r.size()-1));
            painters.add(destinationPainter);
        }


        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);

        GeoPosition Moscow = new GeoPosition(55.788845, 37.791609);
        addInteractions(Moscow);
    }

    private WaypointPainter<MyWaypoint> setDronePainter(GeoPosition startPos, int droneID) {
        Set<MyWaypoint> drones = new HashSet<>(Set.of(
                new MyWaypoint("Дрон " + droneID, Color.ORANGE, startPos)
        ));
        WaypointPainter<MyWaypoint> dronePainter = new WaypointPainter<>();
        dronePainter.setWaypoints(drones);
        dronePainter.setRenderer(new PointRenderer("/drones.png"));
        return dronePainter;
    }
    private WaypointPainter<MyWaypoint> setDestinationPainter(GeoPosition startPos) {
        Set<MyWaypoint> destination = new HashSet<>(Set.of(
                new MyWaypoint("Пункт назначения", Color.ORANGE, startPos)
        ));
        WaypointPainter<MyWaypoint> destinationPainter = new WaypointPainter<>();
        destinationPainter.setWaypoints(destination);
        destinationPainter.setRenderer(new PointRenderer("/home_black.png"));
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
