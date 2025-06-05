package project.authorization;

import project.authorization.ui.NotificationManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class DeliveryPanel extends JPanel {
    private JComboBox<DeliveryType> deliveryTypeCombo;
    private JTextField addressField;
    private JLabel mapLabel;
    private Order currentOrder;
    private Timer droneTimer;
    private MainWindow mainWindow;
    private JTextField notesField;
    private NotificationManager notificationManager;

    // Dark Theme Color Palette (consistent with MainWindow)
    private static final Color PANEL_BACKGROUND = MainWindow.PANEL_BACKGROUND;
    private static final Color ACCENT_COLOR = MainWindow.ACCENT_COLOR;
    private static final Color TEXT_PRIMARY = MainWindow.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = MainWindow.TEXT_SECONDARY;
    private static final Color INPUT_LINE_COLOR = new Color(100, 100, 100);
    private static final Color INPUT_FOCUS_LINE_COLOR = ACCENT_COLOR;
    private static final Font DEFAULT_FONT = MainWindow.DEFAULT_FONT;
    private static final Font BOLD_FONT = MainWindow.BOLD_FONT;
    private static final Font BUTTON_FONT = MainWindow.BOLD_FONT;

    public DeliveryPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.notificationManager = mainWindow.getNotificationManager();
        setLayout(new BorderLayout(15, 15));
        setBackground(PANEL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(PANEL_BACKGROUND);
        contentPanel.setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(PANEL_BACKGROUND);
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1,1,1,1, MainWindow.TABLE_GRID_COLOR), // Use a subtle border for TitledBorder
                "Информация о доставке",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                BOLD_FONT, TEXT_SECONDARY)); // Title text color
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel typeLabel = new JLabel("Тип доставки:");
        styleFormLabel(typeLabel);
        formPanel.add(typeLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        deliveryTypeCombo = new JComboBox<>(DeliveryType.values());
        styleDarkComboBox(deliveryTypeCombo);
        formPanel.add(deliveryTypeCombo, gbc);
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel addressLabel = new JLabel("Адрес доставки:");
        styleFormLabel(addressLabel);
        formPanel.add(addressLabel, gbc);

        gbc.gridx = 1;
        addressField = new JTextField(25);
        styleDarkTextField(addressField, "Введите адрес");
        formPanel.add(addressField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel notesLabel = new JLabel("Примечания к заказу:");
        styleFormLabel(notesLabel);
        formPanel.add(notesLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        notesField = new JTextField(25);
        notesField.setName("notesField");
        styleDarkTextField(notesField, "Любые пожелания (необязательно)");
        formPanel.add(notesField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(PANEL_BACKGROUND);
        buttonPanel.setOpaque(false);
        JButton confirmButton = new JButton("Подтвердить доставку");
        styleDarkButton(confirmButton);
        confirmButton.addActionListener(e -> processOrderConfirmation());
        buttonPanel.add(confirmButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(25, 10, 10, 10);
        formPanel.add(buttonPanel, gbc);

        contentPanel.add(formPanel, BorderLayout.NORTH);

        mapLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(60, 63, 65)); // Darker map background
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(new Color(75, 78, 80)); // Grid lines
                for (int i = 0; i < getWidth(); i += 25) g2d.drawLine(i, 0, i, getHeight());
                for (int i = 0; i < getHeight(); i += 25) g2d.drawLine(0, i, getWidth(), i);
                int droneSize = 30;
                int x = (int) (getWidth() * 0.8);
                int y = (int) (getHeight() * 0.3 + Math.sin(System.currentTimeMillis() / 300.0) * 10);
                g2d.setColor(ACCENT_COLOR);
                g2d.fillOval(x - droneSize / 2, y - droneSize / 4, droneSize, droneSize / 2);
                g2d.setColor(TEXT_PRIMARY); // Drone propellers
                g2d.fillRect(x - droneSize, y - 2, droneSize * 2, 4);
                g2d.fillRect(x - 2, y - droneSize / 2, 4, droneSize);
                Color destinationColor = new Color(255, 64, 129); // Pinkish accent for destination
                g2d.setColor(new Color(destinationColor.getRed(), destinationColor.getGreen(), destinationColor.getBlue(), 200));
                g2d.fillOval(getWidth() / 2 - 10, getHeight() / 2 - 10, 20, 20);
                g2d.setColor(destinationColor);
                g2d.drawOval(getWidth() / 2 - 10, getHeight() / 2 - 10, 20, 20);
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 6}, 0));
                g2d.setColor(new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(), 180));
                g2d.drawLine(x, y, getWidth() / 2, getHeight() / 2);
                g2d.dispose();
            }
        };
        mapLabel.setPreferredSize(new Dimension(450, 350));
        mapLabel.setBorder(BorderFactory.createLineBorder(MainWindow.TABLE_GRID_COLOR, 1));
        mapLabel.setOpaque(true);

        droneTimer = new Timer(30, e -> mapLabel.repaint());
        droneTimer.start();

        JScrollPane mapScrollPane = new JScrollPane(mapLabel);
        mapScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mapScrollPane.getViewport().setBackground(PANEL_BACKGROUND);
        contentPanel.add(mapScrollPane, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    private void styleFormLabel(JLabel label) {
        label.setFont(DEFAULT_FONT);
        label.setForeground(TEXT_SECONDARY);
    }

    private void styleDarkTextField(JTextField field, String placeholder) {
        field.setFont(DEFAULT_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(PANEL_BACKGROUND);
        field.setCaretColor(TEXT_PRIMARY);
        field.setOpaque(false);

        UnderlineBorder defaultBorder = new UnderlineBorder(INPUT_LINE_COLOR, 1);
        UnderlineBorder focusBorder = new UnderlineBorder(INPUT_FOCUS_LINE_COLOR, 2);
        field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                defaultBorder,
                javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5) // Padding inside the underline
        ));

        if (field.getText().isEmpty() && placeholder != null) {
            field.setText(placeholder);
            field.setForeground(TEXT_SECONDARY);
        }

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        focusBorder,
                        javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                if (placeholder != null && field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        defaultBorder,
                        javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                if (placeholder != null && field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
            }
        });
    }

    private void styleDarkComboBox(JComboBox<?> combo) {
        combo.setFont(DEFAULT_FONT);
        combo.setBackground(PANEL_BACKGROUND); // Background of the combo box itself
        combo.setForeground(TEXT_PRIMARY);     // Text color
        combo.setBorder(BorderFactory.createCompoundBorder(
            new UnderlineBorder(INPUT_LINE_COLOR,1), // Underline similar to text fields
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        // Removing default arrow and border might require a custom UI
        // For now, this provides basic dark theme consistency
        ComboBoxRenderer renderer = new ComboBoxRenderer();
        renderer.setPreferredSize(new Dimension(renderer.getPreferredSize().width, 30)); // Adjust height
        combo.setRenderer(renderer);
    }

    private static class ComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBackground(isSelected ? ACCENT_COLOR.darker() : PANEL_BACKGROUND);
            setForeground(isSelected ? Color.WHITE : TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
            return this;
        }
    }

    private void styleDarkButton(JButton button) {
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT_COLOR);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        Color hoverColor = ACCENT_COLOR.brighter();
        Color originalColor = ACCENT_COLOR;
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
    }

    private void processOrderConfirmation() {
        if (addressField.getText().trim().isEmpty() || addressField.getText().equals("Введите адрес")) {
            notificationManager.show("Укажите адрес доставки.", NotificationManager.NotificationType.WARNING);
            return;
        }

        DeliveryType selectedType = (DeliveryType) deliveryTypeCombo.getSelectedItem();
        String address = addressField.getText().trim();
        String deliveryTypeStr = selectedType.toString(); // Or selectedType.name()
        String notes = (notesField != null) ? notesField.getText().trim() : "";

        // Call new method in MainWindow to handle order creation with CartService and OrderService
        boolean success = mainWindow.processOrderPlacement(address, deliveryTypeStr, notes);

        if (success) {
            notificationManager.show("Заказ успешно создан и отправлен в обработку!", NotificationManager.NotificationType.SUCCESS);
            // Clear fields
            addressField.setText("");
            if (notesField != null) notesField.setText("");
             // Reset placeholder for addressField
            if (addressField.getText().isEmpty()) { // Check placeholder logic needs styleDarkTextField details
                addressField.setText("Введите адрес"); // Assuming this is the placeholder
                addressField.setForeground(TEXT_SECONDARY); // Reset color if needed
            }
            if (notesField != null && notesField.getText().isEmpty()) {
                 notesField.setText("Любые пожелания (необязательно)");
                 notesField.setForeground(TEXT_SECONDARY);
            }
            deliveryTypeCombo.setSelectedIndex(0);

            // Optionally, switch to history or products tab
            // mainWindow.switchToHistoryPanel(); // Example, if such method exists

        } else {
            // Error notification is now handled by mainWindow.processOrderPlacement, so no extra message is needed here.
            // notificationManager.show("Не удалось оформить заказ. Пожалуйста, проверьте корзину и попробуйте снова.", NotificationManager.NotificationType.ERROR);
        }
    }

    private String formatColorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    // Helper to find and add notesField if it was missed during snippet application
    private void ensureNotesFieldPresent(JPanel formPanel, GridBagConstraints gbcDefaults) {
        boolean notesFieldExists = false;
        for (Component comp : formPanel.getComponents()) {
            if (comp instanceof JTextField && ((JTextField)comp).getName() != null && ((JTextField)comp).getName().equals("notesField")) {
                notesFieldExists = true;
                break;
            }
        }
        if (!notesFieldExists) {
            GridBagConstraints gbc = (GridBagConstraints) gbcDefaults.clone(); // Use a copy
            gbc.gridx = 0; gbc.gridy = 2; // Example position, adjust if form has more fields
            gbc.gridwidth = 1; gbc.weightx = 0;
            JLabel notesLabel = new JLabel("Примечания к заказу:");
            styleFormLabel(notesLabel);
            formPanel.add(notesLabel, gbc);

            gbc.gridx = 1; gbc.weightx = 1.0;
            notesField = new JTextField(25);
            notesField.setName("notesField"); // For identification
            styleDarkTextField(notesField, "Любые пожелания (необязательно)");
            formPanel.add(notesField, gbc);
            formPanel.revalidate();
            formPanel.repaint();
        }
    }
}