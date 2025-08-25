package com.recrutech.recrutechauth.validator;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for password strength.
 * Ensures that passwords meet security requirements.
 */
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern COMMON_PATTERNS = Pattern.compile(
            "(?i)(password|123456|qwerty|admin|welcome|letmein|abc123|monkey|1234567890|000000|iloveyou|1234|superman|princess|rockyou|ashley|bailey|shadow|123123|654321|football|baseball|welcome1|!@#\\$%\\^&\\*|donald|password1|qwerty123)"
    );

    /**
     * Validates a password against security requirements.
     *
     * @param password the password to validate
     * @return a list of validation errors, or an empty list if the password is valid
     */
    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("Password cannot be longer than " + MAX_LENGTH + " characters");
        }

        if (!HAS_UPPERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!HAS_LOWERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!HAS_DIGIT.matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }

        if (!HAS_SPECIAL_CHAR.matcher(password).find()) {
            errors.add("Password must contain at least one special character");
        }

        if (COMMON_PATTERNS.matcher(password).find()) {
            errors.add("Password contains a common pattern and is too easy to guess");
        }

        // Check for sequential characters
        if (hasSequentialChars(password, 3)) {
            errors.add("Password contains sequential characters (e.g., 'abc', '123')");
        }

        // Check for repeated characters
        if (hasRepeatedChars(password, 3)) {
            errors.add("Password contains repeated characters (e.g., 'aaa', '111')");
        }

        return errors;
    }

    /**
     * Checks if a password is valid.
     *
     * @param password the password to check
     * @return true if the password is valid, false otherwise
     */
    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }

    /**
     * Checks if a password contains sequential characters.
     *
     * @param password the password to check
     * @param length the minimum length of the sequence
     * @return true if the password contains sequential characters, false otherwise
     */
    private boolean hasSequentialChars(String password, int length) {
        if (password.length() < length) {
            return false;
        }

        for (int i = 0; i <= password.length() - length; i++) {
            boolean isSequential = true;
            for (int j = 1; j < length; j++) {
                if (password.charAt(i + j) != password.charAt(i + j - 1) + 1) {
                    isSequential = false;
                    break;
                }
            }
            if (isSequential) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a password contains repeated characters.
     *
     * @param password the password to check
     * @param length the minimum length of the repetition
     * @return true if the password contains repeated characters, false otherwise
     */
    private boolean hasRepeatedChars(String password, int length) {
        if (password.length() < length) {
            return false;
        }

        for (int i = 0; i <= password.length() - length; i++) {
            boolean isRepeated = true;
            char c = password.charAt(i);
            for (int j = 1; j < length; j++) {
                if (password.charAt(i + j) != c) {
                    isRepeated = false;
                    break;
                }
            }
            if (isRepeated) {
                return true;
            }
        }

        return false;
    }
}