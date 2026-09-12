package com.examforge.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown for any JWT that fails signature verification, has expired,
 * has the wrong token type for the operation, or has been revoked.
 */
public class InvalidTokenException extends BusinessException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
