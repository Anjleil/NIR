package project.NIR.Models.Data;

import project.NIR.Models.Routes.GeoPath;
import project.NIR.Models.Routes.Path;

import java.util.ArrayList;
import java.util.List;

public class SharedData {
    private static final List<GeoPath> paths = new ArrayList<>();
    private static final Object lock = new Object();

    public static List<GeoPath> getPaths() {
        synchronized (lock) {
            return new ArrayList<>(paths);
        }
    }

    public static void addPath(Path path) {
        synchronized (lock) {
            GeoPath geoPath = new GeoPath(path);
            paths.add(geoPath);
        }
    }

    public static void clearPaths() {
        synchronized (lock) {
            paths.clear();
        }
    }
}
