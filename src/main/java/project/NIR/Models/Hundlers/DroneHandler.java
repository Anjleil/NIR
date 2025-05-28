package project.NIR.Models.Hundlers;

import lombok.Getter;
import project.NIR.Models.Drones.DroneData;
import project.NIR.Models.Data.ServerData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Getter
public class DroneHandler implements Runnable {
    private int droneId;
    private final Socket clientSocket;
    private final ObjectInputStream packageStream;
    private final ObjectOutputStream packageStreamOut;

    public DroneHandler(Socket socket, ObjectInputStream packageStream) throws IOException {
        this.droneId = 0;
        this.clientSocket = socket;
        this.packageStream = packageStream;
        this.packageStreamOut = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        while (true) {
            try {
                DroneData data = (DroneData) getPackageStream().readObject();
                this.droneId = data.getId();
                System.out.println("Получено от дрона: " + " { \"ID\": \"" + data.getId() + "\", \"COORDS\": [" + data.getLatitude() + "," + data.getLongitude() + "]}");
                sendMessage();
            } catch (IOException | ClassNotFoundException e) {
                try {
                    packageStream.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public ServerData createPackage(){
        return new ServerData(getDroneId(), 1);
    }

    private void sendMessage(){
        try {
            ServerData data = createPackage();
            getPackageStreamOut().writeObject(data);
            getPackageStreamOut().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}