package project.NIR.JXMapViewer.Renderer;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointPainter;
import project.NIR.Models.Waypoints.DroneWaypoint;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

public class DroneWaypointPainter extends WaypointPainter<DroneWaypoint> {
    @Override
    protected void doPaint(Graphics2D g, JXMapViewer jxMapViewer, int width, int height) {
        for (DroneWaypoint dw : getWaypoints()) {
            Point2D point = jxMapViewer.getTileFactory().geoToPixel(dw.getPosition(), jxMapViewer.getZoom());
            Rectangle rectangle = jxMapViewer.getViewportBounds();

            int buttonX = (int)(point.getX() - rectangle.getX());
            int buttonY = (int)(point.getY() - rectangle.getY());

            JButton button = dw.getButton();
            button.setLocation(buttonX - button.getWidth() / 2, buttonY - button.getHeight() / 2);
        }
    }
} 