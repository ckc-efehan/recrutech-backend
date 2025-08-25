package com.recrutech.common.util;

import com.recrutech.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for StringValidator class.
 * Tests:
 * - String validation methods (requireNonEmpty, validateMaxLength, validateRequired, validateOptional)
 * - Exception throwing with proper field names
 * - Private constructor throws exception
 * - Edge cases with null, empty, and long strings
 * - Utility class structure validation
 */
@DisplayName("StringValidator Tests")
class StringValidatorTest {

    @Test
    @DisplayName("requireNonEmpty should not throw exception for valid string")
    void testRequireNonEmptyWithValidString() {
        // Given: Valid non-empty strings
        String validString = "test";
        String stringWithSpaces = "  test  ";
        String longString = "This is a long string with multiple words";

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(validString, "testField"));
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(stringWithSpaces, "testField"));
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(longString, "testField"));
    }

    @Test
    @DisplayName("requireNonEmpty should throw ValidationException for null string")
    void testRequireNonEmptyWithNullString() {
        // Given: Null string
        String nullString = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.requireNonEmpty(nullString, "testField"));
        
        assertEquals("testField cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("requireNonEmpty should throw ValidationException for empty string")
    void testRequireNonEmptyWithEmptyString() {
        // Given: Empty string
        String emptyString = "";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.requireNonEmpty(emptyString, "testField"));
        
        assertEquals("testField cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("requireNonEmpty should throw ValidationException for whitespace-only string")
    void testRequireNonEmptyWithWhitespaceString() {
        // Given: Whitespace-only strings
        String spacesOnly = "   ";
        String tabsOnly = "\t\t\t";
        String mixedWhitespace = " \t \n ";

        // When/Then: Should throw ValidationException for all
        ValidationException exception1 = assertThrows(ValidationException.class,
            () -> StringValidator.requireNonEmpty(spacesOnly, "testField"));
        assertEquals("testField cannot be empty", exception1.getMessage());

        ValidationException exception2 = assertThrows(ValidationException.class,
            () -> StringValidator.requireNonEmpty(tabsOnly, "testField"));
        assertEquals("testField cannot be empty", exception2.getMessage());

        ValidationException exception3 = assertThrows(ValidationException.class,
            () -> StringValidator.requireNonEmpty(mixedWhitespace, "testField"));
        assertEquals("testField cannot be empty", exception3.getMessage());
    }

    @Test
    @DisplayName("validateMaxLength should not throw exception for valid length")
    void testValidateMaxLengthWithValidLength() {
        // Given: Strings within max length
        String shortString = "test";
        String exactLength = "12345";
        String nullString = null;

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(shortString, 10, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(exactLength, 5, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(nullString, 10, "testField"));
    }

    @Test
    @DisplayName("validateMaxLength should throw ValidationException for exceeding length")
    void testValidateMaxLengthWithExceedingLength() {
        // Given: String exceeding max length
        String longString = "This is a very long string";
        int maxLength = 10;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.validateMaxLength(longString, maxLength, "testField"));
        
        assertEquals("testField cannot exceed 10 characters", exception.getMessage());
    }

    @Test
    @DisplayName("validateMaxLength should handle zero max length")
    void testValidateMaxLengthWithZeroMaxLength() {
        // Given: Non-empty string and zero max length
        String nonEmptyString = "test";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.validateMaxLength(nonEmptyString, 0, "testField"));
        
        assertEquals("testField cannot exceed 0 characters", exception.getMessage());
    }

    @Test
    @DisplayName("validateRequired should not throw exception for valid required string")
    void testValidateRequiredWithValidString() {
        // Given: Valid required strings
        String validString = "test";
        String stringWithinLength = "short";

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> StringValidator.validateRequired(validString, 10, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateRequired(stringWithinLength, 10, "testField"));
    }

    @Test
    @DisplayName("validateRequired should throw ValidationException for empty string")
    void testValidateRequiredWithEmptyString() {
        // Given: Empty string
        String emptyString = "";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.validateRequired(emptyString, 10, "testField"));
        
        assertEquals("testField cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validateRequired should throw ValidationException for null string")
    void testValidateRequiredWithNullString() {
        // Given: Null string
        String nullString = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.validateRequired(nullString, 10, "testField"));
        
        assertEquals("testField cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validateRequired should throw ValidationException for exceeding length")
    void testValidateRequiredWithExceedingLength() {
        // Given: String exceeding max length
        String longString = "This is a very long string that exceeds the maximum allowed length";
        int maxLength = 10;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.validateRequired(longString, maxLength, "testField"));
        
        assertEquals("testField cannot exceed 10 characters", exception.getMessage());
    }

    @Test
    @DisplayName("validateOptional should not throw exception for null string")
    void testValidateOptionalWithNullString() {
        // Given: Null string (optional)
        String nullString = null;

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> StringValidator.validateOptional(nullString, 10, "testField"));
    }

    @Test
    @DisplayName("validateOptional should not throw exception for valid optional string")
    void testValidateOptionalWithValidString() {
        // Given: Valid optional strings
        String validString = "test";
        String emptyString = ""; // Empty is allowed for optional

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> StringValidator.validateOptional(validString, 10, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateOptional(emptyString, 10, "testField"));
    }

    @Test
    @DisplayName("validateOptional should throw ValidationException for exceeding length")
    void testValidateOptionalWithExceedingLength() {
        // Given: Optional string exceeding max length
        String longString = "This is a very long optional string";
        int maxLength = 10;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.validateOptional(longString, maxLength, "testField"));
        
        assertEquals("testField cannot exceed 10 characters", exception.getMessage());
    }

    @Test
    @DisplayName("validation methods should work with different field names")
    void testValidationMethodsWithDifferentFieldNames() {
        // Given: Invalid strings and different field names
        String emptyString = "";
        String longString = "This is too long";

        // When/Then: Should include field name in exception message
        ValidationException exception1 = assertThrows(ValidationException.class,
            () -> StringValidator.requireNonEmpty(emptyString, "userName"));
        assertEquals("userName cannot be empty", exception1.getMessage());

        ValidationException exception2 = assertThrows(ValidationException.class,
            () -> StringValidator.validateMaxLength(longString, 5, "password"));
        assertEquals("password cannot exceed 5 characters", exception2.getMessage());

        ValidationException exception3 = assertThrows(ValidationException.class,
            () -> StringValidator.validateRequired(null, 10, "email"));
        assertEquals("email cannot be empty", exception3.getMessage());

        ValidationException exception4 = assertThrows(ValidationException.class,
            () -> StringValidator.validateOptional(longString, 5, "description"));
        assertEquals("description cannot exceed 5 characters", exception4.getMessage());
    }

    @Test
    @DisplayName("private constructor should throw UnsupportedOperationException")
    void testPrivateConstructorThrowsException() throws NoSuchMethodException {
        // Given: Private constructor
        Constructor<StringValidator> constructor = StringValidator.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When/Then: Should throw UnsupportedOperationException
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, 
            constructor::newInstance);
        
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("This is a utility class and cannot be instantiated", 
            exception.getCause().getMessage());
    }

    @Test
    @DisplayName("utility class should be final")
    void testUtilityClassIsFinal() {
        // When/Then: Class should be final
        assertTrue(java.lang.reflect.Modifier.isFinal(StringValidator.class.getModifiers()));
    }

    @Test
    @DisplayName("all methods should be static")
    void testAllMethodsAreStatic() {
        // Given: All public methods
        java.lang.reflect.Method[] methods = StringValidator.class.getDeclaredMethods();
        
        // When/Then: All public methods should be static
        for (java.lang.reflect.Method method : methods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()), 
                    "Method " + method.getName() + " should be static");
            }
        }
    }

    @Test
    @DisplayName("validation methods should handle special characters")
    void testValidationMethodsWithSpecialCharacters() {
        // Given: Strings with special characters
        String specialChars = "test@#$%^&*()_+-=[]{}|;':\",./<>?";
        String unicode = "test ñáéíóú àèìòù äëïöü";
        String emoji = "test 🚀 🎉 ✨";

        // When/Then: Should handle special characters correctly
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(specialChars, "testField"));
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(unicode, "testField"));
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(emoji, "testField"));
        
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(specialChars, 100, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(unicode, 100, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(emoji, 100, "testField"));
    }

    @Test
    @DisplayName("validation methods should handle edge case lengths")
    void testValidationMethodsWithEdgeCaseLengths() {
        // Given: Edge case scenarios
        String singleChar = "a";
        String exactMaxLength = "12345";
        String oneOverMax = "123456";

        // When/Then: Should handle edge cases correctly
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(singleChar, 1, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(exactMaxLength, 5, "testField"));
        
        ValidationException exception = assertThrows(ValidationException.class,
            () -> StringValidator.validateMaxLength(oneOverMax, 5, "testField"));
        assertEquals("testField cannot exceed 5 characters", exception.getMessage());
    }

    @Test
    @DisplayName("validation methods should handle large max lengths")
    void testValidationMethodsWithLargeMaxLengths() {
        // Given: Large max lengths
        String normalString = "This is a normal string";
        int largeMaxLength = 1000000;

        // When/Then: Should handle large max lengths without issues
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(normalString, largeMaxLength, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateRequired(normalString, largeMaxLength, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateOptional(normalString, largeMaxLength, "testField"));
    }

    @Test
    @DisplayName("validation methods should be consistent")
    void testValidationMethodsConsistency() {
        // Given: Test strings
        String validString = "test";
        String emptyString = "";
        String longString = "This is a very long string";
        int maxLength = 10;

        // When/Then: Methods should be consistent
        // Valid string should pass all validations
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(validString, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(validString, maxLength, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateRequired(validString, maxLength, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateOptional(validString, maxLength, "testField"));

        // Empty string should fail required but pass optional
        assertThrows(ValidationException.class, () -> StringValidator.requireNonEmpty(emptyString, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(emptyString, maxLength, "testField"));
        assertThrows(ValidationException.class, () -> StringValidator.validateRequired(emptyString, maxLength, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateOptional(emptyString, maxLength, "testField"));

        // Long string should fail max length validations
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(longString, "testField"));
        assertThrows(ValidationException.class, () -> StringValidator.validateMaxLength(longString, maxLength, "testField"));
        assertThrows(ValidationException.class, () -> StringValidator.validateRequired(longString, maxLength, "testField"));
        assertThrows(ValidationException.class, () -> StringValidator.validateOptional(longString, maxLength, "testField"));
    }

    @Test
    @DisplayName("validation methods should handle newlines and tabs")
    void testValidationMethodsWithNewlinesAndTabs() {
        // Given: Strings with newlines and tabs
        String withNewlines = "line1\nline2\nline3";
        String withTabs = "col1\tcol2\tcol3";
        String mixed = "text\n\twith\tmixed\nwhitespace";

        // When/Then: Should handle newlines and tabs correctly
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(withNewlines, "testField"));
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(withTabs, "testField"));
        assertDoesNotThrow(() -> StringValidator.requireNonEmpty(mixed, "testField"));
        
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(withNewlines, 100, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(withTabs, 100, "testField"));
        assertDoesNotThrow(() -> StringValidator.validateMaxLength(mixed, 100, "testField"));
    }
}