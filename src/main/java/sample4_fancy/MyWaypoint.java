
package sample4_fancy;

import lombok.Getter;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;

/**
 * A waypoint that also has a color and a label
 * @author Martin Steiger
 */
@Getter
public class MyWaypoint extends DefaultWaypoint
{
    /**
     * -- GETTER --
     *
     * @return the label text
     */
    private final String label;
    /**
     * -- GETTER --
     *
     * @return the color
     */
    private final Color color;

    /**
     * @param label the text
     * @param color the color
     * @param coord the coordinate
     */
    public MyWaypoint(String label, Color color, GeoPosition coord)
    {
        super(coord);
        this.label = label;
        this.color = color;
    }


}
