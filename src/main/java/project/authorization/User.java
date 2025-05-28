package project.authorization;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int userId;
    private String username;
    private String passwordHash; // Only for DB interaction, not for session
    private String email;
    private String firstName;
    private String lastName;
    private Timestamp createdAt;

    // Constructor for session user (omitting passwordHash)
    public User(int userId, String username, String email, String firstName, String lastName, Timestamp createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
    }
} 