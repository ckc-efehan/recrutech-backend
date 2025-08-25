package com.recrutech.common.util;

import com.recrutech.common.exception.ValidationException;

/**
 * Utility class for validating entities.
 * This class provides methods for validating entity objects and their IDs.
 * It includes validation for null checks and optional UUID validation.
 */
public final class EntityValidator {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EntityValidator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Validates that an object is not null.
     *
     * @param entity the object to validate
     * @param entityName the name of the entity being validated (for error messages)
     * @throws ValidationException if the object is null
     */
    public static void requireNonNull(Object entity, String entityName) {
        if (entity == null) {
            throw new ValidationException(entityName + " cannot be null");
        }
    }

    /**
     * Validates an entity ID.
     *
     * @param id the ID to validate
     * @param entityName the name of the entity being validated (for error messages)
     * @param validateUuid whether to validate that the ID is a valid UUID
     * @throws ValidationException if the ID is invalid
     */
    public static void validateId(String id, String entityName, boolean validateUuid) {
        StringValidator.requireNonEmpty(id, entityName + " ID");

        // Optionally validate that the ID is a valid UUID
        if (validateUuid) {
            UuidValidator.validateUuid(id, entityName + " ID");
        }
    }

    /**
     * Validates an entity ID without UUID validation.
     *
     * @param id the ID to validate
     * @param entityName the name of the entity being validated (for error messages)
     * @throws ValidationException if the ID is invalid
     */
    public static void validateId(String id, String entityName) {
        validateId(id, entityName, false);
    }
}
