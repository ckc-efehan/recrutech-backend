package com.recrutech.common.exception;

import java.io.Serial;

/**
 * Exception thrown when file storage operations fail.
 * This exception is used to indicate issues with file upload, download,
 * or storage operations.
 */
public class FileStorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new file storage exception with the specified detail message.
     *
     * @param message the detail message
     */
    public FileStorageException(String message) {
        super(message);
    }

    /**
     * Constructs a new file storage exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
