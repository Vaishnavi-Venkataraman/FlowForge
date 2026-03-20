package com.flowforge.web;
import java.util.logging.Logger;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());
    private final HttpServer server;
    private final int port;

    public WebServer(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
    }

    public void registerHandler(String path, com.sun.net.httpserver.HttpHandler handler) {
        server.createContext(path, handler);
    }

    public void start() {
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
        LOGGER.info(() -> "[WebServer] Started on http://localhost:" + port);
    }

    public void stop() {
        server.stop(1);
        LOGGER.info("[WebServer] Stopped");
    }
}
