package project.NIR.Models.Waypoints;

import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import project.NIR.Models.MapModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class DroneWaypoint extends DefaultWaypoint {
    private final JButton button;
    private final int droneId;
    private final ImageIcon originalIcon;

    public DroneWaypoint(int droneId, GeoPosition coord) {
        super(coord);
        this.droneId = droneId;

        this.originalIcon = new ImageIcon(getClass().getResource("/images/drones.png"));
        
        int w = originalIcon.getIconWidth();
        int h = originalIcon.getIconHeight();
        int size = (int) Math.ceil(Math.sqrt(w * w + h * h));

        this.button = new JButton(originalIcon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setSize(size, size);
        button.setPreferredSize(new Dimension(size, size));

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
                e.consume();
            }
        });
        button.setVisible(true);
    }

    public JButton getButton() {
        return button;
    }

    public void setRotationAngle(double newAngle) {
        if (originalIcon.getImage() == null) return;
        
        int w = originalIcon.getIconWidth();
        int h = originalIcon.getIconHeight();
        int size = (int) Math.ceil(Math.sqrt(w * w + h * h));
        
        BufferedImage rotatedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotatedImage.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double centerX = size / 2.0;
        double centerY = size / 2.0;

        AffineTransform tx = AffineTransform.getRotateInstance(newAngle, centerX, centerY);
        tx.translate(centerX - w / 2.0, centerY - h / 2.0);

        g2d.drawImage(originalIcon.getImage(), tx, null);
        g2d.dispose();
        
        button.setIcon(new ImageIcon(rotatedImage));
    }
} 