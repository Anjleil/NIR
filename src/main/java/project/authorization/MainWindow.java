package project.authorization;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;
import java.awt.*;
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

    public static final Color WINDOW_BACKGROUND = new Color(45, 45, 45);
    public static final Color PANEL_BACKGROUND = new Color(55, 58, 60);
    public static final Color ACCENT_COLOR = new Color(242, 169, 0);
    public static final Color TEXT_PRIMARY = new Color(220, 220, 220);
    public static final Color TEXT_SECONDARY = new Color(150, 150, 150);
    public static final Color TABLE_GRID_COLOR = new Color(70, 70, 70);
    public static final Color TABLE_HEADER_BG_COLOR = new Color(65, 68, 70);
    public static final Color TABLE_SELECTION_BG_COLOR = ACCENT_COLOR.darker();
    public static final Color TABLE_SELECTION_FG_COLOR = Color.WHITE;
    public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font TAB_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font TABLE_HEADER_FONT = new Font("SansSerif", Font.BOLD, 14);

    public MainWindow() {
        super("DroneExpress - Система заказов");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(WINDOW_BACKGROUND);

        tabbedPane = new JTabbedPane();
        styleTabbedPane(tabbedPane);

        orderService = new OrderService();

        ProductSelectionPanel productPanel = new ProductSelectionPanel();
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

        add(tabbedPane);

        if (!UserSession.getInstance().isLoggedIn()) {
            dispose();
            SwingUtilities.invokeLater(() -> new DroneLoginWindow().setVisible(true));
            return;
        }
        if (cartPanel != null) {
            cartPanel.refreshCartDisplay();
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

    public boolean processOrderPlacement(String deliveryAddress, String deliveryType, String customerNotes) {
        if (!UserSession.getInstance().isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, войдите в систему для оформления заказа.", "Требуется вход", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        User currentUser = UserSession.getInstance().getCurrentUser();
        Order newDbOrder = orderService.createOrderFromCart(currentUser.getUserId(), deliveryAddress, deliveryType, customerNotes);

        if (newDbOrder != null) {
            support.firePropertyChange("orderSaved", null, newDbOrder);
            return true;
        } else {
            JOptionPane.showMessageDialog(this, 
                "Не удалось создать заказ. Возможные причины: корзина пуста, проблемы с наличием товара или ошибка сервера.", 
                "Ошибка создания заказа", 
                JOptionPane.ERROR_MESSAGE);
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