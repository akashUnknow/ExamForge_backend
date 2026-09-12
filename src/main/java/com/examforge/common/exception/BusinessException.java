package com.examforge.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all deliberate, application-level exceptions.
 * Carries an HTTP status so the global handler can translate it correctly.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
