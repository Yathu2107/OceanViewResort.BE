package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.repository.ReservationRepository;
import com.oceanviewresort.repository.RoomRepository;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Service layer for Reservation operations
 * Handles business logic and validation for reservation management
 * Status values: OCCUPIED, COMPLETED, CANCELLED
 */
public class ReservationService {

    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final EmailService emailService = new EmailService();

    /**
     * Add a new reservation
     * Validates guest, room, and date information
     * Sets initial status to OCCUPIED
     */
    public void addReservation(Reservation reservation) {

        if (reservation.getGuest() == null || reservation.getGuest().getId() <= 0) {
            throw new ValidationException("Guest details are required");
        }

        if (reservation.getRoomId() <= 0) {
            throw new ValidationException("Room ID is required");
        }

        // Verify room exists
        if (roomRepository.findById(reservation.getRoomId()) == null) {
            throw new ValidationException("Room not found with ID: " + reservation.getRoomId());
        }

        if (reservation.getCheckInDate() == null) {
            throw new ValidationException("Check-in date is required");
        }

        if (reservation.getCheckOutDate() == null) {
            throw new ValidationException("Check-out date is required");
        }

        if (reservation.getCheckOutDate().isBefore(reservation.getCheckInDate()) ||
                reservation.getCheckOutDate().equals(reservation.getCheckInDate())) {
            throw new ValidationException("Check-out date must be after check-in date");
        }

        // Check room availability
        java.sql.Date checkInSql = Date.valueOf(reservation.getCheckInDate());
        java.sql.Date checkOutSql = Date.valueOf(reservation.getCheckOutDate());

        if (!reservationRepository.isRoomAvailable(reservation.getRoomId(), checkInSql, checkOutSql)) {
            throw new ValidationException("Room is not available for the selected dates");
        }

        // Set initial status to OCCUPIED
        reservation.setStatus("OCCUPIED");
        int reservationId = reservationRepository.saveReservation(reservation);

        System.out.println("[RESERVATION] Reservation created successfully. ID: " + reservationId);
        System.out.println("[RESERVATION] Triggering email notification to guest...");

        // Fetch the complete reservation with guest details for email
        Reservation savedReservation = reservationRepository.findById(reservationId);

        // Log what we retrieved
        if (savedReservation != null) {
            System.out
                    .println("[RESERVATION] Retrieved reservation from DB. ID: " + savedReservation.getReservationId());
            if (savedReservation.getGuest() != null) {
                System.out.println("[RESERVATION] Guest found. ID: " + savedReservation.getGuest().getId() +
                        ", Name: " + savedReservation.getGuest().getName() +
                        ", Email: " + savedReservation.getGuest().getEmail());
            } else {
                System.out.println("[RESERVATION] ERROR: Guest is NULL in retrieved reservation!");
            }
        } else {
            System.out.println("[RESERVATION] ERROR: Failed to retrieve reservation from database!");
        }

        // Send reservation confirmation email with complete guest details
        if (savedReservation != null) {
            emailService.sendReservationConfirmation(savedReservation);
        } else {
            System.err.println("[RESERVATION] Failed to fetch saved reservation for email notification");
        }
    }

    /**
     * Get reservation by ID
     */
    public Reservation getReservation(int reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId);

        if (reservation == null) {
            throw new ValidationException("Reservation not found");
        }

        return reservation;
    }

    /**
     * Mark reservation as COMPLETED
     * Use this when guest checks out or reservation ends
     */
    public void completeReservation(int reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new ValidationException("Reservation not found with ID: " + reservationId);
        }
        reservationRepository.updateStatus(reservationId, "COMPLETED");
    }

    /**
     * Update an existing reservation
     * Validates room, dates, status, and availability
     * Can update room_id, check-in/check-out dates, and status
     * Cannot update guest_id through this method
     * Manager role required to update TO/FROM COMPLETED status
     */
    public void updateReservation(int reservationId, String userRole, Integer roomId, LocalDate checkInDate,
            LocalDate checkOutDate, String newStatus) {
        // Check if reservation exists
        if (!reservationRepository.existsById(reservationId)) {
            throw new ValidationException("Reservation not found with ID: " + reservationId);
        }

        // Get existing reservation
        Reservation existingReservation = reservationRepository.findById(reservationId);
        if (existingReservation == null) {
            throw new ValidationException("Reservation not found with ID: " + reservationId);
        }

        // Authorization check: Manager required for COMPLETED status changes
        if (newStatus != null) {
            // Validate status value
            if (!newStatus.equals("OCCUPIED") && !newStatus.equals("COMPLETED") && !newStatus.equals("CANCELLED")) {
                throw new ValidationException("Invalid status. Must be OCCUPIED, COMPLETED, or CANCELLED");
            }

            // Manager required if changing TO COMPLETED or FROM COMPLETED
            if ("COMPLETED".equals(newStatus) || "COMPLETED".equals(existingReservation.getStatus())) {
                if (userRole == null || !"MANAGER".equals(userRole)) {
                    throw new ValidationException("Only managers can change COMPLETED status");
                }
            }

            // CANCELLED reservations cannot be updated to anything else (but can be changed
            // by manager)
            if ("CANCELLED".equals(existingReservation.getStatus()) && !newStatus.equals("CANCELLED")) {
                if (userRole == null || !"MANAGER".equals(userRole)) {
                    throw new ValidationException("Cannot update a cancelled reservation");
                }
            }

            existingReservation.setStatus(newStatus);
        } else {
            // If no status provided, apply authorization check for existing status
            // Only OCCUPIED reservations can be updated by all users
            // COMPLETED reservations can only be updated by MANAGER role
            if ("COMPLETED".equals(existingReservation.getStatus())) {
                if (userRole == null || !"MANAGER".equals(userRole)) {
                    throw new ValidationException("Only managers can update completed reservations");
                }
            }

            // CANCELLED reservations cannot be updated by anyone
            if ("CANCELLED".equals(existingReservation.getStatus())) {
                throw new ValidationException("Cannot update a cancelled reservation");
            }
        }

        // Validate and set room_id if provided
        if (roomId != null && roomId > 0) {
            if (roomRepository.findById(roomId) == null) {
                throw new ValidationException("Room not found with ID: " + roomId);
            }
            existingReservation.setRoomId(roomId);
        }

        // Validate and set check-in date
        if (checkInDate != null) {
            existingReservation.setCheckInDate(checkInDate);
        }

        // Validate and set check-out date
        if (checkOutDate != null) {
            existingReservation.setCheckOutDate(checkOutDate);
        }

        // Validate date logic
        if (existingReservation.getCheckOutDate().isBefore(existingReservation.getCheckInDate()) ||
                existingReservation.getCheckOutDate().equals(existingReservation.getCheckInDate())) {
            throw new ValidationException("Check-out date must be after check-in date");
        }

        // Check room availability for the new dates (excluding current reservation)
        // Only validate availability if room_id or dates were changed
        if (roomId != null || checkInDate != null || checkOutDate != null) {
            java.sql.Date checkInSql = Date.valueOf(existingReservation.getCheckInDate());
            java.sql.Date checkOutSql = Date.valueOf(existingReservation.getCheckOutDate());

            if (!reservationRepository.isRoomAvailableForUpdate(existingReservation.getRoomId(), checkInSql,
                    checkOutSql,
                    reservationId)) {
                throw new ValidationException("Room is not available for the selected dates");
            }
        }

        // Update in database
        reservationRepository.updateReservation(existingReservation);
    }

    /**
     * Cancel/delete reservation (marks as CANCELLED)
     * Note: This only updates the status to CANCELLED, does not change other fields
     */
    public void cancelReservation(int reservationId) {
        // Check if reservation exists
        if (!reservationRepository.existsById(reservationId)) {
            throw new ValidationException("Reservation not found with ID: " + reservationId);
        }

        // Update status to CANCELLED
        reservationRepository.updateStatus(reservationId, "CANCELLED");
    }
}