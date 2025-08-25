package com.recrutech.common.util;

import com.recrutech.common.exception.ValidationException;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for validating UUIDs.
 * This class provides methods for validating UUIDs with different levels of strictness
 * and for different UUID versions.
 */
public final class UuidValidator {

    /**
     * Regular expression pattern for validating UUID format.
     * Format: 8-4-4-4-12 hexadecimal digits separated by hyphens.
     */
    private static final Pattern UUID_PATTERN = 
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * Regular expression pattern for validating UUID version 4 format.
     * Version 4 UUIDs have the form xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     * where x is any hexadecimal digit and y is one of 8, 9, A, or B.
     */
    private static final Pattern UUID_V4_PATTERN = 
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private UuidValidator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Validates if a string is a valid UUID.
     *
     * @param uuid the string to validate
     * @return true if the string is a valid UUID, false otherwise
     */
    public static boolean isValidUuid(String uuid) {
        return UuidGenerator.isValidUuid(uuid);
    }

    /**
     * Validates if a string is a valid UUID using regex pattern matching.
     * This method is more strict than isValidUuid() as it checks the exact format.
     *
     * @param uuid the string to validate
     * @return true if the string matches the UUID pattern, false otherwise
     */
    public static boolean isValidUuidFormat(String uuid) {
        if (uuid == null) {
            return false;
        }

        return UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Validates if a string is a valid UUID version 4.
     * Version 4 UUIDs are randomly generated and have specific bits set to indicate the version.
     *
     * @param uuid the string to validate
     * @return true if the string is a valid UUID version 4, false otherwise
     */
    public static boolean isValidUuidV4(String uuid) {
        if (uuid == null) {
            return false;
        }

        return UUID_V4_PATTERN.matcher(uuid).matches();
    }

    /**
     * Validates if a string is a valid UUID and throws an exception if it's not.
     *
     * @param uuid the string to validate
     * @param fieldName the name of the field being validated (for error messages)
     * @throws ValidationException if the string is not a valid UUID
     */
    public static void validateUuid(String uuid, String fieldName) {
        if (uuid == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }

        if (!isValidUuid(uuid)) {
            throw new ValidationException(fieldName + " must be a valid UUID");
        }
    }

    /**
     * Validates if a string is a valid UUID version 4 and throws an exception if it's not.
     *
     * @param uuid the string to validate
     * @param fieldName the name of the field being validated (for error messages)
     * @throws ValidationException if the string is not a valid UUID version 4
     */
    public static void validateUuidV4(String uuid, String fieldName) {
        if (uuid == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }

        if (!isValidUuidV4(uuid)) {
            throw new ValidationException(fieldName + " must be a valid UUID version 4");
        }
    }

    /**
     * Gets the version of a UUID.
     *
     * @param uuid the UUID to check
     * @return the version number (1-5) or -1 if the UUID is invalid
     */
    public static int getUuidVersion(String uuid) {
        if (!isValidUuid(uuid)) {
            return -1;
        }

        try {
            UUID parsedUuid = UUID.fromString(uuid);
            return parsedUuid.version();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }
}
