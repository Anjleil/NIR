import project.authorization.MainWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DroneLoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public DroneLoginWindow() {
        super("DroneExpress - Авторизация");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 20));

        // Создаем кастомную иконку дрона
        setIconImage(createDroneIcon());

        // Панель заголовка с логотипом
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Основная панель авторизации
        JPanel authPanel = createAuthPanel();
        add(authPanel, BorderLayout.CENTER);

        // Панель с дополнительной информацией
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Градиентный фон
                GradientPaint gradient = new GradientPaint(0, 0, new Color(255, 204, 0), 0, getHeight(), new Color(255, 223, 0));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Логотип дрона
                g2d.setColor(Color.BLACK);
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                // Корпус дрона
                g2d.fillOval(centerX - 40, centerY - 20, 80, 40);

                // Лопасти
                g2d.fillRect(centerX - 60, centerY - 5, 120, 10);
                g2d.fillRect(centerX - 5, centerY - 40, 10, 80);

                // Анимационные круги
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.drawOval(centerX - 60, centerY - 40, 120, 80);
                g2d.drawOval(centerX - 70, centerY - 50, 140, 100);
            }
        };
        panel.setPreferredSize(new Dimension(500, 150));

        // Текст логотипа
        JLabel logoLabel = new JLabel("DRONEXPRESS");
        logoLabel.setFont(new Font("Roboto", Font.BOLD, 28));
        logoLabel.setForeground(Color.BLACK);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.setLayout(new BorderLayout());
        panel.add(logoLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Стилизованные поля ввода
        JLabel loginLabel = new JLabel("Логин:");
        loginLabel.setFont(new Font("Roboto", Font.BOLD, 14));
        loginLabel.setForeground(new Color(50, 50, 50));
        panel.add(loginLabel, gbc);

        gbc.gridy++;
        usernameField = new JTextField(20);
        styleInputField(usernameField);
        panel.add(usernameField, gbc);

        gbc.gridy++;
        JLabel passLabel = new JLabel("Пароль:");
        passLabel.setFont(new Font("Roboto", Font.BOLD, 14));
        passLabel.setForeground(new Color(50, 50, 50));
        panel.add(passLabel, gbc);

        gbc.gridy++;
        passwordField = new JPasswordField(20);
        styleInputField(passwordField);
        panel.add(passwordField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(20, 20, 10, 20);
        JButton loginButton = new JButton("Войти в систему");
        styleLoginButton(loginButton);
        panel.add(loginButton, gbc);

        // Добавляем обработчик
        loginButton.addActionListener(e -> attemptLogin());

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Иконка дрона с анимацией
        JLabel droneIcon = new JLabel(new ImageIcon(createDroneImage(60, 40))) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Анимационный эффект
                long time = System.currentTimeMillis() / 20;
                int pulse = (int) (Math.abs(Math.sin(time * 0.01)) * 10);

                g2d.setColor(new Color(255, 204, 0, 50));
                g2d.fillOval(20 - pulse, 10 - pulse, 20 + pulse * 2, 20 + pulse * 2);
            }
        };

        JLabel infoLabel = new JLabel("<html><div style='text-align: center;'>"
                + "<b>Доставка будущего уже здесь!</b><br>"
                + "Быстрее. Выше. Точнее.</div></html>");
        infoLabel.setFont(new Font("Roboto", Font.PLAIN, 12));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(droneIcon, BorderLayout.WEST);
        panel.add(infoLabel, BorderLayout.CENTER);

        return panel;
    }

    private void styleInputField(JComponent field) {
        field.setFont(new Font("Roboto", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
        field.setBackground(new Color(250, 250, 250));
    }

    private void styleLoginButton(JButton button) {
        button.setFont(new Font("Roboto", Font.BOLD, 16));
        button.setForeground(Color.BLACK);
        button.setBackground(new Color(255, 204, 0));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 150, 0), 1),
                BorderFactory.createEmptyBorder(10, 25, 10, 25)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Эффект при наведении
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(255, 217, 61));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(255, 204, 0));
            }
        });
    }

    private Image createDroneIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Корпус дрона
        g2d.setColor(Color.BLACK);
        g2d.fillOval(8, 12, 16, 8);

        // Лопасти
        g2d.fillRect(4, 15, 24, 2);
        g2d.fillRect(15, 4, 2, 24);

        g2d.dispose();
        return image;
    }

    private Image createDroneImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Корпус дрона
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillOval(width/2 - width/3, height/2 - height/4, width/3*2, height/2);

        // Лопасти
        g2d.fillRect(width/2 - width/2, height/2 - 2, width, 4);
        g2d.fillRect(width/2 - 2, height/2 - height/2, 4, height);

        g2d.dispose();
        return image;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Здесь будет реальная проверка
        if (!username.isEmpty() && !password.isEmpty()) {
            dispose();
            new MainWindow().setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Пожалуйста, заполните все поля",
                    "Ошибка ввода",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            // Установка современного стиля
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            DroneLoginWindow window = new DroneLoginWindow();
            window.setVisible(true);
        });
    }
}