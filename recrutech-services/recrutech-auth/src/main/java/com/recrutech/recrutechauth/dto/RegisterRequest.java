package com.recrutech.recrutechauth.dto;

/**
 * DTO for user registration request.
 */
public record RegisterRequest(String username, String email, String password, 
                             String firstName, String lastName) {
}