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
public class GroundRoute extends Route {
    private int maxSpeed;  // Максимальная скорость для наземного маршрута


    // Реализация абстрактного метода для добавления точки в наземный маршрут
    @Override
    public void addWaypoint(GeographicalPoint waypoint) {
        // Логика добавления точки в наземный маршрут
        // ...
    }

    // Реализация абстрактного метода для отображения информации о наземном маршруте
    @Override
    public void displayRouteInfo() {
        // Логика отображения информации о наземном маршруте
        // ...
    }

    // Дополнительные методы и свойства для наземного маршрута
    // ...
}

