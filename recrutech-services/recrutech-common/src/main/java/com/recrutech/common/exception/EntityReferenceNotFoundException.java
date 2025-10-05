package com.recrutech.common.exception;

import java.io.Serial;

/**
 * Exception thrown when a referenced entity does not exist.
 * Used for foreign key validation before database operations to provide
 * clear, user-friendly error messages instead of SQL constraint violations.
 * 
 * <p>Common use cases:</p>
 * <ul>
 *   <li>Attempting to create an application with non-existent applicant ID</li>
 *   <li>Referencing a deleted or invalid job posting</li>
 *   <li>Linking to a user that doesn't exist in the system</li>
 * </ul>
 * 
 * <p>This exception should be thrown BEFORE attempting database operations
 * that would result in foreign key constraint violations, allowing for
 * better error handling and user experience.</p>
 */
public class EntityReferenceNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new entity reference not found exception with a formatted message.
     * Creates a message in the format: "{EntityType} with ID '{id}' does not exist"
     *
     * @param entityType the type of entity that was not found (e.g., "Applicant", "Job Posting", "Company")
     * @param entityId the ID of the entity that was not found
     */
    public EntityReferenceNotFoundException(String entityType, String entityId) {
        super(String.format("%s with ID '%s' does not exist", entityType, entityId));
    }

    /**
     * Constructs a new entity reference not found exception with a custom message.
     * Use this constructor when you need a more specific or context-aware error message.
     *
     * @param message the detail message explaining what entity reference was not found
     */
    public EntityReferenceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new entity reference not found exception with a message and cause.
     * Use this constructor when the exception is triggered by another exception.
     *
     * @param message the detail message
     * @param cause the cause of the exception (e.g., database exception)
     */
    public EntityReferenceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
