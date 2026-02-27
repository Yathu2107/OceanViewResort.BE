package com.oceanviewresort.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JwtUtil {

    private static final String SECRET_KEY;
    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1 hour

    static {
        String key = ConfigLoader.getProperty("jwt.secret");
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException(
                "jwt.secret is not configured in application.properties. " +
                "Please add a strong random secret key.");
        }
        SECRET_KEY = key;
    }

    public static String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        long expiry = now + EXPIRATION_TIME;

        String header = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        String payload = String.format(
                "{\"sub\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                username, role, now, expiry
        );

        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signature = sign(header + "." + encodedPayload);

        return header + "." + encodedPayload + "." + signature;
    }

    public static boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!expectedSignature.equals(parts[2])) return false;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
            long exp = payload.get("exp").getAsLong();

            return System.currentTimeMillis() < exp;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
            return payload.get("sub").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRoleFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
            return payload.get("role").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("JWT signing failed", e);
        }
    }
}