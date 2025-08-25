package com.recrutech.common.exception;

import java.io.Serial;

/**
 * Exception thrown when validation of input data fails.
 * This exception is used to indicate that the provided data does not meet
 * the required validation criteria.
 */
public class ValidationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new validation exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new validation exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
