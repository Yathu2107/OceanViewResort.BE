package com.oceanviewresort.controller;

import com.oceanviewresort.exception.ForbiddenException;
import com.oceanviewresort.exception.UnauthorizedException;
import com.oceanviewresort.exception.UserNotFoundException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.JwtUtil;
import com.oceanviewresort.util.TokenBlacklist;
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
     * 
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

        // Check if token is blacklisted (user logged out)
        if (TokenBlacklist.isTokenBlacklisted(token)) {
            throw new UnauthorizedException("Session expired. Please login again");
        }

        if (!JwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Session expired. Please login again");
        }

        return token;
    }

    /**
     * Check if a user has a MANAGER role
     * 
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
        } else if (path.endsWith("/logout")) {
            handleLogout(exchange);
        } else if (path.endsWith("/me")) {
            handleGetLoggedUserDetails(exchange);
        } else if (path.matches(".*/user$")) {
            handleGetAllUsers(exchange);
        } else if (path.matches(".*/user/[a-f0-9-]+$")) {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                handleGetUserById(exchange);
            } else {
                handleUpdate(exchange);
            }
        } else {
            JsonUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    /**
     * GET /users - Get all users (MANAGER only)
     */
    private void handleGetAllUsers(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            JsonUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String token = extractAndValidateToken(exchange);
            requireManagerRole(token);

            List<com.oceanviewresort.model.User> users = userService.getAllUsers();

            com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
            for (com.oceanviewresort.model.User u : users) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", u.getId());
                obj.addProperty("name", u.getName());
                obj.addProperty("username", u.getUsername());
                obj.addProperty("role", u.getRole());
                obj.addProperty("is_active", u.isActive());
                jsonArray.add(obj);
            }

            JsonUtil.sendJsonWithMessage(exchange, "Users retrieved successfully", jsonArray);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ForbiddenException e) {
            System.err.println("Forbidden: " + e.getMessage());
            JsonUtil.sendError(exchange, 403, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in UserController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * GET /user/{id} - Get user by ID (MANAGER only)
     */
    private void handleGetUserById(HttpExchange exchange) throws IOException {

        try {
            String token = extractAndValidateToken(exchange);
            requireManagerRole(token);

            String path = exchange.getRequestURI().getPath();
            String userId = path.substring(path.lastIndexOf("/") + 1);

            com.oceanviewresort.model.User user = userService.getUserById(userId);

            JsonObject response = new JsonObject();
            response.addProperty("id", user.getId());
            response.addProperty("name", user.getName());
            response.addProperty("username", user.getUsername());
            response.addProperty("role", user.getRole());
            response.addProperty("is_active", user.isActive());

            JsonUtil.sendJsonWithMessage(exchange, "User retrieved successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ForbiddenException e) {
            System.err.println("Forbidden: " + e.getMessage());
            JsonUtil.sendError(exchange, 403, e.getMessage());
        } catch (com.oceanviewresort.exception.UserNotFoundException e) {
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

    private void handleGetLoggedUserDetails(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            JsonUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);
            String username = JwtUtil.getUsernameFromToken(token);

            // Get user details
            com.oceanviewresort.model.User user = userService.getUserByUsername(username);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("id", user.getId());
            response.addProperty("name", user.getName());
            response.addProperty("username", user.getUsername());
            response.addProperty("role", user.getRole());
            response.addProperty("is_active", user.isActive());

            JsonUtil.sendJsonWithMessage(exchange, "User details retrieved successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
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

    /**
     * POST /user/logout - Logout user
     * Invalidates JWT token and clears session
     */
    private void handleLogout(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            JsonUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            // Extract and validate JWT token
            String token = extractAndValidateToken(exchange);
            String username = JwtUtil.getUsernameFromToken(token);

            // Add token to blacklist to prevent further use
            TokenBlacklist.revokeToken(token);
            System.out.println("[AUTH] User '" + username + "' logged out successfully. Token revoked.");

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("username", username);
            response.addProperty("message", "Logged out successfully");

            JsonUtil.sendJsonWithMessage(exchange, "Logout successful", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized logout: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Logout failed");
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
