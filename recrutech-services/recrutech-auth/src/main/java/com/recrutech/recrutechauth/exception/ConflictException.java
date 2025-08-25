package com.recrutech.recrutechauth.exception;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate email, username already exists).
 * This exception typically results in HTTP 409 Conflict status.
 */
public class ConflictException extends RuntimeException {

    /**
     * Constructs a new ConflictException with the specified detail message.
     *
     * @param message the detail message explaining the conflict
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConflictException with the specified detail message and cause.
     *
     * @param message the detail message explaining the conflict
     * @param cause the cause of the conflict
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}