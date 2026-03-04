package com.oceanviewresort.controller;

import com.oceanviewresort.model.Bill;
import com.oceanviewresort.service.BillingService;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.TokenBlacklist;
import com.oceanviewresort.util.JwtUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class BillingController implements HttpHandler {

    private final BillingService billingService = new BillingService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!authorize(exchange))
            return;

        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            handleGetAllBills(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
            handleGenerateBill(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    /**
     * GET /billing - Get all bills with full guest, reservation and room details
     */
    private void handleGetAllBills(HttpExchange exchange) throws IOException {
        try {
            List<Bill> bills = billingService.getAllBillsWithDetails();

            JsonArray billsArray = new JsonArray();

            for (Bill bill : bills) {
                JsonObject obj = new JsonObject();
                obj.addProperty("billId", bill.getBillId());
                obj.addProperty("generatedDate", bill.getGeneratedDate().toString());
                obj.addProperty("totalAmount", bill.getTotalAmount());

                // Guest details
                JsonObject guestJson = new JsonObject();
                guestJson.addProperty("id", bill.getReservation().getGuest().getId());
                guestJson.addProperty("name", bill.getReservation().getGuest().getName());
                guestJson.addProperty("address", bill.getReservation().getGuest().getAddress());
                guestJson.addProperty("contactNumber", bill.getReservation().getGuest().getContactNumber());
                guestJson.addProperty("email", bill.getReservation().getGuest().getEmail());
                obj.add("guest", guestJson);

                // Reservation details
                JsonObject reservationJson = new JsonObject();
                reservationJson.addProperty("reservationId", bill.getReservation().getReservationId());
                reservationJson.addProperty("checkInDate", bill.getReservation().getCheckInDate().toString());
                reservationJson.addProperty("checkOutDate", bill.getReservation().getCheckOutDate().toString());
                reservationJson.addProperty("numberOfNights", bill.getNumberOfNights());
                reservationJson.addProperty("status", bill.getReservation().getStatus());
                obj.add("reservation", reservationJson);

                // Room details
                JsonArray roomsArray = new JsonArray();
                if (bill.getRoomDetails() != null) {
                    for (Bill.RoomDetail room : bill.getRoomDetails()) {
                        JsonObject roomJson = new JsonObject();
                        roomJson.addProperty("roomId", room.getRoomId());
                        roomJson.addProperty("roomNumber", room.getRoomNumber());
                        roomJson.addProperty("pricePerNight", room.getPricePerNight());
                        roomJson.addProperty("totalForRoom", room.getTotalForRoom());
                        roomsArray.add(roomJson);
                    }
                }
                obj.add("rooms", roomsArray);

                billsArray.add(obj);
            }

            JsonUtil.sendJsonWithMessage(exchange, "Bills retrieved successfully", billsArray);

        } catch (Exception e) {
            System.err.println("Error fetching bills: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Failed to retrieve bills");
        }
    }

    /**
     * POST /billing - Generate a bill for a reservation
     */
    private void handleGenerateBill(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            int reservationId = json.get("reservationId").getAsInt();
            double roomRate = json.get("roomRate").getAsDouble();

            Bill bill = billingService.checkoutAndGenerateBill(reservationId, roomRate);

            JsonUtil.sendJson(exchange, bill);

        } catch (Exception e) {
            JsonUtil.sendError(exchange, 400, e.getMessage());
        }
    }

    private boolean authorize(HttpExchange exchange) throws IOException {

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            JsonUtil.sendError(exchange, 401, "Missing token");
            return false;
        }

        String token = authHeader.replace("Bearer ", "");

        // Check if token is blacklisted (user logged out)
        if (TokenBlacklist.isTokenBlacklisted(token)) {
            JsonUtil.sendError(exchange, 401, "Session expired. Please login again");
            return false;
        }

        if (!JwtUtil.validateToken(token)) {
            JsonUtil.sendError(exchange, 401, "Invalid token");
            return false;
        }

        return true;
    }
}