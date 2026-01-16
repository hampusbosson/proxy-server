package org.example.api.json;

import org.example.api.ApiResponse;
import org.example.api.TransactionController;
import org.example.log.Transaction;

import java.util.List;

/**
 * Minimal JSON serializer for API responses.
 * - This is intentionally limited: it only supports the shapes returned today.
 * - Normally one would use Spring Boot for this API, but i wanted to build it without frameworks.
 */
public final class JsonWriter {

    private JsonWriter() {}

    /** Serializes ApiResponse wrapper. */
    public static String toJson(ApiResponse<?> resp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(resp.success).append(",");
        sb.append("\"status\":").append(resp.statusCode).append(",");
        sb.append("\"data\":").append(valueToJson(resp.data)).append(",");
        sb.append("\"error\":").append(resp.error == null ? "null" : jsonString(resp.error));
        sb.append("}");
        return sb.toString();
    }

    /** Convenience JSON for a typical error payload. */
    public static String jsonError(int status, String msg) {
        return "{\"success\":false,\"status\":" + status + ",\"data\":null,\"error\":" + jsonString(msg) + "}";
    }

    private static String valueToJson(Object v) {
        if (v == null) return "null";

        // StatsResponse record
        if (v instanceof TransactionController.StatsResponse s) {
            return "{"
                    + "\"total\":" + s.total() + ","
                    + "\"allowed\":" + s.allowed() + ","
                    + "\"blocked\":" + s.blocked() + ","
                    + "\"error\":" + s.error() + ","
                    + "\"bytesFromServerTotal\":" + s.bytesFromServerTotal() + ","
                    + "\"avgDurationMs\":" + s.avgDurationMs()
                    + "}";
        }

        // List<?> (used for List<Transaction>)
        if (v instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Object o : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(valueToJson(o));
            }
            sb.append("]");
            return sb.toString();
        }

        // Transaction
        if (v instanceof Transaction t) {
            return "{"
                    + "\"method\":" + jsonString(t.getMethod()) + ","
                    + "\"host\":" + jsonString(t.getHost()) + ","
                    + "\"port\":" + t.getPort() + ","
                    + "\"path\":" + jsonString(t.getPath()) + ","
                    + "\"verdict\":" + (t.getVerdict() == null ? "null" : jsonString(t.getVerdict().name())) + ","
                    + "\"bytesFromServer\":" + t.getBytesFromServer() + ","
                    + "\"durationMs\":" + t.getDurationMs() + ","
                    + "\"errorMessage\":" + (t.getErrorMessage() == null ? "null" : jsonString(t.getErrorMessage()))
                    + "}";
        }

        // Fallback: string
        return jsonString(String.valueOf(v));
    }

    /** Escapes a Java string into a JSON string literal. */
    private static String jsonString(String s) {
        if (s == null) return "null";
        String esc = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + esc + "\"";
    }
}