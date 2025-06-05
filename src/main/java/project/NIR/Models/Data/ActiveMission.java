package project.NIR.Models.Data;

import lombok.Getter;
import lombok.Setter;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.Routes.GeoPath;

import java.util.List;

@Getter
@Setter
public class ActiveMission {
    private int droneId;
    private GeoPath path;
    private GeoPath originalPath;
    private GeoPosition currentDronePosition;
    private int currentSegmentTargetIndex; // Index of the next waypoint in the path's points list
    private long lastUpdateTime;
    private boolean assigned; // To track if a drone has been assigned this mission
    private boolean isReturning = false;
    private double batteryLevel = 100.0;
    private double altitude;
    private double speed;

    // Constructor for a mission created from a client request (initially unassigned to a specific drone)
    public ActiveMission(GeoPath path) {
        this.path = path;
        if (path != null && path.getPoints() != null && !path.getPoints().isEmpty()) {
            this.currentDronePosition = path.getPoints().get(0);
            this.currentSegmentTargetIndex = 1; 
        } else {
            // Handle cases where path or points might be null/empty to avoid NullPointerException
            this.currentDronePosition = null; // Or a default safe position
            this.currentSegmentTargetIndex = 0;
        }
        this.droneId = 0; // Placeholder, indicates it's a path waiting for a drone
        this.assigned = false;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // Constructor for initializing an idle drone at a specific location (e.g., warehouse)
    public ActiveMission(int droneId, GeoPosition initialPosition) {
        this.droneId = droneId;
        this.path = null; // No path initially for an idle drone
        this.currentDronePosition = initialPosition;
        this.assigned = false; // Not assigned a task/path yet
        this.currentSegmentTargetIndex = 0; // No segments to target
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // Method to get the list of GeoPositions from GeoPath
    public List<GeoPosition> getPathPoints() {
        return (this.path != null) ? this.path.getPoints() : null;
    }
} 