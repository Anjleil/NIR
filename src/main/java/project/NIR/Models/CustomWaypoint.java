package project.NIR.Models;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

import java.awt.*;

public class CustomWaypoint implements Waypoint {
    private final String label;
    private final Color color;
    private final GeoPosition position;

    public CustomWaypoint(String label, Color color, GeoPosition position) {
        this.label = label;
        this.color = color;
        this.position = position;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }

    public GeoPosition getPosition() {
        return position;
    }
}
