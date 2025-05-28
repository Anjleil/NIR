package project.authorization;

import project.authorization.db.DatabaseManager;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    private CartService cartService; // To get cart items for creating an order

    public OrderService() {
        this.cartService = new CartService();
    }

    /**
     * Creates an order in the database based on the current user's cart and delivery details.
     * This involves:
     * 1. Starting a database transaction.
     * 2. Inserting a new record into the 'orders' table.
     * 3. Retrieving the generated order_id.
     * 4. For each item in the cart:
     *    a. Inserting a record into the 'order_items' table with the new order_id.
     *    b. Decrementing stock_quantity in the 'products' table.
     * 5. Clearing the user's cart.
     * 6. Committing the transaction (or rolling back if any step fails).
     * @param userId The ID of the user placing the order.
     * @param deliveryAddress The delivery address.
     * @param deliveryType The type of delivery (e.g., "STANDARD", "EXPRESS").
     * @param customerNotes Optional notes from the customer.
     * @return The created Order object with its database-generated ID, or null if creation failed.
     */
    public Order createOrderFromCart(int userId, String deliveryAddress, String deliveryType, String customerNotes) {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser().getUserId() != userId) {
            System.err.println("Order creation attempt by mismatched or logged-out user.");
            return null; // Or throw an exception
        }

        int cartId = session.getCartId();
        List<CartItem> cartItems = cartService.getCartItems(cartId);

        if (cartItems.isEmpty()) {
            System.out.println("Cart is empty. Cannot create order.");
            return null; // Or throw an exception
        }

        BigDecimal totalAmount = cartService.getCartTotal(cartId);
        Order newOrder = null;
        Connection conn = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert into 'orders' table
            String orderSql = "INSERT INTO orders (user_id, total_amount, status, delivery_address, delivery_type, customer_notes) " +
                              "VALUES (?, ?, ?, ?, ?, ?) RETURNING order_id, order_date, created_at, updated_at";
            try (PreparedStatement pstmtOrder = conn.prepareStatement(orderSql)) {
                pstmtOrder.setInt(1, userId);
                pstmtOrder.setBigDecimal(2, totalAmount);
                pstmtOrder.setString(3, "Pending Confirmation"); // Initial status
                pstmtOrder.setString(4, deliveryAddress);
                pstmtOrder.setString(5, deliveryType);
                pstmtOrder.setString(6, customerNotes);

                ResultSet rsOrder = pstmtOrder.executeQuery();
                if (rsOrder.next()) {
                    int orderId = rsOrder.getInt("order_id");
                    Timestamp orderDate = rsOrder.getTimestamp("order_date");
                    Timestamp createdAt = rsOrder.getTimestamp("created_at");
                    Timestamp updatedAt = rsOrder.getTimestamp("updated_at");
                    
                    newOrder = new Order(orderId, userId, orderDate, totalAmount, "Pending Confirmation", 
                                         deliveryAddress, deliveryType, customerNotes, createdAt, updatedAt, new ArrayList<>());
                } else {
                    throw new SQLException("Failed to create order, no ID obtained.");
                }
            }

            // 2. Insert into 'order_items' and update product stock
            String orderItemSql = "INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase) VALUES (?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";

            for (CartItem cartItem : cartItems) {
                // Check stock again before attempting to decrement (critical section)
                try (PreparedStatement pstmtCheckStock = conn.prepareStatement("SELECT stock_quantity FROM products WHERE product_id = ? FOR UPDATE")) {
                    pstmtCheckStock.setInt(1, cartItem.getProductId());
                    ResultSet rsStock = pstmtCheckStock.executeQuery();
                    if (rsStock.next()) {
                        int currentStock = rsStock.getInt("stock_quantity");
                        if (currentStock < cartItem.getQuantity()) {
                            throw new SQLException("Insufficient stock for product ID: " + cartItem.getProductId() + ". Required: " + cartItem.getQuantity() + ", Available: " + currentStock + ". Order rolled back.");
                        }
                    } else {
                         throw new SQLException("Product not found for stock check, ID: " + cartItem.getProductId());
                    }
                }

                // Update stock
                try (PreparedStatement pstmtUpdateStock = conn.prepareStatement(updateStockSql)) {
                    pstmtUpdateStock.setInt(1, cartItem.getQuantity());
                    pstmtUpdateStock.setInt(2, cartItem.getProductId());
                    pstmtUpdateStock.setInt(3, cartItem.getQuantity()); // Ensure stock_quantity >= quantity to avoid going negative
                    int rowsAffected = pstmtUpdateStock.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new SQLException("Failed to update stock or insufficient stock for product ID: " + cartItem.getProductId() + ". Order rolled back.");
                    }
                }
                
                // Insert order item
                try (PreparedStatement pstmtItem = conn.prepareStatement(orderItemSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmtItem.setInt(1, newOrder.getOrderId());
                    pstmtItem.setInt(2, cartItem.getProductId());
                    pstmtItem.setInt(3, cartItem.getQuantity());
                    pstmtItem.setBigDecimal(4, cartItem.getProductPrice()); // Price from cart item (which should be current product price)
                    pstmtItem.executeUpdate();
                    
                    ResultSet generatedKeys = pstmtItem.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        newOrder.getItems().add(new OrderItem(generatedKeys.getInt(1), newOrder.getOrderId(), cartItem.getProductId(), cartItem.getQuantity(), cartItem.getProductPrice(), cartItem.getProductName()));
                    } else {
                        throw new SQLException("Creating order item failed, no ID obtained.");
                    }
                }
            }

            // 3. Clear the cart
            cartService.clearCart(cartId); // This uses its own connection, which is not ideal in a transaction.
                                          // For true atomicity, clearCart should accept a Connection argument.
                                          // For now, we proceed, but this is a refactoring point for robust transactions.

            conn.commit(); // Commit transaction
            System.out.println("Order created successfully with ID: " + newOrder.getOrderId());

        } catch (SQLException e) {
            System.err.println("Order creation failed: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    System.err.println("Transaction is being rolled back");
                    conn.rollback();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
            }
            newOrder = null; // Ensure null is returned on failure
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return newOrder;
    }

    public List<Order> getOrdersForUser(int userId) {
        List<Order> userOrders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Order order = new Order(
                        rs.getInt("order_id"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("order_date"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getString("delivery_address"),
                        rs.getString("delivery_type"),
                        rs.getString("customer_notes"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at"),
                        new ArrayList<>() // Initialize empty items list, to be populated separately
                );
                // Optionally, fetch order items here or make it a separate call
                order.setItems(getOrderItemsForOrder(conn, order.getOrderId())); 
                userOrders.add(order);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching orders for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return userOrders;
    }

    public List<OrderItem> getOrderItemsForOrder(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        // Join with products to get product name, if desired for display
        String sql = "SELECT oi.*, p.name as product_name " +
                     "FROM order_items oi JOIN products p ON oi.product_id = p.product_id " +
                     "WHERE oi.order_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(new OrderItem(
                        rs.getInt("order_item_id"),
                        rs.getInt("order_id"),
                        rs.getInt("product_id"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price_at_purchase"),
                        rs.getString("product_name")
                ));
            }
        }
        // No new connection created here, uses the one passed in.
        return items;
    }
} 