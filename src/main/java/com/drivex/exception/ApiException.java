package com.drivex.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════════
// ApiException — throw from any service layer
// ═══════════════════════════════════════════════════════════════════════════════
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String     errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }

    // ── Factory shortcuts ─────────────────────────────────────────────────────

    public static ApiException notFound(String resource, String id) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND",
            resource + " not found with id: " + id);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GlobalExceptionHandler — converts exceptions → consistent JSON responses
// ═══════════════════════════════════════════════════════════════════════════════
@RestControllerAdvice
class GlobalExceptionHandler {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ErrorResponse(
        String              timestamp,
        int                 status,
        String              error,
        String              code,
        String              message,
        String              path,
        Map<String, String> validationErrors
    ) {}

    private ResponseEntity<ErrorResponse> build(
        HttpStatus status, String code, String message,
        String path, Map<String, String> validationErrors
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
            LocalDateTime.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            path,
            validationErrors
        ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, WebRequest req) {
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(),
            req.getDescription(false).replace("uri=", ""), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex, WebRequest req
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field   = err instanceof FieldError fe ? fe.getField() : err.getObjectName();
            String message = err.getDefaultMessage();
            errors.put(field, message);
        });
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
            "Request validation failed", req.getDescription(false).replace("uri=",""), errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
        BadCredentialsException ex, WebRequest req
    ) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
            "Invalid email or password", req.getDescription(false).replace("uri=",""), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex, WebRequest req
    ) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN",
            "You do not have permission to perform this action",
            req.getDescription(false).replace("uri=",""), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "An unexpected error occurred: " + ex.getMessage(),
            req.getDescription(false).replace("uri=",""), null);
    }
}
