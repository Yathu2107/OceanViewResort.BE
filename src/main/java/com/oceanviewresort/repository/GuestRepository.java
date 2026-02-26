package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.Guest;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Guest entity
 * Handles all database operations for guests with proper JDBC best practices
 */
public class GuestRepository {

    /**
     * Create a new guest
     */
    public int createGuest(Guest guest) {
        String sql = "INSERT INTO guests (name, address, contact_number, email) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, guest.getName());
            ps.setString(2, guest.getAddress());
            ps.setString(3, guest.getContactNumber());
            ps.setString(4, guest.getEmail());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Creating guest failed, no rows affected");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new DatabaseException("Creating guest failed, no ID obtained");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to create guest: " + guest.getName(), e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Update an existing guest
     */
    public void updateGuest(Guest guest) {
        String sql = "UPDATE guests SET name = ?, address = ?, contact_number = ?, email = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setString(1, guest.getName());
            ps.setString(2, guest.getAddress());
            ps.setString(3, guest.getContactNumber());
            ps.setString(4, guest.getEmail());
            ps.setInt(5, guest.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Guest not found with ID: " + guest.getId());
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update guest with ID: " + guest.getId(), e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Find guest by ID
     */
    public Guest findById(int guestId) {
        String sql = "SELECT * FROM guests WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, guestId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToGuest(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch guest by ID: " + guestId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find guest by contact number
     */
    public Guest findByContactNumber(String contactNumber) {
        String sql = "SELECT * FROM guests WHERE contact_number = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, contactNumber);

            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToGuest(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch guest by contact number: " + contactNumber, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Get all guests
     */
    public List<Guest> findAll() {
        List<Guest> guests = new ArrayList<>();
        String sql = "SELECT * FROM guests ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                guests.add(mapResultSetToGuest(rs));
            }

            return guests;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve guests", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Delete guest by ID
     */
    public void deleteById(int guestId) {
        String sql = "DELETE FROM guests WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, guestId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Guest not found with ID: " + guestId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete guest with ID: " + guestId, e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Check if a guest exists by ID
     */
    public boolean existsById(int guestId) {
        String sql = "SELECT COUNT(*) FROM guests WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, guestId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check guest existence for ID: " + guestId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Check if a guest exists by contact number
     */
    public boolean existsByContactNumber(String contactNumber) {
        String sql = "SELECT COUNT(*) FROM guests WHERE contact_number = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, contactNumber);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check guest existence for contact number: " + contactNumber, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Map ResultSet to Guest object
     */
    private Guest mapResultSetToGuest(ResultSet rs) throws SQLException {
        Guest guest = new Guest();
        guest.setId(rs.getInt("id"));
        guest.setName(rs.getString("name"));
        guest.setAddress(rs.getString("address"));
        guest.setContactNumber(rs.getString("contact_number"));
        guest.setEmail(rs.getString("email"));
        return guest;
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
