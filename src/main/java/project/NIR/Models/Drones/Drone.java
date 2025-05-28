package project.NIR.Models.Drones;

import lombok.*;
import project.NIR.Models.Routes.Route;
import project.NIR.Models.Data.ServerData;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class Drone implements Runnable {
    private int id;
    private double currentLatitude;
    private double currentLongitude;
    private double altitude;
    private Route route;

    public abstract void move(double newLatitude, double newLongitude, double newAltitude);


    //==================================== СЕРВЕРНАЯ ЧАСТЬ ===========================================================//
    private Socket droneSocket;
    private ObjectOutputStream out;
    private ObjectInput in;
//    @Override
//    public void run() {
//        Thread thread = new Thread(() -> {
//            try {
//                while (true) {
//                    String serverWord = in.readLine();
//                    if (serverWord == null) {
//                        System.out.println("Сервер разорвал соединение");
//                        break;
//                    }
//                    System.out.println("Дрон " + getId() + " получил сообщение от сервера: " + serverWord);
//                }
//            } catch (Exception e) {
//                try {
//                    droneSocket.close();
//                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        });
//        thread.start();
//    }

    @SneakyThrows
    public void connectToServer(String host, int port) {
        try {
            setDroneSocket(new Socket(host, port));
            setOut(new ObjectOutputStream(getDroneSocket().getOutputStream()));
            sendMessage();
            setIn(new ObjectInputStream(getDroneSocket().getInputStream()));

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setCurrentLatitude(Math.random()-0.5);
                    setCurrentLongitude(Math.random()-0.5);

                    sendMessage();

                    ServerData serverData;
                    try {
                        serverData = (ServerData) in.readObject();
                        System.out.println(serverData.toString());
                    } catch (ClassNotFoundException | IOException e) {
                        System.out.println("Не удалось прочитать данные сервера");
                    }
                }
            }, 1000, 5000);


        } catch (IOException e){
            droneSocket.close();
        }
    }


    public DroneData createPackage(){
        return new DroneData(getId(), getCurrentLatitude(), getCurrentLongitude(), getAltitude(), 0, 0);
    }

    private void sendMessage(){
        try {
            DroneData data = createPackage();
            getOut().writeObject(data);
            getOut().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
