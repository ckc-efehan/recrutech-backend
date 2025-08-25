package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when user email is not verified.
 * 
 * This exception is thrown when:
 * - User attempts to login with an unverified email address
 * - Email verification is required but not completed
 * - Email verification token has expired
 */
public class EmailNotVerifiedException extends RuntimeException {
    
    public EmailNotVerifiedException(String message) {
        super(message);
    }
    
    public EmailNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}