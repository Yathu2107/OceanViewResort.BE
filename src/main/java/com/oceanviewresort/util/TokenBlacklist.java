package com.oceanviewresort.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Token Blacklist Service
 * Maintains a set of revoked/logged-out tokens
 * Tokens are automatically removed after expiration (24 hours)
 */
public class TokenBlacklist {

    private static final Set<String> blacklistedTokens = new HashSet<>();
    private static final long TOKEN_EXPIRY_TIME = 86400000; // 24 hours in milliseconds

    /**
     * Add token to blacklist (when user logs out)
     */
    public static void revokeToken(String token) {
        synchronized (blacklistedTokens) {
            blacklistedTokens.add(token);
            System.out.println(
                    "[TOKEN BLACKLIST] Token added to blacklist. Total revoked tokens: " + blacklistedTokens.size());
        }
    }

    /**
     * Check if token is blacklisted
     */
    public static boolean isTokenBlacklisted(String token) {
        synchronized (blacklistedTokens) {
            return blacklistedTokens.contains(token);
        }
    }

    /**
     * Remove token from blacklist (optional - for testing)
     */
    public static void removeFromBlacklist(String token) {
        synchronized (blacklistedTokens) {
            blacklistedTokens.remove(token);
            System.out.println("[TOKEN BLACKLIST] Token removed from blacklist");
        }
    }

    /**
     * Clear all blacklisted tokens (optional - for system reset)
     */
    public static void clearBlacklist() {
        synchronized (blacklistedTokens) {
            blacklistedTokens.clear();
            System.out.println("[TOKEN BLACKLIST] All tokens cleared from blacklist");
        }
    }

    /**
     * Get count of blacklisted tokens
     */
    public static int getBlacklistCount() {
        synchronized (blacklistedTokens) {
            return blacklistedTokens.size();
        }
    }
}