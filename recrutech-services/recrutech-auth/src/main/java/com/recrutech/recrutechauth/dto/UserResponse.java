package com.recrutech.recrutechauth.dto;

/**
 * DTO for user information responses.
 * This DTO matches the structure expected by other services.
 */
public record UserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        boolean active
) {
}