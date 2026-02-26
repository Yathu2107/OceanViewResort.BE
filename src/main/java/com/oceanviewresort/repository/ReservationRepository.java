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
     * Save a new reservation
     * 
     * @param reservation The reservation to save
     * @return The ID of the created reservation
     */
    public int saveReservation(Reservation reservation) {
        String sql = "INSERT INTO reservations (guest_id, room_id, check_in, check_out, status) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

            ps.setInt(1, reservation.getGuest().getId());
            ps.setInt(2, reservation.getRoomId());
            ps.setDate(3, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(4, Date.valueOf(reservation.getCheckOutDate()));
            ps.setString(5, reservation.getStatus());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Creating reservation failed, no rows affected");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
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
     * Find reservation by ID with guest details
     * 
     * @param reservationId The reservation ID
     * @return Reservation object or null if not found
     */
    public Reservation findById(int reservationId) {
        String sql = "SELECT r.id, r.guest_id, r.room_id, r.check_in, r.check_out, r.status, " +
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
                System.out.println("[DB QUERY] Retrieved guest email: " + rs.getString("email"));
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
     * Find all reservations by guest ID
     * 
     * @param guestId The guest ID
     * @return List of reservations
     */
    public List<Reservation> findByGuestId(int guestId) {
        String sql = "SELECT r.id, r.guest_id, r.room_id, r.check_in, r.check_out, r.status, " +
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
                reservations.add(mapResultSetToReservation(rs));
            }

            return reservations;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch reservations for guest ID: " + guestId, e);
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
     * Update reservation details (room_id, check_in, check_out dates, and status)
     * 
     * @param reservation The updated reservation object
     */
    public void updateReservation(Reservation reservation) {
        String sql = "UPDATE reservations SET room_id = ?, check_in = ?, check_out = ?, status = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setInt(1, reservation.getRoomId());
            ps.setDate(2, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(3, Date.valueOf(reservation.getCheckOutDate()));
            ps.setString(4, reservation.getStatus());
            ps.setInt(5, reservation.getReservationId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Reservation not found with ID: " + reservation.getReservationId());
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
        String sql = "SELECT COUNT(*) FROM reservations " +
                "WHERE room_id = ? " +
                "AND status = 'OCCUPIED' " +
                "AND id != ? " +
                "AND ((check_in <= ? AND check_out >= ?) " +
                "OR (check_in <= ? AND check_out >= ?) " +
                "OR (check_in >= ? AND check_out <= ?))";

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
        String sql = "SELECT COUNT(*) FROM reservations " +
                "WHERE room_id = ? " +
                "AND status = 'OCCUPIED' " +
                "AND ((check_in <= ? AND check_out >= ?) " +
                "OR (check_in <= ? AND check_out >= ?) " +
                "OR (check_in >= ? AND check_out <= ?))";

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
        reservation.setRoomId(rs.getInt("room_id"));
        reservation.setCheckInDate(rs.getDate("check_in").toLocalDate());
        reservation.setCheckOutDate(rs.getDate("check_out").toLocalDate());
        reservation.setStatus(rs.getString("status"));

        return reservation;
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