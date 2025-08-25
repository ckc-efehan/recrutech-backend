package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when authentication service encounters an error.
 * 
 * This exception is thrown when:
 * - Internal service errors occur during authentication operations
 * - Database connectivity issues affect authentication
 * - External service dependencies fail
 * - Unexpected system errors during authentication processing
 */
public class AuthServiceException extends RuntimeException {
    
    public AuthServiceException(String message) {
        super(message);
    }
    
    public AuthServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}