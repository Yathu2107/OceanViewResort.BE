package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.Bill;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Bill entity
 * Handles all database operations for bills with proper JDBC best practices
 */
public class BillRepository {

    /**
     * Save a new bill
     *
     * @param bill The bill to save
     */
    public void saveBill(Bill bill) {
        String sql = "INSERT INTO bills (reservation_id, nights, rate_per_night, total_amount, generated_date) " +
                     "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

            ps.setInt(1, bill.getReservation().getReservationId());
            ps.setInt(2, bill.getNumberOfNights());
            ps.setDouble(3, bill.getRoomRatePerNight());
            ps.setDouble(4, bill.getTotalAmount());
            ps.setDate(5, Date.valueOf(bill.getGeneratedDate()));

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Creating bill failed, no rows affected");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                rs.getInt(1);
            } else {
                throw new DatabaseException("Creating bill failed, no ID obtained");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to save bill", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find bill by ID
     * @param billId The bill ID
     * @return Bill object or null if not found
     */
    public Bill findById(int billId) {
        String sql = "SELECT id, reservation_id, nights, rate_per_night, total_amount, generated_date " +
                     "FROM bills WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, billId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToBill(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch bill with ID: " + billId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find bill by reservation ID
     * @param reservationId The reservation ID
     * @return Bill object or null if not found
     */
    public Bill findByReservationId(int reservationId) {
        String sql = "SELECT id, reservation_id, nights, rate_per_night, total_amount, generated_date " +
                     "FROM bills WHERE reservation_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reservationId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToBill(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch bill for reservation ID: " + reservationId, e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Find all bills
     * @return List of all bills
     */
    public List<Bill> findAll() {
        String sql = "SELECT id, reservation_id, nights, rate_per_night, total_amount, generated_date " +
                     "FROM bills ORDER BY generated_date DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Bill> bills = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

            return bills;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all bills", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Update bill payment status
     * @param billId The bill ID
     * @param isPaid Payment status
     */
    public void updatePaymentStatus(int billId, boolean isPaid) {
        String sql = "UPDATE bills SET is_paid = ?, payment_date = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setBoolean(1, isPaid);
            ps.setDate(2, isPaid ? Date.valueOf(java.time.LocalDate.now()) : null);
            ps.setInt(3, billId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Bill not found with ID: " + billId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update payment status for bill ID: " + billId, e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Delete a bill
     * @param billId The bill ID
     */
    public void deleteBill(int billId) {
        String sql = "DELETE FROM bills WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, billId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Bill not found with ID: " + billId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete bill with ID: " + billId, e);
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Calculate total revenue
     * @return Total revenue amount
     */
    public double calculateTotalRevenue() {
        String sql = "SELECT SUM(total_amount) as total FROM bills";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
            return 0.0;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to calculate total revenue", e);
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * Map ResultSet to a Bill object
     */
    private Bill mapResultSetToBill(ResultSet rs) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId(rs.getInt("id"));
        bill.setNumberOfNights(rs.getInt("nights"));
        bill.setRoomRatePerNight(rs.getDouble("rate_per_night"));
        bill.setTotalAmount(rs.getDouble("total_amount"));
        bill.setGeneratedDate(rs.getDate("generated_date").toLocalDate());

        Reservation reservation = new Reservation();
        reservation.setReservationId(rs.getInt("reservation_id"));
        bill.setReservation(reservation);

        return bill;
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