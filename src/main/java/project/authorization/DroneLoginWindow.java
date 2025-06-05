package project.authorization;

import project.authorization.ui.NotificationManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import project.authorization.db.DatabaseManager; // Import DatabaseManager
import javax.imageio.ImageIO; // Added for image loading
import java.io.IOException; // Added for image loading
import java.io.InputStream; // Added for image loading

public class DroneLoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel signUpLabel, signInLabel; // For Sign In / Sign Up tabs
    private AuthService authService; // Added AuthService instance
    private JPanel cardPanel; // To hold sign-in and sign-up panels
    private CardLayout cardLayout; // To switch between panels
    private JPanel signInPanel; // Panel for sign-in components
    private JPanel signUpPanel; // Panel for sign-up components
    private JTextField usernameSignUpField;
    private JTextField emailFieldSignUp; // Email field for sign-up
    private JPasswordField passwordFieldSignUp; // Password field for sign-up
    private JPasswordField confirmPasswordFieldSignUp; // Confirm password field for sign-up
    private JTextField firstNameSignUpField; // First name field for sign-up
    private JTextField lastNameSignUpField; // Last name field for sign-up
    private NotificationManager notificationManager;

    // Dark Theme Color Palette
    private static final Color WINDOW_BACKGROUND = new Color(45, 45, 45);
    // New semi-transparent background for content panels
    private static final Color PANEL_BACKGROUND_TRANSPARENT = new Color(55, 58, 60, 200); // Added alpha for transparency
    private static final Color PANEL_BACKGROUND = new Color(55, 58, 60); // Kept for opaque elements if needed, or remove if all are transparent
    private static final Color ACCENT_COLOR = new Color(242, 169, 0); // Orange accent
    private static final Color TEXT_PRIMARY = new Color(180, 180, 180); // Darker Off-white
    private static final Color TEXT_SECONDARY = new Color(120, 120, 120); // Darker Light gray
    private static final Color INPUT_LINE_COLOR = new Color(100, 100, 100);
    private static final Color INPUT_FOCUS_LINE_COLOR = ACCENT_COLOR;
    private static final Color BUTTON_TEXT_COLOR = Color.WHITE;

    private static final Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 18);
    private static final Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font TAB_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 16);

    private Image backgroundImage;
    private static final String BACKGROUND_IMAGE_PATH = "/images/login_background.jpg"; // Configurable path

    public DroneLoginWindow() {
        super("DroneExpress - Авторизация");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 650); // Adjusted size for a potentially taller look with image
        setLocationRelativeTo(null);
        // Removed setUndecorated(false) to keep standard window controls
        // getContentPane().setBackground(WINDOW_BACKGROUND); // Will be handled by BackgroundPanel

        loadBackgroundImage();

        // Create the JLayeredPane to hold everything
        JLayeredPane layeredPane = new JLayeredPane();
        setContentPane(layeredPane);

        // The NotificationManager will manage the top layer
        notificationManager = new NotificationManager(layeredPane);

        // Background panel will be on the bottom layer
        BackgroundPanel backgroundPanel = new BackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        // We need to size the background panel manually since JLayeredPane has no layout manager
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                backgroundPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            }
        });

        // Main content panel that will hold cardPanel and tabs
        JPanel mainPanelContainer = new JPanel(new BorderLayout(0,0));
        // mainPanelContainer.setBackground(WINDOW_BACKGROUND); // Now transparent
        mainPanelContainer.setOpaque(false);
        mainPanelContainer.setBorder(BorderFactory.createEmptyBorder(30, 40, 40, 40)); // Overall padding

        // Tab-like Sign In / Sign Up
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0)); // Centered tabs
        // tabPanel.setBackground(WINDOW_BACKGROUND); // Now transparent
        tabPanel.setOpaque(false);
        signInLabel = new JLabel("ВХОД");
        styleTabLabel(signInLabel, true); 
        tabPanel.add(signInLabel);

        signUpLabel = new JLabel("РЕГИСТРАЦИЯ");
        styleTabLabel(signUpLabel, false);
        tabPanel.add(signUpLabel);
        mainPanelContainer.add(tabPanel, BorderLayout.NORTH);
        
        // CardPanel for switching between Sign In and Sign Up views
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        // cardPanel.setBackground(PANEL_BACKGROUND); // Will use transparent background
        cardPanel.setOpaque(false); 
        // cardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding within card area

        // Create Sign In Panel
        signInPanel = createSignInPanel();
        cardPanel.add(signInPanel, "Вход");

        // Create Sign Up Panel (will include email)
        signUpPanel = createSignUpPanel();
        cardPanel.add(signUpPanel, "Регистрация");
        
        mainPanelContainer.add(cardPanel, BorderLayout.CENTER);
        // add(mainPanelContainer, BorderLayout.CENTER); // Added to backgroundPanel instead
        backgroundPanel.add(mainPanelContainer, BorderLayout.CENTER);
        
        // Set default pre-filled credentials for sign-in form
        usernameField.setText("testuser");
        passwordField.setText("password123");
        // Adjust focus away from prefilled password to avoid immediate un-placeholder
        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());

        // Event Handlers for tabs
        signInLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                styleTabLabel(signInLabel, true);
                styleTabLabel(signUpLabel, false);
                cardLayout.show(cardPanel, "Вход");
            }
        });
        signUpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                styleTabLabel(signInLabel, false);
                styleTabLabel(signUpLabel, true);
                cardLayout.show(cardPanel, "Регистрация");
            }
        });

        authService = new AuthService(); 
        // Login button action is set within createSignInPanel
        // Sign Up button action will be set within createSignUpPanel
        
        // Show Sign In panel by default
        cardLayout.show(cardPanel, "Вход");
        pack(); // Pack after all components are added and styled for better sizing
        setMinimumSize(new Dimension(450, 600)); // Ensure minimum size
        setLocationRelativeTo(null); // Center after pack
    }
    
    private JPanel createSignInPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        // panel.setBackground(PANEL_BACKGROUND); // Use transparent background
        panel.setBackground(PANEL_BACKGROUND_TRANSPARENT);
        panel.setOpaque(true); // The panel itself needs to be opaque to show its semi-transparent color
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(255,255,255,50)), // Subtle border
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        // Username Field (re-initialize here for this panel)
        usernameField = new JTextField(20); 
        styleTextField(usernameField, "Логин");
        gbc.insets = new Insets(15, 0, 15, 0);
        panel.add(usernameField, gbc);

        // Password Field (re-initialize here for this panel)
        passwordField = new JPasswordField(20); 
        styleTextField(passwordField, "Пароль");
        panel.add(passwordField, gbc);

        JCheckBox keepLoggedInCheckbox = new JCheckBox("Запомнить меня");
        styleCheckbox(keepLoggedInCheckbox);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(10, 0, 20, 0);
        panel.add(keepLoggedInCheckbox, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.anchor = GridBagConstraints.CENTER; 

        loginButton = new JButton("Вход");
        styleLoginButton(loginButton);
        gbc.insets = new Insets(20, 0, 10, 0);
        panel.add(loginButton, gbc);
        loginButton.addActionListener(e -> attemptLogin());

        JLabel forgotPasswordLabel = new JLabel("Забыли пароль?");
        forgotPasswordLabel.setFont(SMALL_FONT);
        forgotPasswordLabel.setForeground(TEXT_SECONDARY);
        forgotPasswordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        forgotPasswordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.insets = new Insets(10, 0, 20, 0);
        panel.add(forgotPasswordLabel, gbc);
        
        gbc.weighty = 1.0; // Spacer
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }
    
    private JPanel createSignUpPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        // panel.setBackground(PANEL_BACKGROUND); // Use transparent background
        panel.setBackground(PANEL_BACKGROUND_TRANSPARENT);
        panel.setOpaque(true); // The panel itself needs to be opaque to show its semi-transparent color
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(255,255,255,50)), // Subtle border
            BorderFactory.createEmptyBorder(20, 30, 20, 30) // Adjusted padding slightly
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0); // Slightly reduced insets for more fields

        usernameSignUpField = new JTextField(20);
        styleTextField(usernameSignUpField, "Логин");
        panel.add(usernameSignUpField, gbc);

        emailFieldSignUp = new JTextField(20);
        styleTextField(emailFieldSignUp, "Почта");
        panel.add(emailFieldSignUp, gbc);
        
        firstNameSignUpField = new JTextField(20);
        styleTextField(firstNameSignUpField, "Имя");
        panel.add(firstNameSignUpField, gbc);

        lastNameSignUpField = new JTextField(20);
        styleTextField(lastNameSignUpField, "Фамилия");
        panel.add(lastNameSignUpField, gbc);

        passwordFieldSignUp = new JPasswordField(20);
        styleTextField(passwordFieldSignUp, "Введите пароль");
        panel.add(passwordFieldSignUp, gbc);

        confirmPasswordFieldSignUp = new JPasswordField(20);
        styleTextField(confirmPasswordFieldSignUp, "Подтвердите пароль");
        panel.add(confirmPasswordFieldSignUp, gbc);

        JButton signUpButton = new JButton("Регистрация");
        styleLoginButton(signUpButton); // Re-use login button style
        gbc.insets = new Insets(20, 0, 10, 0); // Larger top inset for button
        panel.add(signUpButton, gbc);
        signUpButton.addActionListener(e -> attemptSignUp(
                usernameSignUpField.getText(), 
                emailFieldSignUp.getText(), 
                new String(passwordFieldSignUp.getPassword()), 
                new String(confirmPasswordFieldSignUp.getPassword()),
                firstNameSignUpField.getText(),
                lastNameSignUpField.getText()
        ));
        
        gbc.weighty = 1.0; // Spacer to push content up
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private void styleTabLabel(JLabel label, boolean isActive) {
        label.setFont(TAB_FONT);
        label.setForeground(isActive ? ACCENT_COLOR : TEXT_SECONDARY);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Simple underline effect for active tab
        if (isActive) {
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_COLOR));
        } else {
            label.setBorder(BorderFactory.createEmptyBorder(0,0,2,0)); // Keep space for underline
        }
    }

    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(DEFAULT_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setOpaque(false);
        field.setCaretColor(TEXT_PRIMARY);
        field.setHorizontalAlignment(JTextField.CENTER); // Center align the text
        // Increased vertical and left padding to make field taller and fix text clipping
        field.setMargin(new Insets(8, 10, 8, 5));

        UnderlineBorder defaultBorder = new UnderlineBorder(INPUT_LINE_COLOR, 1);
        UnderlineBorder focusBorder = new UnderlineBorder(INPUT_FOCUS_LINE_COLOR, 2);

        field.setBorder(defaultBorder);

        // Placeholder text behavior is handled differently for JTextField and JPasswordField
        if (field instanceof JPasswordField) {
            JPasswordField pf = (JPasswordField) field;

            // Initial state for placeholder
            if (pf.getPassword().length == 0) {
                pf.setEchoChar((char) 0);
                pf.setText(placeholder);
                pf.setForeground(TEXT_SECONDARY);
            }

            pf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    pf.setBorder(focusBorder);
                    if (String.valueOf(pf.getPassword()).equals(placeholder)) {
                        pf.setText("");
                        pf.setEchoChar('*'); // Use echo char for actual input
                        pf.setForeground(TEXT_PRIMARY);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    pf.setBorder(defaultBorder);
                    if (pf.getPassword().length == 0) {
                        pf.setEchoChar((char) 0); // Hide echo char for placeholder
                        pf.setText(placeholder);
                        pf.setForeground(TEXT_SECONDARY);
                    }
                }
            });
        } else {
            // Initial state for placeholder
            if (field.getText().isEmpty()) {
                field.setText(placeholder);
                field.setForeground(TEXT_SECONDARY);
            }

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(focusBorder);
                    if (field.getText().equals(placeholder)) {
                        field.setText("");
                        field.setForeground(TEXT_PRIMARY);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(defaultBorder);
                    if (field.getText().isEmpty()) {
                        field.setText(placeholder);
                        field.setForeground(TEXT_SECONDARY);
                    }
                }
            });
        }
    }

    private void styleCheckbox(JCheckBox checkbox) {
        checkbox.setFont(SMALL_FONT);
        checkbox.setForeground(TEXT_SECONDARY);
        // checkbox.setBackground(PANEL_BACKGROUND); // No, should be transparent to panel bg
        checkbox.setOpaque(false);
        checkbox.setFocusPainted(false);
        // Basic styling for checkbox, more advanced requires custom icon painting
    }

    private void styleLoginButton(JButton button) {
        button.setFont(BOLD_FONT);
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setBackground(ACCENT_COLOR);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 50)); // Fixed height
        // Rounded corners - this is tricky with standard JButton, often needs custom painting
        // For a simpler approach, we rely on L&F. For truly rounded, custom component is better.
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        button.addMouseListener(new MouseAdapter() {
            Color originalColor = ACCENT_COLOR;
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(originalColor.brighter());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.equals("Логин") || password.equals("Пароль")) {
             notificationManager.show("Пожалуйста, введите имя пользователя и пароль.", NotificationManager.NotificationType.WARNING);
            return;
        }

        User user = authService.login(username, password);

        if (user != null) {
            UserSession.getInstance().setCurrentUser(user);
            // UserSession cartId is already set by AuthService.login()
            System.out.println("Login successful for user: " + user.getUsername() + " with cart ID: " + UserSession.getInstance().getCartId());
            dispose();
            SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
        } else {
            notificationManager.show("Неверное имя пользователя или пароль.", NotificationManager.NotificationType.ERROR);
        }
    }

    private void attemptSignUp(String username, String email, String password, String confirmPassword, String firstName, String lastName) {
        username = username.trim();
        email = email.trim();
        firstName = firstName.trim();
        lastName = lastName.trim();

        if (username.isEmpty() || username.equals("Логин") ||
            email.isEmpty() || email.equals("Почта") ||
            password.isEmpty() || password.equals("Введите пароль") ||
            confirmPassword.isEmpty() || confirmPassword.equals("Подтвердите пароль") ||
            firstName.isEmpty() || firstName.equals("Имя") ||
            lastName.isEmpty() || lastName.equals("Фамилия")) {
            notificationManager.show("Все поля обязательны для заполнения.", NotificationManager.NotificationType.WARNING);
            return;
        }
        if (!password.equals(confirmPassword)) {
            notificationManager.show("Пароли не совпадают.", NotificationManager.NotificationType.ERROR);
            return;
        }

        // Optional fields check removed as placeholders no longer suggest they are optional.

        try {
            User newUser = authService.signUp(username, email, password, firstName, lastName);
            if (newUser != null) {
                notificationManager.show("Регистрация прошла успешно! Теперь вы можете войти.", NotificationManager.NotificationType.SUCCESS);
                // Switch to sign-in tab and clear sign-up fields
                styleTabLabel(signInLabel, true);
                styleTabLabel(signUpLabel, false);
                cardLayout.show(cardPanel, "Вход");
                clearSignUpFields();
            } else {
                // This case should ideally not be reached if AuthService throws exceptions for specific errors
                notificationManager.show("Произошла неизвестная ошибка при регистрации.", NotificationManager.NotificationType.ERROR);
            }
        } catch (RuntimeException ex) {
            notificationManager.show(ex.getMessage(), NotificationManager.NotificationType.ERROR);
        }
    }

    private void clearSignUpFields() {
        // The focus listeners attached in styleTextField will restore the placeholders
        // when the fields lose focus (e.g., when the tab is switched).
        if (usernameSignUpField != null) usernameSignUpField.setText("");
        if (emailFieldSignUp != null) emailFieldSignUp.setText("");
        if (firstNameSignUpField != null) firstNameSignUpField.setText("");
        if (lastNameSignUpField != null) lastNameSignUpField.setText("");
        if (passwordFieldSignUp != null) passwordFieldSignUp.setText("");
        if (confirmPasswordFieldSignUp != null) confirmPasswordFieldSignUp.setText("");
    }

    // Inner class for drawing background image
    class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                int imgWidth = backgroundImage.getWidth(this);
                int imgHeight = backgroundImage.getHeight(this);
                int panelWidth = getWidth();
                int panelHeight = getHeight();

                if (imgWidth <= 0 || imgHeight <= 0) return; // Avoid division by zero if image is invalid

                double imgAspect = (double) imgWidth / imgHeight;
                double panelAspect = (double) panelWidth / panelHeight;

                int drawWidth = panelWidth;
                int drawHeight = panelHeight;
                int x = 0;
                int y = 0;

                if (imgAspect > panelAspect) { // Image is wider than panel (or less tall)
                    // Fit height, crop width
                    drawHeight = panelHeight;
                    drawWidth = (int) (panelHeight * imgAspect);
                    x = (panelWidth - drawWidth) / 2; // Center horizontally
                } else { // Image is taller than panel (or less wide)
                    // Fit width, crop height
                    drawWidth = panelWidth;
                    drawHeight = (int) (panelWidth / imgAspect);
                    y = (panelHeight - drawHeight) / 2; // Center vertically
                }
                g.drawImage(backgroundImage, x, y, drawWidth, drawHeight, this);
            } else {
                g.setColor(WINDOW_BACKGROUND);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    private void loadBackgroundImage() {
        try (InputStream is = DroneLoginWindow.class.getResourceAsStream(BACKGROUND_IMAGE_PATH)) {
            if (is == null) {
                System.err.println("Background image not found at path: " + BACKGROUND_IMAGE_PATH);
                backgroundImage = null;
                return;
            }
            backgroundImage = ImageIO.read(is);
        } catch (IOException e) {
            System.err.println("Error loading background image: " + BACKGROUND_IMAGE_PATH);
            e.printStackTrace();
            backgroundImage = null;
        }
    }

    public static void main(String[] args) {
        // Initialize the database first
        DatabaseManager.initializeDatabase();

        // Set a modern L&F. FlatLaf is excellent for this kind of styling.
        // If FlatLaf is not available, Nimbus is a good fallback.
        try {
            // UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf()); // Example with FlatLaf
            // If not using FlatLaf, try Nimbus or System L&F
            boolean foundLaf = false;
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    foundLaf = true;
                    break;
                }
            }
            if (!foundLaf) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Code to list all available font family names
        String[] fontFamilyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        System.out.println("---- Available Font Families ----");
        for (String name : fontFamilyNames) {
            System.out.println(name);
        }
        System.out.println("---------------------------------");

        SwingUtilities.invokeLater(() -> {
            DroneLoginWindow window = new DroneLoginWindow();
            window.setVisible(true);
        });
    }
}