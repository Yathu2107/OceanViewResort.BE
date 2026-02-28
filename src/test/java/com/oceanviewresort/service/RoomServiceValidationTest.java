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
    @DisplayName("TC-RS-02: createRoom with empty room type throws ValidationException")
    void createRoom_emptyRoomType_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom("101", "", 1, 100.0, "AVAILABLE"));
        assertEquals("Room type is required", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-ROOM-04: Valid Room Types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-RS-03: createRoom with invalid room type throws ValidationException")
    void createRoom_invalidRoomType_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom("101", "PENTHOUSE", 2, 500.0, "AVAILABLE"));
        assertEquals("Room type must be SINGLE, DOUBLE, or SUITE", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-ROOM-05: Positive Numbers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-RS-04: createRoom with zero capacity throws ValidationException")
    void createRoom_zeroCapacity_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom("101", "DOUBLE", 0, 100.0, "AVAILABLE"));
        assertEquals("Capacity must be greater than 0", ex.getMessage());
    }

    @Test
    @DisplayName("TC-RS-05: createRoom with negative price throws ValidationException")
    void createRoom_negativePrice_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom("101", "DOUBLE", 2, -50.0, "AVAILABLE"));
        assertEquals("Price per night must be greater than 0", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-ROOM-06: Valid Status Values
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-RS-06: createRoom with invalid status throws ValidationException")
    void createRoom_invalidStatus_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.createRoom("101", "SINGLE", 1, 100.0, "OCCUPIED"));
        assertEquals("Status must be AVAILABLE, BOOKED, or MAINTENANCE", ex.getMessage());
    }

    @Test
    @DisplayName("TC-RS-07: getAllRooms with invalid status filter throws ValidationException")
    void getAllRooms_invalidStatusFilter_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> roomService.getAllRooms("UNKNOWN"));
        assertEquals("Invalid status. Must be AVAILABLE, BOOKED, or MAINTENANCE", ex.getMessage());
    }
}
