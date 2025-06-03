package project.NIR.Models;

import lombok.Getter;
import org.jxmapviewer.viewer.GeoPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class Warehouse {
    private final int id;
    private final String name;
    private final GeoPosition location;
    // Using a thread-safe list for drone IDs as they might be modified concurrently
    // when drones are assigned or returned.
    private final List<Integer> residentDroneIds = new CopyOnWriteArrayList<>();
    private static int nextId = 1;

    public Warehouse(String name, GeoPosition location) {
        this.id = nextId++;
        this.name = name;
        this.location = location;
    }

    public void addDrone(int droneId) {
        this.residentDroneIds.add(droneId);
    }

    public void removeDrone(int droneId) {
        this.residentDroneIds.remove(Integer.valueOf(droneId));
    }

    public List<Integer> getResidentDroneIdsView() {
        // Return an unmodifiable view if direct modification from outside is not intended
        // For now, returning the list directly for easier modification if SharedData handles it.
        return residentDroneIds;
    }

    @Override
    public String toString() {
        return "Warehouse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", residentDroneIds=" + residentDroneIds.size() +
                '}';
    }
} 