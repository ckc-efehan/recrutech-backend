package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Enterprise-grade logout request record.
 * Features:
 * - Immutable design for thread safety
 * - User ID validation
 * - Support for logging out from all devices
 * - Optional logout reason tracking
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogoutRequest(
    
    @NotBlank(message = "User ID is required for logout")
    @Size(max = 36, message = "User ID cannot exceed 36 characters")
    @JsonProperty("userId")
    String userId,
    
    @JsonProperty("logoutFromAllDevices")
    Boolean logoutFromAllDevices,
    
    @JsonProperty("reason")
    @Size(max = 100, message = "Logout reason cannot exceed 100 characters")
    String reason
) {
    
    /**
     * Default constructor with default values
     */
    public LogoutRequest {
        if (logoutFromAllDevices == null) {
            logoutFromAllDevices = false;
        }
    }
}