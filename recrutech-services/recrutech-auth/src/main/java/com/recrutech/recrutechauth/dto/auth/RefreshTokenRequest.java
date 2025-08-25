package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Enterprise-grade refresh token request record.
 * Features:
 * - Immutable design for thread safety
 * - Comprehensive validation
 * - Token format validation
 * - Device information support
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefreshTokenRequest(
    
    @NotBlank(message = "Refresh token is required and cannot be empty")
    @Size(max = 2048, message = "Refresh token cannot exceed 2048 characters")
    @JsonProperty("refreshToken")
    String refreshToken,
    
    @JsonProperty("deviceInfo")
    LoginRequest.DeviceInfo deviceInfo
) {
    
    /**
     * Validates the refresh token format
     */
    public void validateTokenFormat() {
        if (refreshToken != null && !refreshToken.matches("^[A-Za-z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }
    }
}