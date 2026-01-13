package org.example.http;

import java.net.URI;
import java.util.Map;

public class HttpRequest {
    private final String method;
    private final String target;
    private final String version;
    private final Map<String, String> headers;
    private final String body;
    private transient HostPort cachedHostPort;

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

    public String getHost() {
        HostPort header = parseHostHeader();
        return header.host;
    }

    public int getPort () {
        HostPort header = parseHostHeader();
        return header.port;
    }

    public String getPath() {
        return toOriginFormTarget(target);
    }

    private String toOriginFormTarget(String target) {
        if (target == null || target.isEmpty()) {
            return "/";
        }

        if (target.startsWith("/")) {
            return target;
        }

        if (target.startsWith("http://") || target.startsWith("https://")) {
            try {
                URI uri = URI.create(target);
                String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) ? "/" : uri.getRawPath();
                String query = uri.getRawQuery();
                return (query == null) ? path : path + "?" + query;
            } catch (IllegalArgumentException ignored) {

            }
        }
        return target;
    }

    private String getHeaderIgnoreCase(String name) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private HostPort parseHostHeader() {
        if (cachedHostPort != null) return cachedHostPort;

        String hostHeader = getHeaderIgnoreCase("Host");
        if (hostHeader == null || hostHeader.isEmpty()) {
            throw new InvalidRequestException("Missing host header");
        }


        try {
            String hostPart;
            int portPart;

            if (hostHeader.startsWith("[")) {
                int closing = hostHeader.indexOf(']');
                if (closing == -1) throw new InvalidRequestException("Invalid IPv6 Host header");

                hostPart = hostHeader.substring(1, closing);

                if (hostHeader.length() > closing + 1) {
                    if (hostHeader.charAt(closing + 1) != ':') {
                        throw new InvalidRequestException("Invalid IPv6 Host header format");
                    }
                    portPart = Integer.parseInt(hostHeader.substring(closing + 2));
                } else {
                    portPart = 80;
                }
            } else {
                String[] parts = hostHeader.split(":", 2);
                hostPart = parts[0];
                portPart = (parts.length == 2) ? Integer.parseInt(parts[1]) : 80;
            }

            if (hostPart.isBlank() || portPart <= 0 || portPart > 65535) {
                throw new InvalidRequestException("Invalid Host header value");
            }

            cachedHostPort = new HostPort(hostPart, portPart);
            return cachedHostPort;

        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid port in Host header", e);
        }

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

    private static final class HostPort {
        final String host;
        final int port;

        HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}