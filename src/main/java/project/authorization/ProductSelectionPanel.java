package project.authorization;

import project.authorization.db.DatabaseManager;
import project.authorization.ui.NotificationManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class ProductSelectionPanel extends JPanel {
    private DefaultListModel<Product> productListModel = new DefaultListModel<>();
    private JList<Product> productJList;
    private CartService cartService;
    private NotificationManager notificationManager;

    // Dark Theme Color Palette (consistent with MainWindow)
    private static final Color PANEL_BACKGROUND = MainWindow.PANEL_BACKGROUND;
    private static final Color ACCENT_COLOR = MainWindow.ACCENT_COLOR;
    private static final Color TEXT_PRIMARY = MainWindow.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = MainWindow.TEXT_SECONDARY;
    private static final Color LIST_SELECTION_BG_COLOR = MainWindow.ACCENT_COLOR; // Use accent for selection
    private static final Color LIST_SELECTION_FG_COLOR = Color.WHITE; // Text on selection
    private static final Color LIST_ITEM_HOVER_BG_COLOR = new Color(70, 73, 75);
    private static final Font DEFAULT_FONT = MainWindow.DEFAULT_FONT;
    private static final Font BOLD_FONT = MainWindow.BOLD_FONT;
    private static final Font BUTTON_FONT = MainWindow.BOLD_FONT;

    public ProductSelectionPanel(NotificationManager notificationManager) {
        super(new BorderLayout(10, 15)); // Increased bottom gap
        this.notificationManager = notificationManager;
        setBackground(PANEL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        loadProductsFromDB(); // Load products initially

        productJList = new JList<>(productListModel);
        styleProductList(productJList);

        JScrollPane scrollPane = new JScrollPane(productJList);
        styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        JButton addToCartButton = new JButton("Добавить в корзину");
        styleDarkButton(addToCartButton);
        addToCartButton.addActionListener(e -> addSelectedProductToCart());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(PANEL_BACKGROUND);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        buttonPanel.add(addToCartButton);
        add(buttonPanel, BorderLayout.SOUTH);

        cartService = new CartService(); // Initialize CartService
    }

    private void loadProductsFromDB() {
        productListModel.clear();
        List<Product> products = fetchAllProducts();
        for (Product product : products) {
            productListModel.addElement(product);
        }
    }

    public List<Product> fetchAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT product_id, name, description, price, stock_quantity, image_url FROM products ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("image_url")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching products: " + e.getMessage());
            e.printStackTrace();
            // Optionally show a user-friendly error message
            notificationManager.show("Ошибка загрузки товаров из базы данных.", NotificationManager.NotificationType.ERROR);
        }
        return products;
    }
    
    public Product getProductById(int productId) {
        String sql = "SELECT product_id, name, description, price, stock_quantity, image_url FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Product(
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getBigDecimal("price"),
                    rs.getInt("stock_quantity"),
                    rs.getString("image_url")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching product by ID " + productId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void styleProductList(JList<Product> list) {
        list.setFont(DEFAULT_FONT);
        list.setBackground(PANEL_BACKGROUND);
        list.setSelectionBackground(LIST_SELECTION_BG_COLOR);
        list.setSelectionForeground(LIST_SELECTION_FG_COLOR);
        list.setCellRenderer(new DarkProductCellRenderer());
        list.setBorder(BorderFactory.createEmptyBorder()); // No border for list itself, scrollpane will have it
        list.setFixedCellHeight(90); // Increased height for description preview and new font
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(MainWindow.TABLE_GRID_COLOR));
        scrollPane.getViewport().setBackground(PANEL_BACKGROUND);
        // Assuming CustomScrollBarUI is not yet implemented or available
        // scrollPane.getVerticalScrollBar().setUI(new project.authorization.CustomScrollBarUI());
        // scrollPane.getHorizontalScrollBar().setUI(new project.authorization.CustomScrollBarUI());
    }

    private void styleDarkButton(JButton button) {
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.WHITE); // Text color for dark buttons
        button.setBackground(ACCENT_COLOR);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30)); // Padding

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

    private void addSelectedProductToCart() {
        Product selectedProduct = productJList.getSelectedValue();
        if (selectedProduct != null) {
            if (!UserSession.getInstance().isLoggedIn()) {
                notificationManager.show("Пожалуйста, войдите, чтобы добавить товары в корзину.", NotificationManager.NotificationType.WARNING);
                return;
            }

            if (selectedProduct.getStockQuantity() <= 0) {
                notificationManager.show("Товар \"" + selectedProduct.getName() + "\" отсутствует на складе.", NotificationManager.NotificationType.WARNING);
                return;
            }

            try {
                cartService.addOrUpdateCartItem(UserSession.getInstance().getCartId(), selectedProduct.getProductId(), 1);
                notificationManager.show("Товар \"" + selectedProduct.getName() + "\" добавлен в корзину.", NotificationManager.NotificationType.SUCCESS);
                // TODO: Consider updating a cart count label in MainWindow or firing an event
            } catch (RuntimeException e) {
                // Error already logged by CartService, show user-friendly message
                notificationManager.show("Не удалось добавить товар в корзину. Пожалуйста, попробуйте еще раз.", NotificationManager.NotificationType.ERROR);
            }

        } else {
            notificationManager.show("Пожалуйста, выберите товар.", NotificationManager.NotificationType.WARNING);
        }
    }

    private static class DarkProductCellRenderer extends JPanel implements ListCellRenderer<Product> {
        private JLabel nameLabel;
        private JLabel priceLabel;
        private JLabel stockLabel;
        private JPanel textPanel;

        public DarkProductCellRenderer() {
            super(new BorderLayout(15, 5));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            setBackground(ProductSelectionPanel.PANEL_BACKGROUND);

            nameLabel = new JLabel();
            nameLabel.setFont(ProductSelectionPanel.BOLD_FONT.deriveFont(16f));
            nameLabel.setForeground(ProductSelectionPanel.TEXT_PRIMARY);

            priceLabel = new JLabel();
            priceLabel.setFont(ProductSelectionPanel.DEFAULT_FONT.deriveFont(Font.ITALIC, 14f));
            priceLabel.setForeground(ProductSelectionPanel.ACCENT_COLOR);
            
            stockLabel = new JLabel();
            stockLabel.setFont(ProductSelectionPanel.DEFAULT_FONT.deriveFont(12f));
            stockLabel.setForeground(ProductSelectionPanel.TEXT_SECONDARY);

            textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.add(nameLabel);
            textPanel.add(priceLabel);
            textPanel.add(stockLabel);

            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Product> list,
                                                      Product product, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(product.getName());
            priceLabel.setText(String.format("Цена: %,.2f ₽", product.getPrice()));
            stockLabel.setText("В наличии: " + product.getStockQuantity() + " шт.");
            
            if (product.getStockQuantity() == 0) {
                stockLabel.setForeground(Color.RED.darker());
                stockLabel.setText("Нет в наличии");
            } else {
                 stockLabel.setForeground(ProductSelectionPanel.TEXT_SECONDARY);
            }

            if (isSelected) {
                setBackground(ProductSelectionPanel.LIST_SELECTION_BG_COLOR);
                nameLabel.setForeground(ProductSelectionPanel.LIST_SELECTION_FG_COLOR);
                priceLabel.setForeground(ProductSelectionPanel.LIST_SELECTION_FG_COLOR);
                stockLabel.setForeground(ProductSelectionPanel.LIST_SELECTION_FG_COLOR);
            } else {
                setBackground(ProductSelectionPanel.PANEL_BACKGROUND);
                nameLabel.setForeground(ProductSelectionPanel.TEXT_PRIMARY);
                priceLabel.setForeground(ProductSelectionPanel.ACCENT_COLOR);
            }
            return this;
        }
    }
}