package project.NIR.Models.Hundlers;

import lombok.Getter;
import project.NIR.Models.ClientData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

@Getter
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectInputStream packageStream;

    public ClientHandler(Socket socket, ObjectInputStream packageStream) {
        this.clientSocket = socket;
        this.packageStream = packageStream;
    }

    @Override
    public void run() {
        try {
            ClientData data = (ClientData) getPackageStream().readObject();
            System.out.println("Получено от клиента: \n" +
                    "departure: " + data.getDeparture().getLatitude() + ", " + data.getDeparture().getLongitude() + "\n" +
                    "delivery: " + data.getDelivery().getLatitude() + ", " + data.getDelivery().getLongitude());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
