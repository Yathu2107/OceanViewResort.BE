package com.oceanviewresort.controller;

import com.oceanviewresort.exception.ForbiddenException;
import com.oceanviewresort.exception.UnauthorizedException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Room;
import com.oceanviewresort.service.RoomService;
import com.oceanviewresort.repository.ReservationRepository;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.JwtUtil;
import com.oceanviewresort.util.TokenBlacklist;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.sql.Date;
import java.util.List;

public class RoomController implements HttpHandler {

    private final RoomService roomService = new RoomService();
    private final ReservationRepository reservationRepository = new ReservationRepository();

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

    /**
     * Check if a user has a MANAGER role
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
        String method = exchange.getRequestMethod();

        if (path.equals("/rooms") && method.equalsIgnoreCase("GET")) {
            handleGetAllRooms(exchange);
        } else if (path.equals("/rooms") && method.equalsIgnoreCase("POST")) {
            handleCreateRoom(exchange);
        } else if (path.matches(".*/rooms/\\d+$") && method.equalsIgnoreCase("GET")) {
            handleGetRoomById(exchange);
        } else if (path.matches(".*/rooms/\\d+$") && method.equalsIgnoreCase("PUT")) {
            handleUpdateRoom(exchange);
        } else if (path.matches(".*/rooms/\\d+$") && method.equalsIgnoreCase("DELETE")) {
            handleDeleteRoom(exchange);
        } else {
            JsonUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    /**
     * GET /rooms - Get all rooms (optionally filtered by status and/or availability
     * dates)
     * Query params:
     * ?status=AVAILABLE (optional - filters by room status)
     * ?checkInDate=2026-03-01&checkOutDate=2026-03-05 (optional - filters by
     * availability for specific dates)
     * Examples:
     * GET /rooms - All rooms
     * GET /rooms?status=AVAILABLE - All AVAILABLE rooms
     * GET /rooms?checkInDate=2026-03-01&checkOutDate=2026-03-05 - All rooms
     * available for date range
     * GET /rooms?status=AVAILABLE&checkInDate=2026-03-01&checkOutDate=2026-03-05 -
     * AVAILABLE rooms for date range
     */
    private void handleGetAllRooms(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Extract query parameters
            String statusParam = null;
            String checkInDateParam = null;
            String checkOutDateParam = null;

            String query = exchange.getRequestURI().getQuery();
            if (query != null && !query.isEmpty()) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");

                        if ("status".equals(key)) {
                            statusParam = value;
                        } else if ("checkInDate".equals(key)) {
                            checkInDateParam = value;
                        } else if ("checkOutDate".equals(key)) {
                            checkOutDateParam = value;
                        }
                    }
                }
            }

            // Get all rooms (optionally filtered by status)
            List<Room> rooms = roomService.getAllRooms(statusParam);

            // Filter by availability dates if provided
            List<Room> filteredRooms = new java.util.ArrayList<>(rooms);
            if (checkInDateParam != null && checkOutDateParam != null) {
                try {
                    LocalDate checkInDate = LocalDate.parse(checkInDateParam);
                    LocalDate checkOutDate = LocalDate.parse(checkOutDateParam);

                    // Validate dates
                    if (checkOutDate.isBefore(checkInDate) || checkOutDate.equals(checkInDate)) {
                        JsonUtil.sendError(exchange, 400, "Check-out date must be after check-in date");
                        return;
                    }

                    Date checkInSql = Date.valueOf(checkInDate);
                    Date checkOutSql = Date.valueOf(checkOutDate);

                    // Filter rooms - keep only those available for the date range
                    filteredRooms.removeIf(
                            room -> !reservationRepository.isRoomAvailable(room.getId(), checkInSql, checkOutSql));

                    System.out.println("[ROOMS] Filtered " + rooms.size() + " rooms by date range " + checkInDate
                            + " to " + checkOutDate);
                    System.out.println("[ROOMS] " + filteredRooms.size() + " rooms available for selected dates");

                } catch (java.time.format.DateTimeParseException e) {
                    JsonUtil.sendError(exchange, 400, "Invalid date format. Use YYYY-MM-DD");
                    return;
                }
            }

            // Build response
            JsonArray roomsArray = new JsonArray();
            for (Room room : filteredRooms) {
                JsonObject roomJson = new JsonObject();
                roomJson.addProperty("id", room.getId());
                roomJson.addProperty("room_number", room.getRoomNumber());
                roomJson.addProperty("room_type", room.getRoomType());
                roomJson.addProperty("capacity", room.getCapacity());
                roomJson.addProperty("price_per_night", room.getPricePerNight());
                roomJson.addProperty("status", room.getStatus());
                roomsArray.add(roomJson);
            }

            JsonObject response = new JsonObject();
            response.add("rooms", roomsArray);
            response.addProperty("total", filteredRooms.size());

            JsonUtil.sendJsonWithMessage(exchange, "Rooms retrieved successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in RoomController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * GET /rooms/{id} - Get room by ID
     */
    private void handleGetRoomById(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Extract room ID from a path
            String path = exchange.getRequestURI().getPath();
            int roomId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            // Get room
            Room room = roomService.getRoomById(roomId);

            // Build response
            JsonObject roomJson = new JsonObject();
            roomJson.addProperty("id", room.getId());
            roomJson.addProperty("room_number", room.getRoomNumber());
            roomJson.addProperty("room_type", room.getRoomType());
            roomJson.addProperty("capacity", room.getCapacity());
            roomJson.addProperty("price_per_night", room.getPricePerNight());
            roomJson.addProperty("status", room.getStatus());

            JsonUtil.sendJsonWithMessage(exchange, "Room retrieved successfully", roomJson);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 404, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid room ID");
        } catch (Exception e) {
            System.err.println("Unexpected Error in RoomController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * POST /rooms - Create a new room (Manager only)
     */
    private void handleCreateRoom(HttpExchange exchange) throws IOException {
        try {
            // Authenticate and authorize
            String token = extractAndValidateToken(exchange);
            requireManagerRole(token);

            // Parse request body
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            String roomNumber = json.has("room_number") ? json.get("room_number").getAsString() : null;
            String roomType = json.has("room_type") ? json.get("room_type").getAsString().toUpperCase() : null;
            int capacity = json.has("capacity") ? json.get("capacity").getAsInt() : 0;
            double pricePerNight = json.has("price_per_night") ? json.get("price_per_night").getAsDouble() : 0.0;
            String status = json.has("status") ? json.get("status").getAsString().toUpperCase() : "AVAILABLE";

            // Create room
            int roomId = roomService.createRoom(roomNumber, roomType, capacity, pricePerNight, status);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("room_id", roomId);
            response.addProperty("room_number", roomNumber);
            response.addProperty("room_type", roomType);
            response.addProperty("capacity", capacity);
            response.addProperty("price_per_night", pricePerNight);
            response.addProperty("status", status);

            JsonUtil.sendJsonWithMessage(exchange, "Room created successfully", response);

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
            System.err.println("Unexpected Error in RoomController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * PUT /rooms/{id} - Update room (Manager only)
     */
    private void handleUpdateRoom(HttpExchange exchange) throws IOException {
        try {
            // Authenticate and authorize
            String token = extractAndValidateToken(exchange);
            requireManagerRole(token);

            // Extract room ID from a path
            String path = exchange.getRequestURI().getPath();
            int roomId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            // Parse request body
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            String roomNumber = json.has("room_number") ? json.get("room_number").getAsString() : null;
            String roomType = json.has("room_type") ? json.get("room_type").getAsString().toUpperCase() : null;
            Integer capacity = json.has("capacity") ? json.get("capacity").getAsInt() : null;
            Double pricePerNight = json.has("price_per_night") ? json.get("price_per_night").getAsDouble() : null;
            String status = json.has("status") ? json.get("status").getAsString().toUpperCase() : null;

            // Update room
            roomService.updateRoom(roomId, roomNumber, roomType, capacity, pricePerNight, status);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("room_id", roomId);

            JsonUtil.sendJsonWithMessage(exchange, "Room updated successfully", response);

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
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid room ID");
        } catch (Exception e) {
            System.err.println("Unexpected Error in RoomController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * DELETE /rooms/{id} - Delete room (Manager only)
     */
    private void handleDeleteRoom(HttpExchange exchange) throws IOException {
        try {
            // Authenticate and authorize
            String token = extractAndValidateToken(exchange);
            requireManagerRole(token);

            // Extract room ID from a path
            String path = exchange.getRequestURI().getPath();
            int roomId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            // Delete room
            roomService.deleteRoom(roomId);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("room_id", roomId);

            JsonUtil.sendJsonWithMessage(exchange, "Room deleted successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ForbiddenException e) {
            System.err.println("Forbidden: " + e.getMessage());
            JsonUtil.sendError(exchange, 403, e.getMessage());
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 404, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid room ID");
        } catch (Exception e) {
            System.err.println("Unexpected Error in RoomController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }
}
