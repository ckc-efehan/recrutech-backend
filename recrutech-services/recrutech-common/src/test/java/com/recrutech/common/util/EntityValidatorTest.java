package com.recrutech.common.util;

import com.recrutech.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for EntityValidator class.
 * Tests:
 * - Entity validation methods (requireNonNull, validateId with and without UUID validation)
 * - Exception throwing with proper field names
 * - Private constructor throws exception
 * - Edge cases with null entities and invalid IDs
 * - Utility class structure validation
 */
@DisplayName("EntityValidator Tests")
class EntityValidatorTest {

    @Test
    @DisplayName("requireNonNull should not throw exception for non-null entity")
    void testRequireNonNullWithValidEntity() {
        // Given: Non-null entities
        Object validEntity = new Object();
        String stringEntity = "test entity";
        Integer numberEntity = 42;

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> EntityValidator.requireNonNull(validEntity, "testEntity"));
        assertDoesNotThrow(() -> EntityValidator.requireNonNull(stringEntity, "stringEntity"));
        assertDoesNotThrow(() -> EntityValidator.requireNonNull(numberEntity, "numberEntity"));
    }

    @Test
    @DisplayName("requireNonNull should throw ValidationException for null entity")
    void testRequireNonNullWithNullEntity() {
        // Given: Null entity
        Object nullEntity = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.requireNonNull(nullEntity, "testEntity"));
        
        assertEquals("testEntity cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("requireNonNull should work with different entity names")
    void testRequireNonNullWithDifferentEntityNames() {
        // Given: Null entity and different entity names
        Object nullEntity = null;

        // When/Then: Should include entity name in exception message
        ValidationException exception1 = assertThrows(ValidationException.class,
            () -> EntityValidator.requireNonNull(nullEntity, "user"));
        assertEquals("user cannot be null", exception1.getMessage());

        ValidationException exception2 = assertThrows(ValidationException.class,
            () -> EntityValidator.requireNonNull(nullEntity, "company"));
        assertEquals("company cannot be null", exception2.getMessage());

        ValidationException exception3 = assertThrows(ValidationException.class,
            () -> EntityValidator.requireNonNull(nullEntity, "customEntity"));
        assertEquals("customEntity cannot be null", exception3.getMessage());
    }

    @Test
    @DisplayName("validateId with UUID validation should not throw exception for valid UUID")
    void testValidateIdWithUuidValidationAndValidUuid() {
        // Given: Valid UUID
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> EntityValidator.validateId(validUuid, "testEntity", true));
    }

    @Test
    @DisplayName("validateId with UUID validation should throw ValidationException for null ID")
    void testValidateIdWithUuidValidationAndNullId() {
        // Given: Null ID
        String nullId = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(nullId, "testEntity", true));
        
        assertEquals("testEntity ID cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validateId with UUID validation should throw ValidationException for empty ID")
    void testValidateIdWithUuidValidationAndEmptyId() {
        // Given: Empty ID
        String emptyId = "";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(emptyId, "testEntity", true));
        
        assertEquals("testEntity ID cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validateId with UUID validation should throw ValidationException for invalid UUID")
    void testValidateIdWithUuidValidationAndInvalidUuid() {
        // Given: Invalid UUID
        String invalidUuid = "not-a-valid-uuid";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(invalidUuid, "testEntity", true));
        
        assertEquals("testEntity ID must be a valid UUID", exception.getMessage());
    }

    @Test
    @DisplayName("validateId without UUID validation should not throw exception for valid ID")
    void testValidateIdWithoutUuidValidationAndValidId() {
        // Given: Valid IDs (not necessarily UUIDs)
        String validId1 = "user123";
        String validId2 = "12345";
        String validId3 = "entity-id-with-dashes";

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> EntityValidator.validateId(validId1, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(validId2, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(validId3, "testEntity", false));
    }

    @Test
    @DisplayName("validateId without UUID validation should throw ValidationException for null ID")
    void testValidateIdWithoutUuidValidationAndNullId() {
        // Given: Null ID
        String nullId = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(nullId, "testEntity", false));
        
        assertEquals("testEntity ID cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validateId without UUID validation should throw ValidationException for empty ID")
    void testValidateIdWithoutUuidValidationAndEmptyId() {
        // Given: Empty ID
        String emptyId = "";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(emptyId, "testEntity", false));
        
        assertEquals("testEntity ID cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validateId without UUID validation should not validate UUID format")
    void testValidateIdWithoutUuidValidationIgnoresUuidFormat() {
        // Given: Non-UUID IDs
        String nonUuidId1 = "simple-id";
        String nonUuidId2 = "123";
        String nonUuidId3 = "entity_id_with_underscores";

        // When/Then: Should not throw exception (UUID validation is disabled)
        assertDoesNotThrow(() -> EntityValidator.validateId(nonUuidId1, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(nonUuidId2, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(nonUuidId3, "testEntity", false));
    }

    @Test
    @DisplayName("validateId convenience method should not validate UUID by default")
    void testValidateIdConvenienceMethod() {
        // Given: Non-UUID ID
        String nonUuidId = "simple-id";

        // When/Then: Should not throw exception (convenience method defaults to no UUID validation)
        assertDoesNotThrow(() -> EntityValidator.validateId(nonUuidId, "testEntity"));
    }

    @Test
    @DisplayName("validateId convenience method should throw ValidationException for null ID")
    void testValidateIdConvenienceMethodWithNullId() {
        // Given: Null ID
        String nullId = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(nullId, "testEntity"));
        
        assertEquals("testEntity ID cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validateId convenience method should throw ValidationException for empty ID")
    void testValidateIdConvenienceMethodWithEmptyId() {
        // Given: Empty ID
        String emptyId = "";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(emptyId, "testEntity"));
        
        assertEquals("testEntity ID cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("validation methods should work with different entity names")
    void testValidationMethodsWithDifferentEntityNames() {
        // Given: Invalid values and different entity names
        String emptyId = "";
        String invalidUuid = "invalid-uuid";

        // When/Then: Should include entity name in exception message
        ValidationException exception1 = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(emptyId, "user"));
        assertEquals("user ID cannot be empty", exception1.getMessage());

        ValidationException exception2 = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(invalidUuid, "company", true));
        assertEquals("company ID must be a valid UUID", exception2.getMessage());

        ValidationException exception3 = assertThrows(ValidationException.class,
            () -> EntityValidator.validateId(null, "product"));
        assertEquals("product ID cannot be empty", exception3.getMessage());
    }

    @Test
    @DisplayName("private constructor should throw UnsupportedOperationException")
    void testPrivateConstructorThrowsException() throws NoSuchMethodException {
        // Given: Private constructor
        Constructor<EntityValidator> constructor = EntityValidator.class.getDeclaredConstructor();
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
        assertTrue(java.lang.reflect.Modifier.isFinal(EntityValidator.class.getModifiers()));
    }

    @Test
    @DisplayName("all methods should be static")
    void testAllMethodsAreStatic() {
        // Given: All public methods
        java.lang.reflect.Method[] methods = EntityValidator.class.getDeclaredMethods();
        
        // When/Then: All public methods should be static
        for (java.lang.reflect.Method method : methods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()), 
                    "Method " + method.getName() + " should be static");
            }
        }
    }

    @Test
    @DisplayName("validation methods should handle whitespace in IDs")
    void testValidationMethodsWithWhitespaceIds() {
        // Given: IDs with whitespace
        String idWithSpaces = "  id-with-spaces  ";
        String idWithTabs = "\tid-with-tabs\t";
        String idWithNewlines = "\nid-with-newlines\n";

        // When/Then: Should not throw exception (whitespace is preserved in IDs)
        assertDoesNotThrow(() -> EntityValidator.validateId(idWithSpaces, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(idWithTabs, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(idWithNewlines, "testEntity", false));
    }

    @Test
    @DisplayName("validation methods should handle special characters in IDs")
    void testValidationMethodsWithSpecialCharacterIds() {
        // Given: IDs with special characters
        String idWithSpecialChars = "id@#$%^&*()_+-=[]{}|;':\",./<>?";
        String idWithUnicode = "id-ñáéíóú-àèìòù-äëïöü";
        String idWithEmoji = "id-🚀-🎉-✨";

        // When/Then: Should not throw exception (special characters are allowed in IDs)
        assertDoesNotThrow(() -> EntityValidator.validateId(idWithSpecialChars, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(idWithUnicode, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(idWithEmoji, "testEntity", false));
    }

    @Test
    @DisplayName("validation methods should be consistent")
    void testValidationMethodsConsistency() {
        // Given: Test values
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";
        String validNonUuid = "simple-id";
        String invalidUuid = "invalid-uuid";
        String emptyId = "";
        Object validEntity = new Object();
        Object nullEntity = null;

        // When/Then: Methods should be consistent
        // Valid UUID should pass all validations
        assertDoesNotThrow(() -> EntityValidator.validateId(validUuid, "testEntity", true));
        assertDoesNotThrow(() -> EntityValidator.validateId(validUuid, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(validUuid, "testEntity"));

        // Valid non-UUID should pass non-UUID validations but fail UUID validation
        assertThrows(ValidationException.class, () -> EntityValidator.validateId(validNonUuid, "testEntity", true));
        assertDoesNotThrow(() -> EntityValidator.validateId(validNonUuid, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(validNonUuid, "testEntity"));

        // Invalid UUID should fail UUID validation but pass non-UUID validation
        assertThrows(ValidationException.class, () -> EntityValidator.validateId(invalidUuid, "testEntity", true));
        assertDoesNotThrow(() -> EntityValidator.validateId(invalidUuid, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(invalidUuid, "testEntity"));

        // Empty ID should fail all validations
        assertThrows(ValidationException.class, () -> EntityValidator.validateId(emptyId, "testEntity", true));
        assertThrows(ValidationException.class, () -> EntityValidator.validateId(emptyId, "testEntity", false));
        assertThrows(ValidationException.class, () -> EntityValidator.validateId(emptyId, "testEntity"));

        // Valid entity should pass null check
        assertDoesNotThrow(() -> EntityValidator.requireNonNull(validEntity, "testEntity"));

        // Null entity should fail null check
        assertThrows(ValidationException.class, () -> EntityValidator.requireNonNull(nullEntity, "testEntity"));
    }

    @Test
    @DisplayName("validation methods should handle long IDs")
    void testValidationMethodsWithLongIds() {
        // Given: Very long ID
        String longId = "this-is-a-very-long-id-that-contains-many-characters-and-should-be-handled-correctly-by-the-validation-methods-without-any-issues-or-truncation-problems";

        // When/Then: Should handle long IDs without issues
        assertDoesNotThrow(() -> EntityValidator.validateId(longId, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(longId, "testEntity"));
    }

    @Test
    @DisplayName("validation methods should handle numeric IDs")
    void testValidationMethodsWithNumericIds() {
        // Given: Numeric IDs as strings
        String numericId1 = "123";
        String numericId2 = "999999999";
        String numericId3 = "0";

        // When/Then: Should handle numeric IDs correctly
        assertDoesNotThrow(() -> EntityValidator.validateId(numericId1, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(numericId2, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(numericId3, "testEntity", false));
        assertDoesNotThrow(() -> EntityValidator.validateId(numericId1, "testEntity"));
        assertDoesNotThrow(() -> EntityValidator.validateId(numericId2, "testEntity"));
        assertDoesNotThrow(() -> EntityValidator.validateId(numericId3, "testEntity"));
    }
}