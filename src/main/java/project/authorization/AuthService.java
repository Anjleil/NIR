package project.authorization;

import org.mindrot.jbcrypt.BCrypt;
import project.authorization.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuthService {

    public User login(String username, String password) {
        String sql = "SELECT user_id, username, password_hash, email, first_name, last_name, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, storedHash)) {
                    // Password matches, create user object for session
                    User user = new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getTimestamp("created_at")
                    );
                    // Fetch or create cart for the user
                    int cartId = getOrCreateCartForUser(user.getUserId(), conn);
                    UserSession.getInstance().setCartId(cartId);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Login database error: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Login failed
    }

    private int getOrCreateCartForUser(int userId, Connection conn) throws SQLException {
        // Try to find an existing cart
        String selectCartSql = "SELECT cart_id FROM cart WHERE user_id = ?";
        try (PreparedStatement pstmtSelect = conn.prepareStatement(selectCartSql)) {
            pstmtSelect.setInt(1, userId);
            ResultSet rs = pstmtSelect.executeQuery();
            if (rs.next()) {
                return rs.getInt("cart_id");
            }
        }

        // No cart found, create a new one
        String insertCartSql = "INSERT INTO cart (user_id) VALUES (?) RETURNING cart_id";
        try (PreparedStatement pstmtInsert = conn.prepareStatement(insertCartSql)) {
            pstmtInsert.setInt(1, userId);
            ResultSet rs = pstmtInsert.executeQuery();
            if (rs.next()) {
                return rs.getInt("cart_id");
            }
        }
        throw new SQLException("Could not create or find cart for user: " + userId);
    }

    // Utility to hash a password (e.g., for a registration feature later)
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    // Example: Add this to a main method or a test to generate hash for 'password123'
    // public static void main(String[] args) {
    //     System.out.println(hashPassword("password123"));
    // }

    public User signUp(String username, String email, String plainTextPassword, String firstName, String lastName) {
        // 1. Check if username or email already exists
        String checkUserSql = "SELECT user_id FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmtCheck = conn.prepareStatement(checkUserSql)) {
            pstmtCheck.setString(1, username);
            pstmtCheck.setString(2, email);
            ResultSet rsCheck = pstmtCheck.executeQuery();
            if (rsCheck.next()) {
                System.err.println("Sign up failed: Username or email already exists.");
                // Optionally, you can determine which one exists and provide a more specific message
                throw new RuntimeException("Пользователь с таким именем или email уже существует.");
            }
        }
        catch (SQLException e) {
            System.err.println("Database error during sign up user check: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Ошибка базы данных при проверке пользователя: " + e.getMessage());
        }

        // 2. Hash the password
        String hashedPassword = hashPassword(plainTextPassword);

        // 3. Insert the new user
        String insertUserSql = "INSERT INTO users (username, password_hash, email, first_name, last_name) VALUES (?, ?, ?, ?, ?) RETURNING user_id, created_at";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmtInsert = conn.prepareStatement(insertUserSql)) {
            
            conn.setAutoCommit(false); // Start transaction for user and cart creation

            pstmtInsert.setString(1, username);
            pstmtInsert.setString(2, hashedPassword);
            pstmtInsert.setString(3, email);
            pstmtInsert.setString(4, firstName); // Can be null or empty if not provided
            pstmtInsert.setString(5, lastName);  // Can be null or empty if not provided

            ResultSet rsInsert = pstmtInsert.executeQuery();
            if (rsInsert.next()) {
                int newUserId = rsInsert.getInt("user_id");
                Timestamp createdAt = rsInsert.getTimestamp("created_at");
                
                // 4. Create a cart for the new user (within the same transaction)
                int cartId = getOrCreateCartForUser(newUserId, conn); // Pass the existing connection

                conn.commit(); // Commit transaction

                User newUser = new User(newUserId, username, email, firstName, lastName, createdAt);
                // UserSession.getInstance().setCurrentUser(newUser); // Optional: auto-login after sign up
                // UserSession.getInstance().setCartId(cartId);
                return newUser;
            } else {
                conn.rollback();
                throw new SQLException("User registration failed, no ID obtained.");
            }
        } catch (SQLException e) {
            System.err.println("Database error during sign up: " + e.getMessage());
            e.printStackTrace();
            // Attempt to rollback if connection was established and an error occurred before commit
            // This part is tricky as conn might be auto-closed by try-with-resources before rollback here
            // The rollback in the try block is more reliable for insertUserSql execution error.
            throw new RuntimeException("Ошибка регистрации в базе данных: " + e.getMessage());
        }
    }
} 