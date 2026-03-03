package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserService input-validation rules.
 * Only tests logic that executes BEFORE any database interaction.
 *
 * TDD Traceability:
 * REQ-USER-04: Username and password are mandatory for login
 * REQ-USER-05: Name, username, password and role are mandatory for registration
 * REQ-USER-06: Password must be at least 6 characters long
 */
@DisplayName("UserService Validation Tests")
class UserServiceValidationTest {

    private final UserService userService = new UserService();

    // -------------------------------------------------------------------------
    // REQ-USER-01: Login Validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-US-01: login with null username throws ValidationException")
    void login_nullUsername_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.login(null, "password123"));
        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-US-02: login with null password throws ValidationException")
    void login_nullPassword_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.login("admin", null));
        assertEquals("Password is required", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-USER-03 : Registration Validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-US-03: registerUser with invalid input throws ValidationException")
    void registerUser_nullName_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.registerUser(null, "jdoe", "password1", "STAFF", true));
        assertEquals("Name is required", ex.getMessage());
    }

}
