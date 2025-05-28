package project.NIR.JXMapViewer;

import org.jxmapviewer.viewer.TileFactoryInfo;

/**
 * Uses Geoapify Positron Tile Layer
 */
public class GeoapifyPositronTileFactoryInfo extends TileFactoryInfo {
    private static final int MAX_ZOOM = 20; // Максимальное приближение для Geoapify Positron
    private static final String API_KEY = "11f24b9115a94994b6f5758713e48509"; // Ваш API ключ
    private static final String BASE_URL = "https://maps.geoapify.com/v1/tile/positron"; // Корректный базовый URL

    /**
     * Default constructor for Geoapify Positron Layer
     */
    public GeoapifyPositronTileFactoryInfo() {
        this("Geoapify Positron", BASE_URL);
    }

    /**
     * @param name    the name of the factory
     * @param baseURL the base URL to load tiles from
     */
    public GeoapifyPositronTileFactoryInfo(String name, String baseURL) {
        super(name,
                0, MAX_ZOOM, MAX_ZOOM,
                256, true, true,                     // tile size is 256 and x/y orientation is normal
                baseURL,
                "x", "y", "z");                        // URL format: /{z}/{x}/{y}.png
    }

    @Override
    public String getTileUrl(int x, int y, int zoom) {
        int invZoom = MAX_ZOOM - zoom; // Преобразование zoom уровня
        String url = this.baseURL + "/" + invZoom + "/" + x + "/" + y + ".png?apiKey=" + API_KEY;
        return url;
    }

    @Override
    public String getAttribution() {
        return "\u00A9 Geoapify contributors, Positron Style";
    }

    @Override
    public String getLicense() {
        return "Geoapify Terms of Use: https://www.geoapify.com/terms-of-service";
    }
}
