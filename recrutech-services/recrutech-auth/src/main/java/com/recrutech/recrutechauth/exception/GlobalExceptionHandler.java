package com.recrutech.recrutechauth.exception;

import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global exception handler that maps domain and validation exceptions
 * to consistent HTTP status codes and a unified error response format.
 * Response schema: { code, message, timestamp, requestId }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    // 401 Unauthorized
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "AUTH_BAD_CREDENTIALS", ex.getMessage(), request);
    }

    // 403 Forbidden group
    @ExceptionHandler({EmailNotVerifiedException.class, PasswordExpiredException.class, SuspiciousActivityException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", ex.getMessage(), request);
    }

    // 423 Locked
    @ExceptionHandler({AccountDisabledException.class, AccountLockedException.class})
    public ResponseEntity<ErrorResponse> handleLocked(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.LOCKED, "AUTH_LOCKED", ex.getMessage(), request);
    }

    // 400 Validation (domain-level)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", safeMessage(ex), request);
    }

    // 400 Validation (bean validation: @Valid/@Validated)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + (err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid"))
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + (err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid"))
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    // 400 Bad Request
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed JSON request", request);
    }

    // 404 Not Found
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", safeMessage(ex), request);
    }

    // 500 Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        String requestId = extractRequestId(request);
        ErrorResponse body = ErrorResponse.of(code, message, requestId);
        return ResponseEntity.status(status).body(body);
    }

    private String extractRequestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }
        Object attr = request.getAttribute(REQUEST_ID_HEADER);
        return attr != null ? Objects.toString(attr, null) : null;
    }

    private String safeMessage(Throwable ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    public record ErrorResponse(String code, String message, Instant timestamp, String requestId) {
        public static ErrorResponse of(String code, String message, String requestId) {
            return new ErrorResponse(code, message, Instant.now(), requestId);
        }
    }
}
