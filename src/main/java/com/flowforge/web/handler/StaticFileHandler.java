package com.flowforge.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serves static files from classpath (src/main/resources/static/).
 * Falls back to index.html for SPA routing.
 */
public class StaticFileHandler implements HttpHandler {

    private static final Map<String, String> MIME_TYPES = Map.of(
            "html", "text/html",
            "css", "text/css",
            "js", "application/javascript",
            "json", "application/json",
            "png", "image/png",
            "svg", "image/svg+xml",
            "ico", "image/x-icon"
    );

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Map / to /index.html
        if (path.equals("/")) {
            path = "/index.html";
        }

        // Try to serve from classpath
        String resourcePath = "/static" + path;
        InputStream is = getClass().getResourceAsStream(resourcePath);

        // SPA fallback: if not a static file, serve index.html
        if (is == null && !path.startsWith("/api/")) {
            is = getClass().getResourceAsStream("/static/index.html");
            path = "/index.html";
        }

        if (is == null) {
            String notFound = "Not found: " + path;
            exchange.sendResponseHeaders(404, notFound.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        // Determine content type
        String ext = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : "html";
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

        byte[] bytes = is.readAllBytes();
        is.close();

        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}