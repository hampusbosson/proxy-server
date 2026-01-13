package org.example.server;

import org.example.http.HttpParser;
import org.example.http.HttpRequest;
import org.example.http.HttpSerializer;
import org.example.proxy.Forwarder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientConnectionHandler implements Callable<Void> {
    private final Socket connection;
    private final AtomicInteger connectionCounter;
    private HttpParser parser;

    ClientConnectionHandler(Socket connection, AtomicInteger connectionCounter) {
        this.connection = connection;
        this.connectionCounter = connectionCounter;
    }

    @Override
    public Void call() {
        try {
            int updatedCount = connectionCounter.incrementAndGet(); // update client-connection count
            // 1) read request
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            printClientInfo(connection, updatedCount);

            // 2) parse and print request
            parser = new HttpParser(in);
            HttpRequest request = parser.readRequest();
            System.out.println("new request: " + request);

            // 3) forward to end server
            HttpSerializer serializer = new HttpSerializer();
            Forwarder forwarder = new Forwarder(request, serializer);
            forwarder.forwardToServer(connection.getOutputStream());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
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
}
