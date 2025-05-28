package project.Pathfind;

import lombok.Data;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class GridVisualizer extends JPanel {
    private Point A;
    private Point B;
    private final List<NoFlyZone> noFlyZones;
    private final Set<Envelope> gridCells;
    private final double scale = 2500; // Масштаб отображения
    private final static int radius = 15000;
    private final static double buffer = 0.003;

    private final double initialAX;
    private final double initialAY;
    private final GeometryFactory factory;
    private Path path;

    @Data
    public static class Path {
        private List<Point> points;
        private double distance;

        private double calculatePathLength() {
            double totalLength = 0;

            for (int i = 0; i < points.size() - 1; i++) {
                Point current = points.get(i);
                Point next = points.get(i + 1);

                // Расстояние в метрах с учетом разницы в градусах широты и долготы
                double distance = calculateDistanceInMeters(
                        current.getY(), current.getX(),
                        next.getY(), next.getX()
                );
                totalLength += distance;
            }

            return totalLength;
        }


        @Override
        public String toString() {
            if (!points.isEmpty()){
                StringBuilder str = new StringBuilder();
                str.append("PathResult{");

                for (Point p : points){
                    str.append("new GeoPosition(").append(p.getY()).append(", ").append(p.getX()).append("),");
                }

                str.append(", distance=" + calculatePathLength() + " meters" + '}');
                return str.toString();
            }
            else return "No path";
        }

        private double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
            // Перевод широты и долготы из градусов в метры
            double degreesPerMeterLat = degreesPerMeterLat(lat1);
            double degreesPerMeterLon = degreesPerMeterLon(lat1);

            // Разница в градусах
            double deltaLat = Math.abs(lat2 - lat1);
            double deltaLon = Math.abs(lon2 - lon1);

            // Расстояние по широте и долготе в метрах
            double distanceLat = deltaLat / degreesPerMeterLat;
            double distanceLon = deltaLon / degreesPerMeterLon;

            // Гипотенуза (теорема Пифагора)
            return Math.sqrt(distanceLat * distanceLat + distanceLon * distanceLon);
        }


        private double degreesPerMeterLat(double latitude) {
            double earthRadius = 6371000; // Радиус Земли в метрах
            return 360 / (2 * Math.PI * earthRadius); // Перевод метров в градусы
        }

        private double degreesPerMeterLon(double latitude) {
            double earthRadius = 6371000; // Радиус Земли в метрах
            double degreesPerMeterLat = 360 / (2 * Math.PI * earthRadius); // Перевод метров в градусы
            return degreesPerMeterLat / Math.cos(Math.toRadians(latitude));
        }

        public boolean isEmpty() {
            return points.isEmpty();
        }

        public Point get(int i) {
            return points.get(i);
        }

        public int size() {
            return points.size();
        }
    }




    public GridVisualizer(Point A, Point B, List<NoFlyZone> noFlyZones, Set<Envelope> gridCells, GeometryFactory factory) {
        this.A = A;
        this.B = B;
        this.noFlyZones = noFlyZones;
        this.gridCells = gridCells;
        this.factory = factory;

        this.initialAX = A.getX();
        this.initialAY = A.getY(); // Фиксируем начальные координаты A

        setPreferredSize(new Dimension(1200, 1000));
        updatePath();
    }

    private void updatePath() {
        PathFinder pathFinder = new PathFinder(gridCells, A, B, noFlyZones);

        this.path = new Path();
        this.path.setPoints(pathFinder.findPath());

        System.out.println(path.toString());

        repaint(); // Перерисовываем панель
    }

    private void randomizePointA(int dispersion){
        this.A = factory.createPoint(new Coordinate(
                37.637326 - Math.random() / dispersion + Math.random() / dispersion,
                55.763979 - Math.random() / dispersion + Math.random() / dispersion
        ));
    }

    private void randomizePointB(int dispersion){
        this.B = factory.createPoint(new Coordinate(
                37.637326 - Math.random() / dispersion + Math.random() / dispersion,
                55.763979 - Math.random() / dispersion + Math.random() / dispersion
        ));
    }

    public void updatePoints() {
        int dispersion = 15;
        // Генерируем новые случайные точки
        randomizePointA(dispersion);
        randomizePointB(dispersion);

        while (isInsideNoFlyZone(this.A)){
            randomizePointA(dispersion);
        }
        while (isInsideNoFlyZone(this.B)){
            randomizePointB(dispersion);
        }

        // Обновляем сетку вокруг новой точки A
//        gridCells.clear();
//        gridCells.addAll(grid.createGridAroundPoint(A, 10000, noFlyZones));

        // Строим новый маршрут
        updatePath();
    }

    private boolean isInsideNoFlyZone(Point point) {
        for (NoFlyZone zone : noFlyZones) {
            if (zone.getBoundaryPolygon().contains(point)) {
                return true;
            }
        }
        return false;
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
            drawPoints(g2d, centerX, centerY, polygon, coordinates);
        }

        // Отображаем буффер бесполетных зон
        g2d.setColor(Color.BLUE);
        for (NoFlyZone zone : noFlyZones) {
            Path2D polygon = new Path2D.Double();
            Coordinate[] coordinates = zone.getBoundaryPolygon().buffer(buffer).getCoordinates();
            drawPoints(g2d, centerX, centerY, polygon, coordinates);
        }

        // Отображаем узлы сетки
        g2d.setColor(Color.GRAY);
        for (Envelope cell : gridCells) {
//            double cellX = transformX(cell.getMinX(), centerX);
//            double cellY = transformY(cell.getMinY(), centerY);

            double cellX = transformX(cell.centre().getX(), centerX);
            double cellY = transformY(cell.centre().getY(), centerY);

            // Размер точек узлов сетки
            double pointSize = 3;
            g2d.fill(new Ellipse2D.Double(cellX - pointSize / 2, cellY - pointSize / 2, pointSize, pointSize));
        }

        // Отображение маршрута
        if (path != null && !path.isEmpty()) {
            g2d.setColor(Color.ORANGE);
            Path2D route = new Path2D.Double();
            Point first = path.get(0);
            route.moveTo(transformX(first.getX(), centerX), transformY(first.getY(), centerY));

            for (int i = 1; i < path.size(); i++) {
                Point p = path.get(i);
                route.lineTo(transformX(p.getX(), centerX), transformY(p.getY(), centerY));
            }
            g2d.draw(route);
        }
    }

    private void drawPoints(Graphics2D g2d, int centerX, int centerY, Path2D polygon, Coordinate[] coordinates) {
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

    private double transformX(double longitude, int centerX) {
        return centerX + (longitude - initialAX) * scale; // Используем initialAX
    }

    private double transformY(double latitude, int centerY) {
        return centerY - (latitude - initialAY) * scale; // Используем initialAY
    }


    private void drawPoint(Graphics2D g2d, double longitude, double latitude, int centerX, int centerY, double size) {
        double x = transformX(longitude, centerX);
        double y = transformY(latitude, centerY);
        g2d.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
    }

    public static void main(String[] args) {
        GeometryFactory factory = new GeometryFactory();

        // Начальные точки
        Point A = factory.createPoint(new Coordinate(37.686005, 55.776536 ));
        Point B = factory.createPoint(new Coordinate(37.699462, 55.714708 ));

        NoFlyZoneLoader loader = new NoFlyZoneLoader(factory);
        List<NoFlyZone> noFlyZones;

        try {
            noFlyZones = loader.loadNoFlyZones("src/main/resources/no_fly_zones_moscow.json");
        } catch (IOException e) {
            System.err.println("Failed to load no-fly zones: " + e.getMessage());
            return;
        }

        AdaptiveGrid grid = new AdaptiveGrid();
//        Set<Envelope> gridCells = grid.createRadialGrid(A, 10000, noFlyZones);
        Set<Envelope> gridCells = grid.createGridAroundPoint(A, radius, noFlyZones);

        JFrame frame = new JFrame("Grid Visualizer");
        GridVisualizer visualizer = new GridVisualizer(A, B, noFlyZones, gridCells, factory);

        frame.add(visualizer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Таймер для обновления каждые 2 секунды
        Timer timer = new Timer(2000, e -> visualizer.updatePoints());
//        timer.start();
    }
}
