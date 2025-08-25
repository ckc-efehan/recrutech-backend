package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when user registration fails.
 * 
 * This exception is thrown when:
 * - Registration data validation fails
 * - Email address is already in use
 * - Business rules are violated during registration
 * - External service dependencies fail during registration
 */
public class RegistrationException extends RuntimeException {
    
    public RegistrationException(String message) {
        super(message);
    }
    
    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}