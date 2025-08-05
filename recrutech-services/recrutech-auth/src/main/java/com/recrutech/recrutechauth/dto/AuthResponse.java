package com.recrutech.recrutechauth.dto;

/**
 * DTO for authentication response.
 */
public record AuthResponse(String accessToken, String refreshToken, String tokenType, 
                          Long expiresIn, String username, String email, String[] roles) {
}