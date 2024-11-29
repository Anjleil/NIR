package project.Pathfind;

import org.locationtech.jts.geom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
class AdaptiveGrid {
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final double baseResolution = 500; // Начальный размер ячейки в метрах
    private final double maxDetailResolution = 100; // Максимальная детализация 10 м

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

    public Set<Envelope> createRadialGrid(Point base, double radius, List<NoFlyZone> noFlyZones) {
        Set<Envelope> cells = new HashSet<>();
        double baseLat = base.getY();
        double baseLon = base.getX();

        double degreesPerMeterLat = 360 / (2 * Math.PI * 6371000); // Радиус Земли в метрах
        double degreesPerMeterLon = degreesPerMeterLat / Math.cos(Math.toRadians(baseLat));

        double currentRadius = 0; // Текущий радиус построения
        while (currentRadius <= radius) {
            double resolution = calculateResolution(currentRadius, noFlyZones, base, degreesPerMeterLat, degreesPerMeterLon);
            createCircle(baseLat, baseLon, currentRadius, resolution, degreesPerMeterLat, degreesPerMeterLon, cells, noFlyZones, 1);
            currentRadius += 300; // Увеличиваем радиус с шагом
        }
        return cells;
    }

    private void createCircle(double baseLat, double baseLon, double radius, double resolution,
                              double degreesPerMeterLat, double degreesPerMeterLon,
                              Set<Envelope> cells, List<NoFlyZone> noFlyZones, int step) {
        int points = (int) Math.ceil(2 * Math.PI * radius / resolution); // Количество точек на круге
        double angleStep = 2 * Math.PI / points;

        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            double lat = baseLat + radius * degreesPerMeterLat * Math.sin(angle);
            double lon = baseLon + radius * degreesPerMeterLon * Math.cos(angle);

            Envelope cell = new Envelope(lon - resolution * degreesPerMeterLon / step, lon + resolution * degreesPerMeterLon / step,
                    lat - resolution * degreesPerMeterLat / step, lat + resolution * degreesPerMeterLat/ step);

            while (shouldRefineWithBuffer(cell, noFlyZones, 0) && step < 2)
            {
                step+=1;
                createCircle(baseLat, baseLon, radius, resolution / step, degreesPerMeterLat, degreesPerMeterLon, cells, noFlyZones, step);
                cells.add(cell);
            }

            if (step <= 2)
                cells.add(cell);
        }
    }

    private double calculateResolution(double currentRadius, List<NoFlyZone> noFlyZones, Point base,
                                       double degreesPerMeterLat, double degreesPerMeterLon) {
        // Базовый шаг
        double resolution = 500;

//        for (NoFlyZone zone : noFlyZones) {
//            if (zone.distance(base) <= currentRadius) {
//                // Если в пределах текущего радиуса, уменьшаем шаг для повышения детализации
//                resolution = Math.max(maxDetailResolution, resolution / 2);
//            }
//        }

        return resolution;
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
                if (shouldRefineWithBuffer(cell, noFlyZones, 0)) {
                    if (resolution > maxDetailResolution) {
                        // Делим текущую ячейку на более мелкие
                        createCells(
                                cell.getMinY(), cell.getMaxY(), cell.getMinX(), cell.getMaxX(),
                                Math.max(resolution / 5, maxDetailResolution), cells, noFlyZones
                        );
                    }
                } else{
                    if (shouldRefineWithBuffer(cell, noFlyZones, 0.002)){
                        if (resolution > maxDetailResolution) {
                            createCells(
                                    cell.getMinY(), cell.getMaxY(), cell.getMinX(), cell.getMaxX(),
                                    Math.max(resolution / 5, maxDetailResolution), cells, noFlyZones
                            );
                        }
                    }
                    cells.add(cell); // Добавляем ячейку, если нет пересечения или влияния
                }
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

    private boolean shouldRefine(Envelope cell, List<NoFlyZone> noFlyZones) {
        Geometry geometry = geometryFactory.toGeometry(cell);
        for (NoFlyZone zone : noFlyZones) {
            if (zone.intersects(geometry)) {
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