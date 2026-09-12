package com.examforge.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a delete is blocked because other active records still
 * reference the resource (e.g. deleting an exam category that still has
 * exams assigned to it).
 */
public class ResourceInUseException extends BusinessException {

    public ResourceInUseException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
