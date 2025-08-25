package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when user account is disabled.
 * This exception is thrown when:
 * - User account has been administratively disabled
 * - Account is suspended due to policy violations
 * - Account is temporarily deactivated
 */
public class AccountDisabledException extends RuntimeException {
    
    public AccountDisabledException(String message) {
        super(message);
    }
    
    public AccountDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}