package project.authorization;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

public class CartPanel extends JPanel {
    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private JLabel totalLabel;
    private CartService cartService;
    private MainWindow mainWindowRef;

    // Column Names (final for consistency)
    private static final String COL_PRODUCT_NAME = "Товар";
    private static final String COL_QUANTITY = "Кол-во";
    private static final String COL_PRICE_PER_UNIT = "Цена за шт.";
    private static final String COL_TOTAL_PRICE = "Сумма";
    private static final String COL_ACTIONS = "Действия";
    private static final String COL_CART_ITEM_ID = "CartItemID_hidden"; // For internal use
    private static final String COL_PRODUCT_ID = "ProductID_hidden";   // For internal use

    // Column Indices (based on order of addition to cartTableModel)
    private static final int IDX_PRODUCT_NAME = 0;
    private static final int IDX_QUANTITY = 1;
    private static final int IDX_PRICE_PER_UNIT = 2;
    private static final int IDX_TOTAL_PRICE = 3;
    private static final int IDX_ACTIONS = 4;
    private static final int IDX_CART_ITEM_ID = 5;
    private static final int IDX_PRODUCT_ID = 6;

    public CartPanel(MainWindow mainWindow) {
        this.mainWindowRef = mainWindow;
        this.cartService = new CartService();
        setLayout(new BorderLayout(10, 10));
        setBackground(MainWindow.PANEL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        setupCartTable();
        JScrollPane scrollPane = new JScrollPane(cartTable);
        styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10,10));
        bottomPanel.setOpaque(false);

        totalLabel = new JLabel("Общая сумма: 0.00 ₽");
        totalLabel.setFont(MainWindow.BOLD_FONT.deriveFont(18f));
        totalLabel.setForeground(MainWindow.TEXT_PRIMARY);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalLabel.setBorder(BorderFactory.createEmptyBorder(10,0,10,10));
        bottomPanel.add(totalLabel, BorderLayout.CENTER);

        JButton checkoutButton = new JButton("Оформить заказ");
        MainWindow.styleDarkButtonShared(checkoutButton, MainWindow.ACCENT_COLOR, MainWindow.ACCENT_COLOR.brighter(), Color.WHITE);
        checkoutButton.addActionListener(e -> proceedToCheckout());
        
        JButton clearCartButton = new JButton("Очистить корзину");
        MainWindow.styleDarkButtonShared(clearCartButton, new Color(108, 117, 125), new Color(80,85,90), Color.WHITE);
        clearCartButton.addActionListener(e -> clearCartAction());

        JPanel buttonActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonActionsPanel.setOpaque(false);
        buttonActionsPanel.add(clearCartButton);
        buttonActionsPanel.add(checkoutButton);
        bottomPanel.add(buttonActionsPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupCartTable() {
        cartTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == IDX_QUANTITY; // Only quantity is editable
            }
        };
        cartTableModel.addColumn(COL_PRODUCT_NAME);      // IDX 0
        cartTableModel.addColumn(COL_QUANTITY);          // IDX 1
        cartTableModel.addColumn(COL_PRICE_PER_UNIT);    // IDX 2
        cartTableModel.addColumn(COL_TOTAL_PRICE);       // IDX 3
        cartTableModel.addColumn(COL_ACTIONS);           // IDX 4
        cartTableModel.addColumn(COL_CART_ITEM_ID);      // IDX 5 (Hidden)
        cartTableModel.addColumn(COL_PRODUCT_ID);        // IDX 6 (Hidden)

        cartTable = new JTable(cartTableModel);
        styleTable(cartTable); 

        hideTableColumn(cartTable, COL_CART_ITEM_ID);
        hideTableColumn(cartTable, COL_PRODUCT_ID);

        TableColumn actionsColumn = cartTable.getColumnModel().getColumn(IDX_ACTIONS);
        actionsColumn.setCellRenderer(new ButtonCellRenderer("Удалить", MainWindow.TEXT_SECONDARY.darker(), Color.WHITE));
        actionsColumn.setCellEditor(new ButtonCellEditor("Удалить", MainWindow.TEXT_SECONDARY.darker(), Color.WHITE, cartItemId -> {
            removeCartItemById(cartItemId);
        }));
        actionsColumn.setMinWidth(100);
        actionsColumn.setMaxWidth(120);
        actionsColumn.setPreferredWidth(110);

        TableColumn quantityColumn = cartTable.getColumnModel().getColumn(IDX_QUANTITY);
        quantityColumn.setCellEditor(new SpinnerCellEditor(1, 100)); 
        quantityColumn.setMinWidth(80);
        quantityColumn.setMaxWidth(100);
        quantityColumn.setPreferredWidth(90);
        
        cartTableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (row >= 0 && column == IDX_QUANTITY) { 
                    updateQuantityFromTable(row);
                }
            }
        });
    }
    
    private void updateQuantityFromTable(int row) {
        try {
            int cartItemId = (int) cartTableModel.getValueAt(row, IDX_CART_ITEM_ID);
            int newQuantity = Integer.parseInt(cartTableModel.getValueAt(row, IDX_QUANTITY).toString());
            
            if (newQuantity < 0) { 
                 JOptionPane.showMessageDialog(this, "Количество не может быть отрицательным.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                 refreshCartDisplay();
                 return;
            }

            cartService.updateCartItemQuantity(cartItemId, newQuantity);
            refreshCartDisplay(); 
        } catch (NumberFormatException ex) {
            System.err.println("Invalid quantity format: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Пожалуйста, введите корректное число для количества.", "Ошибка формата", JOptionPane.ERROR_MESSAGE);
            refreshCartDisplay(); 
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("Error accessing table data, column index might be wrong: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Произошла ошибка при обновлении данных таблицы.", "Ошибка таблицы", JOptionPane.ERROR_MESSAGE);
            refreshCartDisplay();
        } catch (RuntimeException dbEx) {
            JOptionPane.showMessageDialog(this, "Ошибка обновления количества: " + dbEx.getMessage(), "Ошибка базы данных", JOptionPane.ERROR_MESSAGE);
            refreshCartDisplay(); 
        }
    }

    private void hideTableColumn(JTable table, String columnName) {
        try {
            // Find column by name to hide it - more robust if column order might change later
            int columnIndex = table.getColumnModel().getColumnIndex(columnName);
            TableColumn column = table.getColumnModel().getColumn(columnIndex);
            column.setMinWidth(0);
            column.setMaxWidth(0);
            column.setWidth(0);
            column.setPreferredWidth(0);
        } catch (IllegalArgumentException e) {
            // This can happen if the column name constant doesn't match what was added to table model
            System.err.println("Error hiding column: Column '" + columnName + "' not found. Ensure column names are consistent.");
        }
    }

    public void refreshCartDisplay() {
        if (!UserSession.getInstance().isLoggedIn()) {
            if (cartTableModel != null) cartTableModel.setRowCount(0);
            if (totalLabel != null) totalLabel.setText("Общая сумма: 0.00 ₽");
            return;
        }
        int cartId = UserSession.getInstance().getCartId();
        List<CartItem> items = cartService.getCartItems(cartId);
        cartTableModel.setRowCount(0); 

        BigDecimal cartTotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Vector<Object> row = new Vector<>();
            row.setSize(cartTableModel.getColumnCount()); // Ensure vector is correct size
            row.setElementAt(item.getProductName(), IDX_PRODUCT_NAME);
            row.setElementAt(item.getQuantity(), IDX_QUANTITY);
            row.setElementAt(String.format("%,.2f ₽", item.getProductPrice()), IDX_PRICE_PER_UNIT);
            row.setElementAt(String.format("%,.2f ₽", item.getTotalPrice()), IDX_TOTAL_PRICE);
            row.setElementAt("Удалить", IDX_ACTIONS); 
            row.setElementAt(item.getCartItemId(), IDX_CART_ITEM_ID);
            row.setElementAt(item.getProductId(), IDX_PRODUCT_ID);
            cartTableModel.addRow(row);
            if(item.getTotalPrice() != null) { // Guard against null if product price wasn't set
                 cartTotal = cartTotal.add(item.getTotalPrice());
            }
        }
        totalLabel.setText(String.format("Общая сумма: %,.2f ₽", cartTotal));
    }

    private void removeCartItemById(int cartItemId) {
        try {
            cartService.removeCartItem(cartItemId);
            refreshCartDisplay();
        } catch (RuntimeException e) {
             JOptionPane.showMessageDialog(this, "Не удалось удалить товар из корзины: " + e.getMessage(), "Ошибка базы данных", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void clearCartAction() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        int choice = JOptionPane.showConfirmDialog(this, 
                "Вы уверены, что хотите очистить корзину?", 
                "Подтверждение", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                cartService.clearCart(UserSession.getInstance().getCartId());
                refreshCartDisplay();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, "Не удалось очистить корзину: " + e.getMessage(), "Ошибка базы данных", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void proceedToCheckout() {
        if (cartTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Ваша корзина пуста.", "Корзина пуста", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        mainWindowRef.switchToDeliveryPanel(); 
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBackground(MainWindow.PANEL_BACKGROUND);
        scrollPane.setBorder(BorderFactory.createLineBorder(MainWindow.TABLE_GRID_COLOR));
    }

    private void styleTable(JTable table) {
        table.setFont(MainWindow.DEFAULT_FONT);
        table.setForeground(MainWindow.TEXT_PRIMARY);
        table.setBackground(MainWindow.PANEL_BACKGROUND);
        table.setFillsViewportHeight(true);
        table.setRowHeight(35);
        table.setGridColor(MainWindow.TABLE_GRID_COLOR);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(MainWindow.TABLE_SELECTION_BG_COLOR);
        table.setSelectionForeground(MainWindow.TABLE_SELECTION_FG_COLOR);
        javax.swing.table.JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(MainWindow.TABLE_HEADER_FONT);
        tableHeader.setBackground(MainWindow.TABLE_HEADER_BG_COLOR);
        tableHeader.setForeground(MainWindow.TEXT_PRIMARY);
        tableHeader.setOpaque(true);
        tableHeader.setDefaultRenderer(new DarkHeaderRenderer(table));
        tableHeader.setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new DarkCellRenderer());
    }

    private static class DarkHeaderRenderer extends JLabel implements TableCellRenderer {
        public DarkHeaderRenderer(JTable table) {
            super(); setOpaque(true); setFont(table.getTableHeader().getFont());
            setBackground(table.getTableHeader().getBackground()); setForeground(table.getTableHeader().getForeground());
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0, MainWindow.TABLE_GRID_COLOR), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            setHorizontalAlignment(SwingConstants.LEADING);
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : ""); return this;
        }
    }

    private static class DarkCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(MainWindow.DEFAULT_FONT); setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            if (isSelected) {
                c.setBackground(MainWindow.TABLE_SELECTION_BG_COLOR); c.setForeground(MainWindow.TABLE_SELECTION_FG_COLOR);
            } else {
                c.setBackground(row % 2 == 0 ? MainWindow.PANEL_BACKGROUND : new Color(60,63,65)); c.setForeground(MainWindow.TEXT_PRIMARY);
            }
            return c;
        }
    }

    static class ButtonCellRenderer extends JButton implements TableCellRenderer {
        public ButtonCellRenderer(String text, Color background, Color foreground) {
            setOpaque(true); setText(text); setBackground(background); setForeground(foreground);
            setFont(MainWindow.DEFAULT_FONT.deriveFont(12f)); setBorder(BorderFactory.createEmptyBorder(2,5,2,5));
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            return this;
        }
    }

    static class ButtonCellEditor extends DefaultCellEditor {
        protected JButton button; private int cartItemId; private boolean isPushed; private Consumer<Integer> actionListener;
        public ButtonCellEditor(String text, Color background, Color foreground, Consumer<Integer> listener) {
            super(new JCheckBox()); this.actionListener = listener; button = new JButton(text);
            button.setOpaque(true); button.setBackground(background); button.setForeground(foreground);
            button.setFont(MainWindow.DEFAULT_FONT.deriveFont(12f)); button.setBorder(BorderFactory.createEmptyBorder(2,5,2,5));
            button.addActionListener(e -> fireEditingStopped());
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.cartItemId = (int) table.getModel().getValueAt(row, IDX_CART_ITEM_ID); // Use predefined index
            isPushed = true; return button;
        }
        @Override public Object getCellEditorValue() {
            if (isPushed && actionListener != null) { actionListener.accept(cartItemId); } isPushed = false; return button.getText();
        }
        @Override public boolean stopCellEditing() { isPushed = false; return super.stopCellEditing(); }
    }
    
    static class SpinnerCellEditor extends DefaultCellEditor {
        JSpinner spinner; JSpinner.DefaultEditor editor; JTextField textField;
        public SpinnerCellEditor(int min, int max) {
            super(new JTextField()); spinner = new JSpinner(new SpinnerNumberModel(1, min, max, 1)); editor = (JSpinner.DefaultEditor) spinner.getEditor();
            textField = editor.getTextField(); textField.setForeground(MainWindow.TEXT_PRIMARY); textField.setBackground(MainWindow.PANEL_BACKGROUND);
            textField.setBorder(BorderFactory.createLineBorder(MainWindow.ACCENT_COLOR, 1)); textField.setFont(MainWindow.DEFAULT_FONT);
            textField.setHorizontalAlignment(JTextField.CENTER); spinner.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value != null) { try { spinner.setValue(Integer.parseInt(value.toString())); } catch (NumberFormatException e) { System.err.println("SpinnerCE: Invalid num format: " + value); }}
            SwingUtilities.invokeLater(() -> { textField.requestFocusInWindow(); textField.selectAll(); }); return spinner;
        }
        @Override public Object getCellEditorValue() { return spinner.getValue(); }
        @Override public boolean stopCellEditing() {
            try { editor.commitEdit(); } catch (java.text.ParseException e) {
                JOptionPane.showMessageDialog(null, "Неверное значение: " + textField.getText(), "Ошибка", JOptionPane.ERROR_MESSAGE); return false;
            }
            return super.stopCellEditing();
        }
    }   
} 