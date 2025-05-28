package project.NIR.Models.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ServerData extends Data {
    private int droneId;
    private int command;

    public ServerData(int command){
        this.command = command;
    }

    @Override
    public String getType() {
        return "SERVER";
    }
    @Override
    public String toString() {
        if (droneId != 0){
            return "Drone{" +
                    "ID=" + droneId +
                    ", command=" + command +
                    '}';
        }
        else return "Unknown";
    }

}
