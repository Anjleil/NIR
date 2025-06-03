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
        if(path != null && path.getPoints() != null && !path.isEmpty()){
            for (Point p : path.getPoints()){
                // JTS Point: getX() is longitude, getY() is latitude
                // GeoPosition constructor: GeoPosition(latitude, longitude)
                GeoPosition pos = new GeoPosition(p.getY(), p.getX()); 
                this.points.add(pos);
            }
        }
    }

    @Override
    public String toString() {
        if (points != null && !points.isEmpty()) {
            return "path = [" + points.get(0).getLatitude() + ", " + points.get(0).getLongitude() + "]->[" + points.get(points.size()-1).getLatitude() + ", " + points.get(points.size()-1).getLongitude() + "] (" + points.size() + " pts)";
        } else return "No path or empty path";
    }
}
