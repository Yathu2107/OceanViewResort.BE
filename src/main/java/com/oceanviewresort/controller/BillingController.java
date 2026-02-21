package com.oceanviewresort.controller;

import com.oceanviewresort.model.Bill;
import com.oceanviewresort.service.BillingService;
import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JsonUtil;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class BillingController implements HttpHandler {

    private final BillingService billingService = new BillingService();
    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!authorize(exchange)) return;

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            int reservationId = json.get("reservationId").getAsInt();
            double roomRate = json.get("roomRate").getAsDouble();

            Bill bill =
                    billingService.checkoutAndGenerateBill(
                            reservationId, roomRate);

            JsonUtil.sendJson(exchange, bill);

        } catch (Exception e) {
            JsonUtil.sendError(exchange, 400, e.getMessage());
        }
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