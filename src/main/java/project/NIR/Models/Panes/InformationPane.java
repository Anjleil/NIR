package project.NIR.Models.Panes;

import project.NIR.Models.Data.ActiveMission;
import project.NIR.Models.Data.SharedData;
import project.NIR.Models.MapModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.stream.Collectors;

public class InformationPane extends RoundedPanel {
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    private static final String GENERAL_INFO_PANEL = "GENERAL_INFO_PANEL";
    private static final String DRONE_INFO_PANEL = "DRONE_INFO_PANEL";

    // Components for General Info
    private final JLabel totalMissionsLabel;
    private final JLabel availableDronesLabel;
    private final JLabel averageDeliveryTimeLabel;

    // Components for Drone Info
    private final JLabel droneIdLabel;
    private final JLabel batteryStatusLabel;
    private final JLabel payloadLabel;
    private final JLabel assignedMissionLabel;
    private final JLabel altitudeLabel;

    public InformationPane() {
        this.cardLayout = new CardLayout();
        this.cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        
        Font font = new Font("Arial", Font.BOLD, 16);

        totalMissionsLabel = new JLabel();
        totalMissionsLabel.setFont(font);
        availableDronesLabel = new JLabel();
        availableDronesLabel.setFont(font);
        averageDeliveryTimeLabel = new JLabel();
        averageDeliveryTimeLabel.setFont(font);
        
        droneIdLabel = new JLabel();
        droneIdLabel.setFont(font);
        batteryStatusLabel = new JLabel();
        batteryStatusLabel.setFont(font);
        payloadLabel = new JLabel();
        payloadLabel.setFont(font);
        assignedMissionLabel = new JLabel();
        assignedMissionLabel.setFont(font);
        altitudeLabel = new JLabel();
        altitudeLabel.setFont(font);

        cardPanel.add(createGeneralInfoPanel(), GENERAL_INFO_PANEL);
        cardPanel.add(createDroneInfoPanel(), DRONE_INFO_PANEL);

        this.setLayout(new BorderLayout());
        this.add(cardPanel, BorderLayout.CENTER);
        this.setBackground(new Color(230, 230, 230));

        showGeneralInfo();
    }

    public void showGeneralInfo() {
        updateGeneralInfo();
        cardLayout.show(cardPanel, GENERAL_INFO_PANEL);
    }

    public void showDroneInfo(ActiveMission mission) {
        if (mission == null) {
            showGeneralInfo();
            return;
        }
        droneIdLabel.setText("ID дрона: " + mission.getDroneId());
        batteryStatusLabel.setText(String.format("Заряд: %.0f%%", mission.getBatteryLevel()));
        payloadLabel.setText("Статус: " + (mission.isAssigned() ? "На миссии" : "Свободен"));
        assignedMissionLabel.setText("Цель: " + (mission.isReturning() ? "Возврат на базу" : "Доставка"));
        altitudeLabel.setText("Высота: 100 м"); // Placeholder

        cardLayout.show(cardPanel, DRONE_INFO_PANEL);
    }

    public void updateGeneralInfo() {
        long activeMissions = SharedData.getAllActiveMissions().stream().filter(ActiveMission::isAssigned).count();
        long totalDrones = SharedData.getAllActiveMissions().stream().filter(m -> m.getDroneId() > 0).count();
        long availableDrones = totalDrones - activeMissions;

        totalMissionsLabel.setText("Активных миссий: " + activeMissions);
        availableDronesLabel.setText("Дронов свободно: " + availableDrones);
        averageDeliveryTimeLabel.setText("Среднее время доставки: 14 мин");
    }


    private JPanel createGeneralInfoPanel() {
        JPanel infoPanel = new RoundedPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setOpaque(false);

        // Left part (buttons)
        JPanel leftPanel = new RoundedPanel();
        leftPanel.setLayout(new GridLayout(1, 1, 10, 10));
        leftPanel.setPreferredSize(new Dimension(300, 200));
        leftPanel.setBackground(new Color(230, 230, 230));

        JButton cancelAllMissionsButton = createButton("Отменить все миссии");
        cancelAllMissionsButton.addActionListener(e -> {
            SharedData.getAllActiveMissions().stream()
                .filter(ActiveMission::isAssigned)
                .forEach(mission -> SharedData.recallDrone(mission.getDroneId(), true));
        });
        leftPanel.add(cancelAllMissionsButton);

        // Right part (text info)
        JPanel rightPanel = new RoundedPanel();
        rightPanel.setLayout(new GridLayout(3, 1, 5, 5));
        rightPanel.setPreferredSize(new Dimension(190, 180));
        rightPanel.setBackground(new Color(230, 230, 230));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        rightPanel.add(totalMissionsLabel);
        rightPanel.add(availableDronesLabel);
        rightPanel.add(averageDeliveryTimeLabel);

        infoPanel.add(leftPanel, BorderLayout.WEST);
        infoPanel.add(rightPanel, BorderLayout.CENTER);
        
        return infoPanel;
    }

    private JPanel createDroneInfoPanel() {
        JPanel infoPanel = new RoundedPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setOpaque(false);

        // Left part (buttons)
        JPanel leftPanel = new RoundedPanel();
        leftPanel.setLayout(new GridLayout(3, 1, 10, 10));
        leftPanel.setPreferredSize(new Dimension(300, 200));
        leftPanel.setBackground(new Color(230, 230, 230));

        JButton recallDroneButton = createButton("Отозвать дрон");
        recallDroneButton.addActionListener(e -> {
            Integer selectedDroneId = MapModel.getSelectedDroneId();
            if (selectedDroneId != null) {
                SharedData.recallDrone(selectedDroneId, true);
            }
        });

        JButton recalculateRouteButton = createButton("Пересчитать маршрут");
        JButton manualControlButton = createButton("Ручное управление");

        leftPanel.add(recallDroneButton);
        leftPanel.add(recalculateRouteButton);
        leftPanel.add(manualControlButton);

        // Right part (drone info)
        JPanel rightPanel = new RoundedPanel();
        rightPanel.setLayout(new GridLayout(5, 1, 5, 5));
        rightPanel.setPreferredSize(new Dimension(190, 180));
        rightPanel.setBackground(new Color(230, 230, 230));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        rightPanel.add(droneIdLabel);
        rightPanel.add(batteryStatusLabel);
        rightPanel.add(payloadLabel);
        rightPanel.add(assignedMissionLabel);
        rightPanel.add(altitudeLabel);

        infoPanel.add(leftPanel, BorderLayout.WEST);
        infoPanel.add(rightPanel, BorderLayout.CENTER);
        return infoPanel;
    }


    private static JButton createButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(220, 220, 220) : new Color(230, 230, 230));
                if (getModel().isPressed()) {
                    g2.setColor(new Color(200, 200, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
            }
        };
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        return button;
    }
}
