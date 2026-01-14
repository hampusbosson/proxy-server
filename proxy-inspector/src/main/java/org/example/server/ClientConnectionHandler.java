package org.example.server;

import org.example.http.HttpParser;
import org.example.http.HttpRequest;
import org.example.http.HttpSerializer;
import org.example.log.Transaction;
import org.example.log.Verdict;
import org.example.proxy.Forwarder;

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

    ClientConnectionHandler(Socket connection, AtomicInteger connectionCounter) {
        this.connection = connection;
        this.connectionCounter = connectionCounter;
    }

    @Override
    public Void call() {
        Transaction transaction = null;

        try {
            int updatedCount = connectionCounter.incrementAndGet(); // update client-connection count

            // 1) read request
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            printClientInfo(connection, updatedCount);

            // 2) parse and print request
            HttpParser parser = new HttpParser(in);
            HttpRequest request = parser.readRequest();
            if (request == null) {
                return null;
            }
            System.out.println("new request from client: " + request);


            // 3) forward to end server
            HttpSerializer serializer = new HttpSerializer();
            // transaction object for info logging
            transaction = new Transaction(request.getMethod(), request.getHost(), request.getPort(), request.getPath(), System.nanoTime());
            Forwarder forwarder = new Forwarder(request, serializer);
            forwarder.forwardToServer(connection.getOutputStream(), transaction);

            transaction.setVerdict(Verdict.ALLOWED); // on success, set path to allowed
            System.out.println(transaction); // log transaction

        } catch (org.example.http.InvalidRequestException e) {
            // 400
            if (transaction != null) {
                transaction.setVerdict(Verdict.ERROR);
                transaction.setErrorMessage(e.getMessage());
                transaction.setEndNs(System.nanoTime());
                System.out.println(transaction);
            }
            writeErrorResponse(400, "Bad Request", e.getMessage());

        } catch (RuntimeException e) {
            // 502 (forwarding/dns/connect/read failures bubbled up)
            if (transaction != null) {
                transaction.setVerdict(Verdict.ERROR);
                transaction.setErrorMessage(e.getMessage());
                transaction.setEndNs(System.nanoTime());
                System.out.println(transaction);
            }
            writeErrorResponse(502, "Bad Gateway", e.getMessage());

        } catch (Exception e) {
            // fallback, still return something
            if (transaction != null) {
                transaction.setVerdict(Verdict.ERROR);
                transaction.setErrorMessage(e.getMessage());
                transaction.setEndNs(System.nanoTime());
                System.out.println(transaction);
            }
            writeErrorResponse(502, "Bad Gateway", "Unexpected proxy error");
        } finally {
            try {
                connection.close();
                int updatedCount = connectionCounter.decrementAndGet();
                System.out.println("CONNECT -- active connections: " + updatedCount);
            } catch (IOException ignored) {

            }
        }
        return null;
    }

    private void printClientInfo(Socket connection, int counter) {
        String clientHost = connection.getInetAddress().toString();
        int clientPort = connection.getPort();

        System.out.println("New client connected from: " + clientHost + " port: " + clientPort);
        System.out.println("CONNECT ++ active connections: " + counter);
    }

    private void writeErrorResponse(int statusCode, String error, String message) {
        try {
            String body = (message == null || message.isEmpty()) ? error : message;
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            String headers =
                    "HTTP/1.1 " + statusCode + " " + error + "\r\n" +
                            "Connection: close\r\n" +
                            "Content-Type: text/plain; charset=utf-8\r\n" +
                            "Content-Length: " + bodyBytes.length + "\r\n" +
                            "\r\n";

            connection.getOutputStream().write(headers.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            connection.getOutputStream().write(bodyBytes);
            connection.getOutputStream().flush();
        } catch (IOException ignored) {
            // nothing more to do if we cant write the error back
        }

    }
}
