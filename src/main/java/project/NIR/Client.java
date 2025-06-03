package project.NIR;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.Data.ClientData;
import project.NIR.Models.Data.ServerData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Getter
@Setter
@NoArgsConstructor
public class Client {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GeoPosition delivery;

    public Client(GeoPosition delivery){
        setDelivery(delivery);
    }

    @SneakyThrows
    public void connectToServer(String host, int port) {
        try {
            setClientSocket(new Socket(host, port));
            setOut(new ObjectOutputStream(getClientSocket().getOutputStream()));
            getOut().flush();
            sendMessage();
            setIn(new ObjectInputStream(getClientSocket().getInputStream()));

            sendMessage();

            ServerData serverData;
            try {
                serverData = (ServerData) in.readObject();
                System.out.println(serverData.toString());
            } catch (ClassNotFoundException | IOException e) {
                System.out.println("Не удалось прочитать данные сервера");
            }
        } catch (IOException e){
            clientSocket.close();
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client(new GeoPosition(55.751425, 37.664654));
        client.connectToServer("localhost", 12345);
    }

    private ClientData createPackage(){
        return new ClientData(getDelivery());
    }

    private void sendMessage(){
        try {
            ClientData data = createPackage();
            getOut().writeObject(data);
            getOut().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}