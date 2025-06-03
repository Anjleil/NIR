package project.NIR.Utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import project.NIR.Models.Routes.Path;
import project.Pathfind.AdaptiveGrid;
import project.Pathfind.NoFlyZone;
import project.Pathfind.NoFlyZoneLoader;
import project.Pathfind.PathFinder;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Pathfinder {
    private GeometryFactory factory;
    private List<NoFlyZone> noFlyZones;

    public Pathfinder() {
        this.factory = new GeometryFactory();
        setNoFlyZone();
    }

    public Path createPath(Point startPoint, Point destinationPoint) {
        System.out.println("Pathfinder: Creating path from " + startPoint + " to " + destinationPoint);

        AdaptiveGrid gridGenerator = new AdaptiveGrid();
        int pathSpecificRadius = 20000;
        
        System.out.println("Pathfinder: Generating grid centered at " + startPoint + " with radius " + pathSpecificRadius);
        Set<Envelope> currentGridCells = gridGenerator.createGridAroundPoint(startPoint, pathSpecificRadius, this.noFlyZones);
        
        if (currentGridCells == null || currentGridCells.isEmpty()) {
            System.err.println("Pathfinder: Grid generation failed or resulted in empty grid for start " + startPoint + " and radius " + pathSpecificRadius);
            Path emptyPath = new Path();
            emptyPath.setPoints(List.of());
            return emptyPath; 
        }
        System.out.println("Pathfinder: Dynamically generated grid for path. Cells: " + currentGridCells.size());

        PathFinder pathFinderAlgorithm = new PathFinder(currentGridCells, startPoint, destinationPoint, this.noFlyZones);

        Path path = new Path();
        List<Point> foundPoints = pathFinderAlgorithm.findPath();
        path.setPoints(foundPoints != null ? foundPoints : List.of());

        if (foundPoints != null && !foundPoints.isEmpty()) {
            System.out.println("Pathfinder: Path found with " + foundPoints.size() + " points. " + path);
        } else {
            System.out.println("Pathfinder: No path found from " + startPoint + " to " + destinationPoint);
        }
        return path;
    }

    private void setNoFlyZone(){
        NoFlyZoneLoader loader = new NoFlyZoneLoader(factory);
        try {
            noFlyZones = loader.loadNoFlyZones("src/main/resources/no_fly_zones_moscow.json");
            System.out.println("Pathfinder: NoFlyZones loaded. Count: " + (noFlyZones != null ? noFlyZones.size() : "null"));
        } catch (IOException e) {
            System.err.println("Failed to load no-fly zones: " + e.getMessage());
        }
    }
}
