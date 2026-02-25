package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Room;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomService {

    /**
     * Create a new room (Manager only)
     */
    public int createRoom(String roomNumber, String roomType, int capacity, double pricePerNight, String status) {
        // Validate inputs
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new ValidationException("Room number is required");
        }
        if (roomType == null || roomType.trim().isEmpty()) {
            throw new ValidationException("Room type is required");
        }
        if (!roomType.equals("SINGLE") && !roomType.equals("DOUBLE") && !roomType.equals("SUITE")) {
            throw new ValidationException("Room type must be SINGLE, DOUBLE, or SUITE");
        }
        if (capacity <= 0) {
            throw new ValidationException("Capacity must be greater than 0");
        }
        if (pricePerNight <= 0) {
            throw new ValidationException("Price per night must be greater than 0");
        }
        if (status == null || status.trim().isEmpty()) {
            status = "AVAILABLE";
        }
        if (!status.equals("AVAILABLE") && !status.equals("BOOKED") && !status.equals("MAINTENANCE")) {
            throw new ValidationException("Status must be AVAILABLE, BOOKED, or MAINTENANCE");
        }

        String sql = "INSERT INTO rooms (room_number, room_type, capacity, price_per_night, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, roomNumber);
            pstmt.setString(2, roomType);
            pstmt.setInt(3, capacity);
            pstmt.setDouble(4, pricePerNight);
            pstmt.setString(5, status);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating room failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating room failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new ValidationException("Room number already exists");
            }
            System.err.println("Database error in createRoom: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create room: " + e.getMessage());
        }
    }

    /**
     * Update an existing room (Manager only)
     */
    public void updateRoom(int roomId, String roomNumber, String roomType, Integer capacity, Double pricePerNight, String status) {
        // Validate inputs
        if (roomType != null && !roomType.trim().isEmpty()) {
            if (!roomType.equals("SINGLE") && !roomType.equals("DOUBLE") && !roomType.equals("SUITE")) {
                throw new ValidationException("Room type must be SINGLE, DOUBLE, or SUITE");
            }
        }
        if (capacity != null && capacity <= 0) {
            throw new ValidationException("Capacity must be greater than 0");
        }
        if (pricePerNight != null && pricePerNight <= 0) {
            throw new ValidationException("Price per night must be greater than 0");
        }
        if (status != null && !status.trim().isEmpty()) {
            if (!status.equals("AVAILABLE") && !status.equals("BOOKED") && !status.equals("MAINTENANCE")) {
                throw new ValidationException("Status must be AVAILABLE, BOOKED, or MAINTENANCE");
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE rooms SET ");
        List<Object> params = new ArrayList<>();

        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            sql.append("room_number = ?, ");
            params.add(roomNumber);
        }
        if (roomType != null && !roomType.trim().isEmpty()) {
            sql.append("room_type = ?, ");
            params.add(roomType);
        }
        if (capacity != null) {
            sql.append("capacity = ?, ");
            params.add(capacity);
        }
        if (pricePerNight != null) {
            sql.append("price_per_night = ?, ");
            params.add(pricePerNight);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("status = ?, ");
            params.add(status);
        }

        if (params.isEmpty()) {
            throw new ValidationException("No fields to update");
        }

        // Remove last comma and space
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(roomId);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new ValidationException("Room not found with ID: " + roomId);
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new ValidationException("Room number already exists");
            }
            System.err.println("Database error in updateRoom: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update room: " + e.getMessage());
        }
    }

    /**
     * Get all rooms
     */
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM rooms ORDER BY room_number";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room();
                room.setId(rs.getInt("id"));
                room.setRoomNumber(rs.getString("room_number"));
                room.setRoomType(rs.getString("room_type"));
                room.setCapacity(rs.getInt("capacity"));
                room.setPricePerNight(rs.getDouble("price_per_night"));
                room.setStatus(rs.getString("status"));
                rooms.add(room);
            }

        } catch (SQLException e) {
            System.err.println("Database error in getAllRooms: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve rooms: " + e.getMessage());
        }

        return rooms;
    }

    /**
     * Get room by ID
     */
    public Room getRoomById(int roomId) {
        String sql = "SELECT * FROM rooms WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setId(rs.getInt("id"));
                    room.setRoomNumber(rs.getString("room_number"));
                    room.setRoomType(rs.getString("room_type"));
                    room.setCapacity(rs.getInt("capacity"));
                    room.setPricePerNight(rs.getDouble("price_per_night"));
                    room.setStatus(rs.getString("status"));
                    return room;
                } else {
                    throw new ValidationException("Room not found with ID: " + roomId);
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error in getRoomById: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve room: " + e.getMessage());
        }
    }

    /**
     * Delete room by ID (Manager only)
     */
    public void deleteRoom(int roomId) {
        String sql = "DELETE FROM rooms WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new ValidationException("Room not found with ID: " + roomId);
            }

        } catch (SQLException e) {
            System.err.println("Database error in deleteRoom: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete room: " + e.getMessage());
        }
    }
}
