package project.NIR;

import lombok.Getter;
import lombok.Setter;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.ClientData;

import java.io.*;
import java.net.Socket;

@Getter
@Setter
public class Client {
    private Socket clientSocket;
    private ObjectOutputStream outObj;
    private GeoPosition departure;
    private GeoPosition delivery;

    public void connectToServer(String serverAddress, int serverPort) throws IOException {
        this.clientSocket = new Socket(serverAddress, serverPort);
    }

    public void run() {
        try {
            setOutObj(new ObjectOutputStream(getClientSocket().getOutputStream()));
            sendMessage(); //Запрос на подключение

            sendMessage(); //Отправка данных
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.connectToServer("localhost", 12345);
        client.run();
    }

    private ClientData createPackage(){
        return new ClientData(getDeparture(), getDelivery());
    }

    private void sendMessage(){
        try {
            ClientData data = createPackage();
            getOutObj().writeObject(data);
            getOutObj().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}