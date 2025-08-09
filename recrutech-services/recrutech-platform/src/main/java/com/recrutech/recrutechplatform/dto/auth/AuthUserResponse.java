package com.recrutech.recrutechplatform.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for user information received from auth service.
 * This DTO represents the user data structure returned by the auth service API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        boolean active
) {
    /**
     * Creates an unknown user response for fallback scenarios.
     *
     * @param userId the user ID
     * @return AuthUserResponse with unknown values
     */
    public static AuthUserResponse unknown(String userId) {
        return new AuthUserResponse(userId, "Unknown", "User", null, false);
    }
}