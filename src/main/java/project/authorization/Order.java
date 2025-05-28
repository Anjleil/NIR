package project.authorization;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int orderId;         // Corresponds to order_id in DB
    private int userId;          // Corresponds to user_id in DB
    private Timestamp orderDate;     // Corresponds to order_date in DB
    private BigDecimal totalAmount;   // Corresponds to total_amount in DB
    private String status;           // Corresponds to status in DB (e.g., "Pending", "Shipped", "Delivered")
    private String deliveryAddress;  // Corresponds to delivery_address in DB
    private String deliveryType;     // Corresponds to delivery_type in DB (e.g., "STANDARD", "EXPRESS")
    private String customerNotes;    // Corresponds to customer_notes in DB
    private Timestamp createdAt;       // Corresponds to created_at in DB (auto by DB)
    private Timestamp updatedAt;       // Corresponds to updated_at in DB (auto by DB)

    // This field is for carrying OrderItem objects when an order is fully loaded.
    // It will not be directly mapped to a single column in the 'orders' table.
    private List<OrderItem> items;

    // Constructor for creating an order before saving to DB (orderId, createdAt, updatedAt are auto-generated)
    public Order(int userId, BigDecimal totalAmount, String status, String deliveryAddress, String deliveryType, String customerNotes) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.deliveryType = deliveryType;
        this.customerNotes = customerNotes;
    }
}