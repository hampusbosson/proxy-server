package org.example.proxy;

import org.example.http.HttpRequest;
import org.example.http.HttpSerializer;
import org.example.log.Transaction;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class Forwarder {
    private final HttpRequest request;
    private final HttpSerializer serializer;

    public Forwarder(HttpRequest request, HttpSerializer serializer) {
        this.request = request;
        this.serializer = serializer;
    }


    public void forwardToServer(OutputStream clientOut, Transaction transaction) {
        String host = request.getHost();
        int port = request.getPort();

        // serialize request
        String rawRequest = serializer.serializeRequest(request);
        if (rawRequest == null || rawRequest.isEmpty()) {
            System.err.println("Could not serialize request");
            return;
        }

        int connectTimeoutMs = 3_000;
        int readTimeoutMs = 15_000;

        try (Socket targetSocket = new Socket()) {
            SocketAddress address = new InetSocketAddress(host, port);

            targetSocket.connect(address, connectTimeoutMs); // connect and timeout after 3 seconds if not able to
            targetSocket.setSoTimeout(readTimeoutMs); // timeout after 15 seconds, avoids hanging forever
            System.out.println("Connected to server " + host + " on port " + port);

            OutputStream serverOut = targetSocket.getOutputStream(); // used for sending to server
            InputStream serverIn = targetSocket.getInputStream(); // used for listening to server

            // 1) send request to end server
            byte[] rawBytes = rawRequest.getBytes(StandardCharsets.UTF_8);
            serverOut.write(rawBytes); // send raw bytes to server
            serverOut.flush();  // flush buffered bytes

            System.out.println("Sending serialized request to end server: " + rawRequest);

            // 2) send response from server back to client
            byte[] buffer = new byte[8192];
            int n;
            long bytesFromServer = 0;
            while ((n = serverIn.read(buffer)) != -1) {
                bytesFromServer += n;
                clientOut.write(buffer, 0, n);
                clientOut.flush();
            }

            transaction.setBytesFromServer(bytesFromServer); // set response bytes to transaction object
            transaction.setEndNs(System.nanoTime()); // set end time for response


        } catch (IOException e) {
            throw new RuntimeException("Failed to forward to " + host + ":" + port + " cause: ", e);
        }
    }

}

