package project.NIR.Models.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirRoute extends Route {
    private double maxFlightAltitude;

    @Override
    public void displayRouteInfo() {

    }
}
