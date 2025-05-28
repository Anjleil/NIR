package project.NIR.Models.Drones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.NIR.Models.Data.Data;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DroneData extends Data {

    private int id;
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private int batteryLevel;

    @Override
    public String getType() {
        return "DRONE";
    }
}
