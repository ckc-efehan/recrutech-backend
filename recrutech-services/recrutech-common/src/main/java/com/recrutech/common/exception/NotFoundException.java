package com.recrutech.common.exception;

import java.io.Serial;

/**
 * Exception thrown when a requested resource cannot be found.
 * This exception is used to indicate that the requested entity or resource
 * does not exist in the system.
 */
public class NotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new not found exception with the specified detail message.
     *
     * @param message the detail message
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new not found exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
