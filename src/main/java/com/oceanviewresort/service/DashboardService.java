package com.oceanviewresort.service;

import com.oceanviewresort.repository.DashboardRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Service layer for Dashboard operations
 * Handles business logic for dashboard statistics
 */
public class DashboardService {

    private final DashboardRepository dashboardRepository = new DashboardRepository();

    /**
     * Get count of rooms by status (AVAILABLE, BOOKED, MAINTENANCE)
     */
    public Map<String, Integer> getRoomStatusCounts() {
        return dashboardRepository.getRoomStatusCounts();
    }

    /**
     * Get count of available rooms
     */
    public int getAvailableRoomsCount() {
        return dashboardRepository.countRoomsByStatus("AVAILABLE");
    }

    /**
     * Get count of booked rooms
     */
    public int getBookedRoomsCount() {
        return dashboardRepository.countRoomsByStatus("BOOKED");
    }

    /**
     * Get count of rooms in maintenance
     */
    public int getMaintenanceRoomsCount() {
        return dashboardRepository.countRoomsByStatus("MAINTENANCE");
    }

    /**
     * Get total count of all rooms
     */
    public int getTotalRoomsCount() {
        return dashboardRepository.countTotalRooms();
    }

    /**
     * Get comprehensive dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();

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

        return statistics;
    }
}
