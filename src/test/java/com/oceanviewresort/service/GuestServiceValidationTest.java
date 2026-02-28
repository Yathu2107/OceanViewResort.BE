package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GuestService input-validation rules.
 * Only tests logic that executes BEFORE any database interaction.
 */
@DisplayName("GuestService Validation Tests")
class GuestServiceValidationTest {

    private final GuestService guestService = new GuestService();

    // -------------------------------------------------------------------------
    // REQ-GUEST-01: Mandatory Name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-GS-01: addGuest with null name throws ValidationException")
    void addGuest_nullName_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.addGuest(null, "123 Street", "07911123456", "a@b.com"));
        assertEquals("Guest name is required", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-GUEST-02: Contact Number Format
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-GS-02: addGuest with an email missing '@' throws ValidationException")
    void addGuest_emailMissingAtSign_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> guestService.addGuest("Alice", "Addr", "07911000000", "notanemail"));
        assertEquals("Invalid email format", ex.getMessage());
    }
}
