package com.examforge.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single field-level or general error inside an error response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String field;
    private final String message;

    public ApiError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public static ApiError of(String message) {
        return new ApiError(null, message);
    }

    public static ApiError of(String field, String message) {
        return new ApiError(field, message);
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}
