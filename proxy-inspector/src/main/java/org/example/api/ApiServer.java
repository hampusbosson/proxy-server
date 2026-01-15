package org.example.api;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class ApiServer {
    private final int PORT = 9090;
    HttpServer apiServer;

    public ApiServer() {
        try {
            apiServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
            throw new RuntimeException();
        }
    }

    public void start() {
        apiServer.createContext("/health", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        apiServer.setExecutor(Executors.newFixedThreadPool(8));
        apiServer.start();
        System.out.println("API server running on port: " + PORT);
    }

}
