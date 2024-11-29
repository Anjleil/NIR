package project.Pathfind;

import org.locationtech.jts.geom.*;

import java.util.*;

public class PathFinder {
    private final Set<Envelope> gridCells;
    private final GeometryFactory geometryFactory;
    private final Point start;
    private final Point goal;
    private final List<NoFlyZone> noFlyZones;
    double maxDistance = 0.01; // Радиус соседства (примерно 500 м)

    public PathFinder(Set<Envelope> gridCells, Point start, Point goal, List<NoFlyZone> noFlyZones) {
        this.geometryFactory = new GeometryFactory();
        this.gridCells = gridCells;
        this.start = start;
        this.goal = goal;
        this.noFlyZones = noFlyZones;
    }

    public List<Point> findPath() {
        // Список всех узлов
        List<Point> nodes = getGridNodes();

        // Карта рёбер графа
        Map<Point, List<Point>> graph = buildGraph(nodes);

        List<Point> path = aStar(graph, start, goal);

        if (path.isEmpty()) {
//            throw new IllegalStateException("Cannot find path: " + start + ":" + goal);
            System.out.println("Cannot find path: " + start + ":" + goal);
        }

        // Реализация A*
        return path;
    }

    private List<Point> getGridNodes() {
        List<Point> nodes = new ArrayList<>();
        for (Envelope cell : gridCells) {

//            Point node = new GeometryFactory().createPoint(new Coordinate(cell.getMinX(), cell.getMinY()));
            Point node = new GeometryFactory().createPoint(new Coordinate(cell.centre()));
            if (!isInsideNoFlyZone(node)) {
                nodes.add(node);
            }
}

//        for (NoFlyZone zone : noFlyZones) {
//            Coordinate[] coordinates = zone.getBoundaryPolygon().getCoordinates();
//            for (Coordinate coord : coordinates) {
//                nodes.add(geometryFactory.createPoint(coord));
//            }
//        }
        return nodes;
    }

    private Map<Point, List<Point>> buildGraph(List<Point> nodes) {
        Map<Point, List<Point>> graph = new HashMap<>();

        // Добавляем A и B в список узлов
        nodes.add(start);
        nodes.add(goal);

        // Проверяем, что точки A и B не внутри бесполетных зон
        if (isInsideNoFlyZone(start)) {
            throw new IllegalArgumentException("Start point A is inside a no-fly zone!");
        }
        if (isInsideNoFlyZone(goal)) {
            throw new IllegalArgumentException("Goal point B is inside a no-fly zone!");
        }

        // Построение рёбер графа
        for (Point node1 : nodes) {
            List<Point> neighbors = new ArrayList<>();
            for (Point node2 : nodes) {
                if (!node1.equals(node2) && node1.distance(node2) <= maxDistance && isEdgeValid(node1, node2)) {
                    neighbors.add(node2);
                }
            }
            graph.put(node1, neighbors);
        }
        return graph;
    }


    private boolean isInsideNoFlyZone(Point point) {
        for (NoFlyZone zone : noFlyZones) {
            if (zone.getBoundaryPolygon().contains(point)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEdgeValid(Point node1, Point node2) {
        LineString edge = geometryFactory.createLineString(new Coordinate[]{
                node1.getCoordinate(), node2.getCoordinate()
        });

        for (NoFlyZone zone : noFlyZones) {
            if (edge.intersects(zone.getBoundaryPolygon())) {
                return false; // Если ребро пересекает бесполетную зону
            }
        }
        return true; // Ребро безопасно
    }

    private List<Point> aStar(Map<Point, List<Point>> graph, Point start, Point goal) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Point, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, heuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.point.equals(goal)) {
                return reconstructPath(current);
            }

            for (Point neighbor : graph.getOrDefault(current.point, new ArrayList<>())) {
                double tentativeG = current.g + current.point.distance(neighbor);

                Node neighborNode = allNodes.getOrDefault(neighbor, new Node(neighbor));
                if (tentativeG < neighborNode.g) {
                    neighborNode.previous = current;
                    neighborNode.g = tentativeG;
                    neighborNode.f = tentativeG + heuristic(neighbor, goal);

                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                    allNodes.put(neighbor, neighborNode);
                }
            }
        }

        return new ArrayList<>(); // Путь не найден
    }

    private double heuristic(Point p1, Point p2) {
        return p1.distance(p2); // Евклидово расстояние
    }

    private List<Point> reconstructPath(Node node) {
        List<Point> path = new ArrayList<>();
        while (node != null) {
            path.add(node.point);
            node = node.previous;
        }
        Collections.reverse(path);
        return path;
    }

    private static class Node {
        Point point;
        Node previous;
        double g; // Расстояние от старта
        double f; // Оценка полного пути (g + h)

        public Node(Point point) {
            this(point, null, Double.MAX_VALUE, Double.MAX_VALUE);
        }

        public Node(Point point, Node previous, double g, double f) {
            this.point = point;
            this.previous = previous;
            this.g = g;
            this.f = f;
        }
    }
}
