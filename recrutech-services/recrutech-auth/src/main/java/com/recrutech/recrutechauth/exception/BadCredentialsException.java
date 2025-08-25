package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when user credentials are invalid.
 * 
 * This exception is thrown during login attempts when:
 * - Email address is not found
 * - Password does not match
 * - Credentials are malformed
 */
public class BadCredentialsException extends RuntimeException {
    
    public BadCredentialsException(String message) {
        super(message);
    }
    
    public BadCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}