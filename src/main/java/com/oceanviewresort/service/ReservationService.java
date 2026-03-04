package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.model.Bill;
import com.oceanviewresort.repository.ReservationRepository;
import com.oceanviewresort.repository.RoomRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service layer for Reservation operations
 * Handles business logic and validation for reservation management
 * Status values: OCCUPIED, COMPLETED, CANCELLED
 */
public class ReservationService {

    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final EmailService emailService = new EmailService();
    private final com.oceanviewresort.repository.BillRepository billRepository = new com.oceanviewresort.repository.BillRepository();

    /**
     * Add a new reservation with multiple rooms
     * Validates guest, rooms, and date information
     * Sets initial status to OCCUPIED
     */
    public void addReservation(Reservation reservation) {

        if (reservation.getGuest() == null || reservation.getGuest().getId() <= 0) {
            throw new ValidationException("Guest details are required");
        }

        if (reservation.getRoomIds() == null || reservation.getRoomIds().isEmpty()) {
            throw new ValidationException("At least one room ID is required");
        }

        // Verify all rooms exist and validate each room
        for (Integer roomId : reservation.getRoomIds()) {
            if (roomId <= 0) {
                throw new ValidationException("Invalid room ID: " + roomId);
            }

            // Verify room exists
            com.oceanviewresort.model.Room room = roomRepository.findById(roomId);
            if (room == null) {
                throw new ValidationException("Room not found with ID: " + roomId);
            }

            // Check room status - only AVAILABLE rooms can be booked
            if (!room.getStatus().equalsIgnoreCase("AVAILABLE")) {
                throw new ValidationException("Room with ID " + roomId
                        + " is not available for booking. Current status: " + room.getStatus());
            }
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

        // Check availability for all rooms
        java.sql.Date checkInSql = java.sql.Date.valueOf(reservation.getCheckInDate());
        java.sql.Date checkOutSql = java.sql.Date.valueOf(reservation.getCheckOutDate());

        for (Integer roomId : reservation.getRoomIds()) {
            if (!reservationRepository.isRoomAvailable(roomId, checkInSql, checkOutSql)) {
                throw new ValidationException("Room with ID " + roomId + " is not available for the selected dates");
            }
        }

        // Set initial status to OCCUPIED
        reservation.setStatus("OCCUPIED");
        int reservationId = reservationRepository.saveReservation(reservation);

        System.out.println("[RESERVATION] Reservation created successfully. ID: " + reservationId);
        System.out.println("[RESERVATION] Assigned " + reservation.getRoomIds().size() + " rooms to reservation");
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
     * Get all reservations
     * 
     * @return List of all reservations
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
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
     * Complete reservation with billing and email notification
     * Generates final bill using room prices from database, sends to guest email,
     * and updates status to COMPLETED
     * 
     * @param reservationId The reservation ID to complete
     * @return Bill object with all details (uses actual room prices from database)
     */
    public Bill completeReservationWithBilling(int reservationId) {
        // Verify reservation exists
        if (!reservationRepository.existsById(reservationId)) {
            throw new ValidationException("Reservation not found with ID: " + reservationId);
        }

        // Get full reservation details
        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("Failed to retrieve reservation details");
        }

        // Only OCCUPIED reservations can be completed
        if (!"OCCUPIED".equals(reservation.getStatus())) {
            throw new ValidationException(
                    "Only OCCUPIED reservations can be completed. Current status: " + reservation.getStatus());
        }

        // Calculate number of nights
        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate());

        if (nights <= 0) {
            throw new ValidationException("Invalid reservation dates for billing");
        }

        // Fetch room prices from database for each reserved room
        double totalRoomPrice = 0.0;
        java.util.List<com.oceanviewresort.model.Bill.RoomDetail> roomDetails = new java.util.ArrayList<>();

        for (Integer roomId : reservation.getRoomIds()) {
            com.oceanviewresort.model.Room room = roomRepository.findById(roomId);
            if (room == null) {
                throw new ValidationException("Room not found with ID: " + roomId);
            }
            totalRoomPrice += room.getPricePerNight();
            double roomTotal = nights * room.getPricePerNight();
            roomDetails.add(new com.oceanviewresort.model.Bill.RoomDetail(
                    roomId, room.getRoomNumber(), room.getPricePerNight(), roomTotal));
            System.out.println("[BILLING] Room " + room.getRoomNumber() + " - Price: Rs." + room.getPricePerNight()
                    + " - Total: Rs." + roomTotal);
        }

        // Calculate total: (number of nights) × (sum of all room prices per night)
        double totalAmount = nights * totalRoomPrice;

        System.out.println("[BILLING] Calculating bill for reservation " + reservationId);
        System.out.println("[BILLING] Rooms: " + reservation.getRoomIds().size() + ", Nights: " + nights
                + ", Total Room Rate/Night: Rs." + totalRoomPrice);
        System.out.println("[BILLING] Total amount: Rs." + totalAmount);

        // Create and save bill
        Bill bill = new Bill();
        bill.setReservation(reservation);
        bill.setNumberOfNights((int) nights);
        bill.setTotalAmount(totalAmount);
        bill.setGeneratedDate(LocalDate.now());
        bill.setRoomDetails(roomDetails);

        billRepository.saveBill(bill);
        System.out.println("[BILLING] Bill saved to database");

        // Update reservation status to COMPLETED
        reservationRepository.updateStatus(reservationId, "COMPLETED");
        System.out.println("[BILLING] Reservation status updated to COMPLETED");

        // Send bill email to guest
        try {
            emailService.sendBillEmail(reservation.getGuest().getEmail(), bill);
            System.out.println("[BILLING] Bill email sent to guest: " + reservation.getGuest().getEmail());
        } catch (Exception e) {
            System.err.println("[BILLING ERROR] Failed to send bill email: " + e.getMessage());
            // Continue even if email fails - billing should still complete
        }

        return bill;
    }

    /**
     * Update an existing reservation
     * Validates rooms, dates, status, and availability
     * Can update room_ids (list of rooms), check-in/check-out dates, and status
     * Cannot update guest_id through this method
     * Manager role required to update TO/FROM COMPLETED status
     * 
     * @param reservationId The reservation ID to update
     * @param userRole      User's role for authorization
     * @param roomIds       List of new room IDs (null to keep existing)
     * @param checkInDate   New check-in date (null to keep existing)
     * @param checkOutDate  New check-out date (null to keep existing)
     * @param newStatus     New status (null to keep existing)
     */
    public void updateReservation(int reservationId, String userRole, List<Integer> roomIds, LocalDate checkInDate,
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

        // Validate and set room IDs if provided
        if (roomIds != null && !roomIds.isEmpty()) {
            for (Integer roomId : roomIds) {
                if (roomId <= 0) {
                    throw new ValidationException("Invalid room ID: " + roomId);
                }
                // Verify room exists
                com.oceanviewresort.model.Room room = roomRepository.findById(roomId);
                if (room == null) {
                    throw new ValidationException("Room not found with ID: " + roomId);
                }

                // Check room status - only AVAILABLE rooms can be booked
                if (!room.getStatus().equalsIgnoreCase("AVAILABLE")) {
                    throw new ValidationException("Room with ID " + roomId
                            + " is not available for booking. Current status: " + room.getStatus());
                }
            }
            existingReservation.setRoomIds(roomIds);
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

        // Check room availability for all room IDs (excluding current reservation)
        // Only validate availability if room_ids or dates were changed
        if (roomIds != null || checkInDate != null || checkOutDate != null) {
            java.sql.Date checkInSql = Date.valueOf(existingReservation.getCheckInDate());
            java.sql.Date checkOutSql = Date.valueOf(existingReservation.getCheckOutDate());

            for (Integer roomId : existingReservation.getRoomIds()) {
                if (!reservationRepository.isRoomAvailableForUpdate(roomId, checkInSql, checkOutSql, reservationId)) {
                    throw new ValidationException(
                            "Room with ID " + roomId + " is not available for the selected dates");
                }
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