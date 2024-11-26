package project.NIR;

import project.NIR.Models.Drones.AirDrone;

import java.io.IOException;

public class TestConnection {
    public static void main(String[] args) throws IOException {
        int numberOfProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("Количество доступных процессоров: " + numberOfProcessors);

        for(int i = 1; i < 5; i++){
            CreateDrone(i);
        }
    }

    private static void CreateDrone(int id) throws IOException {
        AirDrone drone = new AirDrone(id); // идентификатор дрона
        drone.connectToServer("localhost", 12345); // подключаемся к серверу
        drone.run(); // запускаем дрон
    }
}
