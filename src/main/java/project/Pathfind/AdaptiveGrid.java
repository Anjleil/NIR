package project.Pathfind;

import org.locationtech.jts.geom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
class AdaptiveGrid {
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final double baseResolution = 400; // Начальный размер ячейки в метрах
    private final double maxDetailResolution = 50; // Максимальная детализация 10 м

    public Set<Envelope> createGridAroundPoint(Point base, double radius, List<NoFlyZone> noFlyZones) {
        Set<Envelope> cells = new HashSet<>();
        // Определим границы начальной области вокруг базы
        double lat = base.getY();
        double lon = base.getX();
        double earthRadius = 6371000; // Радиус Земли в метрах
        double degreesPerMeterLat = 360 / (2 * Math.PI * earthRadius); // Перевод метров в градусы
        double degreesPerMeterLon = degreesPerMeterLat / Math.cos(Math.toRadians(lat));
        System.out.printf("Degrees per meter lat: %f, degrees per meter lon: %f%n", degreesPerMeterLat, degreesPerMeterLon);

        double minLat = lat - radius * degreesPerMeterLat;
        double maxLat = lat + radius * degreesPerMeterLat;
        double minLon = lon - radius * degreesPerMeterLon;
        double maxLon = lon + radius * degreesPerMeterLon;

        System.out.printf("Min Lat: %f, Max Lat: %f, Min Lon: %f, Min Lon: %f%n", minLat, maxLat, minLon, maxLon);
        System.out.printf("Degrees per meter: %f%n", degreesPerMeterLat);
        System.out.printf("Latitude meters: %f%n", (maxLat - minLat) / degreesPerMeterLat);
        System.out.printf("Longitude meters: %f%n", (maxLon - minLon) / degreesPerMeterLon);

        // Начнем строить крупную сетку 500 м x 500 м
        createCells(minLat, maxLat, minLon, maxLon, baseResolution, cells, noFlyZones);

        return cells;
    }

    private void createCells(double minLat, double maxLat, double minLon, double maxLon,
                             double resolution, Set<Envelope> cells, List<NoFlyZone> noFlyZones) {
        for (double lat = minLat; lat <= maxLat; lat += resolution * degreesPerMeterLat(lat)) {
            for (double lon = minLon; lon <= maxLon; lon += resolution * degreesPerMeterLon(lat)) {
                Envelope cell = new Envelope(lon, lon + resolution * degreesPerMeterLon(lat),
                        lat, lat + resolution * degreesPerMeterLat(lat));
                //System.out.printf("%f: %s%n", resolution, cell);

                // Проверяем, нужно ли детализировать ячейку
                if (shouldRefine(cell, noFlyZones)) {
                    if (resolution > maxDetailResolution) {
                        createCells(cell.getMinY(), cell.getMaxY(), cell.getMinX(), cell.getMaxX(),
                        Math.max(resolution/2, maxDetailResolution), cells, noFlyZones); // рекурсивное деление
                    }
                } else {
                    cells.add(cell); // Добавляем крупную ячейку, если нет пересечения с зоной
                }
            }
        }
    }

    private boolean shouldRefine(Envelope cell, List<NoFlyZone> noFlyZones) {
        Geometry cellGeometry = geometryFactory.toGeometry(cell);
        for (NoFlyZone zone : noFlyZones) {
            if (zone.intersects(cellGeometry)) {
                return true; // Если пересекается с бесполетной зоной, делим дальше
            }
        }
        return false;
    }

    // Перевод градусов в метры с учетом широты
    private double degreesPerMeterLat(double latitude) {
        double earthRadius = 6371000; // Радиус Земли в метрах
        return 360 / (2 * Math.PI * earthRadius); // Перевод метров в градусы
    }

    private double degreesPerMeterLon(double latitude) {
        double earthRadius = 6371000; // Радиус Земли в метрах
        double degreesPerMeterLat = 360 / (2 * Math.PI * earthRadius); // Перевод метров в градусы
        return degreesPerMeterLat / Math.cos(Math.toRadians(latitude));
    }
}