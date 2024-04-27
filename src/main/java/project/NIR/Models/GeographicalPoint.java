package project.NIR.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeographicalPoint {
    private double latitude;
    private double longitude;

    // Другие методы, если необходимо
    // ...
}
