package project.NIR.Models.Panes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class InformationPane {

    public static JPanel createInformationPane(int width, int height) {

        JPanel infoPanel = new RoundedPanel();
        infoPanel.setLayout(new BorderLayout());

        // Левая часть (кнопки)
        JPanel leftPanel = new RoundedPanel();
        leftPanel.setLayout(new GridLayout(1, 1, 10, 10));
        leftPanel.setPreferredSize(new Dimension(300, 200));
        leftPanel.setBackground(new Color(230, 230, 230)); // Светлый фон

        JButton cancelAllMissionsButton = createButton("Отменить все миссии");
        leftPanel.add(cancelAllMissionsButton);

        // Правая часть (текстовая информация)
        JPanel rightPanel = new RoundedPanel();
        rightPanel.setLayout(new GridLayout(3, 1, 5, 5)); // Интервал между строками
        rightPanel.setPreferredSize(new Dimension(190, 180));
        rightPanel.setBackground(new Color(230, 230, 230));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel totalMissionsLabel = new JLabel("Активных миссий: 2");
        totalMissionsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel availableDronesLabel = new JLabel("Дронов свободно: 6");
        availableDronesLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel averageDeliveryTimeLabel = new JLabel("Среднее время доставки: 14 мин");
        averageDeliveryTimeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        rightPanel.add(totalMissionsLabel);
        rightPanel.add(availableDronesLabel);
        rightPanel.add(averageDeliveryTimeLabel);

        infoPanel.add(leftPanel, BorderLayout.WEST);
        infoPanel.add(rightPanel, BorderLayout.CENTER);

        // Добавляем кнопку для переключения панели
        JButton showDroneInfoButton = createButton("Информация о дроне");
        leftPanel.add(showDroneInfoButton);

        // Логика переключения панели
        showDroneInfoButton.addActionListener(e -> showDroneInfo(infoPanel));

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

    private static void showDroneInfo(JPanel infoPanel) {
        // Очищаем содержимое панели
        infoPanel.removeAll();

        // Левая часть (кнопки)
        JPanel leftPanel = new RoundedPanel();
        leftPanel.setLayout(new GridLayout(3, 1, 10, 10));
        leftPanel.setPreferredSize(new Dimension(300, 200));
        leftPanel.setBackground(new Color(230, 230, 230));

        JButton recallDroneButton = createButton("Отозвать дрон");
        JButton recalculateRouteButton = createButton("Пересчитать маршрут");
        JButton manualControlButton = createButton("Ручное управление");

        leftPanel.add(recallDroneButton);
        leftPanel.add(recalculateRouteButton);
        leftPanel.add(manualControlButton);

        // Правая часть (информация о дроне)
        JPanel rightPanel = new RoundedPanel();
        rightPanel.setLayout(new GridLayout(3, 2, 5, 5));
        rightPanel.setPreferredSize(new Dimension(190, 180));
        rightPanel.setBackground(new Color(230, 230, 230));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel droneIdLabel = new JLabel("ID дрона: 1");
        JLabel batteryStatusLabel = new JLabel("Заряд: 75%");
        JLabel payloadLabel = new JLabel("Груз: 2.5 кг");
        JLabel assignedMissionLabel = new JLabel("Миссия: Доставка посылки");
        JLabel altitudeLabel = new JLabel("Высота: 120 м");

        droneIdLabel.setFont(new Font("Arial", Font.BOLD, 16));
        batteryStatusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        payloadLabel.setFont(new Font("Arial", Font.BOLD, 16));
        assignedMissionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        altitudeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        rightPanel.add(droneIdLabel);
        rightPanel.add(batteryStatusLabel);
        rightPanel.add(payloadLabel);
        rightPanel.add(assignedMissionLabel);
        rightPanel.add(altitudeLabel);

        // Добавляем обновлённые панели в infoPanel
        infoPanel.add(leftPanel, BorderLayout.WEST);
        infoPanel.add(rightPanel, BorderLayout.CENTER);

        // Перерисовываем панель
        infoPanel.revalidate();
        infoPanel.repaint();
    }
}
