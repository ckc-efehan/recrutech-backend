package com.recrutech.recrutechauth.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic API error response record for consistent error handling.
 * Features:
 * - Immutable design for thread safety
 * - Comprehensive error information
 * - Validation error support
 * - Correlation ID for tracking
 * - Factory methods for common error types
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    
    @JsonProperty("error")
    @NotNull
    String error,
    
    @JsonProperty("message")
    @NotNull
    String message,
    
    @JsonProperty("timestamp")
    LocalDateTime timestamp,
    
    @JsonProperty("path")
    String path,
    
    @JsonProperty("status")
    Integer status,
    
    @JsonProperty("details")
    List<String> details,
    
    @JsonProperty("correlationId")
    String correlationId
) {
    
    /**
     * Default constructor with default values
     */
    public ErrorResponse {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    /**
     * Creates a validation error response
     */
    public static ErrorResponse createValidationError(List<String> validationErrors) {
        return ErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message("Request validation failed")
            .details(validationErrors)
            .status(400)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates a generic error response
     */
    public static ErrorResponse createError(String error, String message, int status) {
        return ErrorResponse.builder()
            .error(error)
            .message(message)
            .status(status)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates an authentication error response
     */
    public static ErrorResponse createAuthenticationError(String message) {
        return ErrorResponse.builder()
            .error("AUTHENTICATION_ERROR")
            .message(message)
            .status(401)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates an authorization error response
     */
    public static ErrorResponse createAuthorizationError(String message) {
        return ErrorResponse.builder()
            .error("AUTHORIZATION_ERROR")
            .message(message)
            .status(403)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates a not found error response
     */
    public static ErrorResponse createNotFoundError(String resource) {
        return ErrorResponse.builder()
            .error("NOT_FOUND")
            .message(resource + " not found")
            .status(404)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates a conflict error response
     */
    public static ErrorResponse createConflictError(String message) {
        return ErrorResponse.builder()
            .error("CONFLICT")
            .message(message)
            .status(409)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates a rate limit error response
     */
    public static ErrorResponse createRateLimitError(String message) {
        return ErrorResponse.builder()
            .error("RATE_LIMIT_EXCEEDED")
            .message(message)
            .status(429)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates an internal server error response
     */
    public static ErrorResponse createInternalServerError(String message) {
        return ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message(message)
            .status(500)
            .timestamp(LocalDateTime.now())
            .build();
    }
}