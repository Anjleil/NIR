package project.Pathfind;

import lombok.Data;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MultiStepVisualizer {

    @Data
    public static class Path {
        private List<Point> points;
        private double distance;

        private double calculatePathLength() {
            double totalLength = 0;

            for (int i = 0; i < points.size() - 1; i++) {
                Point current = points.get(i);
                Point next = points.get(i + 1);

                double distance = calculateDistanceInMeters(
                        current.getY(), current.getX(),
                        next.getY(), next.getX()
                );
                totalLength += distance;
            }
            return totalLength;
        }

        private double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
            double degreesPerMeterLat = degreesPerMeterLat(lat1);
            double degreesPerMeterLon = degreesPerMeterLon(lat1);

            double deltaLat = Math.abs(lat2 - lat1);
            double deltaLon = Math.abs(lon2 - lon1);

            double distanceLat = deltaLat / degreesPerMeterLat;
            double distanceLon = deltaLon / degreesPerMeterLon;

            return Math.sqrt(distanceLat * distanceLat + distanceLon * distanceLon);
        }

        private double degreesPerMeterLat(double latitude) {
            double earthRadius = 6371000;
            return 360 / (2 * Math.PI * earthRadius);
        }

        private double degreesPerMeterLon(double latitude) {
            double earthRadius = 6371000;
            double degreesPerMeterLat = 360 / (2 * Math.PI * earthRadius);
            return degreesPerMeterLat / Math.cos(Math.toRadians(latitude));
        }

        public boolean isEmpty() {
            return points.isEmpty();
        }

    }

    private static class StepVisualizer extends JPanel {
        private final GeometryFactory factory;
        private final Set<Envelope> gridCells;
        private final List<NoFlyZone> noFlyZones;
        private final Path path;
        private final Point A;
        private final Point B;


        private final double initialAX;
        private final double initialAY;
        private String stepDescription;
        private final double scale = 20000;

        public StepVisualizer(Point A, Point B, Set<Envelope> gridCells, List<NoFlyZone> noFlyZones, Path path, String stepDescription, GeometryFactory factory) {
            this.A = A;
            this.B = B;
            this.gridCells = gridCells;
            this.noFlyZones = noFlyZones;
            this.path = path;
            this.initialAX = A.getX();
            this.initialAY = A.getY(); // Фиксируем начальные координаты A

            this.stepDescription = stepDescription;
            this.factory = factory;
            setPreferredSize(new Dimension(1200, 1000));
        }

        public void setStepDescription(String stepDescription) {
            this.stepDescription = stepDescription;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            // Отображаем точку A (центр карты)
            g2d.setColor(Color.RED);
            drawPoint(g2d, A.getX(), A.getY(), centerX, centerY, 10);

            // Отображаем точку B
            g2d.setColor(Color.GREEN);
            drawPoint(g2d, B.getX(), B.getY(), centerX, centerY, 10);

            // Шаг: описание текущей стадии
            g2d.setColor(Color.BLACK);
            g2d.drawString(stepDescription, 10, 20);

            // Отображение сетки
            if (stepDescription.contains("сетки") || stepDescription.contains("Итог")) {
                g2d.setColor(Color.GRAY);
                for (Envelope cell : gridCells) {
                    double cellX = transformX(cell.centre().getX(), centerX);
                    double cellY = transformY(cell.centre().getY(), centerY);
                    g2d.fill(new Ellipse2D.Double(cellX - 2, cellY - 2, 4, 4));
                }
            }

            // Отображение бесполетных зон
            if (stepDescription.contains("Бесполетные зоны") || stepDescription.contains("Итог")) {
                g2d.setColor(Color.BLUE);
                for (NoFlyZone zone : noFlyZones) {
                    Path2D polygon = new Path2D.Double();
                    Coordinate[] coordinates = zone.getBoundaryPolygon().getCoordinates();
                    drawPolygon(g2d, centerX, centerY, polygon, coordinates);
                }
            }

            // Отображение маршрута
            if (stepDescription.contains("маршрута") || stepDescription.contains("Итог")) {
                if (path != null && !path.isEmpty()) {
                    g2d.setColor(Color.ORANGE);
                    Path2D route = new Path2D.Double();
                    Point first = path.getPoints().get(0);
                    route.moveTo(transformX(first.getX(), centerX), transformY(first.getY(), centerY));

                    for (int i = 1; i < path.getPoints().size(); i++) {
                        Point p = path.getPoints().get(i);
                        route.lineTo(transformX(p.getX(), centerX), transformY(p.getY(), centerY));
                    }
                    g2d.draw(route);
                }
            }
        }

        private void drawPoint(Graphics2D g2d, double longitude, double latitude, int centerX, int centerY, double size) {
            double x = transformX(longitude, centerX);
            double y = transformY(latitude, centerY);
            g2d.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
        }

        private double transformX(double longitude, int centerX) {
            return centerX + (longitude - initialAX) * scale; // Используем initialAX
        }

        private double transformY(double latitude, int centerY) {
            return centerY - (latitude - initialAY) * scale; // Используем initialAY
        }

        private void drawPolygon(Graphics2D g2d, int centerX, int centerY, Path2D polygon, Coordinate[] coordinates) {
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
    }

    public static void main(String[] args) throws IOException {
        GeometryFactory factory = new GeometryFactory();
        Point A = factory.createPoint(new Coordinate(37.637326, 55.763979));
        Point B = factory.createPoint(new Coordinate(37.654, 55.74));

        NoFlyZoneLoader loader = new NoFlyZoneLoader(factory);
        List<NoFlyZone> noFlyZones = loader.loadNoFlyZones("src/main/resources/no_fly_zones.json");

        AdaptiveGrid grid = new AdaptiveGrid();
        Set<Envelope> gridCells = grid.createGridAroundPoint(A, 10000, noFlyZones);

        PathFinder pathFinder = new PathFinder(gridCells, A, B, noFlyZones);
        Path path = new Path();
        path.setPoints(pathFinder.findPath());

        gridCells.clear();
        gridCells.addAll(grid.createGridAroundPoint(A, 10000, new ArrayList<>()));

        JFrame mainFrame = new JFrame("Пошаговое построение");
        StepVisualizer visualizer = new StepVisualizer(A, B, gridCells, noFlyZones, path, "", factory);

        mainFrame.add(visualizer);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);

        Timer timer = new Timer(5000, new ActionListener() {
            private int step = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                switch (step) {
                    case 1 -> visualizer.setStepDescription("Шаг 1: Построение сетки");
                    case 2 -> {
                        visualizer.setStepDescription("Шаг 2: Бесполетные зоны, сетки");
                    }
                    case 3 -> {
                        gridCells.clear();
                        gridCells.addAll(grid.createGridAroundPoint(A, 10000, noFlyZones));
                        visualizer.setStepDescription("Шаг 2: Бесполетные зоны, сетки");
                    }
                    case 4 -> visualizer.setStepDescription("Шаг 3: Построение маршрута, Бесполетные зоны, сетки");
                    case 5 -> {
                        visualizer.setStepDescription("Шаг 4: Итоговое представление");
                        ((Timer) e.getSource()).stop(); // Остановить таймер после финального шага
                    }
                }
                visualizer.repaint();
            }
        });
        timer.setInitialDelay(0);
        timer.start();
    }


}
