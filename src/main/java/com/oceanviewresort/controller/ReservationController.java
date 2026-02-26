package com.oceanviewresort.controller;

import com.oceanviewresort.exception.ForbiddenException;
import com.oceanviewresort.exception.UnauthorizedException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Guest;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.service.ReservationService;
import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JwtUtil;
import com.oceanviewresort.util.JsonUtil;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDate;

public class ReservationController implements HttpHandler {

    private final ReservationService reservationService = new ReservationService();
    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // 🔐 JWT validation
        if (!authorize(exchange))
            return;

        String method = exchange.getRequestMethod();

        try {
            if (method.equalsIgnoreCase("POST")) {
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

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoomId(json.get("roomId").getAsInt());
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

        JsonUtil.sendJson(exchange, reservation);
    }

    /**
     * PUT /reservations/{id} - Update reservation (room, dates, status)
     * Request body (all fields optional):
     * {
     * "roomId": 3,
     * "checkInDate": "2026-03-01",
     * "checkOutDate": "2026-03-05",
     * "status": "COMPLETED"
     * }
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

            Integer roomId = json.has("roomId") ? json.get("roomId").getAsInt() : null;
            LocalDate checkInDate = json.has("checkInDate") ? LocalDate.parse(json.get("checkInDate").getAsString())
                    : null;
            LocalDate checkOutDate = json.has("checkOutDate") ? LocalDate.parse(json.get("checkOutDate").getAsString())
                    : null;
            String status = json.has("status") ? json.get("status").getAsString() : null;

            // Update reservation
            reservationService.updateReservation(reservationId, userRole, roomId, checkInDate, checkOutDate, status);

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

        if (!userService.validateToken(token)) {
            JsonUtil.sendError(exchange, 401, "Invalid token");
            return false;
        }

        return true;
    }
}