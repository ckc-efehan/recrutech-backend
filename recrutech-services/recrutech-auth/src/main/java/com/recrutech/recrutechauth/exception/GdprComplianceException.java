package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when GDPR compliance operations encounter errors.
 * Used for:
 * - Data deletion failures
 * - Data export failures
 * - Data rectification failures
 * - Audit logging failures
 * - GDPR compliance validation errors
 */
public class GdprComplianceException extends RuntimeException {

    /**
     * Constructs a new GDPR compliance exception with the specified detail message.
     * @param message the detail message explaining the GDPR compliance error
     */
    public GdprComplianceException(String message) {
        super(message);
    }

    /**
     * Constructs a new GDPR compliance exception with the specified detail message and cause.
     * @param message the detail message explaining the GDPR compliance error
     * @param cause the cause of the exception (which is saved for later retrieval)
     */
    public GdprComplianceException(String message, Throwable cause) {
        super(message, cause);
    }
}