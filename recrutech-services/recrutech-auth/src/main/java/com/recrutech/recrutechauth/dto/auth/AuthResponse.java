package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.recrutech.recrutechauth.model.UserRole;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Enterprise-grade authentication response record.
 * Features:
 * - Immutable design for thread safety
 * - Comprehensive token information
 * - User context and role information
 * - Two-factor authentication support
 * - Factory methods for common responses
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    
    @JsonProperty("accessToken")
    String accessToken,
    
    @JsonProperty("refreshToken")
    String refreshToken,
    
    @JsonProperty("expiresIn")
    Long expiresIn,
    
    @JsonProperty("userRole")
    UserRole userRole,
    
    @JsonProperty("userId")
    String userId,
    
    @JsonProperty("companyId")
    String companyId,
    
    @JsonProperty("userContext")
    Object userContext,
    
    @JsonProperty("requiresTwoFactor")
    Boolean requiresTwoFactor,
    
    @JsonProperty("tempToken")
    String tempToken,
    
    @JsonProperty("message")
    String message
) {
    
    /**
     * Default constructor with default values
     */
    public AuthResponse {
        if (requiresTwoFactor == null) {
            requiresTwoFactor = false;
        }
    }
    
    /**
     * Creates a successful authentication response
     */
    public static AuthResponse createSuccessResponse(
            String accessToken, String refreshToken, long expiresIn, 
            UserRole userRole, String userId, String companyId, Object userContext) {
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(expiresIn)
            .userRole(userRole)
            .userId(userId)
            .companyId(companyId)
            .userContext(userContext)
            .requiresTwoFactor(false)
            .build();
    }
    
    /**
     * Creates an error response for authentication failures
     */
    public static AuthResponse createErrorResponse(String message) {
        return AuthResponse.builder()
            .message(message)
            .build();
    }
    
    /**
     * Creates a two-factor authentication required response
     */
    public static AuthResponse createTwoFactorRequiredResponse(String tempToken) {
        return AuthResponse.builder()
            .tempToken(tempToken)
            .requiresTwoFactor(true)
            .message("Two-factor authentication required")
            .build();
    }
}