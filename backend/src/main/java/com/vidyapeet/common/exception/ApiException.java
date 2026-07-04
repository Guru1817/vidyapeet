package com.vidyapeet.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for expected, client-facing errors that map to a specific HTTP
 * status and a consistent JSON body.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
