package project.NIR.Models.Drones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirDrone extends Drone {
    private double maxFlightAltitude;

    // Реализация абстрактного метода для перемещения воздушного дрона
    @Override
    public void move(double newLatitude, double newLongitude, double newAltitude) {
        // Логика перемещения воздушного дрона
        // ...
    }

    // Дополнительные методы и свойства для воздушного дрона
    // ...
}
