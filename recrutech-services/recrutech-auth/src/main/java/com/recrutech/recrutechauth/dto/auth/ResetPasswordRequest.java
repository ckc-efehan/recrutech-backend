package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Immutable reset password request record with comprehensive validation.
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
public record ResetPasswordRequest(
    
    @NotBlank(message = "Reset token is required and cannot be empty")
    @Size(
        min = 10, 
        max = 255, 
        message = "Reset token must be between 10 and 255 characters"
    )
    @JsonProperty("token")
    String token,
    
    @NotBlank(message = "New password is required and cannot be empty")
    @Size(
        min = 8, 
        max = 128, 
        message = "Password must be between 8 and 128 characters"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character (!@#$%^&*)"
    )
    @JsonProperty("newPassword")
    String newPassword,
    
    @NotBlank(message = "Password confirmation is required and cannot be empty")
    @JsonProperty("confirmPassword")
    String confirmPassword
) {
    
    /**
     * Validates the reset password request with business rules
     */
    public void validateBusinessRules() {
        if (newPassword != null && confirmPassword != null && !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation password do not match");
        }
        
        if (newPassword != null && newPassword.toLowerCase().contains("password")) {
            throw new IllegalArgumentException("Password cannot contain the word 'password'");
        }
        
        if (token != null && token.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset token cannot be empty or whitespace only");
        }
    }
}