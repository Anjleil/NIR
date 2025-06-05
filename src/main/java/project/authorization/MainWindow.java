package project.authorization;

import project.authorization.ui.NotificationManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import project.authorization.CartPanel;
import project.authorization.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import project.authorization.OrderService;

public class MainWindow extends JFrame {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private JTabbedPane tabbedPane;
    private CartPanel cartPanel;
    private DeliveryPanel deliveryPanel;
    private OrderHistoryPanel historyPanel;
    private OrderService orderService;
    private NotificationManager notificationManager;

    public static final Color WINDOW_BACKGROUND = new Color(45, 45, 45);
    public static final Color PANEL_BACKGROUND = new Color(55, 58, 60);
    public static final Color ACCENT_COLOR = new Color(242, 169, 0);
    public static final Color TEXT_PRIMARY = new Color(220, 220, 220);
    public static final Color TEXT_SECONDARY = new Color(150, 150, 150);
    public static final Color TABLE_GRID_COLOR = new Color(70, 70, 70);
    public static final Color TABLE_HEADER_BG_COLOR = new Color(65, 68, 70);
    public static final Color TABLE_SELECTION_BG_COLOR = ACCENT_COLOR.darker();
    public static final Color TABLE_SELECTION_FG_COLOR = Color.WHITE;
    public static final Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font TAB_FONT = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    public MainWindow() {
        super("DroneExpress - Система заказов");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        JLayeredPane layeredPane = new JLayeredPane();
        this.notificationManager = new NotificationManager(layeredPane);
        setContentPane(layeredPane);

        // Create and add a background panel to ensure the dark theme is always visible
        JPanel backgroundPanel = new JPanel();
        backgroundPanel.setBackground(WINDOW_BACKGROUND);
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        tabbedPane = new JTabbedPane();
        styleTabbedPane(tabbedPane);
        // The tabbed pane will be on a higher layer to be visible above the background
        layeredPane.add(tabbedPane, Integer.valueOf(10));

        // Ensure both panels resize with the window
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                backgroundPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                tabbedPane.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            }
        });

        orderService = new OrderService();

        ProductSelectionPanel productPanel = new ProductSelectionPanel(this.notificationManager);
        productPanel.setBackground(PANEL_BACKGROUND);
        tabbedPane.addTab("🛒 Товары", productPanel);

        cartPanel = new CartPanel(this);
        cartPanel.setBackground(PANEL_BACKGROUND);
        tabbedPane.addTab("🛍️ Корзина", cartPanel);

        deliveryPanel = new DeliveryPanel(this);
        deliveryPanel.setBackground(PANEL_BACKGROUND);
        tabbedPane.addTab("🚚 Доставка", deliveryPanel);

        historyPanel = new OrderHistoryPanel();
        historyPanel.setBackground(PANEL_BACKGROUND);
        tabbedPane.addTab("📋 История", historyPanel);

        ProfilePanel profilePanel = new ProfilePanel(this);
        profilePanel.setBackground(PANEL_BACKGROUND);
        tabbedPane.addTab("👤 Профиль", profilePanel);

        this.addPropertyChangeListener("orderSaved", evt -> {
            System.out.println("MainWindow: 'orderSaved' event received.");
            if (cartPanel != null) {
                cartPanel.refreshCartDisplay();
            }
            if (historyPanel != null) {
                historyPanel.refreshOrderHistory();
            }
        });

        tabbedPane.addChangeListener(e -> {
            Component selectedComponent = tabbedPane.getSelectedComponent();
            if (selectedComponent instanceof CartPanel) {
                ((CartPanel) selectedComponent).refreshCartDisplay();
            } else if (selectedComponent instanceof OrderHistoryPanel) {
                ((OrderHistoryPanel) selectedComponent).refreshOrderHistory();
            }
        });

        // The main content is now the tabbedPane, which is inside the layeredPane.
        // The layeredPane itself is the content pane. We no longer add tabbedPane directly to the frame.

        if (!UserSession.getInstance().isLoggedIn()) {
            dispose();
            SwingUtilities.invokeLater(() -> new DroneLoginWindow().setVisible(true));
            return;
        }
        if (cartPanel != null) {
            cartPanel.refreshCartDisplay();
        }
        if (deliveryPanel != null) {
            tabbedPane.setSelectedComponent(deliveryPanel);
        }
    }

    public static void styleDarkButtonShared(JButton button, Color bgColor, Color hoverColor, Color textColor) {
        button.setFont(MainWindow.BOLD_FONT);
        button.setForeground(textColor);
        button.setBackground(bgColor);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    public void switchToDeliveryPanel() {
        if (deliveryPanel != null) {
            tabbedPane.setSelectedComponent(deliveryPanel);
        }
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public boolean processOrderPlacement(String deliveryAddress, String deliveryType, String customerNotes) {
        if (!UserSession.getInstance().isLoggedIn()) {
            notificationManager.show("Пожалуйста, войдите в систему для оформления заказа.", NotificationManager.NotificationType.WARNING);
            return false;
        }
        User currentUser = UserSession.getInstance().getCurrentUser();
        Order newDbOrder = orderService.createOrderFromCart(currentUser.getUserId(), deliveryAddress, deliveryType, customerNotes);

        if (newDbOrder != null) {
            support.firePropertyChange("orderSaved", null, newDbOrder);
            return true;
        } else {
            notificationManager.show("Не удалось создать заказ. Возможные причины: корзина пуста или ошибка сервера.", NotificationManager.NotificationType.ERROR);
            return false;
        }
    }

    public void saveOrder(Order legacyOrderObject) {
        System.out.println("MainWindow.saveOrder (legacy) called. Firing 'orderSaved' event.");
        support.firePropertyChange("orderSaved", legacyOrderObject, null);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    private void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setFont(TAB_FONT);
        tabbedPane.setBackground(WINDOW_BACKGROUND); 
        tabbedPane.setForeground(TEXT_SECONDARY);     

        UIManager.put("TabbedPane.contentOpaque", Boolean.FALSE);
        UIManager.put("TabbedPane.tabAreaBackground", WINDOW_BACKGROUND);
        UIManager.put("TabbedPane.selected", ACCENT_COLOR); 
        UIManager.put("TabbedPane.selectHighlight", ACCENT_COLOR); 
        UIManager.put("TabbedPane.focus", ACCENT_COLOR); 
        UIManager.put("TabbedPane.unselectedTabForeground", TEXT_SECONDARY);
        UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(5, 10, 5, 10)); 
        UIManager.put("TabbedPane.tabInsets", new Insets(5, 10, 5, 10));

        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                g.setColor(isSelected ? PANEL_BACKGROUND : WINDOW_BACKGROUND);
                g.fillRect(x, y, w, h);
            }
            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                // No default border
            }
            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // No content border from tabbed pane itself
            }
            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
                if (isSelected && tabbedPane.hasFocus()) {
                    g.setColor(ACCENT_COLOR);
                    g.drawRect(textRect.x -2, textRect.y -2, textRect.width + 4, textRect.height + 4); 
                }
            }
            @Override
            protected Insets getTabInsets(int tabPlacement, int tabIndex) {
                return new Insets(8, 15, 8, 15); 
            }
             @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                g.setFont(font);
                View v = getTextViewForTab(tabIndex);
                if (v != null) {
                    v.paint(g, textRect);
                } else { 
                    int x = textRect.x + (textRect.width - metrics.stringWidth(title)) / 2;
                    int y = textRect.y + metrics.getAscent() + (textRect.height - metrics.getHeight()) / 2;
                    g.setColor(isSelected ? ACCENT_COLOR : TEXT_SECONDARY);
                    g.drawString(title, x, y);
                }
                 if (isSelected) { 
                    int lineY = textRect.y + textRect.height + 2; // Position underline below text
                    g.setColor(ACCENT_COLOR);
                    g.fillRect(textRect.x, lineY, textRect.width, 3); // Underline thickness
                }
            }
        });
    }
}

// NO OrderHistoryPanel class definition here. It must be in its own file. 