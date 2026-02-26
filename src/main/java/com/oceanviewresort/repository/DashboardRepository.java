package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for Dashboard data access
 * Handles all database queries for dashboard statistics
 */
public class DashboardRepository {

    /**
     * Get count of rooms by status
     */
    public Map<String, Integer> getRoomStatusCounts() {
        Map<String, Integer> statusCounts = new HashMap<>();

        // Initialize all counts to 0
        statusCounts.put("AVAILABLE", 0);
        statusCounts.put("BOOKED", 0);
        statusCounts.put("MAINTENANCE", 0);
        statusCounts.put("TOTAL", 0);

        String sql = "SELECT status, COUNT(*) as count FROM rooms GROUP BY status";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            int totalCount = 0;
            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                statusCounts.put(status, count);
                totalCount += count;
            }

            statusCounts.put("TOTAL", totalCount);

            return statusCounts;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve room status counts", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Get count of rooms by specific status
     */
    public int countRoomsByStatus(String status) {
        String sql = "SELECT COUNT(*) as count FROM rooms WHERE status = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

            return 0;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to count rooms by status: " + status, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Get a total count of all rooms
     */
    public int countTotalRooms() {
        String sql = "SELECT COUNT(*) as count FROM rooms";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

            return 0;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to count total rooms", e);
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
