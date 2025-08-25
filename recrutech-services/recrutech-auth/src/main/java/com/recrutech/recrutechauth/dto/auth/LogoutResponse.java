package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Enterprise-grade logout response record.
 * Features:
 * - Immutable design for thread safety
 * - Success tracking
 * - Session termination information
 * - Redirect URL support
 * - Factory methods for common responses
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogoutResponse(
    
    @JsonProperty("message")
    @NotNull
    String message,
    
    @JsonProperty("success")
    Boolean success,
    
    @JsonProperty("logoutTimestamp")
    LocalDateTime logoutTimestamp,
    
    @JsonProperty("sessionsTerminated")
    Integer sessionsTerminated,
    
    @JsonProperty("redirectUrl")
    String redirectUrl
) {
    
    /**
     * Default constructor with default values
     */
    public LogoutResponse {
        if (success == null) {
            success = true;
        }
    }
    
    /**
     * Creates a successful logout response
     */
    public static LogoutResponse createSuccessResponse(boolean allDevices) {
        return LogoutResponse.builder()
            .message(allDevices ? 
                "Successfully logged out from all devices" : 
                "Successfully logged out")
            .success(true)
            .logoutTimestamp(LocalDateTime.now())
            .sessionsTerminated(allDevices ? null : 1)
            .build();
    }
    
    /**
     * Creates a failed logout response
     */
    public static LogoutResponse createFailureResponse(String errorMessage) {
        return LogoutResponse.builder()
            .message("Logout failed: " + errorMessage)
            .success(false)
            .logoutTimestamp(LocalDateTime.now())
            .build();
    }
}