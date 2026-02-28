package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RoomService input-validation rules.
 * Only tests logic that executes BEFORE any database interaction.
 *
 * TDD Traceability:
 * REQ-ROOM-03: Room number, type, capacity, price, and status are required for
 * creation
 * REQ-ROOM-04: Room type must be SINGLE, DOUBLE, or SUITE
 * REQ-ROOM-05: Capacity and pricePerNight must be positive integers/doubles
 * REQ-ROOM-06: Room status must be AVAILABLE, BOOKED, or MAINTENANCE
 */
@DisplayName("RoomService Validation Tests")
class RoomServiceValidationTest {

    private final RoomService roomService = new RoomService();

    // -------------------------------------------------------------------------
    // REQ-ROOM-03: Required Fields
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-RS-01: createRoom with null room number throws ValidationException")
    void createRoom_nullRoomNumber_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom(null, "SINGLE", 1, 100.0, "AVAILABLE"));
        assertEquals("Room number is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-RS-02: createRoom with invalid room type throws ValidationException")
    void createRoom_invalidRoomType_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom("101", "PENTHOUSE", 2, 500.0, "AVAILABLE"));
        assertEquals("Room type must be SINGLE, DOUBLE, or SUITE", ex.getMessage());
    }

    @Test
    @DisplayName("TC-RS-03: createRoom with negative price throws ValidationException")
    void createRoom_negativePrice_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom("101", "DOUBLE", 2, -50.0, "AVAILABLE"));
        assertEquals("Price per night must be greater than 0", ex.getMessage());
    }
}
