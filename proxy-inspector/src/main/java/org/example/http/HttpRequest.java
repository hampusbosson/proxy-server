package org.example.http;

import java.util.Map;

public class HttpRequest {
    private final String method;
    private final String target;
    private final String version;
    private final Map<String, String> headers;
    private final String body;

    public HttpRequest(String method, String target, String version, Map<String, String> headers, String body) {
        this.method = method;
        this.target = target;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() {
        return this.method;
    }

    public String getTarget() {
        return this.target;
    }

    public String getVersion() {
        return this.version;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public String getBody() {
        return this.body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Request line
        sb.append(method)
                .append(" ")
                .append(target)
                .append(" ")
                .append(version)
                .append("\n");

        // Headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }

        // Empty line between headers and body (HTTP semantics)
        sb.append("\n");

        // Body (optional)
        if (body != null) {
            sb.append(body);
        }

        return sb.toString();
    }
}
