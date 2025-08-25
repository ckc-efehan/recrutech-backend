package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when suspicious activity is detected.
 * 
 * This exception is thrown when:
 * - Multiple login attempts from different IP addresses
 * - Unusual login patterns are detected
 * - Security monitoring flags potential threats
 * - Device fingerprinting detects anomalies
 */
public class SuspiciousActivityException extends RuntimeException {
    
    public SuspiciousActivityException(String message) {
        super(message);
    }
    
    public SuspiciousActivityException(String message, Throwable cause) {
        super(message, cause);
    }
}