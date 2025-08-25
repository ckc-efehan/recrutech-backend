package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when token is invalid or expired.
 * 
 * This exception is thrown when:
 * - JWT token has expired
 * - Token signature is invalid
 * - Token format is malformed
 * - Token has been blacklisted or revoked
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}