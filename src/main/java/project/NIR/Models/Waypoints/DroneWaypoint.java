package project.NIR.Models.Waypoints;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.MapModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DroneWaypoint extends DefaultWaypoint {
    private final JButton button;
    private final int droneId;

    public DroneWaypoint(int droneId, GeoPosition coord) {
        super(coord);
        this.droneId = droneId;

        ImageIcon icon = new ImageIcon(getClass().getResource("/images/drones.png"));
        this.button = new JButton(icon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setSize(icon.getIconWidth(), icon.getIconHeight());
        button.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON3) {
                    return;
                }

                Integer currentSelected = MapModel.getSelectedDroneId();
                if (Integer.valueOf(droneId).equals(currentSelected)) {
                    MapModel.setSelectedDroneId(null); 
                } else {
                    MapModel.setSelectedDroneId(droneId);
                }
            }
        });
        button.setVisible(true);
    }

    public JButton getButton() {
        return button;
    }
} 