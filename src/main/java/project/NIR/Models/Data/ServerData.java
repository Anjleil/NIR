package project.NIR.Models.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ServerData extends Data {
    private int droneId;
    private int command;
    private List<GeoPosition> assignedPath; // Optional: for sending a new path to the drone

    public ServerData(int command){
        this.command = command;
        this.assignedPath = null; 
        this.droneId = 0; // Explicitly set droneId to 0 for general commands/responses
    }

    public ServerData(int droneId, int command) {
        this.droneId = droneId;
        this.command = command;
        this.assignedPath = null; 
    }

    // The AllArgsConstructor will generate a constructor for (droneId, command, assignedPath)
    // So, the manual one below is redundant if all fields are meant to be in AllArgsConstructor.
    // public ServerData(int droneId, int command, List<GeoPosition> assignedPath) {
    //     this.droneId = droneId;
    //     this.command = command;
    //     this.assignedPath = assignedPath;
    // }

    @Override
    public String getType() {
        return "SERVER";
    }
    @Override
    public String toString() {
        String pathInfo = (assignedPath != null && !assignedPath.isEmpty()) ? ", pathAssigned=" + assignedPath.size() + "pts" : "";
        if (droneId != 0) { // Message specifically for a drone
            return "ServerData{" +
                    "droneId=" + droneId +
                    ", command=" + command +
                    pathInfo +
                    '}';
        } else { // General server message or response to client (droneId is 0)
            return "ServerData{" +
                    "command=" + command +
                    pathInfo +
                    '}';
        }
    }

}
