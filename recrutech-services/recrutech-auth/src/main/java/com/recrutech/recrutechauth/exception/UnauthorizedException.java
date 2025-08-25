package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when user is not authorized to perform an action.
 * 
 * This exception is thrown when:
 * - User lacks sufficient permissions for the requested operation
 * - Role-based access control denies access
 * - Resource access is restricted
 * - Authorization token is missing or invalid
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}