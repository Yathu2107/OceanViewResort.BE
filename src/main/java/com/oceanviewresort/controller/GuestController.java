package com.oceanviewresort.controller;

import com.oceanviewresort.exception.UnauthorizedException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Guest;
import com.oceanviewresort.service.GuestService;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.JwtUtil;
import com.oceanviewresort.util.TokenBlacklist;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class GuestController implements HttpHandler {

    private final GuestService guestService = new GuestService();

    /**
     * Extract and validate JWT token from the Authorization header
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

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/guests") && method.equalsIgnoreCase("GET")) {
            handleGetAllGuests(exchange);
        } else if (path.equals("/guests") && method.equalsIgnoreCase("POST")) {
            handleAddGuest(exchange);
        } else if (path.matches(".*/guests/\\d+$") && method.equalsIgnoreCase("GET")) {
            handleGetGuestById(exchange);
        } else if (path.matches(".*/guests/\\d+$") && method.equalsIgnoreCase("PUT")) {
            handleUpdateGuest(exchange);
        } else if (path.matches(".*/guests/\\d+$") && method.equalsIgnoreCase("DELETE")) {
            handleDeleteGuest(exchange);
        } else if (path.equals("/guests/search") && method.equalsIgnoreCase("GET")) {
            handleGetGuestByContactNumber(exchange);
        } else {
            JsonUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    /**
     * POST /guests - Add new guest
     */
    private void handleAddGuest(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Parse request body
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            String name = json.has("name") ? json.get("name").getAsString() : null;
            String address = json.has("address") ? json.get("address").getAsString() : null;
            String contactNumber = json.has("contact_number") ? json.get("contact_number").getAsString() : null;
            String email = json.has("email") ? json.get("email").getAsString() : null;

            // Add guest
            int guestId = guestService.addGuest(name, address, contactNumber, email);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("guest_id", guestId);
            response.addProperty("name", name);
            response.addProperty("address", address);
            response.addProperty("contact_number", contactNumber);
            response.addProperty("email", email);

            JsonUtil.sendJsonWithMessage(exchange, "Guest added successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 400, "Invalid JSON format");
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in GuestController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * PUT /guests/{id} - Update guest
     */
    private void handleUpdateGuest(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Extract guest ID from path
            String path = exchange.getRequestURI().getPath();
            int guestId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            // Parse request body
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            String name = json.has("name") ? json.get("name").getAsString() : null;
            String address = json.has("address") ? json.get("address").getAsString() : null;
            String contactNumber = json.has("contact_number") ? json.get("contact_number").getAsString() : null;
            String email = json.has("email") ? json.get("email").getAsString() : null;

            // Update guest
            guestService.updateGuest(guestId, name, address, contactNumber, email);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("guest_id", guestId);

            JsonUtil.sendJsonWithMessage(exchange, "Guest updated successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 400, "Invalid JSON format");
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid guest ID");
        } catch (Exception e) {
            System.err.println("Unexpected Error in GuestController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * GET /guests/search?contact_number={number} - Get guest by contact number
     */
    private void handleGetGuestByContactNumber(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Extract contact_number from query parameters
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.contains("contact_number=")) {
                JsonUtil.sendError(exchange, 400, "contact_number query parameter is required");
                return;
            }

            String contactNumber = null;
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("contact_number=")) {
                    contactNumber = param.substring("contact_number=".length());
                    // URL decode
                    contactNumber = java.net.URLDecoder.decode(contactNumber, "UTF-8");
                    break;
                }
            }

            if (contactNumber == null || contactNumber.trim().isEmpty()) {
                JsonUtil.sendError(exchange, 400, "contact_number cannot be empty");
                return;
            }

            // Get guest by contact number
            Guest guest = guestService.getGuestByContactNumber(contactNumber);

            if (guest == null) {
                JsonUtil.sendError(exchange, 404, "Guest not found with contact number: " + contactNumber);
                return;
            }

            // Build response
            JsonObject guestJson = new JsonObject();
            guestJson.addProperty("id", guest.getId());
            guestJson.addProperty("name", guest.getName());
            guestJson.addProperty("address", guest.getAddress());
            guestJson.addProperty("contact_number", guest.getContactNumber());
            guestJson.addProperty("email", guest.getEmail());

            JsonUtil.sendJsonWithMessage(exchange, "Guest retrieved successfully", guestJson);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in GuestController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * GET /guests - Get all guests
     */
    private void handleGetAllGuests(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Get all guests
            List<Guest> guests = guestService.getAllGuests();

            // Build response
            JsonArray guestsArray = new JsonArray();
            for (Guest guest : guests) {
                JsonObject guestJson = new JsonObject();
                guestJson.addProperty("id", guest.getId());
                guestJson.addProperty("name", guest.getName());
                guestJson.addProperty("address", guest.getAddress());
                guestJson.addProperty("contact_number", guest.getContactNumber());
                guestJson.addProperty("email", guest.getEmail());
                guestsArray.add(guestJson);
            }

            JsonObject response = new JsonObject();
            response.add("guests", guestsArray);
            response.addProperty("total", guests.size());

            JsonUtil.sendJsonWithMessage(exchange, "Guests retrieved successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in GuestController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * GET /guests/{id} - Get guest by ID
     */
    private void handleGetGuestById(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Extract guest ID from path
            String path = exchange.getRequestURI().getPath();
            int guestId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            // Get guest
            Guest guest = guestService.getGuestById(guestId);

            // Build response
            JsonObject guestJson = new JsonObject();
            guestJson.addProperty("id", guest.getId());
            guestJson.addProperty("name", guest.getName());
            guestJson.addProperty("address", guest.getAddress());
            guestJson.addProperty("contact_number", guest.getContactNumber());
            guestJson.addProperty("email", guest.getEmail());

            JsonUtil.sendJsonWithMessage(exchange, "Guest retrieved successfully", guestJson);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 404, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid guest ID");
        } catch (Exception e) {
            System.err.println("Unexpected Error in GuestController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * DELETE /guests/{id} - Delete guest
     */
    private void handleDeleteGuest(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Extract guest ID from path
            String path = exchange.getRequestURI().getPath();
            int guestId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            // Delete guest
            guestService.deleteGuest(guestId);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("guest_id", guestId);

            JsonUtil.sendJsonWithMessage(exchange, "Guest deleted successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 404, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid guest ID");
        } catch (Exception e) {
            System.err.println("Unexpected Error in GuestController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }
}
