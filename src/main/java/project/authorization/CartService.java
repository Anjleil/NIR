package project.authorization;

import project.authorization.db.DatabaseManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CartService {

    // Method from ProductSelectionPanel, moved here for centralization
    public void addOrUpdateCartItem(int cartId, int productId, int quantityToAdd) {
        String selectSql = "SELECT quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE cart_id = ? AND product_id = ?";
        String insertSql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            try (PreparedStatement pstmtSelect = conn.prepareStatement(selectSql)) {
                pstmtSelect.setInt(1, cartId);
                pstmtSelect.setInt(2, productId);
                ResultSet rs = pstmtSelect.executeQuery();

                if (rs.next()) { // Item exists, update quantity
                    try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateSql)) {
                        pstmtUpdate.setInt(1, quantityToAdd);
                        pstmtUpdate.setInt(2, cartId);
                        pstmtUpdate.setInt(3, productId);
                        pstmtUpdate.executeUpdate();
                        System.out.println("Updated quantity for product " + productId + " in cart " + cartId);
                    }
                } else { // Item does not exist, insert new
                    try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                        pstmtInsert.setInt(1, cartId);
                        pstmtInsert.setInt(2, productId);
                        pstmtInsert.setInt(3, quantityToAdd);
                        pstmtInsert.executeUpdate();
                        System.out.println("Added product " + productId + " to cart " + cartId);
                    }
                }
            }
            updateCartTimestamp(conn, cartId);
            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            System.err.println("Error adding/updating cart item: " + e.getMessage());
            // Consider rolling back in a real scenario if conn was class-level and not auto-closed
            e.printStackTrace();
            // Rethrow or handle more gracefully for UI
            throw new RuntimeException("Database error while updating cart: " + e.getMessage(), e);
        }
    }

    public List<CartItem> getCartItems(int cartId) {
        List<CartItem> items = new ArrayList<>();
        // Joins cart_items with products to get product details
        String sql = "SELECT ci.cart_item_id, ci.cart_id, ci.product_id, ci.quantity, " +
                     "p.name AS product_name, p.price AS product_price, p.image_url AS product_image_url " +
                     "FROM cart_items ci JOIN products p ON ci.product_id = p.product_id " +
                     "WHERE ci.cart_id = ? ORDER BY ci.added_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cartId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(new CartItem(
                        rs.getInt("cart_item_id"),
                        rs.getInt("cart_id"),
                        rs.getInt("product_id"),
                        rs.getInt("quantity"),
                        rs.getString("product_name"),
                        rs.getBigDecimal("product_price"),
                        rs.getString("product_image_url")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching cart items: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    public void updateCartItemQuantity(int cartItemId, int newQuantity) {
        if (newQuantity <= 0) {
            removeCartItem(cartItemId);
            return;
        }
        String sql = "UPDATE cart_items SET quantity = ? WHERE cart_item_id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, newQuantity);
                pstmt.setInt(2, cartItemId);
                pstmt.executeUpdate();
            }
            // Need cart_id to update timestamp. This could be fetched or passed.
            // For simplicity, we might omit direct timestamp update here or fetch cart_id first.
            // String fetchCartIdSql = "SELECT cart_id FROM cart_items WHERE cart_item_id = ?";
            // int cartId = ... fetch it ...
            // updateCartTimestamp(conn, cartId);
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error updating cart item quantity: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database error while updating cart quantity: " + e.getMessage(), e);
        }
    }

    public void removeCartItem(int cartItemId) {
        String sql = "DELETE FROM cart_items WHERE cart_item_id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
             conn.setAutoCommit(false);
            // Optional: Fetch cart_id before deleting if needed for timestamp update and there are no other items for that cart_id
            // int cartId = ... (fetch cart_id associated with cartItemId)
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, cartItemId);
                pstmt.executeUpdate();
            }
            // updateCartTimestamp(conn, cartId); // If cartId was fetched
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error removing cart item: " + e.getMessage());
            e.printStackTrace();
             throw new RuntimeException("Database error while removing item from cart: " + e.getMessage(), e);
        }
    }

    public void clearCart(int cartId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, cartId);
                pstmt.executeUpdate();
            }
            updateCartTimestamp(conn, cartId);
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error clearing cart: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database error while clearing cart: " + e.getMessage(), e);
        }
    }

    private void updateCartTimestamp(Connection conn, int cartId) throws SQLException {
        String updateCartTimestampSql = "UPDATE cart SET last_updated_at = CURRENT_TIMESTAMP WHERE cart_id = ?";
        try (PreparedStatement pstmtUpdateCart = conn.prepareStatement(updateCartTimestampSql)) {
            pstmtUpdateCart.setInt(1, cartId);
            pstmtUpdateCart.executeUpdate();
        }
    }
    
    public BigDecimal getCartTotal(int cartId) {
        List<CartItem> items = getCartItems(cartId);
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getTotalPrice());
        }
        return total;
    }
} 