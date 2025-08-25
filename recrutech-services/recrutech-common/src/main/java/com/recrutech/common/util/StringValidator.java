package com.recrutech.common.util;

import com.recrutech.common.exception.ValidationException;

/**
 * Utility class for validating strings.
 * This class provides methods for validating string values, including checks for
 * emptiness and maximum length constraints for both required and optional fields.
 */
public final class StringValidator {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private StringValidator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Validates that a string is not null or empty.
     *
     * @param value the string to validate
     * @param fieldName the name of the field being validated (for error messages)
     * @throws ValidationException if the string is null or empty
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
    }

    /**
     * Validates that a string does not exceed a maximum length.
     *
     * @param value the string to validate
     * @param maxLength the maximum allowed length
     * @param fieldName the name of the field being validated (for error messages)
     * @throws ValidationException if the string exceeds the maximum length
     */
    public static void validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }

    /**
     * Validates that a string is not null or empty and does not exceed a maximum length.
     *
     * @param value the string to validate
     * @param maxLength the maximum allowed length
     * @param fieldName the name of the field being validated (for error messages)
     * @throws ValidationException if the string is null, empty, or exceeds the maximum length
     */
    public static void validateRequired(String value, int maxLength, String fieldName) {
        requireNonEmpty(value, fieldName);
        validateMaxLength(value, maxLength, fieldName);
    }

    /**
     * Validates that a string does not exceed a maximum length if it is not null.
     *
     * @param value the string to validate
     * @param maxLength the maximum allowed length
     * @param fieldName the name of the field being validated (for error messages)
     * @throws ValidationException if the string exceeds the maximum length
     */
    public static void validateOptional(String value, int maxLength, String fieldName) {
        validateMaxLength(value, maxLength, fieldName);
    }
}
