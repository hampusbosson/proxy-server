package org.example.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpParser {
    private BufferedReader reader;

    public HttpParser(BufferedReader reader) {
        this.reader = reader;
    }

    public HttpRequest readRequest() throws IOException {
        // Read request line: "METHOD TARGET VERSION"
        String requestLine = reader.readLine();
        if (requestLine == null) {
            return null; // client closed connection before sending anything
        }
        requestLine = requestLine.trim();
        if (requestLine.isEmpty()) {
            throw new InvalidRequestException("Empty request line");
        }

        String[] parts = requestLine.split("\\s+");
        if (parts.length != 3) {
            throw new InvalidRequestException("Invalid request line");
        }

        String method = parts[0];
        String target = parts[1];
        String version = parts[2];

        // read headers until empty line
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) { // end of headers
                break;
            }

            int colonIndex = line.indexOf(":");

            if (colonIndex <= 0) {
                throw new InvalidRequestException("Invalid header line");
            }

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            headers.put(key, value);
        }

        String body = null; // TODO:add body logic later

        return new HttpRequest(method, target, version, headers, body);

    }
}
