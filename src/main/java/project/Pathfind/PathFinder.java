package project.Pathfind;

import org.locationtech.jts.geom.*;

import java.util.*;

public class PathFinder {
    private final Map<Point, List<Point>> graph;
    private final Set<Envelope> gridCells;
    private final Point start;
    private final Point goal;
    private final List<NoFlyZone> noFlyZones;
    private final GeometryFactory geometryFactory;


    public PathFinder(Set<Envelope> gridCells, Point start, Point goal, List<NoFlyZone> noFlyZones) {
        this.geometryFactory = new GeometryFactory();
        this.gridCells = gridCells;
        this.start = start;
        this.goal = goal;
        this.noFlyZones = noFlyZones;
        // Собираем узлы
        List<Point> nodes = collectNodes(gridCells, start, goal, noFlyZones);

        // Строим граф
        this.graph = buildGraph(nodes);
    }

    private List<Point> collectNodes(Set<Envelope> gridCells, Point start, Point goal, List<NoFlyZone> noFlyZones) {
        List<Point> nodes = new ArrayList<>();

        // Добавляем узлы сетки
        for (Envelope cell : gridCells) {
            nodes.add(geometryFactory.createPoint(new Coordinate(cell.getMinX(), cell.getMinY())));
        }

        // Добавляем начальную и конечную точки
        nodes.add(start);
        nodes.add(goal);

        // Добавляем точки на границах бесполетных зон
        for (NoFlyZone zone : noFlyZones) {
            Coordinate[] coordinates = zone.getBoundaryPolygon().getCoordinates();
            for (Coordinate coord : coordinates) {
                nodes.add(geometryFactory.createPoint(coord));
            }
        }

        return nodes;
    }

    public List<Point> findPath() {
        // Список всех узлов
        List<Point> nodes = getGridNodes();

        // Карта рёбер графа
        Map<Point, List<Point>> graph = buildGraph(nodes);

        // Реализация A*
        return aStar(graph, start, goal);
    }

    private List<Point> getGridNodes() {
        List<Point> nodes = new ArrayList<>();
        for (Envelope cell : gridCells) {
            Point node = new GeometryFactory().createPoint(new Coordinate(cell.getMinX(), cell.getMinY()));
            if (!isInsideNoFlyZone(node)) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private Map<Point, List<Point>> buildGraph(List<Point> nodes) {
        Map<Point, List<Point>> graph = new HashMap<>();
        double maxDistance = 0.005; // Радиус соседства (примерно 500 м)

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
                if (!node1.equals(node2) && node1.distance(node2) <= maxDistance) {
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

    private boolean isEdgeValid(Point node1, Point node2) {
        LineString edge = geometryFactory.createLineString(new Coordinate[]{
                node1.getCoordinate(), node2.getCoordinate()
        });

        for (NoFlyZone zone : noFlyZones) {
            if (edge.intersects(zone.getBoundaryPolygon())) {
                return false; // Ребро пересекает бесполетную зону
            }
        }
        return true; // Ребро безопасно
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
