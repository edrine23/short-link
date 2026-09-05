package com.idipoedrine.shortlink.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for exceptions that should be translated into a specific HTTP
 * status by GlobalExceptionHandler. New domain exceptions in any module
 * should extend this rather than adding a fresh @ExceptionHandler.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}