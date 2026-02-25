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
     * Authenticate a user with a username and password
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
     * Register a new user with a hashed password
     * @param name The full name
     * @param username The username
     * @param plainPassword The plain text password
     * @param role The user role (ADMIN, STAFF, etc.)
     * @param isActive The active status
     * @return The UUID of the created user
     */
    public String registerUser(String name, String username, String plainPassword, String role, boolean isActive) {

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

        // Check if a username already exists
        if (userRepository.usernameExists(username)) {
            throw new ValidationException("Username already exists");
        }

        // Create a new user
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setRole(role.toUpperCase());
        user.setActive(isActive);

        // Create a user with a hashed password
        return userRepository.createUser(user, plainPassword);
    }

    /**
     * Get user details by username
     * @param username The username
     * @return User object without password
     */
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        // Remove password before returning
        user.setPassword(null);

        return user;
    }

    /**
     * Update user details (name, role, active status)
     * @param userId The user UUID
     * @param name The updated name
     * @param role The updated role
     * @param isActive The updated active status
     */
    public void updateUser(String userId, String name, String role, Boolean isActive) {
        User user = userRepository.findById(userId);

        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        if (name != null && !name.trim().isEmpty()) {
            user.setName(name);
        }

        if (role != null && !role.trim().isEmpty()) {
            user.setRole(role.toUpperCase());
        }

        if (isActive != null) {
            user.setActive(isActive);
        }

        userRepository.updateUser(user);
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

        // Update with a new hashed password
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
     * Activate or deactivate a user account
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