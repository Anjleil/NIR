package project.NIR.Models.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.NIR.Models.GeographicalPoint;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Route {
    protected List<GeographicalPoint> waypoints;

    // Абстрактный метод для добавления точки в маршрут
    public abstract void addWaypoint(GeographicalPoint waypoint);

    // Абстрактный метод для отображения информации о маршруте
    public abstract void displayRouteInfo();

    // Другие общие методы для всех маршрутов
    // ...
}

