package project.NIR.Models.Drones;

public class AirDrone extends Drone {
    public AirDrone(int id) {
        super();
        this.setId(id);
    }

    @Override
    public void move(double newLatitude, double newLongitude, double newAltitude) {
        this.setCurrentLatitude(newLatitude);
        this.setCurrentLongitude(newLongitude);
        this.setAltitude(newAltitude);
    }

    // run() method removed as Drone base class no longer implements Runnable
    // and TestConnection calls connectToServer directly within a new Thread.
    /* 
    @Override
    public void run() {
        // This was not being used by TestConnection.
    }
    */
}
