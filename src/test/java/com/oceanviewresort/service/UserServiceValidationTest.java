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
    // REQ-USER-04: Login Validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-US-01: login with null username throws ValidationException")
    void login_nullUsername_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.login(null, "password123"));
        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-US-02: login with empty username throws ValidationException")
    void login_emptyUsername_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.login("   ", "password123"));
        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-US-03: login with null password throws ValidationException")
    void login_nullPassword_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.login("admin", null));
        assertEquals("Password is required", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // REQ-USER-05 & REQ-USER-06: Registration Validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-US-04: registerUser with null name throws ValidationException")
    void registerUser_nullName_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.registerUser(null, "jdoe", "password1", "STAFF", true));
        assertEquals("Name is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-US-05: registerUser with null username throws ValidationException")
    void registerUser_nullUsername_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.registerUser("John Doe", null, "password1", "STAFF", true));
        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    @DisplayName("TC-US-06: registerUser with password shorter than 6 characters throws ValidationException")
    void registerUser_shortPassword_throwsValidationException() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> userService.registerUser("John Doe", "jdoe", "abc", "STAFF", true));
        assertEquals("Password must be at least 6 characters long", ex.getMessage());
    }
}
