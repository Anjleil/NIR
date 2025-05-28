package project.NIR.Models.Routes;

import lombok.Getter;
import org.jxmapviewer.viewer.GeoPosition;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GeoPath {
    private final List<GeoPosition> points;

    public GeoPath (Path path){
        this.points = new ArrayList<>();
        if(!path.isEmpty()){
            for (Point p : path.getPoints()){
                GeoPosition pos = new GeoPosition(p.getX(), p.getY());
                this.points.add(pos);
            }
        }
    }

    @Override
    public String toString() {
        if (!points.isEmpty()) {
            return "path = " + points.get(0) + "->" + points.get(points.size()-1);
        } else return "No path";
    }
}
