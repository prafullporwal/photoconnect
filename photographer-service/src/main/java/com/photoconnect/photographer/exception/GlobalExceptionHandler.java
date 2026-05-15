package com.photoconnect.photographer.exception;

import com.photoconnect.photographer.dto.ErrorResponse;
import com.photoconnect.photographer.security.CorrelationIdServletFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for HTTP error responses in photographer-service.
 * Same shape as auth-service's {@code GlobalExceptionHandler} so the
 * front-end has one error model to handle platform-wide.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PhotographerDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(PhotographerDomainException ex,
                                                       HttpServletRequest req) {
        return build(ex.getStatus(), ex.getStatus().getReasonPhrase(), ex.getMessage(), req);
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
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex,
                                                          HttpServletRequest req) {
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
