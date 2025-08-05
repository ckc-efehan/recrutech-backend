package com.recrutech.recrutechauth.dto;

/**
 * DTO for HR user registration request.
 * This DTO is specifically for HR registration and does not include a role field
 * as the HR role is automatically assigned.
 */
public record HRRegisterRequest(String username, String email, String password, 
                               String firstName, String lastName) {
}