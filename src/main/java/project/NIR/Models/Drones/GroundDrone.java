package project.NIR.Models.Drones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroundDrone extends Drone {
    private double maxSpeed;

    // Реализация абстрактного метода для перемещения наземного дрона
    @Override
    public void move(double newLatitude, double newLongitude, double newAltitude) {
        // Логика перемещения наземного дрона
        // ...
    }

    // @Override
    // public void run() {

    // }

    // Дополнительные методы и свойства для наземного дрона
    // ...
}
