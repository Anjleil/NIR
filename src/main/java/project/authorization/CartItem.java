package project.authorization;

import lombok.Data;
// import lombok.NoArgsConstructor; // Removed
// import lombok.AllArgsConstructor; // Removed

import java.math.BigDecimal;

@Data
public class CartItem {
    private int cartItemId; // From cart_items table
    private int cartId;
    private int productId;
    private int quantity;

    // Joined product details for display
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl; // Optional, if you want to display images in cart

    // Default constructor (explicitly defined if NoArgsConstructor is removed)
    public CartItem() {}

    // Constructor for creating from DB result (with joined product details)
    public CartItem(int cartItemId, int cartId, int productId, int quantity, String productName, BigDecimal productPrice, String productImageUrl) {
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.productName = productName;
        this.productPrice = productPrice;
        this.productImageUrl = productImageUrl;
    }

    // Simpler constructor if not joining immediately
    public CartItem(int cartId, int productId, int quantity) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        if (productPrice == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return productPrice.multiply(new BigDecimal(quantity));
    }
} 