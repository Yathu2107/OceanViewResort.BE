package com.oceanviewresort.controller;

import com.oceanviewresort.exception.ForbiddenException;
import com.oceanviewresort.exception.UnauthorizedException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Guest;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.model.Room;
import com.oceanviewresort.model.Bill;
import com.oceanviewresort.service.ReservationService;
import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JwtUtil;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.TokenBlacklist;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.oceanviewresort.repository.RoomRepository;

import java.io.IOException;
import java.time.LocalDate;

public class ReservationController implements HttpHandler {

    private final ReservationService reservationService = new ReservationService();
    private final UserService userService = new UserService();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // 🔐 JWT validation
        if (!authorize(exchange))
            return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // Route to complete reservation with billing endpoint
            if (path.matches(".*/\\d+/complete$") && method.equalsIgnoreCase("POST")) {
                handleCompleteReservationWithBilling(exchange);
            } else if (method.equalsIgnoreCase("POST")) {
                handleAddReservation(exchange);
            } else if (method.equalsIgnoreCase("GET")) {
                handleGetReservation(exchange);
            } else if (method.equalsIgnoreCase("PUT")) {
                handleUpdateReservation(exchange);
            } else if (method.equalsIgnoreCase("DELETE")) {
                handleCancelReservation(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            JsonUtil.sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleAddReservation(HttpExchange exchange) throws IOException {

        String body = new String(exchange.getRequestBody().readAllBytes());
        JsonObject json = JsonUtil.parse(body);

        Guest guest = new Guest();
        guest.setId(json.get("guestId").getAsInt());

        // Parse room IDs array
        java.util.List<Integer> roomIds = new java.util.ArrayList<>();
        if (json.has("roomIds") && json.get("roomIds").isJsonArray()) {
            JsonArray roomIdsArray = json.getAsJsonArray("roomIds");
            for (JsonElement element : roomIdsArray) {
                roomIds.add(element.getAsInt());
            }
        } else if (json.has("roomId")) {
            // Backward compatibility: single room ID
            roomIds.add(json.get("roomId").getAsInt());
        }

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoomIds(roomIds);
        reservation.setCheckInDate(
                LocalDate.parse(json.get("checkInDate").getAsString()));
        reservation.setCheckOutDate(
                LocalDate.parse(json.get("checkOutDate").getAsString()));

        reservationService.addReservation(reservation);

        JsonUtil.sendMessage(exchange, "Reservation created successfully");
    }

    private void handleGetReservation(HttpExchange exchange) throws IOException {

        int reservationId = Integer.parseInt(exchange.getRequestURI()
                .getQuery().split("=")[1]);

        Reservation reservation = reservationService.getReservation(reservationId);

        // Build response with room details
        JsonObject response = new JsonObject();
        response.addProperty("reservationId", reservation.getReservationId());

        // Add guest details
        JsonObject guestJson = new JsonObject();
        guestJson.addProperty("id", reservation.getGuest().getId());
        guestJson.addProperty("name", reservation.getGuest().getName());
        guestJson.addProperty("address", reservation.getGuest().getAddress());
        guestJson.addProperty("contactNumber", reservation.getGuest().getContactNumber());
        guestJson.addProperty("email", reservation.getGuest().getEmail());
        response.add("guest", guestJson);

        // Add full room details
        JsonArray roomsArray = new JsonArray();
        for (Integer roomId : reservation.getRoomIds()) {
            Room room = roomRepository.findById(roomId);
            if (room != null) {
                JsonObject roomJson = new JsonObject();
                roomJson.addProperty("id", room.getId());
                roomJson.addProperty("roomNumber", room.getRoomNumber());
                roomJson.addProperty("roomType", room.getRoomType());
                roomJson.addProperty("capacity", room.getCapacity());
                roomJson.addProperty("pricePerNight", room.getPricePerNight());
                roomJson.addProperty("status", room.getStatus());
                roomsArray.add(roomJson);
            }
        }
        response.add("rooms", roomsArray);

        // Add reservation dates and status
        response.addProperty("checkInDate", reservation.getCheckInDate().toString());
        response.addProperty("checkOutDate", reservation.getCheckOutDate().toString());
        response.addProperty("status", reservation.getStatus());

        JsonUtil.sendJson(exchange, response);
    }

    /**
     * PUT /reservations/{id} - Update reservation (rooms, dates, status)
     * Request body (all fields optional):
     * {
     * "roomIds": [1, 2, 3], // Array of room IDs (multiple rooms supported)
     * "checkInDate": "2026-03-01",
     * "checkOutDate": "2026-03-05",
     * "status": "COMPLETED"
     * }
     * 
     * Backward compatible with single roomId:
     * {
     * "roomId": 1,
     * "checkInDate": "2026-03-01",
     * "checkOutDate": "2026-03-05",
     * "status": "COMPLETED"
     * }
     * 
     * Status options: OCCUPIED, COMPLETED, CANCELLED
     * Note: Only OCCUPIED reservations can be updated by all
     * COMPLETED/CANCELLED status changes require MANAGER role
     * CANCELLED reservations cannot be updated to other statuses
     */
    private void handleUpdateReservation(HttpExchange exchange) throws IOException {
        try {
            // Extract and validate JWT token
            String token = extractAndValidateToken(exchange);
            String userRole = JwtUtil.getRoleFromToken(token);

            // Extract reservation ID from path
            String path = exchange.getRequestURI().getPath();
            int reservationId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            // Parse request body
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            // Parse room IDs (supports both array and single ID)
            java.util.List<Integer> roomIds = null;
            if (json.has("roomIds") && json.get("roomIds").isJsonArray()) {
                roomIds = new java.util.ArrayList<>();
                JsonArray roomIdsArray = json.getAsJsonArray("roomIds");
                for (JsonElement element : roomIdsArray) {
                    roomIds.add(element.getAsInt());
                }
            } else if (json.has("roomId")) {
                // Backward compatibility: single room ID
                roomIds = new java.util.ArrayList<>();
                roomIds.add(json.get("roomId").getAsInt());
            }

            LocalDate checkInDate = json.has("checkInDate") ? LocalDate.parse(json.get("checkInDate").getAsString())
                    : null;
            LocalDate checkOutDate = json.has("checkOutDate") ? LocalDate.parse(json.get("checkOutDate").getAsString())
                    : null;
            String status = json.has("status") ? json.get("status").getAsString() : null;

            // Update reservation
            reservationService.updateReservation(reservationId, userRole, roomIds, checkInDate, checkOutDate, status);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("reservation_id", reservationId);

            JsonUtil.sendJsonWithMessage(exchange, "Reservation updated successfully", response);

        } catch (UnauthorizedException e) {
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (ForbiddenException e) {
            JsonUtil.sendError(exchange, 403, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid reservation ID");
        } catch (ValidationException e) {
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error updating reservation: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Failed to update reservation");
        }
    }

    /**
     * Extract and validate JWT token from Authorization header
     */
    private String extractAndValidateToken(HttpExchange exchange) throws UnauthorizedException {
        java.util.List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");

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
     * POST /reservations/{id}/complete - Complete reservation and generate bill
     * with auto room rates
     * Request body: {} (empty - uses room prices from database)
     */
    private void handleCompleteReservationWithBilling(HttpExchange exchange) throws IOException {
        try {
            // Extract reservation ID from path
            String path = exchange.getRequestURI().getPath();
            int reservationId = Integer.parseInt(
                    path.substring(path.lastIndexOf("/", path.lastIndexOf("/") - 1) + 1, path.lastIndexOf("/")));

            // Complete reservation and generate bill (uses room prices from database)
            Bill bill = reservationService.completeReservationWithBilling(reservationId);

            // Build response with bill details
            JsonObject billJson = new JsonObject();
            billJson.addProperty("billId", bill.getBillId());
            billJson.addProperty("reservationId", bill.getReservation().getReservationId());
            billJson.addProperty("numberOfNights", bill.getNumberOfNights());
            billJson.addProperty("numberofRooms", bill.getReservation().getRoomIds().size());

            // Add room details with individual amounts
            JsonArray roomsArray = new JsonArray();
            if (bill.getRoomDetails() != null) {
                for (Bill.RoomDetail roomDetail : bill.getRoomDetails()) {
                    JsonObject roomJson = new JsonObject();
                    roomJson.addProperty("roomId", roomDetail.getRoomId());
                    roomJson.addProperty("roomNumber", roomDetail.getRoomNumber());
                    roomJson.addProperty("pricePerNight", roomDetail.getPricePerNight());
                    roomJson.addProperty("totalForRoom", roomDetail.getTotalForRoom());
                    roomsArray.add(roomJson);
                }
            }
            billJson.add("rooms", roomsArray);

            billJson.addProperty("totalAmount", bill.getTotalAmount());
            billJson.addProperty("generatedDate", bill.getGeneratedDate().toString());
            billJson.addProperty("status", "COMPLETED");

            // Add guest info
            JsonObject guestJson = new JsonObject();
            guestJson.addProperty("id", bill.getReservation().getGuest().getId());
            guestJson.addProperty("name", bill.getReservation().getGuest().getName());
            guestJson.addProperty("email", bill.getReservation().getGuest().getEmail());
            billJson.add("guest", guestJson);

            JsonUtil.sendJsonWithMessage(exchange,
                    "Reservation completed successfully. Bill generated and email sent to guest.", billJson);

        } catch (ValidationException e) {
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(exchange, 400, "Invalid reservation ID");
        } catch (Exception e) {
            System.err.println("Error completing reservation: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Failed to complete reservation");
        }
    }

    private void handleCancelReservation(HttpExchange exchange) throws IOException {

        int reservationId = Integer.parseInt(exchange.getRequestURI()
                .getQuery().split("=")[1]);

        reservationService.cancelReservation(reservationId);

        JsonUtil.sendMessage(exchange, "Reservation cancelled");
    }

    private boolean authorize(HttpExchange exchange) throws IOException {

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            JsonUtil.sendError(exchange, 401, "Please login to continue");
            return false;
        }

        String token = authHeader.replace("Bearer ", "");

        // Check if token is blacklisted (user logged out)
        if (TokenBlacklist.isTokenBlacklisted(token)) {
            JsonUtil.sendError(exchange, 401, "Session expired. Please login again");
            return false;
        }

        if (!userService.validateToken(token)) {
            JsonUtil.sendError(exchange, 401, "Invalid token");
            return false;
        }

        return true;
    }
}