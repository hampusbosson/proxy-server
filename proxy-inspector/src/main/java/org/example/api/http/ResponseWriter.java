package org.example.api.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Centralized HTTP response helper for the API server.
 * Handles CORS headers, OPTIONS preflight requests and writes JSON/plain responses
 */
public class ResponseWriter {

    private ResponseWriter() {}

    /**
     * Handles CORS preflight (OPTIONS). If this was an OPTIONS request, we respond 204 and return true.
     * Call this at the top of every handler and early-return if it returns true.
     */
    public static boolean handlePreflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            addCors(ex);
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return true;
        }
        return false;
    }

    /**
     * Adds permissive CORS headers
     */
    public static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    /** Writes a JSON response with Content-Type application/json; charset=utf-8. */
    public static void writeJson(HttpExchange ex, int status, String json) throws IOException {
        addCors(ex);
        byte[] bytes = (json == null ? "" : json).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Writes a plain text response with Content-Type text/plain; charset=utf-8. */
    public static void writePlain(HttpExchange ex, int status, String text) throws IOException {
        addCors(ex);
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
