package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.api.http.ApiHttp;
import org.example.api.http.ResponseWriter;
import org.example.api.json.JsonWriter;
import org.example.log.Transaction;
import org.example.log.TransactionStore;
import org.example.util.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Simple API server
 */
public class ApiServer {
    private final int PORT;
    private final HttpServer apiServer;
    private final TransactionController txController;

    public ApiServer(Config config, TransactionStore store) {
        PORT = config.getApiPort();
        this.txController = new TransactionController(store);

        try {
            this.apiServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create API server", e);
        }
    }

    public void start() {
        apiServer.createContext("/health", this::handleHealth);
        apiServer.createContext("/transactions", this::handleTransactions);
        apiServer.createContext("/stats", this::handleStats);

        apiServer.setExecutor(Executors.newFixedThreadPool(8));
        apiServer.start();
        System.out.println("API server running on port: " + PORT);
    }

    private void handleHealth(HttpExchange ex) throws IOException {
        if (ResponseWriter.handlePreflight(ex)) return;

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ResponseWriter.writeJson(ex, 405, JsonWriter.jsonError(405, "Method Not Allowed"));
            return;
        }
        ResponseWriter.writePlain(ex, 200, "ok");
    }

    private void handleTransactions(HttpExchange ex) throws IOException {
        if (ResponseWriter.handlePreflight(ex)) return;

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ResponseWriter.writeJson(ex, 405, JsonWriter.jsonError(405, "Method Not Allowed"));
            return;
        }

        Map<String, String> q = ApiHttp.parseQuery(ex.getRequestURI().getRawQuery());
        Integer limit = ApiHttp.parseIntOrNull(q.get("limit"));
        String verdict = q.get("verdict");

        ApiResponse<List<Transaction>> resp = txController.listTransactions(limit, verdict);
        ResponseWriter.writeJson(ex, resp.statusCode, JsonWriter.toJson(resp));
    }

    private void handleStats(HttpExchange ex) throws IOException {
        if (ResponseWriter.handlePreflight(ex)) return;

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ResponseWriter.writeJson(ex, 405, JsonWriter.jsonError(405, "Method Not Allowed"));
            return;
        }

        ApiResponse<TransactionController.StatsResponse> resp = txController.stats();
        ResponseWriter.writeJson(ex, resp.statusCode, JsonWriter.toJson(resp));
    }
}