package org.example.proxy;

import org.example.http.HttpRequest;
import org.example.http.HttpSerializer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Forwarder {
    private final HttpRequest request;
    private String host;
    private int port;
    private final HttpSerializer serializer;

    public Forwarder(HttpRequest request, HttpSerializer serializer) {
        this.request = request;
        this.serializer = serializer;
    }

    private void extractHostAndPort() {
        String hostHeader = null;

        // 1) Find Host header
        for (Map.Entry<String, String> e : request.getHeaders().entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase("Host")) {
                hostHeader = e.getValue();
                break;
            }
        }

        // 2) Host is mandatory in HTTP/1.1
        if (hostHeader == null || hostHeader.isBlank()) {
            throw new InvalidRequestException("Missing Host header");
        }

        // 3) parse host and optional port
        try {
            String hostPart;
            int portPart;

            // IPv6 literal: [::1]:8080
            if (hostHeader.startsWith("[")) {
                int closing = hostHeader.indexOf("]");
                if (closing == -1) {
                    throw new InvalidRequestException("Invalid IPv6 Host header");
                }

                hostPart = hostHeader.substring(1, closing);

                // check format
                if (hostHeader.length() > closing + 1) {
                    if (hostHeader.charAt(closing + 1) != ':') {
                        throw new InvalidRequestException("Invalid IPv6 Host header format");
                    }
                    portPart = Integer.parseInt(hostHeader.substring(closing + 2));
                } else {
                    portPart = 80;
                }
            } else {
                // IPv4 or hostname
                String[] parts = hostHeader.split(":", 2);
                hostPart = parts[0];

                if (parts.length == 2) {
                    portPart = Integer.parseInt(parts[1]);
                } else {
                    portPart = 80;
                }
            }

            if (hostPart.isBlank() || portPart <= 0 || portPart > 65535) {
                throw new InvalidRequestException("Invalid host header value");
            }

            this.host = hostPart;
            this.port = portPart;

        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid port in Host header", e);
        }
    }

    public void forwardToServer(OutputStream clientOut) {
        extractHostAndPort();

        // serialize request
        String raw = serializer.serializeRequest(request);
        if (raw == null || raw.isEmpty()) {
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

            OutputStream serverOut = targetSocket.getOutputStream(); // send to server
            InputStream serverIn = targetSocket.getInputStream(); // listen to server

            // 1) send request to end server
            byte[] rawBytes = raw.getBytes(StandardCharsets.UTF_8);
            serverOut.write(rawBytes);
            serverOut.flush();

            // 2) send response from server back to client
            byte[] buffer = new byte[8192];
            int n;
            while ((n = serverIn.read(buffer)) != -1) {
                clientOut.write(buffer, 0, n);
                clientOut.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to forward to " + host + ":" + port + " cause: ", e);
        }
    }

}

