package com.oceanviewresort.controller;

import com.oceanviewresort.exception.InvalidCredentialsException;
import com.oceanviewresort.exception.UserInactiveException;
import com.oceanviewresort.exception.UserNotFoundException;
import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.service.UserService;
import com.oceanviewresort.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class AuthController implements HttpHandler {

    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            JsonUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json = JsonUtil.parse(body);

            String username = json.has("username") ? json.get("username").getAsString() : null;
            String password = json.has("password") ? json.get("password").getAsString() : null;

            // Authenticate user
            String token = userService.login(username, password);

            // Get user details to include the name in the response
            com.oceanviewresort.repository.UserRepository userRepo = new com.oceanviewresort.repository.UserRepository();
            com.oceanviewresort.model.User user = userRepo.findByUsername(username);

            // Build response with token and user details
            JsonObject response = new JsonObject();
            response.addProperty("name", user.getName());
            response.addProperty("role", user.getRole());
            response.addProperty("token", token);

            JsonUtil.sendJsonWithMessage(exchange, "Login Successfully", response);

        } catch (JsonSyntaxException e) {
            System.err.println("JSON Syntax Error: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 400, "Invalid JSON format");
        } catch (ValidationException e) {
            System.err.println("Validation Error: " + e.getMessage());
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (InvalidCredentialsException e) {
            System.err.println("Invalid Credentials: " + e.getMessage());
            JsonUtil.sendError(exchange, 401, "Invalid username or password");
        } catch (UserNotFoundException e) {
            System.err.println("User Not Found: " + e.getMessage());
            JsonUtil.sendError(exchange, 404, "User not found");
        } catch (UserInactiveException e) {
            System.err.println("User Inactive: " + e.getMessage());
            JsonUtil.sendError(exchange, 403, "User account is deactivated");
        } catch (Exception e) {
            System.err.println("Unexpected Error in AuthController:");
            System.err.println("Error Type: " + e.getClass().getName());
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Something went wrong, please try again");
        }
    }
}