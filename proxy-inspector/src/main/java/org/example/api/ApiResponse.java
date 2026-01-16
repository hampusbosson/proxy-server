package org.example.api;

/**
 * Genereic API response wrapper used by the API server
 *
 * This class standardizes all API responses so that the clients (UI, curl, scripts)
 * can rely on a consistent structure regardless of endpoint or outcome
 */

public class ApiResponse<T> {
    public final boolean success;
    public final int statusCode;
    public final T data;  // payload returned on success
    public final String error;

    /**
     * Private constructor to enforce usage of factory methods.
     * This guarantees that no "broken" response-objects can be created
     */
    private ApiResponse(boolean success, int statusCode, T data, String error) {
        this.success = success;
        this.statusCode = statusCode;
        this.data = data;
        this.error = error;
    }

    /**
     * Factory method for creating a successful API-response (HTTP 200)
     *
     * @param data - data payload to be returned to the client
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, data, null);
    }

    /**
     * Factory method for creating error responses.
     * @param status - HTTP statusCode
     * @param message - descriptive error message
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(false, status, null, message);
    }

}
