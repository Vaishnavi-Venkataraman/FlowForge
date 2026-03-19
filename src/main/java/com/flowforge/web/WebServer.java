package com.flowforge.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {

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
        System.out.println("[WebServer] Started on http://localhost:" + port);
    }

    public void stop() {
        server.stop(1);
        System.out.println("[WebServer] Stopped");
    }
}