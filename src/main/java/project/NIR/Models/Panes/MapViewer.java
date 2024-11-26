package project.NIR.Models.Panes;

import lombok.Getter;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import project.NIR.JXMapViewer.OSMHumanitarianTileFactoryInfo;
import project.NIR.JXMapViewer.PanMouseInputListener;

import javax.swing.event.MouseInputListener;
import java.io.File;

@Getter
public class MapViewer {
    private final JXMapViewer mapViewer;

    public MapViewer() {
        mapViewer = new JXMapViewer();

        GeoPosition Moscow = new GeoPosition(55.788845, 37.791609);
        addInteractions(Moscow);
    }

    public void addInteractions(GeoPosition startPos) {
        TileFactoryInfo info = new OSMHumanitarianTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));
        tileFactory.setThreadPoolSize(32);
        mapViewer.setAddressLocation(startPos);
        mapViewer.setZoom(6);

        File cacheDir = new File(System.getProperty("user.home") + File.separator + ".jxmapviewer2");
        tileFactory.setLocalCache(new FileBasedLocalCache(cacheDir, false));
    }
}
