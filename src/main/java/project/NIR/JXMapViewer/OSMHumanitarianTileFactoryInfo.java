package project.NIR.JXMapViewer;

import org.jxmapviewer.viewer.TileFactoryInfo;

/**
 * Uses OpenStreetMap Humanitarian Layer
 */
public class OSMHumanitarianTileFactoryInfo extends TileFactoryInfo {
    private static final int MAX_ZOOM = 20;

    /**
     * Default constructor for Humanitarian Layer
     */
    public OSMHumanitarianTileFactoryInfo() {
        this("OpenStreetMap Humanitarian", "http://tile.openstreetmap.fr/hot");
    }

    /**
     * @param name the name of the factory
     * @param baseURL the base URL to load tiles from
     */
    public OSMHumanitarianTileFactoryInfo(String name, String baseURL) {
        super(name,
                0, MAX_ZOOM, MAX_ZOOM,
                256, true, true,                     // tile size is 256 and x/y orientation is normal
                baseURL,
                "x", "y", "z");                        // URL format: /{z}/{x}/{y}.png
    }

    @Override
    public String getTileUrl(int x, int y, int zoom) {
        int invZoom = MAX_ZOOM - zoom;
        String url = this.baseURL + "/" + invZoom + "/" + x + "/" + y + ".png";
        return url;
    }

    @Override
    public String getAttribution() {
        return "\u00A9 OpenStreetMap contributors, Humanitarian OpenStreetMap Team";
    }

    @Override
    public String getLicense() {
        return "Creative Commons Attribution-ShareAlike 2.0";
    }
}
