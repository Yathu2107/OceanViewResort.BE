package com.oceanviewresort.controller;

import com.oceanviewresort.exception.ForbiddenException;
import com.oceanviewresort.exception.UnauthorizedException;
import com.oceanviewresort.exception.UserNotFoundException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.JwtUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class UserController implements HttpHandler {

    private final UserService userService = new UserService();

    /**
     * Extract and validate JWT token from the Authorization header
     * @return the token string
     * @throws UnauthorizedException if a token is missing or invalid
     */
    private String extractAndValidateToken(HttpExchange exchange) {
        List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");

        if (authHeaders == null || authHeaders.isEmpty()) {
            throw new UnauthorizedException("Please login to continue");
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid token format. Please login again");
        }

        String token = authHeader.substring(7);

        if (!JwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Session expired. Please login again");
        }

        return token;
    }

    /**
     * Check if a user has a MANAGER role
     * @throws ForbiddenException if the user is not a MANAGER
     */
    private void requireManagerRole(String token) {
        String role = JwtUtil.getRoleFromToken(token);

        if (role == null || !role.equals("MANAGER")) {
            throw new ForbiddenException("Access denied. Only managers can perform this action");
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.endsWith("/register")) {
            handleRegister(exchange);
        } else if (path.matches(".*/user/[a-f0-9-]+$")) {
            handleUpdate(exchange);
        } else {
            JsonUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            JsonUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // Authenticate and authorize - only MANAGER can register users
            String token = extractAndValidateToken(exchange);
            requireManagerRole(token);

            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            String name = json.has("name") ? json.get("name").getAsString() : null;
            String username = json.has("username") ? json.get("username").getAsString() : null;
            String password = json.has("password") ? json.get("password").getAsString() : null;
            String role = json.has("role") ? json.get("role").getAsString().toUpperCase() : null;
            boolean isActive = json.has("is_active") ? json.get("is_active").getAsBoolean() : true;

            // Validate that only MANAGER or STAFF roles can be created
            if (role == null || (!role.equals("MANAGER") && !role.equals("STAFF"))) {
                JsonUtil.sendError(exchange, 400, "Only MANAGER and STAFF roles can be registered");
                return;
            }

            // Register a user with a hashed password
            String userId = userService.registerUser(name, username, password, role, isActive);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("userId", userId);
            response.addProperty("name", name);
            response.addProperty("username", username);
            response.addProperty("role", role);
            response.addProperty("is_active", isActive);

            JsonUtil.sendJsonWithMessage(exchange, "User registered successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ForbiddenException e) {
            System.err.println("Forbidden: " + e.getMessage());
            JsonUtil.sendError(exchange, 403, e.getMessage());
        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 400, "Invalid JSON format");
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in UserController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    private void handleUpdate(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            JsonUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // Authenticate and authorize - only MANAGER can update users
            String token = extractAndValidateToken(exchange);
            requireManagerRole(token);

            // Extract userId from a path
            String path = exchange.getRequestURI().getPath();
            String userId = path.substring(path.lastIndexOf("/") + 1);

            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            String name = json.has("name") ? json.get("name").getAsString() : null;
            String role = json.has("role") ? json.get("role").getAsString() : null;
            Boolean isActive = json.has("is_active") ? json.get("is_active").getAsBoolean() : null;

            // Validate role if provided
            if (role != null && !role.isEmpty()) {
                role = role.toUpperCase();
                if (!role.equals("MANAGER") && !role.equals("STAFF")) {
                    JsonUtil.sendError(exchange, 400, "Only MANAGER and STAFF roles are allowed");
                    return;
                }
            }

            // Update user
            userService.updateUser(userId, name, role, isActive);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("userId", userId);

            JsonUtil.sendJsonWithMessage(exchange, "User updated successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ForbiddenException e) {
            System.err.println("Forbidden: " + e.getMessage());
            JsonUtil.sendError(exchange, 403, e.getMessage());
        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 400, "Invalid JSON format");
        } catch (UserNotFoundException e) {
            System.err.println("User Not Found: " + e.getMessage());
            JsonUtil.sendError(exchange, 404, e.getMessage());
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in UserController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }
}
