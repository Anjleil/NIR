package project.NIR.Models.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import project.NIR.Models.GeographicalPoint;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirRoute extends Route {
    private double maxFlightAltitude;  // Максимальная высота полета для воздушного маршрута

    // Реализация абстрактного метода для добавления точки в воздушный маршрут
    @Override
    public void addWaypoint(GeographicalPoint waypoint) {
        // Логика добавления точки в воздушный маршрут
        // ...
    }

    // Реализация абстрактного метода для отображения информации о воздушном маршруте
    @Override
    public void displayRouteInfo() {
        // Логика отображения информации о воздушном маршруте
        // ...
    }

    // Дополнительные методы и свойства для воздушного маршрута
    // ...
}
