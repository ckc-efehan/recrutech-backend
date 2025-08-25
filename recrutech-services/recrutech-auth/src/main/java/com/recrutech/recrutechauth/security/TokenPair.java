package com.recrutech.recrutechauth.security;

/**
 * Token pair record for Access and Refresh tokens.
 * Contains the access token, refresh token, and expiration time.
 */
public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
}