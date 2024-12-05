package project.Pathfind;

import lombok.Getter;
import org.locationtech.jts.geom.*;

import java.util.List;

@Getter
class NoFlyZone {
    private final Polygon boundaryPolygon;

    public NoFlyZone(List<Coordinate> boundaryPoints, GeometryFactory factory) {
        Coordinate[] coordinates = boundaryPoints.toArray(new Coordinate[0]);
        this.boundaryPolygon = factory.createPolygon(coordinates);
    }

    public boolean intersects(Point point) {
        return boundaryPolygon.contains(point);
    }

    public boolean intersects(Geometry geometry) {
        return boundaryPolygon.intersects(geometry);
    }

}
