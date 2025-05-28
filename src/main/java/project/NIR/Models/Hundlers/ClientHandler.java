package project.NIR.Models.Hundlers;

import lombok.Getter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import project.NIR.Models.Data.ClientData;
import project.NIR.Models.Data.ServerData;
import project.NIR.Models.Data.SharedData;
import project.NIR.Models.Routes.Path;
import project.NIR.Utils.Pathfinder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Getter
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectInputStream packageStream;
    private final ObjectOutputStream packageStreamOut;

    public ClientHandler(Socket socket, ObjectInputStream packageStream) throws IOException {
        this.clientSocket = socket;
        this.packageStream = packageStream;
        this.packageStreamOut = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        GeometryFactory factory = new GeometryFactory();
        Pathfinder pathfinder = new Pathfinder();
        try {
            ClientData data = (ClientData) getPackageStream().readObject();
            System.out.println("Получено от клиента: \n" +
//                    "departure: " + data.getDeparture().getLatitude() + ", " + data.getDeparture().getLongitude() + "\n" +
                    "{delivery: " + data.getDelivery().getLatitude() + ", " + data.getDelivery().getLongitude()+ "}");
            Path path = pathfinder.createPath(factory.createPoint(new Coordinate(data.getDelivery().getLatitude(), data.getDelivery().getLongitude())));
            SharedData.addPath(path);
            System.out.println(path.getPoints());
            sendMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ServerData createPackage(){
        return new ServerData(1);
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
