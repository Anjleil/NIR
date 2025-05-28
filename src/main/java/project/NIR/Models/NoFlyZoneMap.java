package project.NIR.Models;

import lombok.Getter;
import org.locationtech.jts.geom.*;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NoFlyZoneMap {
    private final Polygon boundaryPolygon;
    private final List<GeoPosition> geoPositions;

    public NoFlyZoneMap(List<Coordinate> boundaryPoints, GeometryFactory factory) {
        Coordinate[] coordinates = boundaryPoints.toArray(new Coordinate[0]);
        this.boundaryPolygon = factory.createPolygon(coordinates);

        // Преобразуем координаты в GeoPosition для отображения на карте
        this.geoPositions = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            this.geoPositions.add(new GeoPosition(coordinate.y, coordinate.x));
        }
    }

    public boolean intersects(Point point) {
        return boundaryPolygon.contains(point);
    }

    public boolean intersects(Geometry geometry) {
        return boundaryPolygon.intersects(geometry);
    }

}
