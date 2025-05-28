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
    private Point warehouse;
    private GeometryFactory factory;
    private List<NoFlyZone> noFlyZones;
    private Set<Envelope> gridCells;
    private final static int radius = 10000;
    private final static double buffer = 0.003;

    public Pathfinder() {
        this.factory = new GeometryFactory();
        this.warehouse = factory.createPoint(new Coordinate(55.748935, 37.705178));
        setNoFlyZone();
        setGrid();
    }

    public Path createPath(Point destination) {
        PathFinder pathFinder = new PathFinder(gridCells, warehouse, destination, noFlyZones);

        Path path = new Path();
        path.setPoints(pathFinder.findPath());

        System.out.println(path);
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

    private void setGrid(){
        AdaptiveGrid grid = new AdaptiveGrid();
        gridCells = grid.createGridAroundPoint(warehouse, radius, noFlyZones);
    }
}
