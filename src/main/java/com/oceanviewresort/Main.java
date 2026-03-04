package com.oceanviewresort;

import com.oceanviewresort.controller.AuthController;
import com.oceanviewresort.controller.BillingController;
import com.oceanviewresort.controller.DashboardController;
import com.oceanviewresort.controller.GuestController;
import com.oceanviewresort.controller.HelpController;
import com.oceanviewresort.controller.ReservationController;
import com.oceanviewresort.controller.RoomController;
import com.oceanviewresort.controller.UserController;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public static void main(String[] args) {
                try {
                        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

                        server.createContext("/auth/login",
                                        createLoggingHandler(new AuthController(), "/auth/login"));
                        server.createContext("/user/register",
                                        createLoggingHandler(new UserController(), "/user/register"));
                        server.createContext("/user/logout",
                                        createLoggingHandler(new UserController(), "/user/logout"));
                        server.createContext("/user",
                                        createLoggingHandler(new UserController(), "/user"));
                        server.createContext("/user/me",
                                        createLoggingHandler(new UserController(), "/user/me"));
                        server.createContext("/reservations",
                                        createLoggingHandler(new ReservationController(), "/reservations"));
                        server.createContext("/billing",
                                        createLoggingHandler(new BillingController(), "/billing"));
                        server.createContext("/billing/checkout",
                                        createLoggingHandler(new BillingController(), "/billing/checkout"));
                        server.createContext("/rooms",
                                        createLoggingHandler(new RoomController(), "/rooms"));
                        server.createContext("/dashboard",
                                        createLoggingHandler(new DashboardController(), "/dashboard"));
                        server.createContext("/guests",
                                        createLoggingHandler(new GuestController(), "/guests"));
                        server.createContext("/help",
                                        createLoggingHandler(new HelpController(), "/help"));

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

                        // Handle CORS preflight (OPTIONS) before passing to controller
                        if (method.equalsIgnoreCase("OPTIONS")) {
                                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                                exchange.getResponseHeaders().set("Access-Control-Allow-Methods",
                                                "GET, POST, PUT, DELETE, OPTIONS");
                                exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                                                "Content-Type, Authorization");
                                exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
                                exchange.sendResponseHeaders(204, -1);
                                System.out.printf("[%s] %s %s - 204 (CORS preflight)%n", timestamp, method, uri);
                                return;
                        }

                        long startTime = System.currentTimeMillis();

                        handler.handle(exchange);

                        long duration = System.currentTimeMillis() - startTime;
                        int statusCode = exchange.getResponseCode();

                        System.out.printf("[%s] %s %s - %d (%dms)%n",
                                        timestamp, method, uri, statusCode, duration);
                };
        }
}