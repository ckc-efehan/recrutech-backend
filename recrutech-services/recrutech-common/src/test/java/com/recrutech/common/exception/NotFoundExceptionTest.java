package com.recrutech.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for NotFoundException class.
 * Tests:
 * - Exception constructors (message only, message + cause)
 * - Exception throwing and catching
 * - Inheritance from RuntimeException
 * - Serialization support
 * - Message and cause handling
 */
@DisplayName("NotFoundException Tests")
class NotFoundExceptionTest {

    @Test
    @DisplayName("Constructor with message should create exception with correct message")
    void testConstructorWithMessage() {
        // Given
        String message = "Resource not found";

        // When
        NotFoundException exception = new NotFoundException(message);

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
        String message = "Resource not found with cause";
        Throwable cause = new IllegalArgumentException("Root cause");

        // When
        NotFoundException exception = new NotFoundException(message, cause);

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
        String message = "Test exception message";

        // When/Then
        NotFoundException thrownException = assertThrows(NotFoundException.class, () -> {
            throw new NotFoundException(message);
        });

        assertEquals(message, thrownException.getMessage());
    }

    @Test
    @DisplayName("Exception with cause should preserve stack trace")
    void testExceptionWithCausePreservesStackTrace() {
        // Given
        String message = "Not found error";
        RuntimeException originalCause = new RuntimeException("Original error");

        // When
        NotFoundException exception = new NotFoundException(message, originalCause);

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
        NotFoundException exception = new NotFoundException(message);

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
        NotFoundException exception = new NotFoundException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Exception should be serializable")
    void testExceptionSerializability() {
        // Given
        String message = "Serializable exception";
        NotFoundException exception = new NotFoundException(message);

        // When/Then - Exception should implement Serializable through RuntimeException
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    @DisplayName("Multiple exceptions should be independent")
    void testMultipleExceptionsIndependence() {
        // Given
        String message1 = "First exception";
        String message2 = "Second exception";
        Throwable cause1 = new IllegalArgumentException("First cause");
        Throwable cause2 = new IllegalStateException("Second cause");

        // When
        NotFoundException exception1 = new NotFoundException(message1, cause1);
        NotFoundException exception2 = new NotFoundException(message2, cause2);

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
        NotFoundException exception = new NotFoundException("Test");

        // When/Then
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    @DisplayName("Exception should have correct serialVersionUID")
    void testSerialVersionUID() throws NoSuchFieldException, IllegalAccessException {
        // Given: Access to serialVersionUID field
        java.lang.reflect.Field serialVersionUIDField = NotFoundException.class.getDeclaredField("serialVersionUID");
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
        String expectedMessage = "Resource not found in try-catch";
        boolean exceptionCaught = false;
        String actualMessage = null;

        // When
        try {
            throw new NotFoundException(expectedMessage);
        } catch (NotFoundException e) {
            exceptionCaught = true;
            actualMessage = e.getMessage();
        }

        // Then
        assertTrue(exceptionCaught);
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("Exception should work with different message types")
    void testExceptionWithDifferentMessageTypes() {
        // Given: Different message types
        String emptyMessage = "";
        String longMessage = "This is a very long error message that contains multiple words and should be handled correctly by the exception class without any issues or truncation";
        String specialCharsMessage = "Error with special chars: @#$%^&*()_+-=[]{}|;':\",./<>?";

        // When: Create exceptions with different messages
        NotFoundException emptyException = new NotFoundException(emptyMessage);
        NotFoundException longException = new NotFoundException(longMessage);
        NotFoundException specialException = new NotFoundException(specialCharsMessage);

        // Then: Should handle all message types correctly
        assertEquals(emptyMessage, emptyException.getMessage());
        assertEquals(longMessage, longException.getMessage());
        assertEquals(specialCharsMessage, specialException.getMessage());
    }

    @Test
    @DisplayName("Exception should work with nested causes")
    void testExceptionWithNestedCauses() {
        // Given: Nested exception chain
        IllegalArgumentException rootCause = new IllegalArgumentException("Root cause");
        RuntimeException intermediateCause = new RuntimeException("Intermediate cause", rootCause);
        NotFoundException topException = new NotFoundException("Top level exception", intermediateCause);

        // When/Then: Should preserve the entire cause chain
        assertEquals("Top level exception", topException.getMessage());
        assertEquals(intermediateCause, topException.getCause());
        assertEquals(rootCause, topException.getCause().getCause());
        assertEquals("Root cause", topException.getCause().getCause().getMessage());
    }

    @Test
    @DisplayName("Exception should work in method signatures")
    void testExceptionInMethodSignature() {
        // Given: Method that throws NotFoundException
        assertThrows(NotFoundException.class, () -> {
            throwNotFoundException("Method signature test");
        });
    }

    private void throwNotFoundException(String message) throws NotFoundException {
        throw new NotFoundException(message);
    }
}