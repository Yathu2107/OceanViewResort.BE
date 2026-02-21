package com.oceanviewresort.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt
 * BCrypt is a secure password hashing algorithm
 */
public class PasswordUtil {

    // BCrypt work factor (log2 rounds) - higher is more secure but slower
    private static final int WORK_FACTOR = 12;

    /**
     * Hash a plain text password using BCrypt
     * @param plainPassword The plain text password
     * @return The hashed password
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Verify a plain text password against a hashed password
     * @param plainPassword The plain text password to verify
     * @param hashedPassword The hashed password to compare against
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a password needs to be rehashed (for security upgrades)
     * @param hashedPassword The hashed password
     * @return true if password needs rehashing, false otherwise
     */
    public static boolean needsRehash(String hashedPassword) {
        try {
            return !hashedPassword.startsWith("$2a$" + WORK_FACTOR + "$");
        } catch (Exception e) {
            return true;
        }
    }
}
