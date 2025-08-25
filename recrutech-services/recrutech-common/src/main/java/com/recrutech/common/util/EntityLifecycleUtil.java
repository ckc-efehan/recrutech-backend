package com.recrutech.common.util;

import java.time.LocalDateTime;

/**
 * Utility class for entity lifecycle operations.
 * This class provides methods for common entity operations like
 * ID generation and timestamp creation that can be used across different entities.
 */
public final class EntityLifecycleUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private EntityLifecycleUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Generates a new UUID.
     *
     * @return a new UUID as a string
     */
    public static String generateId() {
        return UuidGenerator.generateUuid();
    }

    /**
     * Creates a current timestamp.
     *
     * @return the current timestamp
     */
    public static LocalDateTime createTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Ensures an entity has an ID by generating one if it's null.
     *
     * @param currentId the current ID of the entity
     * @return the current ID if not null, or a new generated ID
     */
    public static String ensureId(String currentId) {
        return currentId != null ? currentId : generateId();
    }

    /**
     * Ensures an entity has a creation timestamp by generating one if it's null.
     *
     * @param currentTimestamp the current timestamp of the entity
     * @return the current timestamp if not null, or a new timestamp
     */
    public static LocalDateTime ensureTimestamp(LocalDateTime currentTimestamp) {
        return currentTimestamp != null ? currentTimestamp : createTimestamp();
    }
}