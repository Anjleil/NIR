package project.NIR.Models.Drones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public abstract class Drone {
    private String id;
    private double currentLatitude;
    private double currentLongitude;
    private double altitude;


    // Абстрактный метод для перемещения дрона
    public abstract void move(double newLatitude, double newLongitude, double newAltitude);

    // Геттеры и сеттеры для характеристик дрона созданы Lombok

}
