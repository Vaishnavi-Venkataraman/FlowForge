package com.flowforge.web.handler;

import com.flowforge.web.UserStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Handles /api/auth/* endpoints: signup, login, logout.
 */
public class AuthHandler implements HttpHandler {

    private final UserStore userStore;

    public AuthHandler(UserStore userStore) {
        this.userStore = userStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.endsWith("/signup") && "POST".equals(exchange.getRequestMethod())) {
                handleSignup(exchange);
            } else if (path.endsWith("/login") && "POST".equals(exchange.getRequestMethod())) {
                handleLogin(exchange);
            } else if (path.endsWith("/logout") && "POST".equals(exchange.getRequestMethod())) {
                handleLogout(exchange);
            } else if (path.endsWith("/me") && "GET".equals(exchange.getRequestMethod())) {
                handleMe(exchange);
            } else {
                HttpHelper.sendJson(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception e) {
            HttpHelper.sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleSignup(HttpExchange exchange) throws IOException {
        Map<String, String> body = HttpHelper.parseBody(exchange);
        String username = body.get("username");
        String password = body.get("password");
        String displayName = body.getOrDefault("displayName", username);

        String error = userStore.register(username, password, displayName);
        if (error != null) {
            HttpHelper.sendJson(exchange, 400, Map.of("error", error));
            return;
        }

        String sessionId = userStore.login(username, password);
        HttpHelper.setCookie(exchange, sessionId);
        HttpHelper.sendJson(exchange, 201, Map.of(
                "success", true,
                "username", username,
                "sessionId", sessionId
        ));
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> body = HttpHelper.parseBody(exchange);
        String username = body.get("username");
        String password = body.get("password");

        String sessionId = userStore.login(username, password);
        if (sessionId == null) {
            HttpHelper.sendJson(exchange, 401, Map.of("error", "Invalid username or password"));
            return;
        }

        HttpHelper.setCookie(exchange, sessionId);
        UserStore.UserRecord user = userStore.getUser(username);
        HttpHelper.sendJson(exchange, 200, Map.of(
                "success", true,
                "username", username,
                "displayName", user.displayName(),
                "sessionId", sessionId
        ));
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String sessionId = HttpHelper.getSessionId(exchange);
        if (sessionId != null) {
            userStore.logout(sessionId);
        }
        HttpHelper.sendJson(exchange, 200, Map.of("success", true));
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        String sessionId = HttpHelper.getSessionId(exchange);
        String username = userStore.getUsername(sessionId);
        if (username == null) {
            HttpHelper.sendJson(exchange, 401, Map.of("error", "Not authenticated"));
            return;
        }
        UserStore.UserRecord user = userStore.getUser(username);
        HttpHelper.sendJson(exchange, 200, Map.of(
                "username", username,
                "displayName", user.displayName()
        ));
    }
}