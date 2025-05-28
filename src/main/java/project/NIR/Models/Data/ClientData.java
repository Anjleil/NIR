package project.NIR.Models.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jxmapviewer.viewer.GeoPosition;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ClientData extends Data {
    private GeoPosition delivery;

    @Override
    public String getType() {
        return "CLIENT";
    }
}
