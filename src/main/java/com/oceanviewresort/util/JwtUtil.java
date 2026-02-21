package com.oceanviewresort.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class JwtUtil {

    private static final String SECRET_KEY = "OceanViewSecretKey";
    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1 hour

    public static String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        long expiry = now + EXPIRATION_TIME;

        String header = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

        String payload = String.format(
                "{\"sub\":\"%s\",\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                username, role, now, expiry
        );

        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes());

        String signature = sign(header + "." + encodedPayload);

        return header + "." + encodedPayload + "." + signature;
    }

    public static boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!expectedSignature.equals(parts[2])) return false;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            long exp = Long.parseLong(payloadJson.split("\"exp\":")[1].split("}")[0]);

            return System.currentTimeMillis() < exp;
        } catch (Exception e) {
            return false;
        }
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256"));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("JWT signing failed", e);
        }
    }
}