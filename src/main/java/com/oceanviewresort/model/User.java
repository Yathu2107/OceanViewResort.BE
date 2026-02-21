package com.oceanviewresort.model;

import java.util.UUID;

/**
 * User entity representing system users
 * Uses UUID for ID and stores hashed passwords
 */
public class User {

    private String id; // UUID
    private String name; // Full name
    private String username;
    private String password; // BCrypt hashed password
    private String role; // ADMIN, STAFF, RECEPTIONIST
    private boolean active;

    public User() {
    }

    public User(String id, String name, String username, String password, String role, boolean active) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Generate a new UUID for this user
     */
    public void generateId() {
        this.id = UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Set hashed password
     * Warning: This should be a BCrypt hashed password, not plain text
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", active=" + active +
                '}';
    }
}