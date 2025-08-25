package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when user password has expired.
 * 
 * This exception is thrown when:
 * - Password has exceeded the maximum age policy
 * - Password change is required before login
 * - Password expiry policy is enforced
 */
public class PasswordExpiredException extends RuntimeException {
    
    public PasswordExpiredException(String message) {
        super(message);
    }
    
    public PasswordExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}