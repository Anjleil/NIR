package project.NIR.Models.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Route {
    protected List<GeoPosition> waypoints;
    public void addWaypoint(GeoPosition waypoint) {
        this.waypoints.add(waypoint);
    }

    public abstract void displayRouteInfo();

}

