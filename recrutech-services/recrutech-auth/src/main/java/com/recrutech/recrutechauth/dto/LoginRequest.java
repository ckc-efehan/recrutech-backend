package com.recrutech.recrutechauth.dto;

/**
 * DTO for user login request.
 */
public record LoginRequest(String username, String password) {
}