package sample4_fancy;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * A simple sample application that shows
 * a OSM map of Europe
 * @author Martin Steiger
 */
public class Sample4
{
    private final JXMapViewer mapViewer;
    private final List<GeoPosition> track;
    double velocity = 60;
    int steps = 0;

    public Sample4(List<GeoPosition> points){
        mapViewer = new JXMapViewer();

        track = points;
        steps = (int) calculateTime(calculateDistance(track.get(0), track.get(1)), velocity);
        time(track);

        addInteractions(track.get(0));
        setPainters(track.get(0));

        JFrame frame = new JFrame("Smooth Route Animation");
        frame.getContentPane().add(mapViewer);
        frame.setSize(1000, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Создаем таймер для анимации
        Timer timer = new Timer(10, e -> {
            // Обновляем координаты для плавной анимации
            updateCoordinates();
            // Перерисовываем карту
            mapViewer.repaint();
        });

        Timer timer1 = new Timer(4000, e -> {
            mapViewer.zoomToBestFit(new HashSet<>(track), 0.5);
        });

        timer.start();
        timer1.start();

    }

    public void time (List<GeoPosition> track){
        double distance = 0;
        for (int i = 0; i < track.size() - 1; i++){
            distance += calculateDistance(track.get(i), track.get(i+1));
        }
        System.out.println("~" + Math.round(distance/this.velocity * 60) + " минут");
    }

    public void setPainters(GeoPosition currPoint){
        RoutePainter routePainter = new RoutePainter(track);
        WaypointPainter<MyWaypoint> dronePainter = setDronePainter(currPoint);
        WaypointPainter<Waypoint> waypointPainter = setWaypointPainter(track.get(track.size()-1));

        List<Painter<JXMapViewer>> painters = new ArrayList<>();
        painters.add(routePainter);
        painters.add(dronePainter);
        painters.add(waypointPainter);

        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);
    }

    public WaypointPainter<Waypoint> setWaypointPainter(GeoPosition endPos){
        Set<Waypoint> waypoints = new HashSet<>(Arrays.asList(
                new DefaultWaypoint(endPos)));
        WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(waypoints);
        return waypointPainter;
    }

    public WaypointPainter<MyWaypoint> setDronePainter(GeoPosition startPos){
        Set<MyWaypoint> drones = new HashSet<>(Arrays.asList(
                new MyWaypoint("Drone 1", Color.ORANGE, startPos)
        ));
        WaypointPainter<MyWaypoint> dronePainter = new WaypointPainter<>();
        dronePainter.setWaypoints(drones);
        dronePainter.setRenderer(new FancyWaypointRenderer());
        return dronePainter;
    }

    public void addInteractions(GeoPosition startPos){
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));


        TileFactoryInfo info = new OSMTileFactoryInfo();
        //TileFactoryInfo info = new VirtualEarthTileFactoryInfo(VirtualEarthTileFactoryInfo.MAP);
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);
        tileFactory.setThreadPoolSize(32);
        mapViewer.setAddressLocation(startPos);
        mapViewer.setZoom(4);

        File cacheDir = new File(System.getProperty("user.home") + File.separator + ".jxmapviewer2");
        tileFactory.setLocalCache(new FileBasedLocalCache(cacheDir, false));
    }

    public static void main(String[] args)
    {
        List<GeoPosition> route1 = new ArrayList<>();
        route1.add(new GeoPosition(55.788845, 37.791609));
        route1.add(new GeoPosition(55.793094, 37.778580));
        route1.add(new GeoPosition(55.789875, 37.773694));
        route1.add(new GeoPosition(55.781875, 37.773694));
        route1.add(new GeoPosition(55.781875, 37.783694));
        SwingUtilities.invokeLater(() -> new Sample4(route1));
    }

    private void updateCoordinates() {
        // Проверяем, что есть следующая точка
        if (track.size() > 1) {
            // Получаем текущие координаты
            GeoPosition currentPos = track.get(0);

            // Получаем координаты следующей точки
            GeoPosition nextPos = track.get(1);

            // Выполняем линейную интерполяцию для широты и долготы
            double interpolatedLat = interpolate(currentPos.getLatitude(), nextPos.getLatitude(), steps);
            double interpolatedLon = interpolate(currentPos.getLongitude(), nextPos.getLongitude(), steps);

            // Обновляем координаты текущей точки
            currentPos = new GeoPosition(interpolatedLat, interpolatedLon);

            track.set(0, currentPos);
            track.set(1, nextPos);

            setPainters(currentPos);

            if (steps > 1) steps--;

            if (steps <= 1) {
                track.remove(0);
                if(track.size() > 1)
                    steps = (int) calculateTime(calculateDistance(track.get(0), track.get(1)), velocity);
            }
        }
    }

    // Функция для линейной интерполяции между start и end с использованием шага и общего количества шагов
    private double interpolate(double start, double end, int steps) {
        return start + (end - start) * ((double) 1 / steps);
    }

    public static double calculateDistance(GeoPosition point1, GeoPosition point2) {
        int earthRadius = 6371; // Earth radius in kilometers

        double lat1 = Math.toRadians(point1.getLatitude());
        double lon1 = Math.toRadians(point1.getLongitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double lon2 = Math.toRadians(point2.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.pow(Math.sin(dlat / 2), 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    public static double calculateTime(double distance, double speed) {
        return (distance / speed) * 100000;
    }
}
