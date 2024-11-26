package project.Pathfind;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NoFlyZoneLoader {
    private final GeometryFactory factory;

    public NoFlyZoneLoader(GeometryFactory factory) {
        this.factory = factory;
    }

    public List<NoFlyZone> loadNoFlyZones(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<NoFlyZoneConfig> zoneConfigs = objectMapper.readValue(new File(filePath), new TypeReference<>() {});

        List<NoFlyZone> noFlyZones = new ArrayList<>();
        for (NoFlyZoneConfig zoneConfig : zoneConfigs) {
            List<Coordinate> coordinates = new ArrayList<>();
            for (double[] coord : zoneConfig.getCoordinates()) {
                coordinates.add(new Coordinate(coord[0], coord[1]));
            }
            noFlyZones.add(new NoFlyZone(coordinates, factory));
        }
        return noFlyZones;
    }
}

class NoFlyZoneConfig {
    private List<double[]> coordinates;

    public List<double[]> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<double[]> coordinates) {
        this.coordinates = coordinates;
    }
}
