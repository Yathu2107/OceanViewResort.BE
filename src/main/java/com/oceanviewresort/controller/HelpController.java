package com.oceanviewresort.controller;

import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JsonUtil;
import com.oceanviewresort.util.TokenBlacklist;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Controller for Help/Guidelines API
 * Provides dynamic help content for staff members
 */
public class HelpController implements HttpHandler {

    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 🔐 JWT validation
        if (!authorize(exchange))
            return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if (method.equalsIgnoreCase("GET")) {
                if (path.contains("/staff-guidelines")) {
                    handleStaffGuidelines(exchange);
                } else if (path.contains("/reservation-faq")) {
                    handleReservationFAQ(exchange);
                } else if (path.contains("/system-help")) {
                    handleSystemHelp(exchange);
                } else if (path.contains("/all")) {
                    handleAllHelp(exchange);
                } else {
                    JsonUtil.sendError(exchange, 404, "Help section not found");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            JsonUtil.sendError(exchange, 400, e.getMessage());
        }
    }

    /**
     * GET /help/staff-guidelines - Staff guidelines and best practices
     */
    private void handleStaffGuidelines(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("section", "Staff Guidelines");

        JsonArray guidelines = new JsonArray();

        // Guideline 1
        JsonObject g1 = new JsonObject();
        g1.addProperty("id", 1);
        g1.addProperty("title", "Check-In Procedure");
        g1.addProperty("description",
                "1. Verify guest identity\n2. Check reservation details\n3. Assign room key\n4. Provide welcome information\n5. Record check-in time");
        guidelines.add(g1);

        // Guideline 2
        JsonObject g2 = new JsonObject();
        g2.addProperty("id", 2);
        g2.addProperty("title", "Room Allocation");
        g2.addProperty("description",
                "1. Only allocate AVAILABLE rooms\n2. Consider guest preferences\n3. Ensure cleanliness before assignment\n4. Document special requests\n5. Notify housekeeping if needed");
        guidelines.add(g2);

        // Guideline 3
        JsonObject g3 = new JsonObject();
        g3.addProperty("id", 3);
        g3.addProperty("title", "Guest Communication");
        g3.addProperty("description",
                "1. Be polite and professional\n2. Respond promptly to inquiries\n3. Document all communications\n4. Escalate issues to manager if needed\n5. Follow up after checkout");
        guidelines.add(g3);

        // Guideline 4
        JsonObject g4 = new JsonObject();
        g4.addProperty("id", 4);
        g4.addProperty("title", "Reservation Management");
        g4.addProperty("description",
                "1. Verify dates and room types\n2. Check availability before confirming\n3. Process cancellations properly\n3. Update status after operations\n4. Maintain accurate records");
        guidelines.add(g4);

        // Guideline 5
        JsonObject g5 = new JsonObject();
        g5.addProperty("id", 5);
        g5.addProperty("title", "Safety & Security");
        g5.addProperty("description",
                "1. Never share guest information\n2. Report suspicious activity\n3. Lock valuables properly\n4. Follow emergency procedures\n5. Use secure passwords");
        guidelines.add(g5);

        response.add("guidelines", guidelines);
        JsonUtil.sendJsonWithMessage(exchange, "Staff Guidelines Retrieved", response);
    }

    /**
     * GET /help/reservation-faq - Frequently asked questions about reservations
     */
    private void handleReservationFAQ(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("section", "Reservation FAQ");

        JsonArray faqs = new JsonArray();

        // FAQ 1
        JsonObject faq1 = new JsonObject();
        faq1.addProperty("question", "Can a guest reserve multiple rooms?");
        faq1.addProperty("answer",
                "Yes! We support multi-room reservations. A single guest can book multiple rooms at once. This is useful for families or group bookings.");
        faqs.add(faq1);

        // FAQ 2
        JsonObject faq2 = new JsonObject();
        faq2.addProperty("question", "What is the check-in and check-out time?");
        faq2.addProperty("answer",
                "Standard check-in time is 3:00 PM and check-out time is 11:00 AM. Early check-in or late check-out can be arranged with manager approval.");
        faqs.add(faq2);

        // FAQ 3
        JsonObject faq3 = new JsonObject();
        faq3.addProperty("question", "How are billing amounts calculated?");
        faq3.addProperty("answer",
                "Total bill = Number of Nights × Sum of all reserved Room Prices. Each room's rate is fetched from the system and multiplied by the number of nights.");
        faqs.add(faq3);

        // FAQ 4
        JsonObject faq4 = new JsonObject();
        faq4.addProperty("question", "Can reservations be modified?");
        faq4.addProperty("answer",
                "Yes, you can update room dates. However, only OCCUPIED reservations can be changed. COMPLETED or CANCELLED reservations require manager approval.");
        faqs.add(faq4);

        // FAQ 5
        JsonObject faq5 = new JsonObject();
        faq5.addProperty("question", "What happens during cancellation?");
        faq5.addProperty("answer",
                "When a reservation is cancelled, the status changes to CANCELLED. Cancelled rooms become available for other guests unless there are special arrangements.");
        faqs.add(faq5);

        // FAQ 6
        JsonObject faq6 = new JsonObject();
        faq6.addProperty("question", "How do I check room availability?");
        faq6.addProperty("answer",
                "Use GET /rooms endpoint with optional parameters: ?status=AVAILABLE&checkInDate=2026-03-01&checkOutDate=2026-03-05");
        faqs.add(faq6);

        response.add("faqs", faqs);
        JsonUtil.sendJsonWithMessage(exchange, "Reservation FAQ Retrieved", response);
    }

    /**
     * GET /help/system-help - General system help and features
     */
    private void handleSystemHelp(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("section", "System Help");

        JsonArray features = new JsonArray();

        // Feature 1
        JsonObject f1 = new JsonObject();
        f1.addProperty("title", "Dashboard");
        f1.addProperty("description",
                "Main hub for all operations. View reservations, manage rooms, and access billing.");
        features.add(f1);

        // Feature 2
        JsonObject f2 = new JsonObject();
        f2.addProperty("title", "Reservation Management");
        f2.addProperty("description",
                "Create, read, update, and cancel reservations. Supports multiple rooms in single reservation.");
        features.add(f2);

        // Feature 3
        JsonObject f3 = new JsonObject();
        f3.addProperty("title", "Room Management");
        f3.addProperty("description", "View available rooms, filter by status and dates, manage room inventory.");
        features.add(f3);

        // Feature 4
        JsonObject f4 = new JsonObject();
        f4.addProperty("title", "Guest Management");
        f4.addProperty("description", "Manage guest profiles, contact information, and reservation history.");
        features.add(f4);

        // Feature 5
        JsonObject f5 = new JsonObject();
        f5.addProperty("title", "Billing System");
        f5.addProperty("description",
                "Automatic bill generation when completing reservations. Email bills to guests with itemized room charges.");
        features.add(f5);

        // Feature 6
        JsonObject f6 = new JsonObject();
        f6.addProperty("title", "User Management");
        f6.addProperty("description",
                "Register new staff members, manage roles (MANAGER, STAFF), handle authentication.");
        features.add(f6);

        response.add("features", features);
        JsonUtil.sendJsonWithMessage(exchange, "System Help Retrieved", response);
    }

    /**
     * GET /help/all - Get all help sections
     */
    private void handleAllHelp(HttpExchange exchange) throws IOException {
        JsonObject response = new JsonObject();

        // Staff Guidelines
        JsonArray guidelines = new JsonArray();
        JsonObject g1 = new JsonObject();
        g1.addProperty("id", 1);
        g1.addProperty("title", "Check-In Procedure");
        g1.addProperty("description",
                "1. Verify guest identity\n2. Check reservation details\n3. Assign room key\n4. Provide welcome information\n5. Record check-in time");
        guidelines.add(g1);

        JsonObject g2 = new JsonObject();
        g2.addProperty("id", 2);
        g2.addProperty("title", "Room Allocation");
        g2.addProperty("description",
                "1. Only allocate AVAILABLE rooms\n2. Consider guest preferences\n3. Ensure cleanliness before assignment\n4. Document special requests\n5. Notify housekeeping if needed");
        guidelines.add(g2);

        // FAQs
        JsonArray faqs = new JsonArray();
        JsonObject faq1 = new JsonObject();
        faq1.addProperty("question", "Can a guest reserve multiple rooms?");
        faq1.addProperty("answer", "Yes! We support multi-room reservations easily.");
        faqs.add(faq1);

        response.add("staff_guidelines", guidelines);
        response.add("reservation_faq", faqs);
        JsonUtil.sendJsonWithMessage(exchange, "All Help Content Retrieved", response);
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
