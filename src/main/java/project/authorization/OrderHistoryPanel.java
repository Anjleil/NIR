package project.authorization;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList; // For placeholder if no orders

public class OrderHistoryPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable orderTable;
    private OrderService orderService;

    public OrderHistoryPanel() {
        super(new BorderLayout(10, 10));
        this.orderService = new OrderService();
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(MainWindow.PANEL_BACKGROUND);

        // Define columns for the order history table
        tableModel = new DefaultTableModel(
                new Object[]{"ID Заказа", "Дата", "Сумма", "Статус", "Адрес доставки", "Тип доставки", "Товары"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };

        orderTable = new JTable(tableModel);
        styleTable(orderTable);

        JScrollPane scrollPane = new JScrollPane(orderTable);
        styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        // Initial load of order history is handled by MainWindow's tab change listener
        // or after an order is placed.
    }

    public void refreshOrderHistory() {
        System.out.println("OrderHistoryPanel: Refreshing order history...");
        tableModel.setRowCount(0); // Clear existing table data

        if (!UserSession.getInstance().isLoggedIn()) {
            System.out.println("OrderHistoryPanel: No user logged in. Skipping refresh.");
            // Optionally display a message like "Please log in to see order history"
            return;
        }

        User currentUser = UserSession.getInstance().getCurrentUser();
        List<Order> orders = orderService.getOrdersForUser(currentUser.getUserId());

        if (orders.isEmpty()) {
            System.out.println("OrderHistoryPanel: No orders found for user " + currentUser.getUsername());
            // Optionally add a row indicating no orders
            // tableModel.addRow(new Object[]{"-", "Нет заказов", "-", "-", "-", "-", "-"});
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            for (Order order : orders) {
                StringBuilder itemsSummary = new StringBuilder();
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    for (int i = 0; i < order.getItems().size(); i++) {
                        OrderItem item = order.getItems().get(i);
                        itemsSummary.append(item.getProductName()).append(" (x").append(item.getQuantity()).append(")");
                        if (i < order.getItems().size() - 1) {
                            itemsSummary.append(", ");
                        }
                    }
                } else {
                    itemsSummary.append("Нет информации о товарах");
                }

                tableModel.addRow(new Object[]{
                        order.getOrderId(),
                        order.getOrderDate() != null ? dateFormat.format(order.getOrderDate()) : "N/A",
                        String.format("%,.2f ₽", order.getTotalAmount()),
                        order.getStatus(),
                        order.getDeliveryAddress(),
                        order.getDeliveryType(),
                        itemsSummary.toString()
                });
            }
        }
    }
    
    // Legacy method, can be removed if DeliveryPanel is fully updated
    // Or kept if some part of the old flow might still call it temporarily.
    public void refreshOrders(List<Order> legacyOrders) { 
        System.out.println("OrderHistoryPanel.refreshOrders (legacy) called. Delegating to refreshOrderHistory().");
        refreshOrderHistory(); 
    }

    private void styleTable(JTable table) {
        table.setFont(MainWindow.DEFAULT_FONT);
        table.setForeground(MainWindow.TEXT_PRIMARY);
        table.setBackground(MainWindow.PANEL_BACKGROUND);
        table.setFillsViewportHeight(true);
        table.setRowHeight(40);
        table.setGridColor(MainWindow.TABLE_GRID_COLOR);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(MainWindow.TABLE_SELECTION_BG_COLOR);
        table.setSelectionForeground(MainWindow.TABLE_SELECTION_FG_COLOR);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(MainWindow.TABLE_HEADER_FONT);
        tableHeader.setBackground(MainWindow.TABLE_HEADER_BG_COLOR);
        tableHeader.setForeground(MainWindow.TEXT_PRIMARY);
        tableHeader.setOpaque(true);
        tableHeader.setDefaultRenderer(new DarkHeaderRenderer(table));
        tableHeader.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DarkCellRenderer());
        
        // Adjust column widths (example)
        table.getColumnModel().getColumn(0).setPreferredWidth(80); // Order ID
        table.getColumnModel().getColumn(1).setPreferredWidth(120); // Date
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Amount
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(4).setPreferredWidth(200); // Address
        table.getColumnModel().getColumn(5).setPreferredWidth(100); // Delivery Type
        table.getColumnModel().getColumn(6).setPreferredWidth(250); // Items summary
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBackground(MainWindow.PANEL_BACKGROUND);
        scrollPane.setBorder(BorderFactory.createLineBorder(MainWindow.TABLE_GRID_COLOR));
        // Add custom scrollbar UI if available and desired
    }

    // Static inner class for custom header rendering (Dark Theme)
    private static class DarkHeaderRenderer extends JLabel implements TableCellRenderer {
        public DarkHeaderRenderer(JTable table) {
            super();
            setOpaque(true);
            setFont(MainWindow.TABLE_HEADER_FONT); // Use font from MainWindow constants
            setBackground(MainWindow.TABLE_HEADER_BG_COLOR);
            setForeground(MainWindow.TEXT_PRIMARY);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.TABLE_GRID_COLOR),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10) // Padding
            ));
            setHorizontalAlignment(SwingConstants.LEADING);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            return this;
        }
    }

    // Static inner class for custom cell rendering (Dark Theme with alternating rows)
    private static class DarkCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(MainWindow.DEFAULT_FONT);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Cell padding
            if (isSelected) {
                c.setBackground(MainWindow.TABLE_SELECTION_BG_COLOR);
                c.setForeground(MainWindow.TABLE_SELECTION_FG_COLOR);
            } else {
                c.setBackground(row % 2 == 0 ? MainWindow.PANEL_BACKGROUND : new Color(60, 63, 65)); // Alternating
                c.setForeground(MainWindow.TEXT_PRIMARY);
            }
            return c;
        }
    }
} 