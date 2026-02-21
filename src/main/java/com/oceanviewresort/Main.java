package com.oceanviewresort;

import com.oceanviewresort.controller.AuthController;
import com.oceanviewresort.controller.BillingController;
import com.oceanviewresort.controller.ReservationController;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/auth/login",
                    createLoggingHandler(new AuthController(), "/auth/login"));
            server.createContext("/reservations",
                    createLoggingHandler(new ReservationController(), "/reservations"));
            server.createContext("/billing/checkout",
                    createLoggingHandler(new BillingController(), "/billing/checkout"));

            server.setExecutor(null);
            server.start();

            System.out.println("========================================");
            System.out.println("Ocean View Resort System Started!");
            System.out.println("========================================");
            System.out.println("Server running at: http://localhost:8080");
            System.out.println("Listening for incoming requests...\n");

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static HttpHandler createLoggingHandler(HttpHandler handler, String path) {
        return exchange -> {
            String timestamp = LocalDateTime.now().format(formatter);
            String method = exchange.getRequestMethod();
            String uri = exchange.getRequestURI().toString();
            String clientAddress = exchange.getRemoteAddress().getAddress().getHostAddress();

            System.out.printf("[%s] %s %s from %s%n",
                    timestamp, method, uri, clientAddress);

            long startTime = System.currentTimeMillis();

            handler.handle(exchange);

            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponseCode();

            System.out.printf("[%s] %s %s - %d (%dms)%n",
                    timestamp, method, uri, statusCode, duration);
        };
    }
}