package project.Pathfind;

import org.locationtech.jts.geom.*;

import java.util.*;

public class PathFinder {
    
    private final Map<Point, List<Point>> graph;
    private final Point start;
    private final Point goal;

    public PathFinder(Map<Point, List<Point>> graph, Point start, Point goal) {
        this.graph = graph;
        this.start = start;
        this.goal = goal;
    }

    public List<Point> findPath() {
        if (graph == null || graph.isEmpty()) {
            System.err.println("PathFinder: Graph is null or empty, cannot find path.");
            return new ArrayList<>();
        }
        return aStar(graph, start, goal);
    }

    private double heuristic(Point p1, Point p2) {
        return p1.distance(p2); // Euclidean distance
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

            if (!graph.containsKey(current.point)) continue;

            for (Point neighbor : graph.get(current.point)) {
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

        return new ArrayList<>(); // Path not found
    }

    private static class Node {
        Point point;
        Node previous;
        double g; // distance from start
        double f; // total estimated cost (g + h)

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
