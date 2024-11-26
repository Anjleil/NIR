package project.NIR;

import project.NIR.Models.Hundlers.ClientHandler;
import project.NIR.Models.Hundlers.DroneHandler;
import project.NIR.Models.ServerData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class CommandCenter {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        App.run();
        startServer();
    }

    private static void startServer() {
        try {
            ServerSocket server = new ServerSocket(PORT);
            System.out.println("Сервер запущен!");
            while (true) {
                Socket clientSocket = server.accept();
                System.out.print("Новое подключение: ");

                ObjectInputStream packageStream = new ObjectInputStream(clientSocket.getInputStream());
                ServerData serverData = (ServerData) packageStream.readObject();

                if ("DRONE".equals(serverData.getType())) {
                    System.out.println("Подключен дрон");
                    Thread thread = new Thread(new DroneHandler(clientSocket, packageStream));
                    thread.start();
                } else if ("CLIENT".equals(serverData.getType())) {
                    System.out.println("Подключен клиент");
                    Thread thread = new Thread(new ClientHandler(clientSocket, packageStream));
                    thread.start();
                } else {
                    System.out.println("Неизвестный тип клиента");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}


