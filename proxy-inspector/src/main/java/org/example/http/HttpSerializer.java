package org.example.http;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpSerializer {
    private static final String CRLF = "\r\n";

    public String serializeRequest(HttpRequest req) {
        String method = req.getMethod();
        String targetForServer = req.getPath(); // convert target URL for end server
        String version = req.getVersion();
        Map<String, String> headers = req.getHeaders();
        String body = req.getBody();

        // Copy + normalize headers for end server
        Map<String, String> filteredHeaders = stripHopHeaders(headers);

        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(targetForServer).append(" ").append(version).append(CRLF);

        for (Map.Entry<String, String> h : filteredHeaders.entrySet()) {
            sb.append(h.getKey()).append(": ").append(h.getValue()).append(CRLF);
        }

        sb.append(CRLF); // end of headers

        if (body != null) {
            sb.append(body);
        }

        return sb.toString();
    }


    /**
     * Copy request headers for forwarding to the end server, but remove proxy-only and hop-by-hop headers.
     *
     * Hop-by-hop headers apply only to a single TCP connection (one "hop"),
     * In our case, that hop is the connection between client and our proxy server.
     *
     * They must NOT be forwarded to the next hop, which is from our proxy server to the end server.
     */
    private Map<String, String> stripHopHeaders(Map<String, String> headers) {
        Map<String, String> filteredHeaders = new LinkedHashMap<>();

        for (Map.Entry<String, String> h : headers.entrySet()) {
            String key = h.getKey();
            String value = h.getValue();

            if (key == null) continue;
            if (key.equalsIgnoreCase("Proxy-Connection")) continue;
            if (key.equalsIgnoreCase("Connection")) continue;
            if (key.equalsIgnoreCase("Keep-Alive")) continue;
            if (key.equalsIgnoreCase("Transfer-Encoding")) continue;
            if (key.equalsIgnoreCase("Upgrade")) continue;

            filteredHeaders.put(key, value);
        }

        // make sure host exists (required for HTTP/1.1). If missing forward will fail
        if (!containsHeaderIgnoreCase(filteredHeaders, "Host")) {
            String host = getHeaderIgnoreCase(headers, "Host");
            if (host != null) {
                filteredHeaders.put("Host", host);
            }
        }

        // force close, simplifies response handling (read until server closes)
        filteredHeaders.put("Connection", "close");

        return filteredHeaders;
    }

    private boolean containsHeaderIgnoreCase(Map<String, String> headers, String name) {
        for (String k : headers.keySet()) {
            if (k != null && k.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private String getHeaderIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }
}
