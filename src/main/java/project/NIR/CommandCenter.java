package project.NIR;

import project.NIR.Models.Data.Data;
import project.NIR.Models.Hundlers.ClientHandler;
import project.NIR.Models.Hundlers.DroneHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class CommandCenter {
    private static final int PORT = 12345;

    public static void main(String[] args) throws IOException {
        App.run();
        startServer();
    }

    private static void startServer() {
        ServerSocket server = null;
        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен!");
        } catch (IOException e) {
            System.out.println("Сервер не удалось запустить");
        }

        while (true) {
            Socket clientSocket;
            try {
                assert server != null;
                clientSocket = server.accept();
                System.out.print("Новое подключение: ");

                ObjectInputStream packageStream = new ObjectInputStream(clientSocket.getInputStream());
                Data serverData = (Data) packageStream.readObject();

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
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Неизвестная ошибка");
            }

        }
    }

}


