package project.NIR.Models.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jxmapviewer.viewer.GeoPosition;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroundRoute extends Route {
    private int maxSpeed;  // Максимальная скорость для наземного маршрута

    @Override
    public void addWaypoint(GeoPosition waypoint) {

    }

    // Реализация абстрактного метода для отображения информации о наземном маршруте
    @Override
    public void displayRouteInfo() {
        // Логика отображения информации о наземном маршруте
        // ...
    }

}

