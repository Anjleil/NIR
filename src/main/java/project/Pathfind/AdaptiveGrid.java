package project.Pathfind;

import org.locationtech.jts.geom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class AdaptiveGrid {
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private static final double baseResolution = 500; // Начальный размер ячейки в метрах
    private static final double maxDetailResolution = 100; // Максимальная детализация 10 м
    private static final double buffer = 0.005; // Максимальная детализация 10 м
    public Set<Envelope> createGridAroundPoint(Point base, double radius, List<NoFlyZone> noFlyZones) {
        Set<Envelope> cells = new HashSet<>();
        // Определим границы начальной области вокруг базы
        double lat = base.getY();
        double lon = base.getX();
        double earthRadius = 6371000; // Радиус Земли в метрах
        double degreesPerMeterLat = 360 / (2 * Math.PI * earthRadius); // Перевод метров в градусы
        double degreesPerMeterLon = degreesPerMeterLat / Math.cos(Math.toRadians(lat));
//        System.out.printf("Degrees per meter lat: %f, degrees per meter lon: %f%n", degreesPerMeterLat, degreesPerMeterLon);

        double minLat = lat - radius * degreesPerMeterLat;
        double maxLat = lat + radius * degreesPerMeterLat;
        double minLon = lon - radius * degreesPerMeterLon;
        double maxLon = lon + radius * degreesPerMeterLon;

//        System.out.printf("Min Lat: %f, Max Lat: %f, Min Lon: %f, Min Lon: %f%n", minLat, maxLat, minLon, maxLon);
//        System.out.printf("Degrees per meter: %f%n", degreesPerMeterLat);
//        System.out.printf("Latitude meters: %f%n", (maxLat - minLat) / degreesPerMeterLat);
//        System.out.printf("Longitude meters: %f%n", (maxLon - minLon) / degreesPerMeterLon);

        // Начнем строить крупную сетку 500 м x 500 м
        createCells(minLat, maxLat, minLon, maxLon, baseResolution, cells, noFlyZones);

        return cells;
    }

    private void createCells(double minLat, double maxLat, double minLon, double maxLon,
                             double resolution, Set<Envelope> cells, List<NoFlyZone> noFlyZones) {
        for (double lat = minLat; lat <= maxLat; lat += resolution * degreesPerMeterLat(lat)) {
            for (double lon = minLon; lon <= maxLon; lon += resolution * degreesPerMeterLon(lat)) {
                Envelope cell = new Envelope(
                        lon, lon + resolution * degreesPerMeterLon(lat),
                        lat, lat + resolution * degreesPerMeterLat(lat)
                );

                // Проверяем пересечение и зону влияния
                if (shouldRefineWithBuffer(cell, noFlyZones, buffer)){
                    if (resolution > maxDetailResolution) {
                        createCells(cell.getMinY(), cell.getMaxY(), cell.getMinX(), cell.getMaxX(),
                                maxDetailResolution, cells, noFlyZones
                        );
                    }
                }
                if ((shouldRefineWithBuffer(cell, noFlyZones, buffer) || (resolution == baseResolution)) && !shouldRefineWithBuffer(cell, noFlyZones, 0))
                    cells.add(cell);
            }
        }
    }

    /**
     * Проверяет, пересекается ли ячейка или область вокруг неё с бесполётной зоной.
     */
    private boolean shouldRefineWithBuffer(Envelope cell, List<NoFlyZone> noFlyZones, double buffer) {
        Geometry bufferedGeometry = geometryFactory.toGeometry(cell).buffer(buffer);
        for (NoFlyZone zone : noFlyZones) {
            if (zone.intersects(bufferedGeometry)) {
                return true; // Если зона пересекает ячейку или её "буфер"
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