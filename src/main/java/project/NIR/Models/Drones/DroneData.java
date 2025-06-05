package project.NIR.Models.Drones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import project.NIR.Models.Data.Data;

@AllArgsConstructor
// @NoArgsConstructor // Removed to avoid ambiguity with AllArgsConstructor if all fields are set
@Getter
public class DroneData extends Data {

    private int id;
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private int batteryLevel;
    private int currentSegmentTargetIndex;

    @Override
    public String getType() {
        return "DRONE";
    }
}
