package project.authorization;

import lombok.Data;
// import lombok.NoArgsConstructor; // Removed
// import lombok.AllArgsConstructor; // Removed

import java.math.BigDecimal;

@Data
public class OrderItem {
    private int orderItemId;    // Corresponds to order_item_id in DB (PK)
    private int orderId;        // Corresponds to order_id in DB (FK)
    private int productId;      // Corresponds to product_id in DB (FK)
    private int quantity;       // Corresponds to quantity in DB
    private BigDecimal priceAtPurchase; // Corresponds to price_at_purchase in DB

    // Optional: Fields from joined Product table for convenience, if needed when loading order items
    private String productName;

    public OrderItem() {
        // Default constructor if needed elsewhere, or for frameworks
    }

    // Constructor for creating an item before saving to DB (orderItemId is auto-generated)
    public OrderItem(int orderId, int productId, int quantity, BigDecimal priceAtPurchase) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    // Constructor for loading from DB including product name (example)
    public OrderItem(int orderItemId, int orderId, int productId, int quantity, BigDecimal priceAtPurchase, String productName) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
        this.productName = productName;
    }
} 