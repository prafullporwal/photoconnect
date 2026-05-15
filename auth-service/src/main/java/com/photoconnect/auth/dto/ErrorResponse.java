package com.photoconnect.auth.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error envelope returned by {@code GlobalExceptionHandler}.
 *
 * <p>Same shape will be reused across every PhotoConnect service so the
 * frontend has one error model to handle.</p>
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message,
                                   String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, error, message,
                path, correlationId, null);
    }

    public static ErrorResponse withFields(int status, String error, String message,
                                           String path, String correlationId,
                                           Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message,
                path, correlationId, fieldErrors);
    }
}
