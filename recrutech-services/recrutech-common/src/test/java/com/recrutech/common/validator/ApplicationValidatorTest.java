package com.recrutech.common.validator;

import com.recrutech.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ApplicationValidator class.
 */
class ApplicationValidatorTest {

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String INVALID_UUID = "not-a-uuid";
    private static final String VALID_FIRST_NAME = "John";
    private static final String VALID_LAST_NAME = "Doe";

    // Mock request class for testing validateCreateApplicationInput
        public record MockApplicationRequest(String cvFileId) {
    }

    // Mock request class without a cvFileId method for testing reflection failure
    public static class InvalidMockRequest {
        // No cvFileId method
    }

    @Test
    void constructor_ShouldThrowUnsupportedOperationException() {
        // Act & Assert
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
            () -> {
                try {
                    var constructor = ApplicationValidator.class.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    constructor.newInstance();
                } catch (Exception e) {
                    if (e.getCause() instanceof UnsupportedOperationException) {
                        throw e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            });
        assertEquals("This is a utility class and cannot be instantiated", exception.getMessage());
    }

    // Tests to validateCreateApplicationInput method
    @Test
    void validateCreateApplicationInput_WithValidInput_ShouldNotThrowException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest(VALID_UUID);

        // Act & Assert
        assertDoesNotThrow(() -> ApplicationValidator.validateCreateApplicationInput(VALID_UUID, request));
    }

    @Test
    void validateCreateApplicationInput_WithNullJobId_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest(VALID_UUID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(null, request));
        assertEquals("Job ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithEmptyJobId_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest(VALID_UUID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput("", request));
        assertEquals("Job ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithWhitespaceJobId_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest(VALID_UUID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput("   ", request));
        assertEquals("Job ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithInvalidJobIdUuid_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest(VALID_UUID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(INVALID_UUID, request));
        assertEquals("Job ID must be a valid UUID", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithNullRequest_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(VALID_UUID, null));
        assertEquals("Application request cannot be null", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithNullCvFileId_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest(null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(VALID_UUID, request));
        assertEquals("Invalid application request structure: CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithEmptyCvFileId_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest("");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(VALID_UUID, request));
        assertEquals("Invalid application request structure: CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithWhitespaceCvFileId_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest("   ");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(VALID_UUID, request));
        assertEquals("Invalid application request structure: CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithInvalidCvFileIdUuid_ShouldThrowValidationException() {
        // Arrange
        MockApplicationRequest request = new MockApplicationRequest(INVALID_UUID);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(VALID_UUID, request));
        assertEquals("Invalid application request structure: CV File ID must be a valid UUID", exception.getMessage());
    }

    @Test
    void validateCreateApplicationInput_WithInvalidRequestStructure_ShouldThrowValidationException() {
        // Arrange
        InvalidMockRequest request = new InvalidMockRequest();

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateCreateApplicationInput(VALID_UUID, request));
        assertTrue(exception.getMessage().startsWith("Invalid application request structure:"));
    }

    // Tests for validateSimplifiedApplicationRequest method
    @Test
    void validateSimplifiedApplicationRequest_WithValidCvFileId_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> ApplicationValidator.validateSimplifiedApplicationRequest(VALID_UUID));
    }

    @Test
    void validateSimplifiedApplicationRequest_WithNullCvFileId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateSimplifiedApplicationRequest(null));
        assertEquals("CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateSimplifiedApplicationRequest_WithEmptyCvFileId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateSimplifiedApplicationRequest(""));
        assertEquals("CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateSimplifiedApplicationRequest_WithWhitespaceCvFileId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateSimplifiedApplicationRequest("   "));
        assertEquals("CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateSimplifiedApplicationRequest_WithInvalidCvFileIdUuid_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateSimplifiedApplicationRequest(INVALID_UUID));
        assertEquals("CV File ID must be a valid UUID", exception.getMessage());
    }

    // Tests for validateApplicationRequestFields method
    @Test
    void validateApplicationRequestFields_WithValidFields_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> ApplicationValidator.validateApplicationRequestFields(
            VALID_UUID, VALID_UUID, VALID_FIRST_NAME, VALID_LAST_NAME));
    }

    @Test
    void validateApplicationRequestFields_WithNullUserId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                null, VALID_UUID, VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("User ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithEmptyUserId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                "", VALID_UUID, VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("User ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithWhitespaceUserId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                "   ", VALID_UUID, VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("User ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithInvalidUserIdUuid_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                INVALID_UUID, VALID_UUID, VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("User ID must be a valid UUID", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithNullCvFileId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, null, VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithEmptyCvFileId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, "", VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithWhitespaceCvFileId_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, "   ", VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("CV File ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithInvalidCvFileIdUuid_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, INVALID_UUID, VALID_FIRST_NAME, VALID_LAST_NAME));
        assertEquals("CV File ID must be a valid UUID", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithNullFirstName_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, VALID_UUID, null, VALID_LAST_NAME));
        assertEquals("First name cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithEmptyFirstName_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, VALID_UUID, "", VALID_LAST_NAME));
        assertEquals("First name cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithWhitespaceFirstName_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, VALID_UUID, "   ", VALID_LAST_NAME));
        assertEquals("First name cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithNullLastName_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, VALID_UUID, VALID_FIRST_NAME, null));
        assertEquals("Last name cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithEmptyLastName_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, VALID_UUID, VALID_FIRST_NAME, ""));
        assertEquals("Last name cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateApplicationRequestFields_WithWhitespaceLastName_ShouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
            () -> ApplicationValidator.validateApplicationRequestFields(
                VALID_UUID, VALID_UUID, VALID_FIRST_NAME, "   "));
        assertEquals("Last name cannot be null or empty", exception.getMessage());
    }
}