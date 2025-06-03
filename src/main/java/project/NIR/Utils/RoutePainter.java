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

    public RoutePainter(List<GeoPosition> route) {
        this.route = route;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        g = (Graphics2D) g.create();
        
        // Convert from viewport to world bitmap
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));

        Path2D path = new Path2D.Double();
        boolean first = true;
        if (route != null && !route.isEmpty()) {
            for (GeoPosition geo : route) {
                Point2D pt = map.getTileFactory().geoToPixel(geo, map.getZoom());
                if (first) {
                    path.moveTo(pt.getX(), pt.getY());
                    first = false;
                } else {
                    path.lineTo(pt.getX(), pt.getY());
                }
            }
            g.draw(path);
        }
        g.dispose();
    }
}
