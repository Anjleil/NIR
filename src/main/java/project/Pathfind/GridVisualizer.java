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

    public GridVisualizer(Point A, Point B, List<NoFlyZone> noFlyZones, Set<Envelope> gridCells) {
        this.A = A;
        this.B = B;
        this.noFlyZones = noFlyZones;
        this.gridCells = gridCells;
        setPreferredSize(new Dimension(800, 800)); // Размер окна
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Масштаб для отображения (чтобы было наглядно)
        double scale = 40000; // Чем выше значение, тем меньше отображаемый участок

        // Центр окна для базовой точки
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Отображаем точку базы
        g2d.setColor(Color.RED);
        double baseX = centerX + (A.getX() - A.getX()) * scale;
        double baseY = centerY - (A.getY() - A.getY()) * scale;
        g2d.fill(new Ellipse2D.Double(centerX, centerY, 10, 10)); // Точка базы (крупная точка)

        // Отображаем бесполетные зоны
        g2d.setColor(Color.BLUE);
        for (NoFlyZone zone : noFlyZones) {
            Path2D polygon = new Path2D.Double();
            Coordinate[] coordinates = zone.getBoundaryPolygon().getCoordinates();
            if (coordinates.length > 0) {
                double startX = centerX + (coordinates[0].x - A.getX()) * scale;
                double startY = centerY - (coordinates[0].y - A.getY()) * scale;
                polygon.moveTo(startX, startY);

                for (int i = 1; i < coordinates.length; i++) {
                    double x = centerX + (coordinates[i].x - A.getX()) * scale;
                    double y = centerY - (coordinates[i].y - A.getY()) * scale;
                    polygon.lineTo(x, y);
                }
                polygon.closePath();
                g2d.draw(polygon);
            }
        }

        // Отображаем узлы сетки
        g2d.setColor(Color.GRAY);
        for (Envelope cell : gridCells) {
            double nodeX = centerX + (cell.getMinX() - A.getX()) * scale;
            double nodeY = centerY - (cell.getMinY() - A.getY()) * scale;

            // Размер точек узлов сетки
            double pointSize = 3;
            g2d.fill(new Ellipse2D.Double(nodeX - pointSize / 2, nodeY - pointSize / 2, pointSize, pointSize));
        }
        g2d.setColor(Color.GREEN);
        g2d.fill(new Ellipse2D.Double(B.getX()-5, B.getY()-5, 10, 10));
    }

    public static void main(String[] args) {
        GeometryFactory factory = new GeometryFactory();

        Point A = factory.createPoint(new Coordinate(37.637326, 55.763979));

        Point B = factory.createPoint(new Coordinate(37.637326, 55.763979));

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
