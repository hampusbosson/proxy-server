package org.example.api.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiHttp {
    private ApiHttp() {}

    /** Parses query into a map.
     * Example: the query: "a=12&b=hello" becomes: k = a - v = 12, k = b - v = hello
     * Returns empty map if query is empty/null
     */
    public static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> out = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return out;
        }

        for (String pair : rawQuery.split("&")) {
            int i = pair.indexOf('=');

            String key;
            String value;

            if (i >= 0) {
                key = pair.substring(0, i);
                value = pair.substring(i + 1);
            } else {
                key = pair;
                value = "";
            }

            key = urlDecode(key);
            value = urlDecode(value);

            if (!key.isEmpty()) {
                out.put(key, value);
            }
        }

        return out;
    }

    /** Returns Integer if parseable, otherwise null */
    public static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
           return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
