package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.Guest;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;

public class ReservationRepository {

    public void saveReservation(Reservation reservation) {
        String sql = """
            INSERT INTO reservations
            (guest_id, room_type, check_in, check_out, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            ps.setInt(1, reservation.getGuest().getId());
            ps.setString(2, reservation.getRoomType());
            ps.setDate(3, Date.valueOf(reservation.getCheckInDate()));
            ps.setDate(4, Date.valueOf(reservation.getCheckOutDate()));
            ps.setString(5, reservation.getStatus());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to save reservation", e);
        }
    }

    public Reservation findById(int reservationId) {
        String sql = """
            SELECT r.*, g.name, g.address, g.contact_number, g.email
            FROM reservations r
            JOIN guests g ON r.guest_id = g.id
            WHERE r.id = ?
        """;

        try (PreparedStatement ps =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Guest guest = new Guest();
                guest.setId(rs.getInt("guest_id"));
                guest.setName(rs.getString("name"));
                guest.setAddress(rs.getString("address"));
                guest.setContactNumber(rs.getString("contact_number"));
                guest.setEmail(rs.getString("email"));

                Reservation reservation = new Reservation();
                reservation.setReservationId(rs.getInt("id"));
                reservation.setGuest(guest);
                reservation.setRoomType(rs.getString("room_type"));
                reservation.setCheckInDate(rs.getDate("check_in").toLocalDate());
                reservation.setCheckOutDate(rs.getDate("check_out").toLocalDate());
                reservation.setStatus(rs.getString("status"));

                return reservation;
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch reservation", e);
        }
    }

    public void updateStatus(int reservationId, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";

        try (PreparedStatement ps =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, reservationId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation status", e);
        }
    }
}