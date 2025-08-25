package com.recrutech.common.util;

import java.util.UUID;

/**
 * Utility class for generating and validating UUIDs.
 * This class provides methods for generating random UUIDs and validating
 * whether a string represents a valid UUID.
 */
public final class UuidGenerator {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private UuidGenerator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Generates a new random UUID as a string.
     *
     * @return a new random UUID as a string
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validates if a string is a valid UUID.
     * A valid UUID must be 36 characters long (32 hexadecimal digits plus 4 hyphens)
     * and must be parseable by UUID.fromString.
     *
     * @param uuid the string to validate
     * @return true if the string is a valid UUID, false otherwise
     */
    public static boolean isValidUuid(String uuid) {
        if (uuid == null) {
            return false;
        }

        // Check if the UUID has the correct length (36 characters)
        if (uuid.length() != 36) {
            return false;
        }

        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
