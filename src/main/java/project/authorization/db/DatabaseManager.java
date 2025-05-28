package project.authorization.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DatabaseManager {
    // Replace with your actual database connection details
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/NIR"; // PLEASE REPLACE 'your_database_name'
    private static final String DB_USER = "postgres"; // PLEASE REPLACE
    private static final String DB_PASSWORD = "postgres"; // PLEASE REPLACE

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found. Include it in your library path.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Method to execute the init.sql script
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Connecting to database and initializing schema...");
            
            // Read the init.sql file
            String sqlScript = readSqlScript("/db/init.sql");
            if (sqlScript == null || sqlScript.trim().isEmpty()) {
                System.err.println("SQL script is empty or could not be read.");
                return;
            }

            // Split script into individual statements (basic split, may need refinement for complex SQL)
            String[] individualStatements = sqlScript.split(";(?=(?:[^']*'[^']*')*[^']*$)"); // Basic split on semicolon, handles semicolons in strings
            
            for (String statement : individualStatements) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error reading SQL script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String readSqlScript(String filePath) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                DatabaseManager.class.getResourceAsStream(filePath), StandardCharsets.UTF_8)) {
            if (reader == null || reader.ready() == false) { // Check if resource exists and is ready
                 System.err.println("Cannot find SQL script file: " + filePath + " or it is empty.");
                 return null;
            }
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                 return bufferedReader.lines().collect(Collectors.joining("\n"));
            }
        }  catch (NullPointerException e) {
            System.err.println("Resource stream is null for: " + filePath + ". Check path.");
            throw new IOException("SQL script not found at: " + filePath, e);
        }
    }

    public static void main(String[] args) {
        // This is a simple test to initialize the DB. 
        // You would typically call initializeDatabase() once at application startup.
        initializeDatabase();
    }
} 