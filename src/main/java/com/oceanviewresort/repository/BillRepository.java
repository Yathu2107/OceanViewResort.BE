package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.Bill;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;

public class BillRepository {

    public void saveBill(Bill bill) {
        String sql = """
            INSERT INTO bills
            (reservation_id, nights, rate_per_night, total_amount, generated_date)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            ps.setInt(1, bill.getReservation().getReservationId());
            ps.setInt(2, bill.getNumberOfNights());
            ps.setDouble(3, bill.getRoomRatePerNight());
            ps.setDouble(4, bill.getTotalAmount());
            ps.setDate(5, Date.valueOf(bill.getGeneratedDate()));

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to save bill", e);
        }
    }

    public Bill findByReservationId(int reservationId) {
        String sql = """
            SELECT * FROM bills WHERE reservation_id = ?
        """;

        try (PreparedStatement ps =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Bill bill = new Bill();
                bill.setBillId(rs.getInt("id"));
                bill.setNumberOfNights(rs.getInt("nights"));
                bill.setRoomRatePerNight(rs.getDouble("rate_per_night"));
                bill.setTotalAmount(rs.getDouble("total_amount"));
                bill.setGeneratedDate(rs.getDate("generated_date").toLocalDate());

                Reservation reservation = new Reservation();
                reservation.setReservationId(reservationId);
                bill.setReservation(reservation);

                return bill;
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch bill", e);
        }
    }
}