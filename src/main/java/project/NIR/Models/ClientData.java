package project.NIR.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jxmapviewer.viewer.GeoPosition;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ClientData extends ServerData {
    private GeoPosition departure;
    private GeoPosition delivery;

    @Override
    public String getType() {
        return "CLIENT";
    }
}
