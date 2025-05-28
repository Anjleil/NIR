package project.NIR.JXMapViewer.Renderer;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointRenderer;
import project.NIR.Models.CustomWaypoint;

import java.awt.*;

public class CustomWaypointRenderer implements WaypointRenderer<CustomWaypoint> {
    @Override
    public void paintWaypoint(Graphics2D g, JXMapViewer viewer, CustomWaypoint waypoint) {
        g.setColor(waypoint.getColor());
        g.fillOval(-5, -5, 10, 10);
        g.drawString(waypoint.getLabel(), 10, -10);
    }
}
