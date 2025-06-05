package project.NIR.Models;

import project.NIR.Models.Data.ActiveMission;
import project.NIR.Models.Data.SharedData;
import project.NIR.Models.Panes.InformationPane;
import project.NIR.Models.Panes.MapViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;

public class MapModel {
    private static MapViewer mapViewer;
    private static InformationPane infoPane;
    private static Integer selectedDroneId = null;
    private static boolean manualControlActive = false;

    public static JLayeredPane createPane() throws IOException {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1200, 1000));

        infoPane = new InformationPane();
        JPanel infoPanel = infoPane;

        mapViewer = new MapViewer();
        JPanel mapPanel = mapViewer.getMapViewer();

        mapPanel.setBounds(0, 0, layeredPane.getPreferredSize().width, layeredPane.getPreferredSize().height);
        layeredPane.add(mapPanel, JLayeredPane.DEFAULT_LAYER);

        infoPanel.setBounds(50, layeredPane.getPreferredSize().height - 200, layeredPane.getPreferredSize().width - 200, 160);
        layeredPane.add(infoPanel, JLayeredPane.PALETTE_LAYER);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mapPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                infoPanel.setBounds(50, layeredPane.getHeight() - 160, layeredPane.getWidth() - 100, 160);
                infoPanel.setBackground(new Color(230, 230, 230));
            }
        });

        return layeredPane;
    }

    public static MapViewer getMapViewer() {
        return mapViewer;
    }

    public static Integer getSelectedDroneId() {
        return selectedDroneId;
    }

    public static void setSelectedDroneId(Integer droneId) {
        if (selectedDroneId != null && !selectedDroneId.equals(droneId) && manualControlActive) {
            toggleManualControl();
        }
        if (droneId == null && manualControlActive) {
            toggleManualControl();
        }

        selectedDroneId = droneId;

        if (infoPane != null) {
            if (selectedDroneId != null) {
                ActiveMission mission = SharedData.getActiveMissionByDroneId(selectedDroneId);
                infoPane.showDroneInfo(mission);
            } else {
                infoPane.showGeneralInfo();
            }
        }

        if (mapViewer != null) {
            mapViewer.updateMapDisplay();
        }
    }
    
    public static void updateInfoPane() {
        if (infoPane != null) {
            infoPane.update();
        }
    }

    public static boolean isManualControlActive() {
        return manualControlActive;
    }

    public static void toggleManualControl() {
        if (selectedDroneId == null) {
            manualControlActive = false;
        } else {
            manualControlActive = !manualControlActive;
        }
        if (infoPane != null) {
            infoPane.updateDroneInfoPanelAppearance();
        }
    }
}
