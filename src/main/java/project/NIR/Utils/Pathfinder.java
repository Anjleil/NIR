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
    private Set<Envelope> gridCells;

    public Pathfinder() {
        this.factory = new GeometryFactory();
        setNoFlyZone();
        setGridAroundDefaultPoint();
    }

    public Path createPath(Point startPoint, Point destinationPoint) {
        System.out.println("Pathfinder: Creating path from " + startPoint + " to " + destinationPoint);
        PathFinder pathFinderAlgorithm = new PathFinder(gridCells, startPoint, destinationPoint, noFlyZones);

        Path path = new Path();
        List<Point> foundPoints = pathFinderAlgorithm.findPath();
        path.setPoints(foundPoints);

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
            noFlyZones = loader.loadNoFlyZones("src/main/resources/no_fly_zones.json");
        } catch (IOException e) {
            System.err.println("Failed to load no-fly zones: " + e.getMessage());
        }
    }

    private void setGridAroundDefaultPoint(){
        AdaptiveGrid grid = new AdaptiveGrid();
        Point gridCenter = factory.createPoint(new Coordinate(37.6173, 55.7558));
        int defaultRadius = 25000;
        gridCells = grid.createGridAroundPoint(gridCenter, defaultRadius, noFlyZones);
        System.out.println("Pathfinder: Grid initialized around " + gridCenter + " with radius " + defaultRadius + ". Cells: " + (gridCells != null ? gridCells.size() : "null"));
    }
}
