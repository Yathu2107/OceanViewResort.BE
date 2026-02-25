package com.oceanviewresort.service;

import com.oceanviewresort.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DashboardService {

    /**
     * Get count of rooms by status (AVAILABLE, BOOKED, MAINTENANCE)
     */
    public Map<String, Integer> getRoomStatusCounts() {
        Map<String, Integer> statusCounts = new HashMap<>();

        // Initialize all counts to 0
        statusCounts.put("AVAILABLE", 0);
        statusCounts.put("BOOKED", 0);
        statusCounts.put("MAINTENANCE", 0);
        statusCounts.put("TOTAL", 0);

        String sql = "SELECT status, COUNT(*) as count FROM rooms GROUP BY status";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int totalCount = 0;
            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                statusCounts.put(status, count);
                totalCount += count;
            }

            statusCounts.put("TOTAL", totalCount);

        } catch (SQLException e) {
            System.err.println("Database error in getRoomStatusCounts: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve room status counts: " + e.getMessage());
        }

        return statusCounts;
    }

    /**
     * Get count of available rooms
     */
    public int getAvailableRoomsCount() {
        String sql = "SELECT COUNT(*) as count FROM rooms WHERE status = 'AVAILABLE'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("Database error in getAvailableRoomsCount: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve available rooms count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get count of booked rooms
     */
    public int getBookedRoomsCount() {
        String sql = "SELECT COUNT(*) as count FROM rooms WHERE status = 'BOOKED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("Database error in getBookedRoomsCount: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve booked rooms count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get count of rooms in maintenance
     */
    public int getMaintenanceRoomsCount() {
        String sql = "SELECT COUNT(*) as count FROM rooms WHERE status = 'MAINTENANCE'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("Database error in getMaintenanceRoomsCount: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve maintenance rooms count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get total count of all rooms
     */
    public int getTotalRoomsCount() {
        String sql = "SELECT COUNT(*) as count FROM rooms";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("Database error in getTotalRoomsCount: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve total rooms count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get comprehensive dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Get room status counts
            Map<String, Integer> roomStatusCounts = getRoomStatusCounts();
            statistics.put("room_statistics", roomStatusCounts);

            // Get total rooms count
            int totalRooms = getTotalRoomsCount();
            statistics.put("total_rooms", totalRooms);

            // Calculate percentages
            if (totalRooms > 0) {
                Map<String, Double> percentages = new HashMap<>();
                percentages.put("available_percentage",
                    (roomStatusCounts.get("AVAILABLE") * 100.0) / totalRooms);
                percentages.put("booked_percentage",
                    (roomStatusCounts.get("BOOKED") * 100.0) / totalRooms);
                percentages.put("maintenance_percentage",
                    (roomStatusCounts.get("MAINTENANCE") * 100.0) / totalRooms);
                statistics.put("percentages", percentages);
            }

        } catch (Exception e) {
            System.err.println("Error in getDashboardStatistics: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve dashboard statistics: " + e.getMessage());
        }

        return statistics;
    }
}
