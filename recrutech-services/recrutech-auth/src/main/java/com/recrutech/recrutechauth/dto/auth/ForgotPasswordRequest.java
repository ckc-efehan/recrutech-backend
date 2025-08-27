package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Immutable forgot password request record with comprehensive validation.
 * Features:
 * - Immutable design prevents accidental modification
 * - Builder pattern for flexible construction
 * - Comprehensive validation with custom messages
 * - JSON serialization optimizations
 * - Security-focused field handling
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordRequest(
    
    @NotBlank(message = "Email address is required and cannot be empty")
    @Email(
        message = "Please provide a valid email address format",
        regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    @Size(
        max = 254, 
        message = "Email address cannot exceed 254 characters"
    )
    @JsonProperty("email")
    String email
) {
    
    /**
     * Validates the forgot password request with business rules
     */
    public void validateBusinessRules() {
        if (email != null && email.toLowerCase().contains("test") && 
            email.toLowerCase().contains("noreply")) {
            throw new IllegalArgumentException("System email addresses cannot request password reset");
        }
    }
}