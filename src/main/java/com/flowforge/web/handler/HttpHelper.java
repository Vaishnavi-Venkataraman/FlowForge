package com.flowforge.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.flowforge.web.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpHelper {

    private static final String SESSION_PREFIX = "session=";

    private HttpHelper() {}

    public static void sendJson(HttpExchange exchange, int status, Map<String, Object> data) throws IOException {
        String json = JsonUtil.toJson(data);
        sendResponse(exchange, status, json, "application/json");
    }

    public static void sendJsonArray(HttpExchange exchange, int status, String jsonArray) throws IOException {
        sendResponse(exchange, status, jsonArray, "application/json");
    }

    public static void sendResponse(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static Map<String, String> parseBody(HttpExchange exchange) throws IOException {
        return JsonUtil.parseJsonFlat(readBody(exchange));
    }

    public static String getSessionId(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) return null;

        for (String part : cookie.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(SESSION_PREFIX)) {
                return trimmed.substring(SESSION_PREFIX.length());
            }
        }

        // Also check Authorization header
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        return null;
    }

    public static void setCookie(HttpExchange exchange, String sessionId) {
        exchange.getResponseHeaders().set("Set-Cookie",
                SESSION_PREFIX + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
    }

    public static void handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }
}