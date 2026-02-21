package com.oceanviewresort.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class JsonUtil {

    private static final Gson gson = new Gson();

    /* PARSE JSON*/
    public static JsonObject parse(String body) {
        return JsonParser.parseString(body).getAsJsonObject();
    }

    /* SEND JSON RESPONSE */
    public static void sendJson(HttpExchange exchange, Object body) throws IOException {
        ApiResponse<Object> response = new ApiResponse<>("S", "Success", "200", body);
        String json = gson.toJson(response);
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /* SEND JSON RESPONSE WITH CUSTOM MESSAGE */
    public static void sendJsonWithMessage(HttpExchange exchange, String message, Object body) throws IOException {
        ApiResponse<Object> response = new ApiResponse<>("S", message, "200", body);
        String json = gson.toJson(response);
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /* SEND ERROR RESPONSE*/
    public static void sendError(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        ApiResponse<Object> response = new ApiResponse<>("E", errorMessage, String.valueOf(statusCode), null);
        String json = gson.toJson(response);
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /* SEND MESSAGE RESPONSE */
    public static void sendMessage(HttpExchange exchange, String message) throws IOException {
        MessageResponse msg = new MessageResponse(message);
        sendJson(exchange, msg);
    }

    /* RESPONSE MODELS */
    private record ErrorResponse(String error) {}
    private record MessageResponse(String message) {}
}