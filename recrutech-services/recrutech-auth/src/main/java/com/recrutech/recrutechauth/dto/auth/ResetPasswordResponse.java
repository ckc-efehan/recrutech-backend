package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Enterprise-grade password reset response record.
 * Features:
 * - Immutable design for thread safety
 * - Comprehensive response information
 * - Factory methods for common responses
 * - Security-focused messaging
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResetPasswordResponse(
    
    @JsonProperty("success")
    Boolean success,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("requestId")
    String requestId,
    
    @JsonProperty("timestamp")
    LocalDateTime timestamp,
    
    @JsonProperty("expiresAt")
    LocalDateTime expiresAt
) {
    
    /**
     * Default constructor with default values
     */
    public ResetPasswordResponse {
        if (success == null) {
            success = false;
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    /**
     * Creates a successful forgot password response
     */
    public static ResetPasswordResponse createForgotPasswordSuccessResponse() {
        return ResetPasswordResponse.builder()
            .success(true)
            .message("Falls eine E-Mail-Adresse in unserem System registriert ist, wurde eine Anleitung zum Zurücksetzen des Passworts gesendet.")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates a successful password reset response
     */
    public static ResetPasswordResponse createPasswordResetSuccessResponse() {
        return ResetPasswordResponse.builder()
            .success(true)
            .message("Ihr Passwort wurde erfolgreich zurückgesetzt. Sie können sich jetzt mit Ihrem neuen Passwort anmelden.")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates an error response for password reset failures
     */
    public static ResetPasswordResponse createErrorResponse(String message) {
        return ResetPasswordResponse.builder()
            .success(false)
            .message(message != null ? message : "Ein Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates an error response for invalid or expired token
     */
    public static ResetPasswordResponse createInvalidTokenResponse() {
        return ResetPasswordResponse.builder()
            .success(false)
            .message("Der Zurücksetzungslink ist ungültig oder abgelaufen. Bitte fordern Sie einen neuen Link an.")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Creates a response for rate limit exceeded
     */
    public static ResetPasswordResponse createRateLimitResponse() {
        return ResetPasswordResponse.builder()
            .success(false)
            .message("Zu viele Anfragen. Bitte warten Sie einige Minuten, bevor Sie es erneut versuchen.")
            .timestamp(LocalDateTime.now())
            .build();
    }
}