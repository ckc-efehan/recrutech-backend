package com.recrutech.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ValidationException class.
 * Tests:
 * - Exception constructors (message only, message + cause)
 * - Exception throwing and catching
 * - Inheritance from RuntimeException
 * - Serialization support
 * - Message and cause handling
 */
@DisplayName("ValidationException Tests")
class ValidationExceptionTest {

    @Test
    @DisplayName("Constructor with message should create exception with correct message")
    void testConstructorWithMessage() {
        // Given
        String message = "Validation failed";

        // When
        ValidationException exception = new ValidationException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("Constructor with message and cause should create exception with both")
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "Validation failed with cause";
        Throwable cause = new IllegalArgumentException("Root cause");

        // When
        ValidationException exception = new ValidationException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("Exception should be throwable and catchable")
    void testExceptionThrowingAndCatching() {
        // Given
        String message = "Test validation exception message";

        // When/Then
        ValidationException thrownException = assertThrows(ValidationException.class, () -> {
            throw new ValidationException(message);
        });

        assertEquals(message, thrownException.getMessage());
    }

    @Test
    @DisplayName("Exception with cause should preserve stack trace")
    void testExceptionWithCausePreservesStackTrace() {
        // Given
        String message = "Validation error";
        RuntimeException originalCause = new RuntimeException("Original error");

        // When
        ValidationException exception = new ValidationException(message, originalCause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(originalCause, exception.getCause());
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    @DisplayName("Exception should handle null message gracefully")
    void testExceptionWithNullMessage() {
        // Given
        String message = null;

        // When
        ValidationException exception = new ValidationException(message);

        // Then
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Exception should handle null cause gracefully")
    void testExceptionWithNullCause() {
        // Given
        String message = "Test message";
        Throwable cause = null;

        // When
        ValidationException exception = new ValidationException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Exception should be serializable")
    void testExceptionSerializability() {
        // Given
        String message = "Serializable validation exception";
        ValidationException exception = new ValidationException(message);

        // When/Then - Exception should implement Serializable through RuntimeException
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    @DisplayName("Multiple exceptions should be independent")
    void testMultipleExceptionsIndependence() {
        // Given
        String message1 = "First validation exception";
        String message2 = "Second validation exception";
        Throwable cause1 = new IllegalArgumentException("First cause");
        Throwable cause2 = new IllegalStateException("Second cause");

        // When
        ValidationException exception1 = new ValidationException(message1, cause1);
        ValidationException exception2 = new ValidationException(message2, cause2);

        // Then
        assertNotEquals(exception1.getMessage(), exception2.getMessage());
        assertNotEquals(exception1.getCause(), exception2.getCause());
        assertEquals(message1, exception1.getMessage());
        assertEquals(message2, exception2.getMessage());
        assertEquals(cause1, exception1.getCause());
        assertEquals(cause2, exception2.getCause());
    }

    @Test
    @DisplayName("Exception should extend RuntimeException")
    void testExceptionInheritance() {
        // Given
        ValidationException exception = new ValidationException("Test");

        // When/Then
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @DisplayName("Exception should have correct serialVersionUID")
    void testSerialVersionUID() throws NoSuchFieldException, IllegalAccessException {
        // Given: Access to serialVersionUID field
        java.lang.reflect.Field serialVersionUIDField = ValidationException.class.getDeclaredField("serialVersionUID");
        serialVersionUIDField.setAccessible(true);

        // When: Get the serialVersionUID value
        long serialVersionUID = (Long) serialVersionUIDField.get(null);

        // Then: Should be 1L as defined in the class
        assertEquals(1L, serialVersionUID);
    }

    @Test
    @DisplayName("Exception should work in try-catch blocks")
    void testExceptionInTryCatch() {
        // Given
        String expectedMessage = "Validation failed in try-catch";
        boolean exceptionCaught = false;
        String actualMessage = null;

        // When
        try {
            throw new ValidationException(expectedMessage);
        } catch (ValidationException e) {
            exceptionCaught = true;
            actualMessage = e.getMessage();
        }

        // Then
        assertTrue(exceptionCaught);
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("Exception should work with validation-specific messages")
    void testExceptionWithValidationMessages() {
        // Given: Validation-specific messages
        String fieldRequiredMessage = "Field 'email' is required";
        String formatMessage = "Field 'email' must be a valid email format";
        String lengthMessage = "Field 'password' must be at least 8 characters long";
        String rangeMessage = "Field 'age' must be between 18 and 120";

        // When: Create exceptions with validation messages
        ValidationException requiredException = new ValidationException(fieldRequiredMessage);
        ValidationException formatException = new ValidationException(formatMessage);
        ValidationException lengthException = new ValidationException(lengthMessage);
        ValidationException rangeException = new ValidationException(rangeMessage);

        // Then: Should handle all validation message types correctly
        assertEquals(fieldRequiredMessage, requiredException.getMessage());
        assertEquals(formatMessage, formatException.getMessage());
        assertEquals(lengthMessage, lengthException.getMessage());
        assertEquals(rangeMessage, rangeException.getMessage());
    }

    @Test
    @DisplayName("Exception should work with nested causes")
    void testExceptionWithNestedCauses() {
        // Given: Nested exception chain
        IllegalArgumentException rootCause = new IllegalArgumentException("Invalid input format");
        RuntimeException intermediateCause = new RuntimeException("Validation processing error", rootCause);
        ValidationException topException = new ValidationException("Field validation failed", intermediateCause);

        // When/Then: Should preserve the entire cause chain
        assertEquals("Field validation failed", topException.getMessage());
        assertEquals(intermediateCause, topException.getCause());
        assertEquals(rootCause, topException.getCause().getCause());
        assertEquals("Invalid input format", topException.getCause().getCause().getMessage());
    }

    @Test
    @DisplayName("Exception should work in method signatures")
    void testExceptionInMethodSignature() {
        // Given: Method that throws ValidationException
        assertThrows(ValidationException.class, () -> {
            throwValidationException("Method signature test");
        });
    }

    @Test
    @DisplayName("Exception should work with complex validation scenarios")
    void testExceptionWithComplexValidationScenarios() {
        // Given: Complex validation scenarios
        String multiFieldMessage = "Multiple validation errors: email is required, password too short, age out of range";
        String jsonValidationMessage = "JSON validation failed: missing required field 'name' at line 5, column 12";
        String businessRuleMessage = "Business rule violation: user cannot have more than 5 active sessions";

        // When: Create exceptions for complex scenarios
        ValidationException multiFieldException = new ValidationException(multiFieldMessage);
        ValidationException jsonException = new ValidationException(jsonValidationMessage);
        ValidationException businessRuleException = new ValidationException(businessRuleMessage);

        // Then: Should handle complex validation messages correctly
        assertEquals(multiFieldMessage, multiFieldException.getMessage());
        assertEquals(jsonValidationMessage, jsonException.getMessage());
        assertEquals(businessRuleMessage, businessRuleException.getMessage());
    }

    @Test
    @DisplayName("Exception should work with different message encodings")
    void testExceptionWithDifferentEncodings() {
        // Given: Messages with different characters
        String unicodeMessage = "Validation failed: ñáéíóú àèìòù äëïöü";
        String emojiMessage = "Validation failed: 🚫 Invalid input 📝";
        String specialCharsMessage = "Validation failed: @#$%^&*()_+-=[]{}|;':\",./<>?";

        // When: Create exceptions with different encodings
        ValidationException unicodeException = new ValidationException(unicodeMessage);
        ValidationException emojiException = new ValidationException(emojiMessage);
        ValidationException specialException = new ValidationException(specialCharsMessage);

        // Then: Should handle all character encodings correctly
        assertEquals(unicodeMessage, unicodeException.getMessage());
        assertEquals(emojiMessage, emojiException.getMessage());
        assertEquals(specialCharsMessage, specialException.getMessage());
    }

    private void throwValidationException(String message) throws ValidationException {
        throw new ValidationException(message);
    }
}