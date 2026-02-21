package com.oceanviewresort.util;

import com.oceanviewresort.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Database Connection Pool using native Java (no frameworks)
 * Implements a simple connection pool for better resource management
 */
public class DatabaseConnection {

    private static final int POOL_SIZE = 10;
    private static final int MAX_TIMEOUT = 5000; // 5 seconds

    private static String DB_URL;
    private static String DB_USERNAME;
    private static String DB_PASSWORD;

    private static final List<Connection> connectionPool = new ArrayList<>();
    private static final List<Connection> usedConnections = new ArrayList<>();
    private static volatile boolean initialized = false;

    static {
        loadDatabaseConfig();
    }

    /**
     * Load database configuration from application.properties
     */
    private static void loadDatabaseConfig() {
        try {
            Properties props = new Properties();
            InputStream input = DatabaseConnection.class
                    .getClassLoader()
                    .getResourceAsStream("application.properties");

            if (input == null) {
                throw new DatabaseException(
                        "application.properties file not found in resources folder");
            }

            props.load(input);

            DB_URL = props.getProperty("db.url");
            DB_USERNAME = props.getProperty("db.username");
            DB_PASSWORD = props.getProperty("db.password");

            if (DB_URL == null || DB_USERNAME == null || DB_PASSWORD == null) {
                throw new DatabaseException(
                        "Database configuration incomplete in application.properties");
            }

            // Load MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (IOException e) {
            throw new DatabaseException("Failed to load database configuration", e);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("MySQL JDBC Driver not found", e);
        }
    }

    /**
     * Initialize the connection pool
     */
    private static synchronized void initializePool() {
        if (!initialized) {
            try {
                for (int i = 0; i < POOL_SIZE; i++) {
                    connectionPool.add(createConnection());
                }
                initialized = true;
                System.out.println("Database connection pool initialized with "
                        + POOL_SIZE + " connections");
            } catch (SQLException e) {
                throw new DatabaseException("Failed to initialize connection pool", e);
            }
        }
    }

    /**
     * Create a new database connection
     */
    private static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    /**
     * Get a connection from the pool
     */
    public static synchronized Connection getConnection() {
        if (!initialized) {
            initializePool();
        }

        if (connectionPool.isEmpty()) {
            try {
                // If pool is empty, create a new connection
                if (usedConnections.size() < POOL_SIZE * 2) {
                    Connection conn = createConnection();
                    usedConnections.add(conn);
                    return conn;
                } else {
                    throw new DatabaseException(
                            "Maximum pool size reached. No available connections.");
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to create new connection", e);
            }
        }

        Connection connection = connectionPool.remove(connectionPool.size() - 1);

        try {
            // Check if connection is still valid
            if (!connection.isValid(2)) {
                connection = createConnection();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to validate connection", e);
        }

        usedConnections.add(connection);
        return connection;
    }

    /**
     * Return a connection back to the pool
     */
    public static synchronized void releaseConnection(Connection connection) {
        if (connection != null) {
            usedConnections.remove(connection);
            connectionPool.add(connection);
        }
    }

    /**
     * Close all connections in the pool (use on application shutdown)
     */
    public static synchronized void closeAllConnections() {
        closeConnections(connectionPool);
        closeConnections(usedConnections);
        System.out.println("All database connections closed");
    }

    private static void closeConnections(List<Connection> connections) {
        for (Connection conn : connections) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        connections.clear();
    }

    /**
     * Get pool statistics
     */
    public static synchronized String getPoolStatistics() {
        return String.format("Connection Pool - Available: %d, In Use: %d",
                connectionPool.size(), usedConnections.size());
    }
}
