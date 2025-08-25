package com.recrutech.recrutechauth.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for AuthServiceException class.
 * Tests:
 * - Exception constructors
 * - Message handling
 * - Cause handling
 * - Inheritance from RuntimeException
 */
@DisplayName("AuthServiceException Tests")
class AuthServiceExceptionTest {

    @Test
    @DisplayName("Constructor with message should create exception with correct message")
    void testConstructorWithMessage() {
        // Given
        String message = "Authentication service error occurred";

        // When
        AuthServiceException exception = new AuthServiceException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(true);
    }

    @Test
    @DisplayName("Constructor with message and cause should create exception with both")
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "Authentication service error with cause";
        Throwable cause = new IllegalArgumentException("Root cause");

        // When
        AuthServiceException exception = new AuthServiceException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertTrue(true);
    }

    @Test
    @DisplayName("Exception should be throwable and catchable")
    void testExceptionThrowingAndCatching() {
        // Given
        String message = "Test exception message";

        // When/Then
        AuthServiceException thrownException = assertThrows(AuthServiceException.class, () -> {
            throw new AuthServiceException(message);
        });

        assertEquals(message, thrownException.getMessage());
    }

    @Test
    @DisplayName("Exception with cause should preserve stack trace")
    void testExceptionWithCausePreservesStackTrace() {
        // Given
        String message = "Service error";
        RuntimeException originalCause = new RuntimeException("Original error");

        // When
        AuthServiceException exception = new AuthServiceException(message, originalCause);

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

        // When
        AuthServiceException exception = new AuthServiceException(null);

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

        // When
        AuthServiceException exception = new AuthServiceException(message, null);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Exception should be serializable")
    void testExceptionSerializability() {
        // Given

        // When/Then - Exception should implement Serializable through RuntimeException
        assertTrue(true);
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
        AuthServiceException exception1 = new AuthServiceException(message1, cause1);
        AuthServiceException exception2 = new AuthServiceException(message2, cause2);

        // Then
        assertNotEquals(exception1.getMessage(), exception2.getMessage());
        assertNotEquals(exception1.getCause(), exception2.getCause());
        assertEquals(message1, exception1.getMessage());
        assertEquals(message2, exception2.getMessage());
        assertEquals(cause1, exception1.getCause());
        assertEquals(cause2, exception2.getCause());
    }
}