package com.oceanviewresort.service;

import com.oceanviewresort.exception.InvalidCredentialsException;
import com.oceanviewresort.exception.UserInactiveException;
import com.oceanviewresort.exception.UserNotFoundException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.User;
import com.oceanviewresort.repository.UserRepository;
import com.oceanviewresort.util.JwtUtil;
import com.oceanviewresort.util.PasswordUtil;

/**
 * Service layer for User operations
 * Handles business logic for authentication and user management
 */
public class UserService {

    private final UserRepository userRepository = new UserRepository();

    /**
     * Authenticate user with username and password
     * @param username The username
     * @param password The plain text password
     * @return JWT token if authentication successful
     */
    public String login(String username, String password) {

        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        if (!user.isActive()) {
            throw new UserInactiveException("User account is deactivated");
        }

        // Verify password using BCrypt
        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return JwtUtil.generateToken(user.getUsername(), user.getRole());
    }

    /**
     * Register a new user with hashed password
     * @param name The full name
     * @param username The username
     * @param plainPassword The plain text password
     * @param role The user role (ADMIN, STAFF, etc.)
     * @return The UUID of the created user
     */
    public String registerUser(String name, String username, String plainPassword, String role) {

        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }

        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }

        if (plainPassword == null || plainPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long");
        }

        if (role == null || role.trim().isEmpty()) {
            throw new ValidationException("Role is required");
        }

        // Check if username already exists
        if (userRepository.usernameExists(username)) {
            throw new ValidationException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setRole(role.toUpperCase());
        user.setActive(true);

        // Create user with hashed password
        return userRepository.createUser(user, plainPassword);
    }

    /**
     * Change user password
     * @param userId The user UUID
     * @param oldPassword The old plain text password
     * @param newPassword The new plain text password
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {

        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long");
        }

        User user = userRepository.findById(userId);

        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        // Verify old password
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Update with new hashed password
        userRepository.updatePassword(userId, newPassword);
    }

    /**
     * Validate JWT token
     * @param token The JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        return JwtUtil.validateToken(token);
    }

    /**
     * Activate or deactivate user account
     * @param userId The user UUID
     * @param isActive The active status
     */
    public void updateUserStatus(String userId, boolean isActive) {
        User user = userRepository.findById(userId);

        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        userRepository.updateUserStatus(userId, isActive);
    }
}