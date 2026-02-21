package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.User;
import com.oceanviewresort.util.DatabaseConnection;
import com.oceanviewresort.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Repository for User entity
 * Handles all database operations for users with proper JDBC best practices
 * Uses UUID for user IDs and BCrypt for password hashing
 */
public class UserRepository {

    /**
     * Find user by username
     * @param username The username to search for
     * @return User object or null if not found
     */
    public User findByUsername(String username) {
        String sql = "SELECT id, name, username, password, role, is_active FROM users WHERE username = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setActive(rs.getBoolean("is_active"));
                return user;
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch user by username: " + username, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find user by UUID
     * @param id The user UUID
     * @return User object or null if not found
     */
    public User findById(String id) {
        String sql = "SELECT id, name, username, password, role, is_active FROM users WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);

            rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setActive(rs.getBoolean("is_active"));
                return user;
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch user by ID: " + id, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Create a new user with UUID and hashed password
     * @param user The user to create
     * @param plainPassword The plain text password to hash
     * @return The UUID of the created user
     */
    public String createUser(User user, String plainPassword) {
        String sql = "INSERT INTO users (id, name, username, password, role, is_active) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            // Generate UUID for new user
            user.generateId();

            // Hash the password
            String hashedPassword = PasswordUtil.hashPassword(plainPassword);
            user.setPassword(hashedPassword);

            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setString(1, user.getId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getUsername());
            ps.setString(4, hashedPassword);
            ps.setString(5, user.getRole());
            ps.setBoolean(6, user.isActive());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Creating user failed, no rows affected");
            }

            return user.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to create user: " + user.getUsername(), e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Update user password with hashing
     * @param userId The user UUID
     * @param plainPassword The new plain text password
     */
    public void updatePassword(String userId, String plainPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            String hashedPassword = PasswordUtil.hashPassword(plainPassword);

            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setString(1, hashedPassword);
            ps.setString(2, userId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("User not found with ID: " + userId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update password for user ID: " + userId, e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Update user active status
     * @param userId The user UUID
     * @param isActive The active status
     */
    public void updateUserStatus(String userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setBoolean(1, isActive);
            ps.setString(2, userId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("User not found with ID: " + userId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user status for ID: " + userId, e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Check if username already exists
     * @param username The username to check
     * @return true if exists, false otherwise
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check username existence: " + username, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Close JDBC resources properly
     */
    private void closeResources(ResultSet rs, PreparedStatement ps, Connection conn) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing ResultSet: " + e.getMessage());
        }

        try {
            if (ps != null) {
                ps.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing PreparedStatement: " + e.getMessage());
        }

        if (conn != null) {
            DatabaseConnection.releaseConnection(conn);
        }
    }
}
