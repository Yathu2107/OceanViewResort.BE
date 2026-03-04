package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.Guest;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Reservation entity
 * Handles all database operations for reservations with proper JDBC best
 * practices
 */
public class ReservationRepository {

    /**
     * Save a new reservation (without rooms)
     * Rooms are added separately via addRoomsToReservation()
     * 
     * @param reservation The reservation to save (must have roomIds populated for
     *                    this to work with new schema)
     * @return The ID of the created reservation
     */
    public int saveReservation(Reservation reservation) {
        String sql = "INSERT INTO reservations (guest_id, check_in, check_out, status) " +
                "VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

            ps.setInt(1, reservation.getGuest().getId());
            ps.setDate(2, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(3, Date.valueOf(reservation.getCheckOutDate()));
            ps.setString(4, reservation.getStatus());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Creating reservation failed, no rows affected");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int reservationId = rs.getInt(1);

                // Add rooms to the reservation using junction table
                if (reservation.getRoomIds() != null && !reservation.getRoomIds().isEmpty()) {
                    addRoomsToReservation(reservationId, reservation.getRoomIds());
                }

                return reservationId;
            } else {
                throw new DatabaseException("Creating reservation failed, no ID obtained");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to save reservation", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find reservation by ID with guest details and all associated rooms
     * 
     * @param reservationId The reservation ID
     * @return Reservation object or null if not found
     */
    public Reservation findById(int reservationId) {
        String sql = "SELECT r.id, r.guest_id, r.check_in, r.check_out, r.status, " +
                "g.name, g.address, g.contact_number, g.email " +
                "FROM reservations r " +
                "JOIN guests g ON r.guest_id = g.id " +
                "WHERE r.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reservationId);

            System.out.println("[DB QUERY] Executing: SELECT reservation with guest details for ID: " + reservationId);

            rs = ps.executeQuery();

            if (rs.next()) {
                Reservation reservation = mapResultSetToReservation(rs);

                // Fetch all rooms for this reservation
                List<Integer> roomIds = getRoomsByReservationId(reservationId);
                reservation.setRoomIds(roomIds);

                // Fetch actual room numbers for email display
                List<String> roomNumbers = getRoomNumbersByReservationId(reservationId);
                reservation.setRoomNumbers(roomNumbers);

                System.out.println("[DB QUERY] Retrieved guest email: " + rs.getString("email") +
                        ", Rooms: " + roomIds.size());
                return reservation;
            }
            System.out.println("[DB QUERY] No reservation found with ID: " + reservationId);
            return null;

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to fetch reservation with ID: " + reservationId);
            System.err.println("[DB ERROR] SQL Error: " + e.getMessage());
            throw new DatabaseException("Failed to fetch reservation with ID: " + reservationId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find all reservations by guest ID with rooms
     * 
     * @param guestId The guest ID
     * @return List of reservations
     */
    public List<Reservation> findByGuestId(int guestId) {
        String sql = "SELECT r.id, r.guest_id, r.check_in, r.check_out, r.status, " +
                "g.name, g.address, g.contact_number, g.email " +
                "FROM reservations r " +
                "JOIN guests g ON r.guest_id = g.id " +
                "WHERE r.guest_id = ? " +
                "ORDER BY r.check_in DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Reservation> reservations = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, guestId);

            rs = ps.executeQuery();

            while (rs.next()) {
                Reservation reservation = mapResultSetToReservation(rs);
                // Fetch rooms for each reservation
                List<Integer> roomIds = getRoomsByReservationId(reservation.getReservationId());
                reservation.setRoomIds(roomIds);
                reservations.add(reservation);
            }

            return reservations;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch reservations for guest ID: " + guestId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find all reservations with guest details and associated rooms
     *
     * @return List of all reservations ordered by check-in date descending
     */
    public List<Reservation> findAll() {
        String sql = "SELECT r.id, r.guest_id, r.check_in, r.check_out, r.status, " +
                "g.name, g.address, g.contact_number, g.email " +
                "FROM reservations r " +
                "JOIN guests g ON r.guest_id = g.id " +
                "ORDER BY r.check_in DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Reservation> reservations = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                Reservation reservation = mapResultSetToReservation(rs);
                List<Integer> roomIds = getRoomsByReservationId(reservation.getReservationId());
                reservation.setRoomIds(roomIds);
                reservations.add(reservation);
            }

            return reservations;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all reservations", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Update reservation status
     * 
     * @param reservationId The reservation ID
     * @param status        The new status
     */
    public void updateStatus(int reservationId, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setString(1, status);
            ps.setInt(2, reservationId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Reservation not found with ID: " + reservationId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation status for ID: " + reservationId, e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Update reservation details (check_in, check_out dates, and status)
     * Rooms are updated separately via updateRoomsForReservation()
     * 
     * @param reservation The updated reservation object
     */
    public void updateReservation(Reservation reservation) {
        String sql = "UPDATE reservations SET check_in = ?, check_out = ?, status = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setDate(1, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(2, Date.valueOf(reservation.getCheckOutDate()));
            ps.setString(3, reservation.getStatus());
            ps.setInt(4, reservation.getReservationId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Reservation not found with ID: " + reservation.getReservationId());
            }

            // Update rooms if provided
            if (reservation.getRoomIds() != null && !reservation.getRoomIds().isEmpty()) {
                updateRoomsForReservation(reservation.getReservationId(), reservation.getRoomIds());
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation with ID: " + reservation.getReservationId(), e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Delete reservation (soft delete by updating status)
     * 
     * @param reservationId The reservation ID
     */
    public void deleteReservation(int reservationId) {
        updateStatus(reservationId, "CANCELLED");
    }

    /**
     * Check room availability for given dates, excluding a specific reservation
     * Used when updating an existing reservation
     * 
     * @param roomId    The room ID
     * @param checkIn   Check-in date
     * @param checkOut  Check-out date
     * @param excludeId The reservation ID to exclude from the check
     * @return true if room is available, false otherwise
     */
    public boolean isRoomAvailableForUpdate(int roomId, Date checkIn, Date checkOut, int excludeId) {
        String sql = "SELECT COUNT(*) FROM reservations r " +
                "JOIN reservation_rooms rr ON r.id = rr.reservation_id " +
                "WHERE rr.room_id = ? " +
                "AND r.status = 'OCCUPIED' " +
                "AND r.id != ? " +
                "AND ((r.check_in <= ? AND r.check_out >= ?) " +
                "OR (r.check_in <= ? AND r.check_out >= ?) " +
                "OR (r.check_in >= ? AND r.check_out <= ?))";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setInt(1, roomId);
            ps.setInt(2, excludeId);
            ps.setDate(3, checkOut);
            ps.setDate(4, checkIn);
            ps.setDate(5, checkOut);
            ps.setDate(6, checkIn);
            ps.setDate(7, checkIn);
            ps.setDate(8, checkOut);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room availability for update", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Check room availability for given dates
     * 
     * @param roomId   The room ID
     * @param checkIn  Check-in date
     * @param checkOut Check-out date
     * @return true if room is available, false otherwise
     */
    public boolean isRoomAvailable(int roomId, Date checkIn, Date checkOut) {
        String sql = "SELECT COUNT(*) FROM reservations r " +
                "JOIN reservation_rooms rr ON r.id = rr.reservation_id " +
                "WHERE rr.room_id = ? " +
                "AND r.status = 'OCCUPIED' " +
                "AND ((r.check_in <= ? AND r.check_out >= ?) " +
                "OR (r.check_in <= ? AND r.check_out >= ?) " +
                "OR (r.check_in >= ? AND r.check_out <= ?))";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setInt(1, roomId);
            ps.setDate(2, checkOut);
            ps.setDate(3, checkIn);
            ps.setDate(4, checkOut);
            ps.setDate(5, checkIn);
            ps.setDate(6, checkIn);
            ps.setDate(7, checkOut);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room availability", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Check if reservation exists by ID
     * 
     * @param reservationId The reservation ID
     * @return true if exists, false otherwise
     */
    public boolean existsById(int reservationId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reservationId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check if reservation exists with ID: " + reservationId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Map ResultSet to Reservation object
     * Note: Doesn't include roomIds - fetch separately using
     * getRoomsByReservationId()
     */
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Guest guest = new Guest();
        guest.setId(rs.getInt("guest_id"));
        guest.setName(rs.getString("name"));
        guest.setAddress(rs.getString("address"));
        guest.setContactNumber(rs.getString("contact_number"));
        guest.setEmail(rs.getString("email"));

        Reservation reservation = new Reservation();
        reservation.setReservationId(rs.getInt("id"));
        reservation.setGuest(guest);
        reservation.setCheckInDate(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOutDate(rs.getDate("check_out").toLocalDate());
        reservation.setStatus(rs.getString("status"));
        // roomIds are fetched separately in findById()

        return reservation;
    }

    /**
     * Add rooms to a reservation (insert into junction table)
     * 
     * @param reservationId The reservation ID
     * @param roomIds       List of room IDs to add
     */
    public void addRoomsToReservation(int reservationId, List<Integer> roomIds) {
        String sql = "INSERT INTO reservation_rooms (reservation_id, room_id) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            for (Integer roomId : roomIds) {
                ps.setInt(1, reservationId);
                ps.setInt(2, roomId);
                ps.addBatch();
            }

            int[] affectedRows = ps.executeBatch();
            System.out.println("[DB QUERY] Added " + affectedRows.length + " rooms to reservation " + reservationId);

        } catch (SQLException e) {
            throw new DatabaseException("Failed to add rooms to reservation", e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Get all room IDs for a specific reservation
     * 
     * @param reservationId The reservation ID
     * @return List of room IDs
     */
    public List<Integer> getRoomsByReservationId(int reservationId) {
        String sql = "SELECT room_id FROM reservation_rooms WHERE reservation_id = ? ORDER BY room_id";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Integer> roomIds = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reservationId);

            rs = ps.executeQuery();

            while (rs.next()) {
                roomIds.add(rs.getInt("room_id"));
            }

            return roomIds;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch rooms for reservation ID: " + reservationId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Fetch actual room numbers (e.g., "101", "102") for the given reservation.
     * Joins reservation_rooms with rooms to get the room_number column.
     */
    public List<String> getRoomNumbersByReservationId(int reservationId) {
        String sql = "SELECT r.room_number FROM rooms r " +
                "JOIN reservation_rooms rr ON r.id = rr.room_id " +
                "WHERE rr.reservation_id = ? ORDER BY r.room_number";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<String> roomNumbers = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reservationId);

            rs = ps.executeQuery();

            while (rs.next()) {
                roomNumbers.add(rs.getString("room_number"));
            }

            return roomNumbers;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch room numbers for reservation ID: " + reservationId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Update rooms for a reservation (remove old, add new)
     * 
     * @param reservationId The reservation ID
     * @param newRoomIds    List of new room IDs
     */
    public void updateRoomsForReservation(int reservationId, List<Integer> newRoomIds) {
        // First, remove all existing rooms
        String deleteSql = "DELETE FROM reservation_rooms WHERE reservation_id = ?";

        Connection conn = null;
        PreparedStatement deletePs = null;

        try {
            conn = DatabaseConnection.getConnection();
            deletePs = conn.prepareStatement(deleteSql);
            deletePs.setInt(1, reservationId);
            deletePs.executeUpdate();

            // Then add new rooms
            if (newRoomIds != null && !newRoomIds.isEmpty()) {
                addRoomsToReservation(reservationId, newRoomIds);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update rooms for reservation", e);
        } finally {
            closeResources(null, deletePs, conn);
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