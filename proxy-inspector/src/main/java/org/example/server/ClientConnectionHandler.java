package org.example.server;

import org.example.http.HttpParser;
import org.example.http.HttpRequest;
import org.example.http.HttpSerializer;
import org.example.log.Transaction;
import org.example.log.TransactionStore;
import org.example.log.Verdict;
import org.example.policy.PolicyDecision;
import org.example.policy.PolicyEngine;
import org.example.proxy.Forwarder;
import org.example.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientConnectionHandler implements Callable<Void> {
    private final Socket connection;
    private final AtomicInteger connectionCounter;
    private final TransactionStore store;
    private final PolicyEngine engine;
    private final Config config;

    ClientConnectionHandler(Socket connection,
                            AtomicInteger connectionCounter,
                            TransactionStore store,
                            PolicyEngine engine,
                            Config config) {
        this.connection = connection;
        this.connectionCounter = connectionCounter;
        this.store = store;
        this.engine = engine;
        this.config = config;
    }

    @Override
    public Void call() {
        Transaction tx = null;
        String clientIp = connection.getInetAddress().getHostAddress();

        try {
            int active = connectionCounter.incrementAndGet();

            // Read and parse one HTTP request from the client connection
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            logVerbose(() -> printClientInfo(connection, active));

            HttpParser parser = new HttpParser(in);
            HttpRequest request = parser.readRequest();
            if (request == null) {
                // Client connected then closed without sending a request
                return null;
            }

            // Create a transaction for logging + store (start timer here)
            tx = new Transaction(
                    request.getMethod(),
                    request.getHost(),
                    request.getPort(),
                    request.getPath(),
                    System.nanoTime()
            );

            // Evaluate policies (rate limit / host block / path block)
            PolicyDecision decision = engine.evaluate(request, clientIp);
            if (decision.isBlocked()) {
                tx.setVerdict(Verdict.BLOCKED);
                tx.setErrorMessage(decision.getReason());
                tx.setBytesFromServer(0);
                tx.setEndNs(System.nanoTime());

                store.add(tx);
                logLine(tx.toString()); // non-verbose: always show the final one-line summary

                writeErrorResponse(decision.getHttpStatus(),
                        statusText(decision.getHttpStatus()),
                        decision.getReason());
                return null;
            }

            // Forward to end server
            logVerbose(() -> System.out.println("new request from client:\n" + request));

            HttpSerializer serializer = new HttpSerializer();
            Forwarder forwarder = new Forwarder(request, serializer);
            forwarder.forwardToServer(connection.getOutputStream(), tx);

            // Mark success, store, log
            tx.setVerdict(Verdict.ALLOWED);
            store.add(tx);

            logLine(tx.toString()); // non-verbose: always show the final one-line summary
            logVerbose(() -> System.out.println("store size=" + store.sizeSafe())); // see helper below

        } catch (org.example.http.InvalidRequestException e) {
            // Parser/validation error => 400
            if (tx != null) {
                tx.setVerdict(Verdict.ERROR);
                tx.setErrorMessage(e.getMessage());
                tx.setBytesFromServer(0);
                tx.setEndNs(System.nanoTime());
                store.add(tx);
                logLine(tx.toString());
            }
            writeErrorResponse(400, "Bad Request", e.getMessage());

        } catch (RuntimeException e) {
            // Forwarding failures => 502
            if (tx != null) {
                tx.setVerdict(Verdict.ERROR);
                tx.setErrorMessage(e.getMessage());
                tx.setBytesFromServer(0);
                tx.setEndNs(System.nanoTime());
                store.add(tx);
                logLine(tx.toString());
            }
            writeErrorResponse(502, "Bad Gateway", e.getMessage());

        } catch (Exception e) {
            // Catch-all fallback => 502 with generic message to client
            if (tx != null) {
                tx.setVerdict(Verdict.ERROR);
                tx.setErrorMessage(e.getMessage());
                tx.setBytesFromServer(0);
                tx.setEndNs(System.nanoTime());
                store.add(tx);
                logLine(tx.toString());
            }
            writeErrorResponse(502, "Bad Gateway", "Unexpected proxy error");

        } finally {
            try {
                connection.close();
            } catch (IOException ignored) {
            } finally {
                int active = connectionCounter.decrementAndGet();
                logVerbose(() -> System.out.println("CONNECT -- active connections: " + active));
            }
        }

        return null;
    }


    //Non-verbose logging:
    //only print the final transaction summary line (ALLOWED/BLOCKED/ERROR).
    private void logLine(String msg) {
        System.out.println(msg);
    }

    //Verbose logging: print internal details only when config.verbose is enabled.
    private void logVerbose(Runnable r) {
        if (config != null && config.isVerbose()) {
            r.run();
        }
    }

    private void printClientInfo(Socket connection, int counter) {
        String clientHost = connection.getInetAddress().toString();
        int clientPort = connection.getPort();

        System.out.println("New client connected from: " + clientHost + " port: " + clientPort);
        System.out.println("CONNECT ++ active connections: " + counter);
    }

    // HTTP error writing

    private void writeErrorResponse(int statusCode, String statusText, String message) {
        try {
            String body = (message == null || message.isEmpty()) ? statusText : message;
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            String headers =
                    "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                            "Connection: close\r\n" +
                            "Content-Type: text/plain; charset=utf-8\r\n" +
                            "Content-Length: " + bodyBytes.length + "\r\n" +
                            "\r\n";

            connection.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));
            connection.getOutputStream().write(bodyBytes);
            connection.getOutputStream().flush();
        } catch (IOException ignored) {
        }
    }

    private String statusText(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 403 -> "Forbidden";
            case 429 -> "Too Many Requests";
            case 502 -> "Bad Gateway";
            default -> "Error";
        };
    }
}