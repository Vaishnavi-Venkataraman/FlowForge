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

    private static final String KEY_ERROR = "error";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_SESSION_ID = "sessionId";

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
                HttpHelper.sendJson(exchange, 404, Map.of(KEY_ERROR, "Not found"));
            }
        } catch (Exception e) {
            HttpHelper.sendJson(exchange, 500, Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    private void handleSignup(HttpExchange exchange) throws IOException {
        Map<String, String> body = HttpHelper.parseBody(exchange);
        String username = body.get(KEY_USERNAME);
        String password = body.get("password");
        String displayName = body.getOrDefault(KEY_DISPLAY_NAME, username);

        String error = userStore.register(username, password, displayName);
        if (error != null) {
            HttpHelper.sendJson(exchange, 400, Map.of(KEY_ERROR, error));
            return;
        }

        String sessionId = userStore.login(username, password);
        HttpHelper.setCookie(exchange, sessionId);

        HttpHelper.sendJson(exchange, 201, Map.of(
                KEY_SUCCESS, true,
                KEY_USERNAME, username,
                KEY_SESSION_ID, sessionId
        ));
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> body = HttpHelper.parseBody(exchange);
        String username = body.get(KEY_USERNAME);
        String password = body.get("password");

        String sessionId = userStore.login(username, password);
        if (sessionId == null) {
            HttpHelper.sendJson(exchange, 401,
                    Map.of(KEY_ERROR, "Invalid username or password"));
            return;
        }

        HttpHelper.setCookie(exchange, sessionId);
        UserStore.UserRecord user = userStore.getUser(username);

        HttpHelper.sendJson(exchange, 200, Map.of(
                KEY_SUCCESS, true,
                KEY_USERNAME, username,
                KEY_DISPLAY_NAME, user.displayName(),
                KEY_SESSION_ID, sessionId
        ));
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String sessionId = HttpHelper.getSessionId(exchange);
        if (sessionId != null) {
            userStore.logout(sessionId);
        }
        HttpHelper.sendJson(exchange, 200, Map.of(KEY_SUCCESS, true));
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        String sessionId = HttpHelper.getSessionId(exchange);
        String username = userStore.getUsername(sessionId);

        if (username == null) {
            HttpHelper.sendJson(exchange, 401,
                    Map.of(KEY_ERROR, "Not authenticated"));
            return;
        }

        UserStore.UserRecord user = userStore.getUser(username);

        HttpHelper.sendJson(exchange, 200, Map.of(
                KEY_USERNAME, username,
                KEY_DISPLAY_NAME, user.displayName()
        ));
    }
}