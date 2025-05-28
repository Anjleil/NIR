package project.NIR.Models.Routes;

import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Data
public class Path {
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
        if (!points.isEmpty()) {
            return "PathResult{" +
                    "path= {" + points.get(0) + "->" + points.get(points.size()-1) + "}" +
                    ", distance=" + calculatePathLength() + " meters" +
                    '}';
        } else return "No path";
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