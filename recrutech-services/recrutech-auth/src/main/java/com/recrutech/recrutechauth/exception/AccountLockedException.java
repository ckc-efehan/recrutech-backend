package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when user account is temporarily locked.
 * This exception is thrown when:
 * - Account is locked due to multiple failed login attempts
 * - Account is temporarily suspended for security reasons
 * - Account lockout period is still active
 */
public class AccountLockedException extends RuntimeException {
    
    public AccountLockedException(String message) {
        super(message);
    }
    
    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}