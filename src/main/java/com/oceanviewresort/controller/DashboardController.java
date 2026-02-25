package com.oceanviewresort.controller;

import com.oceanviewresort.exception.UnauthorizedException;
import com.oceanviewresort.service.DashboardService;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.JwtUtil;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DashboardController implements HttpHandler {

    private final DashboardService dashboardService = new DashboardService();

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

        if (!JwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Session expired. Please login again");
        }

        return token;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/dashboard/statistics") && method.equalsIgnoreCase("GET")) {
            handleGetStatistics(exchange);
        } else if (path.equals("/dashboard/room-status") && method.equalsIgnoreCase("GET")) {
            handleGetRoomStatus(exchange);
        } else {
            JsonUtil.sendError(exchange, 404, "Endpoint not found");
        }
    }

    /**
     * GET /dashboard/statistics - Get comprehensive dashboard statistics
     */
    private void handleGetStatistics(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Get dashboard statistics
            Map<String, Object> statistics = dashboardService.getDashboardStatistics();

            // Build response
            JsonObject response = new JsonObject();

            // Add room statistics
            @SuppressWarnings("unchecked")
            Map<String, Integer> roomStats = (Map<String, Integer>) statistics.get("room_statistics");
            JsonObject roomStatsJson = new JsonObject();
            roomStatsJson.addProperty("available", roomStats.get("AVAILABLE"));
            roomStatsJson.addProperty("booked", roomStats.get("BOOKED"));
            roomStatsJson.addProperty("maintenance", roomStats.get("MAINTENANCE"));
            roomStatsJson.addProperty("total", roomStats.get("TOTAL"));
            response.add("room_statistics", roomStatsJson);

            // Add total rooms
            response.addProperty("total_rooms", (Integer) statistics.get("total_rooms"));

            // Add percentages if available
            if (statistics.containsKey("percentages")) {
                @SuppressWarnings("unchecked")
                Map<String, Double> percentages = (Map<String, Double>) statistics.get("percentages");
                JsonObject percentagesJson = new JsonObject();
                percentagesJson.addProperty("available_percentage",
                    String.format("%.2f", percentages.get("available_percentage")));
                percentagesJson.addProperty("booked_percentage",
                    String.format("%.2f", percentages.get("booked_percentage")));
                percentagesJson.addProperty("maintenance_percentage",
                    String.format("%.2f", percentages.get("maintenance_percentage")));
                response.add("percentages", percentagesJson);
            }

            JsonUtil.sendJsonWithMessage(exchange, "Dashboard statistics retrieved successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in DashboardController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }

    /**
     * GET /dashboard/room-status - Get room counts by status
     */
    private void handleGetRoomStatus(HttpExchange exchange) throws IOException {
        try {
            // Authenticate user
            String token = extractAndValidateToken(exchange);

            // Get room status counts
            Map<String, Integer> statusCounts = dashboardService.getRoomStatusCounts();

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("available_rooms", statusCounts.get("AVAILABLE"));
            response.addProperty("booked_rooms", statusCounts.get("BOOKED"));
            response.addProperty("maintenance_rooms", statusCounts.get("MAINTENANCE"));
            response.addProperty("total_rooms", statusCounts.get("TOTAL"));

            // Calculate availability rate
            int total = statusCounts.get("TOTAL");
            if (total > 0) {
                double availabilityRate = (statusCounts.get("AVAILABLE") * 100.0) / total;
                response.addProperty("availability_rate", String.format("%.2f%%", availabilityRate));
            } else {
                response.addProperty("availability_rate", "0.00%");
            }

            JsonUtil.sendJsonWithMessage(exchange, "Room status counts retrieved successfully", response);

        } catch (UnauthorizedException e) {
            System.err.println("Unauthorized: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error in DashboardController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }
}
