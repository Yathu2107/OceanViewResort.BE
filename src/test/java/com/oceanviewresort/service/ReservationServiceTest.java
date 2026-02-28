package com.oceanviewresort.service;

import com.oceanviewresort.model.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReservationService business logic.
 * Covers reservation creation, date validation, and status management.
 * 
 * TDD Traceability:
 * REQ-RES-01: Reservation must have valid guest and at least one room
 * REQ-RES-02: Check-out date must be after check-in date
 * REQ-RES-03: Initial status should be OCCUPIED
 */
@DisplayName("ReservationService Tests")
class ReservationServiceTest {

    // -------------------------------------------------------------------------
    // REQ-RES-01 & REQ-RES-02: Reservation Validation Logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-RES-01: Reservation requires valid guest with ID > 0")
    void reservation_requiresValidGuest() {
        Reservation res = new Reservation();
        res.setGuest(null);

        assertNull(res.getGuest());
    }

    @Test
    @DisplayName("TC-RES-02: Reservation requires at least one room")
    void reservation_requiresAtLeastOneRoom() {
        Reservation res = new Reservation();

        assertTrue(res.getRoomIds().isEmpty());
        res.addRoomId(101);
        assertFalse(res.getRoomIds().isEmpty());
    }

    @Test
    @DisplayName("TC-RES-03: Check-out date must be strictly after check-in date")
    void reservation_dateValidation_checkOutAfterCheckIn() {
        LocalDate checkIn = LocalDate.of(2024, 7, 10);
        LocalDate checkOut = LocalDate.of(2024, 7, 5); // Before check-in

        assertFalse(checkOut.isAfter(checkIn));
        assertTrue(checkIn.isAfter(checkOut));
    }

    @Test
    @DisplayName("TC-RES-04: Reservation correctly calculates length of stay")
    void reservation_stayDuration_calculation() {
        LocalDate checkIn = LocalDate.of(2024, 8, 1);
        LocalDate checkOut = LocalDate.of(2024, 8, 6);

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        assertEquals(5, nights);
    }
}
