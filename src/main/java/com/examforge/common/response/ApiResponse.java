package com.examforge.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard envelope for every API response in the system.
 *
 * Success:
 * {
 *   "success": true,
 *   "message": "Request successful",
 *   "data": {...},
 *   "timestamp": "..."
 * }
 *
 * Error:
 * {
 *   "success": false,
 *   "message": "Validation failed",
 *   "errors": [...],
 *   "timestamp": "..."
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final List<ApiError> errors;
    private final Instant timestamp;

    private ApiResponse(boolean success, String message, T data, List<ApiError> errors) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Request successful", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String message, List<ApiError> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public List<ApiError> getErrors() {
        return errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
