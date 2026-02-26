package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.Room;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Room entity
 * Handles all database operations for rooms with proper JDBC best practices
 */
public class RoomRepository {

    /**
     * Create a new room
     */
    public int createRoom(Room room) {
        String sql = "INSERT INTO rooms (room_number, room_type, capacity, price_per_night, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getCapacity());
            ps.setDouble(4, room.getPricePerNight());
            ps.setString(5, room.getStatus());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Creating room failed, no rows affected");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new DatabaseException("Creating room failed, no ID obtained");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to create room: " + room.getRoomNumber(), e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Update an existing room
     */
    public void updateRoom(Room room) {
        String sql = "UPDATE rooms SET room_number = ?, room_type = ?, capacity = ?, price_per_night = ?, status = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getCapacity());
            ps.setDouble(4, room.getPricePerNight());
            ps.setString(5, room.getStatus());
            ps.setInt(6, room.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Room not found with ID: " + room.getId());
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update room with ID: " + room.getId(), e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Find room by ID
     */
    public Room findById(int roomId) {
        String sql = "SELECT * FROM rooms WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, roomId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToRoom(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch room by ID: " + roomId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find room by room number
     */
    public Room findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, roomNumber);

            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToRoom(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch room by room number: " + roomNumber, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Get all rooms
     */
    public List<Room> findAll() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM rooms ORDER BY room_number";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                rooms.add(mapResultSetToRoom(rs));
            }

            return rooms;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve rooms", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Get rooms by status
     */
    public List<Room> findByStatus(String status) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM rooms WHERE status = ? ORDER BY room_number";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            rs = ps.executeQuery();

            while (rs.next()) {
                rooms.add(mapResultSetToRoom(rs));
            }

            return rooms;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve rooms with status: " + status, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Delete room by ID
     */
    public void deleteById(int roomId) {
        String sql = "DELETE FROM rooms WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, roomId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Room not found with ID: " + roomId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete room with ID: " + roomId, e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Check if a room exists by ID
     */
    public boolean existsById(int roomId) {
        String sql = "SELECT COUNT(*) FROM rooms WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, roomId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room existence for ID: " + roomId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Check if a room exists by room number
     */
    public boolean existsByRoomNumber(String roomNumber) {
        String sql = "SELECT COUNT(*) FROM rooms WHERE room_number = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, roomNumber);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room number existence: " + roomNumber, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Get count of rooms by status
     */
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM rooms WHERE status = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
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
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM rooms";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to count total rooms", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Map ResultSet to a Room object
     */
    private Room mapResultSetToRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getInt("id"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setRoomType(rs.getString("room_type"));
        room.setCapacity(rs.getInt("capacity"));
        room.setPricePerNight(rs.getDouble("price_per_night"));
        room.setStatus(rs.getString("status"));
        return room;
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
