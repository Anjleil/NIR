package project.authorization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;

public class ProfilePanel extends JPanel {

    private JLabel usernameLabel, emailLabel, nameLabel, joinedLabel;
    private JButton logoutButton;
    private MainWindow mainWindowRef; // Reference to MainWindow for restarting login

    public ProfilePanel(MainWindow mainWindow) {
        this.mainWindowRef = mainWindow;
        setLayout(new GridBagLayout());
        setBackground(MainWindow.PANEL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel titleLabel = new JLabel("Профиль пользователя");
        titleLabel.setFont(MainWindow.TAB_FONT.deriveFont(24f)); // Larger title font
        titleLabel.setForeground(MainWindow.TEXT_PRIMARY);
        add(titleLabel, gbc);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 0, 5, 0); // Adjust spacing for fields

        usernameLabel = createInfoLabel("Имя пользователя:");
        emailLabel = createInfoLabel("Email:");
        nameLabel = createInfoLabel("Полное имя:");
        joinedLabel = createInfoLabel("Дата регистрации:");

        addFormField(this, gbc, "Имя пользователя:", usernameLabel);
        addFormField(this, gbc, "Email:", emailLabel);
        addFormField(this, gbc, "Полное имя:", nameLabel);
        addFormField(this, gbc, "Дата регистрации:", joinedLabel);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(30, 0, 0, 0);
        logoutButton = new JButton("Выйти из системы");
        styleLogoutButton(logoutButton);
        logoutButton.addActionListener(this::performLogout);
        add(logoutButton, gbc);

        loadUserProfile();
    }

    private JLabel createInfoLabel(String initialText) {
        JLabel label = new JLabel(initialText);
        label.setFont(MainWindow.DEFAULT_FONT);
        label.setForeground(MainWindow.TEXT_PRIMARY);
        return label;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String labelText, JLabel valueLabel) {
        JLabel descriptiveLabel = new JLabel(labelText);
        descriptiveLabel.setFont(MainWindow.BOLD_FONT);
        descriptiveLabel.setForeground(MainWindow.TEXT_SECONDARY);
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        panel.add(descriptiveLabel, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.7;
        panel.add(valueLabel, gbc);
    }

    private void loadUserProfile() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn()) {
            User user = session.getCurrentUser();
            usernameLabel.setText(user.getUsername());
            emailLabel.setText(user.getEmail());
            nameLabel.setText(user.getFirstName() + " " + user.getLastName());
            if (user.getCreatedAt() != null) {
                joinedLabel.setText(new SimpleDateFormat("dd MMMM yyyy г. HH:mm").format(user.getCreatedAt()));
            } else {
                joinedLabel.setText("N/A");
            }
        } else {
            // Should not happen if panel is only shown to logged-in users
            usernameLabel.setText("N/A");
            emailLabel.setText("N/A");
            nameLabel.setText("N/A");
            joinedLabel.setText("N/A");
        }
    }

    private void styleLogoutButton(JButton button) {
        button.setFont(MainWindow.BOLD_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(220, 53, 69)); // A shade of red for logout
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        Color hoverColor = new Color(200, 43, 59);
        Color originalColor = button.getBackground();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
    }

    private void performLogout(ActionEvent e) {
        UserSession.getInstance().clearSession();
        mainWindowRef.dispose(); // Close current main window
        SwingUtilities.invokeLater(() -> new DroneLoginWindow().setVisible(true)); // Show login window
    }
} 