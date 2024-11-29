package project.Pathfind;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        GeometryFactory factory = new GeometryFactory();
        AdaptiveGrid grid = new AdaptiveGrid();

        // Точка базы
        Point basePoint = factory.createPoint(new Coordinate(37.637326, 55.763979));

        // Задаем бесполетные зоны сложной формы
        List<NoFlyZone> noFlyZones = new ArrayList<>();

        // Пример произвольных точек для полигона бесполетной зоны
        List<Coordinate> zone1Points = List.of(
                new Coordinate(37.635, 55.760),
                new Coordinate(37.640, 55.765),
                new Coordinate(37.645, 55.760),
                new Coordinate(37.635, 55.760) // Замыкаем контур
        );
        noFlyZones.add(new NoFlyZone(zone1Points, factory));

        // Создаем сетку вокруг базы
////        Set<Envelope> gridCells = grid.createRadialGrid(basePoint, 10000, noFlyZones);
        Set<Envelope> gridCells = grid.createGridAroundPoint(basePoint, 1000, noFlyZones);
//        gridCells.addAll(gridCellsNormal);

        // Вывод информации о созданных ячейках
        System.out.println("Generated cells: " + gridCells.size());
//        for (Envelope cell : gridCells) {
//            System.out.println(cell);
//        }
    }
}
