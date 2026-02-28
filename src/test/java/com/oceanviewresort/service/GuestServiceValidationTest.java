package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GuestService input-validation rules.
 * Only tests logic that executes BEFORE any database interaction.
 *
 * TDD Traceability:
 * REQ-GUEST-02: Guest name and contact number are mandatory fields
 * REQ-GUEST-03: Contact number must match the international phone number
 * pattern
 * REQ-GUEST-04: Email address must be a syntactically valid email format
 */
@DisplayName("GuestService Validation Tests")
class GuestServiceValidationTest {

    private final GuestService guestService = new GuestService();

    // -------------------------------------------------------------------------
    // REQ-GUEST-02: Mandatory Name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-GS-01: addGuest with null name throws ValidationException")
    void addGuest_nullName_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.addGuest(null, "123 Street", "07911123456", "a@b.com"));
        assertEquals("Guest name is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-GS-02: addGuest with empty name throws ValidationException")
    void addGuest_emptyName_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.addGuest("  ", "123 Street", "07911123456", "a@b.com"));
        assertEquals("Guest name is required", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-GUEST-02: Mandatory Contact Number
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-GS-03: addGuest with null contact number throws ValidationException")
    void addGuest_nullContactNumber_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.addGuest("Alice", "123 Street", null, "a@b.com"));
        assertEquals("Contact number is required", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-GUEST-03: Contact Number Format
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-GS-04: addGuest with alphabetic contact number throws ValidationException")
    void addGuest_alphabeticContactNumber_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.addGuest("Alice", "Addr", "ABCDEFGH", "a@b.com"));
        assertEquals("Invalid contact number format", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-GUEST-04: Email Format
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-GS-05: addGuest with an email missing '@' throws ValidationException")
    void addGuest_emailMissingAtSign_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.addGuest("Alice", "Addr", "07911000000", "notanemail"));
        assertEquals("Invalid email format", ex.getMessage());
    }

    @Test
    @DisplayName("TC-GS-06: getGuestByContactNumber with null throws ValidationException")
    void getGuestByContactNumber_null_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.getGuestByContactNumber(null));
        assertEquals("Contact number is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-GS-07: getGuestByContactNumber with empty string throws ValidationException")
    void getGuestByContactNumber_emptyString_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.getGuestByContactNumber(""));
        assertEquals("Contact number is required", ex.getMessage());
    }
}
