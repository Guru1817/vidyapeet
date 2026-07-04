package com.vidyapeet.common.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Consistent JSON error envelope returned for every handled error.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        List<String> details
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null, null);
    }
}
