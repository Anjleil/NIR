package project.NIR.JXMapViewer;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

public class PolygonPainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> polygonPoints;
    private final Color fillColor;
    private final Color borderColor;

    public PolygonPainter(List<GeoPosition> polygonPoints, Color fillColor, Color borderColor) {
        this.polygonPoints = polygonPoints;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);

        Path2D path = new Path2D.Double();
        boolean firstPoint = true;

        for (GeoPosition pos : polygonPoints) {
            Point2D pt = map.getTileFactory().geoToPixel(pos, map.getZoom());
//            System.out.println("Рисуем точку полигона в пикселях: " + pt);
            if (firstPoint) {
                path.moveTo(pt.getX(), pt.getY());
                firstPoint = false;
            } else {
                path.lineTo(pt.getX(), pt.getY());
            }
        }

        path.closePath();

        g.setColor(fillColor);
        g.fill(path);

        g.setColor(borderColor);
        g.setStroke(new BasicStroke(2));
        g.draw(path);
    }


}
