package project.NIR.Models.Hundlers;

import lombok.Getter;
import project.NIR.Models.Drones.DroneData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

@Getter
public class DroneHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectInputStream packageStream;

    public DroneHandler(Socket socket, ObjectInputStream packageStream) {
        this.clientSocket = socket;
        this.packageStream = packageStream;
    }

    @Override
    public void run() {
        try {
            while (true) {
                DroneData data = (DroneData) getPackageStream().readObject();
                System.out.println("Получено от дрона: " + "ID:" + data.getId() + " COORDS:" + data.getLatitude() + "," + data.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}