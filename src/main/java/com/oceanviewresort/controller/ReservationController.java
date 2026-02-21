package com.oceanviewresort.controller;

import com.oceanviewresort.model.Guest;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.service.ReservationService;
import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JsonUtil;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDate;

public class ReservationController implements HttpHandler {

    private final ReservationService reservationService =
            new ReservationService();
    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // 🔐 JWT validation
        if (!authorize(exchange)) return;

        String method = exchange.getRequestMethod();

        try {
            if (method.equalsIgnoreCase("POST")) {
                handleAddReservation(exchange);
            } else if (method.equalsIgnoreCase("GET")) {
                handleGetReservation(exchange);
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
        reservation.setRoomType(json.get("roomType").getAsString());
        reservation.setCheckInDate(
                LocalDate.parse(json.get("checkInDate").getAsString()));
        reservation.setCheckOutDate(
                LocalDate.parse(json.get("checkOutDate").getAsString()));

        reservationService.addReservation(reservation);

        JsonUtil.sendMessage(exchange, "Reservation created successfully");
    }

    private void handleGetReservation(HttpExchange exchange) throws IOException {

        int reservationId =
                Integer.parseInt(exchange.getRequestURI()
                        .getQuery().split("=")[1]);

        Reservation reservation =
                reservationService.getReservation(reservationId);

        JsonUtil.sendJson(exchange, reservation);
    }

    private void handleCancelReservation(HttpExchange exchange) throws IOException {

        int reservationId =
                Integer.parseInt(exchange.getRequestURI()
                        .getQuery().split("=")[1]);

        reservationService.cancelReservation(reservationId);

        JsonUtil.sendMessage(exchange, "Reservation cancelled");
    }

    private boolean authorize(HttpExchange exchange) throws IOException {

        String authHeader =
                exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            JsonUtil.sendError(exchange, 401, "Missing token");
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