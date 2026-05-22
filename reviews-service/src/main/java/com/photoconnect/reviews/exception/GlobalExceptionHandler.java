package com.photoconnect.reviews.exception;

import com.photoconnect.reviews.dto.ErrorResponse;
import com.photoconnect.reviews.security.CorrelationIdServletFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReviewDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(ReviewDomainException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getStatus().getReasonPhrase(), ex.getMessage(), req);
    }

    /**
     * Catch the DB UNIQUE-violation that races with the application-level
     * existence check, and translate it into the same 409 the service would
     * have thrown if the check had won the race.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex,
                                                          HttpServletRequest req) {
        log.warn("DataIntegrityViolation reached the handler — likely a duplicate-review race: {}",
                ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Conflict",
                "A review for this photographer already exists", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.withFields(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "Request validation failed",
                        req.getRequestURI(),
                        correlationId(),
                        fields));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Insufficient permissions", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error,
                                                  String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(
                ErrorResponse.of(status.value(), error, message,
                        req.getRequestURI(), correlationId()));
    }

    private static String correlationId() {
        String cid = MDC.get(CorrelationIdServletFilter.MDC_KEY);
        return cid != null ? cid : "n/a";
    }
}
