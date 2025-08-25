package com.recrutech.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for GlobalExceptionHandler class.
 * Tests:
 * - Exception handler methods for different exception types
 * - HTTP status code mapping
 * - Response body structure and content
 * - Error message handling
 * - Timestamp generation
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleSpringSecurityAccessDeniedException should return 403 FORBIDDEN")
    void testHandleSpringSecurityAccessDeniedException() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied test");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleSpringSecurityAccessDeniedException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(403, body.get("status"));
        assertEquals("Access Denied", body.get("error"));
        assertEquals("Access denied: Insufficient permissions to perform this operation", body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof LocalDateTime);
    }

    @Test
    @DisplayName("handleNotFoundException should return 404 NOT_FOUND")
    void testHandleNotFoundException() {
        // Given
        String errorMessage = "Resource not found";
        NotFoundException exception = new NotFoundException(errorMessage);

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleNotFoundException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertEquals(errorMessage, body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof LocalDateTime);
    }

    @Test
    @DisplayName("handleValidationException should return 400 BAD_REQUEST")
    void testHandleValidationException() {
        // Given
        String errorMessage = "Validation failed";
        ValidationException exception = new ValidationException(errorMessage);

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleValidationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Validation Error", body.get("error"));
        assertEquals(errorMessage, body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof LocalDateTime);
    }

    @Test
    @DisplayName("handleGenericException should return 500 INTERNAL_SERVER_ERROR")
    void testHandleGenericException() {
        // Given
        Exception exception = new Exception("Generic error");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleGenericException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(500, body.get("status"));
        assertEquals("Internal Server Error", body.get("error"));
        assertEquals("An unexpected error occurred", body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof LocalDateTime);
    }

    @Test
    @DisplayName("handleNotFoundException should handle null message")
    void testHandleNotFoundExceptionWithNullMessage() {
        // Given
        NotFoundException exception = new NotFoundException(null);

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleNotFoundException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertNull(body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("handleValidationException should handle empty message")
    void testHandleValidationExceptionWithEmptyMessage() {
        // Given
        ValidationException exception = new ValidationException("");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleValidationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Validation Error", body.get("error"));
        assertEquals("", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("response body should have correct structure")
    void testResponseBodyStructure() {
        // Given
        NotFoundException exception = new NotFoundException("Test message");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleNotFoundException(exception);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        
        // Should have exactly 4 fields
        assertEquals(4, body.size());
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
    }

    @Test
    @DisplayName("timestamp should be recent")
    void testTimestampIsRecent() {
        // Given
        LocalDateTime before = LocalDateTime.now();
        NotFoundException exception = new NotFoundException("Test");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleNotFoundException(exception);

        // Then
        LocalDateTime after = LocalDateTime.now();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        LocalDateTime timestamp = (LocalDateTime) body.get("timestamp");
        
        assertTrue(timestamp.isAfter(before.minusSeconds(1)) || timestamp.isEqual(before.minusSeconds(1)));
        assertTrue(timestamp.isBefore(after.plusSeconds(1)) || timestamp.isEqual(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("different exception types should have different status codes")
    void testDifferentStatusCodes() {
        // Given
        AccessDeniedException accessDenied = new AccessDeniedException("Access denied");
        NotFoundException notFound = new NotFoundException("Not found");
        ValidationException validation = new ValidationException("Validation failed");
        Exception generic = new Exception("Generic error");

        // When
        ResponseEntity<Object> accessResponse = globalExceptionHandler.handleSpringSecurityAccessDeniedException(accessDenied);
        ResponseEntity<Object> notFoundResponse = globalExceptionHandler.handleNotFoundException(notFound);
        ResponseEntity<Object> validationResponse = globalExceptionHandler.handleValidationException(validation);
        ResponseEntity<Object> genericResponse = globalExceptionHandler.handleGenericException(generic);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, accessResponse.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, validationResponse.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, genericResponse.getStatusCode());
    }

    @Test
    @DisplayName("exception handlers should handle long messages")
    void testHandleLongMessages() {
        // Given
        String longMessage = "This is a very long error message that contains multiple sentences and should be handled correctly by the exception handler without any truncation or formatting issues. It includes various characters and should maintain its integrity throughout the entire processing pipeline.";
        ValidationException exception = new ValidationException(longMessage);

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleValidationException(exception);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(longMessage, body.get("message"));
    }

    @Test
    @DisplayName("exception handlers should handle special characters")
    void testHandleSpecialCharacters() {
        // Given
        String specialMessage = "Error with special chars: @#$%^&*()_+-=[]{}|;':\",./<>? and unicode: ñáéíóú";
        NotFoundException exception = new NotFoundException(specialMessage);

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleNotFoundException(exception);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(specialMessage, body.get("message"));
    }

    @Test
    @DisplayName("generic exception handler should not expose internal exception message")
    void testGenericExceptionDoesNotExposeInternalMessage() {
        // Given
        Exception exception = new Exception("Internal sensitive information");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleGenericException(exception);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred", body.get("message"));
        assertNotEquals("Internal sensitive information", body.get("message"));
    }

    @Test
    @DisplayName("exception handlers should work with runtime exceptions")
    void testHandleRuntimeExceptions() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Runtime error");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleGenericException(runtimeException);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred", body.get("message"));
    }

    @Test
    @DisplayName("response body should use LinkedHashMap for consistent ordering")
    void testResponseBodyOrdering() {
        // Given
        NotFoundException exception = new NotFoundException("Test");

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleNotFoundException(exception);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body instanceof java.util.LinkedHashMap);
        
        // Check that keys are in expected order
        String[] keys = body.keySet().toArray(new String[0]);
        assertEquals("timestamp", keys[0]);
        assertEquals("status", keys[1]);
        assertEquals("error", keys[2]);
        assertEquals("message", keys[3]);
    }

    @Test
    @DisplayName("all exception handlers should return non-null response")
    void testAllHandlersReturnNonNullResponse() {
        // Given
        AccessDeniedException accessDenied = new AccessDeniedException("Test");
        NotFoundException notFound = new NotFoundException("Test");
        ValidationException validation = new ValidationException("Test");
        Exception generic = new Exception("Test");

        // When/Then
        assertNotNull(globalExceptionHandler.handleSpringSecurityAccessDeniedException(accessDenied));
        assertNotNull(globalExceptionHandler.handleNotFoundException(notFound));
        assertNotNull(globalExceptionHandler.handleValidationException(validation));
        assertNotNull(globalExceptionHandler.handleGenericException(generic));
    }
}