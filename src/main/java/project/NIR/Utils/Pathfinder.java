package project.NIR.Utils;

import org.jxmapviewer.viewer.GeoPosition;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import project.NIR.Models.Routes.Path;
import project.NIR.Models.Warehouse;
import project.Pathfind.AdaptiveGrid;
import project.Pathfind.NoFlyZone;
import project.Pathfind.NoFlyZoneLoader;
import project.Pathfind.PathFinder;

import java.io.IOException;
import java.util.*;

public class Pathfinder {
    private static Pathfinder instance;
    private final GeometryFactory factory;
    private final List<NoFlyZone> noFlyZones;
    private final double maxNeighborDistance = 0.02;

    // --- Cached Data ---
    private final List<Point> staticNodes;
    private final STRtree nodeIndex;
    private final Map<Point, List<Point>> staticGraph;


    private Pathfinder(Collection<Warehouse> warehouses) {
        System.out.println("Pathfinder Singleton: Initializing...");
        this.factory = new GeometryFactory();
        this.noFlyZones = loadNoFlyZones();

        if (warehouses == null || warehouses.isEmpty()) {
            throw new IllegalArgumentException("Cannot initialize Pathfinder with no warehouses.");
        }
        
        Set<Envelope> navigationGrid = buildGlobalGrid(warehouses);
        this.staticNodes = getGridNodes(navigationGrid);
        this.nodeIndex = buildNodeIndex(this.staticNodes);
        this.staticGraph = buildStaticGraph(this.staticNodes, this.nodeIndex);
        
        System.out.println("Pathfinder Singleton: Pre-computation complete. Static nodes: " + staticNodes.size());
    }

    public static synchronized void initialize(Collection<Warehouse> warehouses) {
        if (instance == null) {
            instance = new Pathfinder(warehouses);
        }
    }

    public static Pathfinder getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Pathfinder is not initialized. Call initialize() in CommandCenter at startup.");
        }
        return instance;
    }

    private List<Point> getGridNodes(Set<Envelope> gridCells) {
        List<Point> nodes = new ArrayList<>();
        for (Envelope cell : gridCells) {
            Point node = factory.createPoint(cell.centre());
            if (!isInsideNoFlyZone(node)) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private STRtree buildNodeIndex(List<Point> nodes) {
        STRtree index = new STRtree();
        for (Point node : nodes) {
            index.insert(node.getEnvelopeInternal(), node);
        }
        return index;
    }

    private Map<Point, List<Point>> buildStaticGraph(List<Point> nodes, STRtree index) {
        Map<Point, List<Point>> graph = new HashMap<>();
        for (Point node1 : nodes) {
            List<Point> neighbors = new ArrayList<>();
            Envelope searchEnvelope = new Envelope(node1.getCoordinate());
            searchEnvelope.expandBy(maxNeighborDistance);
            
            @SuppressWarnings("unchecked")
            List<Point> candidateNeighbors = index.query(searchEnvelope);

            for (Point node2 : candidateNeighbors) {
                if (!node1.equals(node2) && isEdgeValid(node1, node2)) {
                    neighbors.add(node2);
                }
            }
            graph.put(node1, neighbors);
        }
        return graph;
    }

    public Path createPath(Point startPoint, Point destinationPoint) {
        // Since this method is called sequentially by a single-threaded executor,
        // we can temporarily modify the static graph and safely revert the changes.
        // This avoids the overhead of copying the entire graph for each request.

        List<Runnable> cleanupTasks = new ArrayList<>();
        
        try {
            // Connect start and goal points to the graph
            connectDynamicPoint(startPoint, cleanupTasks);
            connectDynamicPoint(destinationPoint, cleanupTasks);

            PathFinder pathFinderAlgorithm = new PathFinder(staticGraph, startPoint, destinationPoint);

            Path path = new Path();
            List<Point> foundPoints = pathFinderAlgorithm.findPath();
            path.setPoints(foundPoints != null ? foundPoints : List.of());

            if (foundPoints != null && !foundPoints.isEmpty()) {
                System.out.println("Pathfinder: Path found with " + foundPoints.size() + " points.");
            } else {
                System.out.println("Pathfinder: No path found from " + startPoint + " to " + destinationPoint);
            }
            return path;

        } finally {
            // Execute all cleanup tasks to revert the static graph to its original state
            for (int i = cleanupTasks.size() - 1; i >= 0; i--) {
                cleanupTasks.get(i).run();
            }
        }
    }
    
    private void connectDynamicPoint(Point dynamicPoint, List<Runnable> cleanupTasks) {
        if (isInsideNoFlyZone(dynamicPoint)) {
            System.err.println("Dynamic point " + dynamicPoint + " is inside a no-fly zone!");
            // Add a cleanup task to remove the (empty) node if it gets added
            cleanupTasks.add(() -> staticGraph.remove(dynamicPoint));
            staticGraph.put(dynamicPoint, new ArrayList<>());
            return;
        }
        
        Envelope searchEnvelope = new Envelope(dynamicPoint.getCoordinate());
        searchEnvelope.expandBy(maxNeighborDistance);
        
        @SuppressWarnings("unchecked")
        List<Point> neighbors = nodeIndex.query(searchEnvelope);
        
        List<Point> validNeighbors = new ArrayList<>();
        for (Point neighbor : neighbors) {
            if (isEdgeValid(dynamicPoint, neighbor)) {
                validNeighbors.add(neighbor);
                // Also add the back-edge to the static node's list
                staticGraph.get(neighbor).add(dynamicPoint);
                // Add a cleanup task to remove the back-edge
                cleanupTasks.add(() -> staticGraph.get(neighbor).remove(dynamicPoint));
            }
        }
        staticGraph.put(dynamicPoint, validNeighbors);
        // Add a cleanup task to remove the node itself
        cleanupTasks.add(() -> staticGraph.remove(dynamicPoint));
    }
    
    private boolean isInsideNoFlyZone(Point point) {
        for (NoFlyZone zone : noFlyZones) {
            if (zone.intersects(point)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEdgeValid(Point node1, Point node2) {
        LineString edge = factory.createLineString(new Coordinate[]{
                node1.getCoordinate(), node2.getCoordinate()
        });

        for (NoFlyZone zone : noFlyZones) {
            if (zone.intersects(edge)) {
                return false;
            }
        }
        return true;
    }
    
    private List<NoFlyZone> loadNoFlyZones() {
        NoFlyZoneLoader loader = new NoFlyZoneLoader(factory);
        try {
            return loader.loadNoFlyZones("src/main/resources/no_fly_zones_moscow.json");
        } catch (IOException e) {
            System.err.println("Pathfinder Singleton: Failed to load no-fly zones: " + e.getMessage());
            return List.of();
        }
    }

    private Set<Envelope> buildGlobalGrid(Collection<Warehouse> warehouses) {
        Envelope globalBounds = new Envelope();
        for (Warehouse warehouse : warehouses) {
            globalBounds.expandToInclude(warehouse.getLocation().getLongitude(), warehouse.getLocation().getLatitude());
        }

        globalBounds.expandBy(0.2);

        Point centerOfBoundsJts = factory.createPoint(globalBounds.centre());
        GeoPosition centerGeo = new GeoPosition(centerOfBoundsJts.getY(), centerOfBoundsJts.getX());
        GeoPosition cornerGeo = new GeoPosition(globalBounds.getMaxY(), globalBounds.getMaxX());
        
        double radiusInMeters = GeoUtils.calculateDistance(centerGeo, cornerGeo);
        
        AdaptiveGrid gridGenerator = new AdaptiveGrid();
        return gridGenerator.createGridAroundPoint(centerOfBoundsJts, radiusInMeters, this.noFlyZones);
    }
}
