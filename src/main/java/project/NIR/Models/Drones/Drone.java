package project.NIR.Models.Drones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.NIR.Models.Routes.Route;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class Drone implements Runnable {
    private int id;
    private double currentLatitude;
    private double currentLongitude;
    private double altitude;
    private Route route;

    public abstract void move(double newLatitude, double newLongitude, double newAltitude);


    //==================================== СЕРВЕРНАЯ ЧАСТЬ ===========================================================//
    private Socket droneSocket;
    private BufferedReader in;
    private BufferedWriter out;
    private ObjectOutputStream outObj;
    @Override
    public void run() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String serverWord = in.readLine();
                    if (serverWord == null) {
                        System.out.println("Сервер разорвал соединение");
                        break;
                    }
                    System.out.println("Дрон " + getId() + " получил сообщение от сервера: " + serverWord);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    droneSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void connectToServer(String host, int port) {
        try {
            setDroneSocket(new Socket(host, port));
            setOutObj(new ObjectOutputStream(getDroneSocket().getOutputStream()));
            sendMessage();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setCurrentLatitude(Math.random()-0.5);
                    setCurrentLongitude(Math.random()-0.5);

                    sendMessage();
                }
            }, 1000, 5000);

        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public DroneData createPackage(){
        return new DroneData(getId(), getCurrentLatitude(), getCurrentLongitude(), getAltitude(), 0, 0);
    }

    private void sendMessage(){
        try {
            DroneData data = createPackage();
            getOutObj().writeObject(data);
            getOutObj().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
