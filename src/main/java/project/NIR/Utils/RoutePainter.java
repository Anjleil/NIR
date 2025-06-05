package project.NIR.Utils;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

public class RoutePainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> route;
    private final GeoPosition currentDronePosition;
    private final int currentSegmentTargetIndex;
    private final Color routeColor = Color.GRAY;

    public RoutePainter(List<GeoPosition> route, GeoPosition currentDronePosition, int currentSegmentTargetIndex) {
        this.route = route;
        this.currentDronePosition = currentDronePosition;
        this.currentSegmentTargetIndex = currentSegmentTargetIndex;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        g = (Graphics2D) g.create();
        
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (route == null || route.isEmpty() || currentDronePosition == null || currentSegmentTargetIndex <= 0) {
            g.dispose();
            return;
        }

        // --- 1. Draw Traveled Path (Dashed Gray) ---
        Path2D traveledPath = new Path2D.Double();
        boolean firstTraveled = true;

        // Draw completed segments
        for (int i = 0; i < currentSegmentTargetIndex - 1; i++) {
            Point2D p1 = map.getTileFactory().geoToPixel(route.get(i), map.getZoom());
            Point2D p2 = map.getTileFactory().geoToPixel(route.get(i + 1), map.getZoom());
            if (firstTraveled) {
                traveledPath.moveTo(p1.getX(), p1.getY());
                firstTraveled = false;
            }
            traveledPath.lineTo(p2.getX(), p2.getY());
        }

        // Draw part of the current segment (from last waypoint to drone's position)
        if (currentSegmentTargetIndex - 1 < route.size()) {
            Point2D lastWaypoint = map.getTileFactory().geoToPixel(route.get(currentSegmentTargetIndex - 1), map.getZoom());
            Point2D dronePos = map.getTileFactory().geoToPixel(currentDronePosition, map.getZoom());
            if (firstTraveled) {
                traveledPath.moveTo(lastWaypoint.getX(), lastWaypoint.getY());
            }
            traveledPath.lineTo(dronePos.getX(), dronePos.getY());
        }
        
        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{9}, 0);
        g.setStroke(dashed);
        g.setColor(routeColor);
        g.draw(traveledPath);


        // --- 2. Draw Remaining Path (Solid Gray) ---
        Path2D remainingPath = new Path2D.Double();
        
        // Start from drone's position
        Point2D dronePos = map.getTileFactory().geoToPixel(currentDronePosition, map.getZoom());
        remainingPath.moveTo(dronePos.getX(), dronePos.getY());

        // Draw line to all subsequent waypoints
        for (int i = currentSegmentTargetIndex; i < route.size(); i++) {
            Point2D p = map.getTileFactory().geoToPixel(route.get(i), map.getZoom());
            remainingPath.lineTo(p.getX(), p.getY());
        }

        g.setStroke(new BasicStroke(2));
        g.setColor(routeColor);
        g.draw(remainingPath);

        g.dispose();
    }
}
