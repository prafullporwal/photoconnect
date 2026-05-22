package com.photoconnect.reviews.dto;

import java.time.Instant;
import java.util.Map;

/** Same error shape used by auth-service, photographer-service and customer-service. */
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
