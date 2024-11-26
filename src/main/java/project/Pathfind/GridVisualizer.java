package project.Pathfind;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GridVisualizer extends JPanel {
    private final Point A;
    private final Point B;
    private final List<NoFlyZone> noFlyZones;
    private final Set<Envelope> gridCells;

    private final double scale; // Масштаб отображения
    private final List<Point> path;

    public GridVisualizer(Point A, Point B, List<NoFlyZone> noFlyZones, Set<Envelope> gridCells) {
        this.A = A;
        this.B = B;
        this.noFlyZones = noFlyZones;
        this.gridCells = gridCells;
        this.scale = 20000; // Чем выше значение, тем крупнее масштаб
        setPreferredSize(new Dimension(800, 800)); // Размер окна// Построение маршрута
        PathFinder pathFinder = new PathFinder(gridCells, A, B, noFlyZones);
        this.path = pathFinder.findPath();

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Центр окна
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Отображаем точку A (центр карты)
        g2d.setColor(Color.RED);
        drawPoint(g2d, A.getX(), A.getY(), centerX, centerY, 10);

        // Отображаем точку B
        g2d.setColor(Color.GREEN);
        drawPoint(g2d, B.getX(), B.getY(), centerX, centerY, 10);

        // Отображаем бесполетные зоны
        g2d.setColor(Color.BLUE);
        for (NoFlyZone zone : noFlyZones) {
            Path2D polygon = new Path2D.Double();
            Coordinate[] coordinates = zone.getBoundaryPolygon().getCoordinates();
            if (coordinates.length > 0) {
                double startX = transformX(coordinates[0].x, centerX);
                double startY = transformY(coordinates[0].y, centerY);
                polygon.moveTo(startX, startY);

                for (int i = 1; i < coordinates.length; i++) {
                    double x = transformX(coordinates[i].x, centerX);
                    double y = transformY(coordinates[i].y, centerY);
                    polygon.lineTo(x, y);
                }
                polygon.closePath();
                g2d.draw(polygon);
            }
        }

        // Отображаем узлы сетки
        g2d.setColor(Color.GRAY);
        for (Envelope cell : gridCells) {
            double cellX = transformX(cell.getMinX(), centerX);
            double cellY = transformY(cell.getMinY(), centerY);

            // Размер точек узлов сетки
            double pointSize = 3;
            g2d.fill(new Ellipse2D.Double(cellX - pointSize / 2, cellY - pointSize / 2, pointSize, pointSize));
        }

        // Отображение маршрута
        if (path != null && !path.isEmpty()) {
            g2d.setColor(Color.ORANGE);
            Path2D route = new Path2D.Double();
            Point first = path.get(0);
            route.moveTo(transformX(first.getX(), getWidth() / 2), transformY(first.getY(), getHeight() / 2));

            for (int i = 1; i < path.size(); i++) {
                Point p = path.get(i);
                route.lineTo(transformX(p.getX(), getWidth() / 2), transformY(p.getY(), getHeight() / 2));
            }
            g2d.draw(route);
        }
    }

    private double transformX(double longitude, int centerX) {
        return centerX + (longitude - A.getX()) * scale;
    }

    private double transformY(double latitude, int centerY) {
        return centerY - (latitude - A.getY()) * scale;
    }

    private void drawPoint(Graphics2D g2d, double longitude, double latitude, int centerX, int centerY, double size) {
        double x = transformX(longitude, centerX);
        double y = transformY(latitude, centerY);
        g2d.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
    }

    public static void main(String[] args) {
        GeometryFactory factory = new GeometryFactory();

        Point A = factory.createPoint(new Coordinate(37.637326, 55.763979));
        Point B = factory.createPoint(new Coordinate(37.645, 55.765));

        List<NoFlyZone> noFlyZones = new ArrayList<>();
        List<Coordinate> zone1Points = List.of(
                new Coordinate(37.635, 55.760),
                new Coordinate(37.640, 55.765),
                new Coordinate(37.645, 55.760),
                new Coordinate(37.635, 55.760)
        );
        noFlyZones.add(new NoFlyZone(zone1Points, factory));

        List<Coordinate> zone2Points = List.of(
                new Coordinate(37.630, 55.770),
                new Coordinate(37.635, 55.770),
                new Coordinate(37.635, 55.765),
                new Coordinate(37.630, 55.765),
                new Coordinate(37.630, 55.770)
        );
        noFlyZones.add(new NoFlyZone(zone2Points, factory));

        AdaptiveGrid grid = new AdaptiveGrid();
        Set<Envelope> gridCells = grid.createGridAroundPoint(A, 10000, noFlyZones);
        System.out.println("Generated cells: " + gridCells.size());

        JFrame frame = new JFrame("Grid Visualizer");
        GridVisualizer visualizer = new GridVisualizer(A, B, noFlyZones, gridCells);
        frame.add(visualizer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
