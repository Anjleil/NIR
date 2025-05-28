package project.NIR.Utils;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;
import project.NIR.JXMapViewer.Renderer.CustomWaypointRenderer;
import project.NIR.Models.CustomWaypoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RouteManager {
    private final JXMapViewer mapViewer;

    public RouteManager(JXMapViewer mapViewer) {
        this.mapViewer = mapViewer;
    }

    public void displayRoute(List<GeoPosition> route) {
        // Создаем список painters для рисования маршрута, дрона и конечной точки
        List<Painter<JXMapViewer>> painters = new ArrayList<>();

        // Painter для маршрута
        RoutePainter routePainter = new RoutePainter(route);
        painters.add(routePainter);

        // Painter для дрона (начальной точки)
        WaypointPainter<CustomWaypoint> dronePainter = createDronePainter(route.get(0));
        painters.add(dronePainter);

        // Painter для конечной точки
        WaypointPainter<Waypoint> endpointPainter = createEndpointPainter(route.get(route.size() - 1));
        painters.add(endpointPainter);

        // Устанавливаем все painters на карту
        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(compoundPainter);
    }

    private WaypointPainter<Waypoint> createEndpointPainter(GeoPosition endPos) {
        Set<Waypoint> waypoints = new HashSet<>(Set.of(
                new DefaultWaypoint(endPos)));
        WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(waypoints);
        return waypointPainter;
    }

    private WaypointPainter<CustomWaypoint> createDronePainter(GeoPosition startPos) {
        Set<CustomWaypoint> drones = new HashSet<>(Set.of(
                new CustomWaypoint("Drone", Color.ORANGE, startPos)
        ));
        WaypointPainter<CustomWaypoint> dronePainter = new WaypointPainter<>();
        dronePainter.setWaypoints(drones);
        dronePainter.setRenderer(new CustomWaypointRenderer());
        return dronePainter;
    }
}
