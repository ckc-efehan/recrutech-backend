package com.recrutech.common.util;

import com.recrutech.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for UuidValidator class.
 * Tests:
 * - UUID validation methods (basic, format, version 4)
 * - Exception throwing validation methods
 * - UUID version detection
 * - Private constructor throws exception
 * - Edge cases with invalid UUIDs
 * - Utility class structure validation
 */
@DisplayName("UuidValidator Tests")
class UuidValidatorTest {

    @Test
    @DisplayName("isValidUuid should return true for valid UUID")
    void testIsValidUuidWithValidUuid() {
        // Given: Valid UUIDs
        String validUuid1 = "123e4567-e89b-12d3-a456-426614174000";
        String validUuid2 = "550e8400-e29b-41d4-a716-446655440000";
        String generatedUuid = UUID.randomUUID().toString();

        // When/Then: Should return true for all valid UUIDs
        assertTrue(UuidValidator.isValidUuid(validUuid1));
        assertTrue(UuidValidator.isValidUuid(validUuid2));
        assertTrue(UuidValidator.isValidUuid(generatedUuid));
    }

    @Test
    @DisplayName("isValidUuid should return false for invalid UUIDs")
    void testIsValidUuidWithInvalidUuid() {
        // Given: Invalid UUIDs
        String nullUuid = null;
        String emptyUuid = "";
        String shortUuid = "123e4567-e89b-12d3-a456";
        String longUuid = "123e4567-e89b-12d3-a456-426614174000-extra";
        String invalidFormat = "not-a-uuid-at-all";

        // When/Then: Should return false for all invalid UUIDs
        assertFalse(UuidValidator.isValidUuid(nullUuid));
        assertFalse(UuidValidator.isValidUuid(emptyUuid));
        assertFalse(UuidValidator.isValidUuid(shortUuid));
        assertFalse(UuidValidator.isValidUuid(longUuid));
        assertFalse(UuidValidator.isValidUuid(invalidFormat));
    }

    @Test
    @DisplayName("isValidUuidFormat should return true for valid UUID format")
    void testIsValidUuidFormatWithValidFormat() {
        // Given: Valid UUID formats
        String validFormat1 = "123e4567-e89b-12d3-a456-426614174000";
        String validFormat2 = "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE";
        String validFormat3 = "00000000-0000-0000-0000-000000000000";
        String mixedCase = "123E4567-e89b-12D3-a456-426614174000";

        // When/Then: Should return true for all valid formats
        assertTrue(UuidValidator.isValidUuidFormat(validFormat1));
        assertTrue(UuidValidator.isValidUuidFormat(validFormat2));
        assertTrue(UuidValidator.isValidUuidFormat(validFormat3));
        assertTrue(UuidValidator.isValidUuidFormat(mixedCase));
    }

    @Test
    @DisplayName("isValidUuidFormat should return false for invalid format")
    void testIsValidUuidFormatWithInvalidFormat() {
        // Given: Invalid UUID formats
        String nullUuid = null;
        String emptyUuid = "";
        String wrongSeparator = "123e4567_e89b_12d3_a456_426614174000";
        String invalidChar = "123e4567-e89b-12d3-a456-42661417400g";
        String wrongLength = "123e4567-e89b-12d3-a456-42661417400";
        String noSeparators = "123e4567e89b12d3a456426614174000";

        // When/Then: Should return false for all invalid formats
        assertFalse(UuidValidator.isValidUuidFormat(nullUuid));
        assertFalse(UuidValidator.isValidUuidFormat(emptyUuid));
        assertFalse(UuidValidator.isValidUuidFormat(wrongSeparator));
        assertFalse(UuidValidator.isValidUuidFormat(invalidChar));
        assertFalse(UuidValidator.isValidUuidFormat(wrongLength));
        assertFalse(UuidValidator.isValidUuidFormat(noSeparators));
    }

    @Test
    @DisplayName("isValidUuidV4 should return true for valid UUID version 4")
    void testIsValidUuidV4WithValidV4() {
        // Given: Valid UUID version 4 formats
        String validV4_1 = "123e4567-e89b-42d3-a456-426614174000"; // Version 4, variant 10
        String validV4_2 = "550e8400-e29b-41d4-9716-446655440000"; // Version 4, variant 10
        String validV4_3 = "6ba7b810-9dad-41d1-b85e-c8b035762c5a"; // Version 4, variant 10

        // When/Then: Should return true for valid version 4 UUIDs
        assertTrue(UuidValidator.isValidUuidV4(validV4_1));
        assertTrue(UuidValidator.isValidUuidV4(validV4_2));
        assertTrue(UuidValidator.isValidUuidV4(validV4_3));
    }

    @Test
    @DisplayName("isValidUuidV4 should return false for non-version 4 UUIDs")
    void testIsValidUuidV4WithNonV4() {
        // Given: Non-version 4 UUIDs
        String nullUuid = null;
        String version1 = "123e4567-e89b-12d3-a456-426614174000"; // Version 1
        String version3 = "123e4567-e89b-32d3-a456-426614174000"; // Version 3
        String version5 = "123e4567-e89b-52d3-a456-426614174000"; // Version 5
        String invalidFormat = "not-a-uuid";

        // When/Then: Should return false for non-version 4 UUIDs
        assertFalse(UuidValidator.isValidUuidV4(nullUuid));
        assertFalse(UuidValidator.isValidUuidV4(version1));
        assertFalse(UuidValidator.isValidUuidV4(version3));
        assertFalse(UuidValidator.isValidUuidV4(version5));
        assertFalse(UuidValidator.isValidUuidV4(invalidFormat));
    }

    @Test
    @DisplayName("validateUuid should not throw exception for valid UUID")
    void testValidateUuidWithValidUuid() {
        // Given: Valid UUID
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> UuidValidator.validateUuid(validUuid, "testField"));
    }

    @Test
    @DisplayName("validateUuid should throw ValidationException for null UUID")
    void testValidateUuidWithNullUuid() {
        // Given: Null UUID
        String nullUuid = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> UuidValidator.validateUuid(nullUuid, "testField"));
        
        assertEquals("testField cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("validateUuid should throw ValidationException for invalid UUID")
    void testValidateUuidWithInvalidUuid() {
        // Given: Invalid UUID
        String invalidUuid = "not-a-valid-uuid";

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> UuidValidator.validateUuid(invalidUuid, "testField"));
        
        assertEquals("testField must be a valid UUID", exception.getMessage());
    }

    @Test
    @DisplayName("validateUuidV4 should not throw exception for valid UUID version 4")
    void testValidateUuidV4WithValidV4() {
        // Given: Valid UUID version 4
        String validV4 = "123e4567-e89b-42d3-a456-426614174000";

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> UuidValidator.validateUuidV4(validV4, "testField"));
    }

    @Test
    @DisplayName("validateUuidV4 should throw ValidationException for null UUID")
    void testValidateUuidV4WithNullUuid() {
        // Given: Null UUID
        String nullUuid = null;

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> UuidValidator.validateUuidV4(nullUuid, "testField"));
        
        assertEquals("testField cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("validateUuidV4 should throw ValidationException for non-version 4 UUID")
    void testValidateUuidV4WithNonV4() {
        // Given: Non-version 4 UUID
        String nonV4Uuid = "123e4567-e89b-12d3-a456-426614174000"; // Version 1

        // When/Then: Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class,
            () -> UuidValidator.validateUuidV4(nonV4Uuid, "testField"));
        
        assertEquals("testField must be a valid UUID version 4", exception.getMessage());
    }

    @Test
    @DisplayName("getUuidVersion should return correct version for valid UUIDs")
    void testGetUuidVersionWithValidUuids() {
        // Given: UUIDs of different versions
        String version1 = "123e4567-e89b-12d3-a456-426614174000";
        String version3 = "123e4567-e89b-32d3-a456-426614174000";
        String version4 = "123e4567-e89b-42d3-a456-426614174000";
        String version5 = "123e4567-e89b-52d3-a456-426614174000";

        // When/Then: Should return correct version numbers
        assertEquals(1, UuidValidator.getUuidVersion(version1));
        assertEquals(3, UuidValidator.getUuidVersion(version3));
        assertEquals(4, UuidValidator.getUuidVersion(version4));
        assertEquals(5, UuidValidator.getUuidVersion(version5));
    }

    @Test
    @DisplayName("getUuidVersion should return -1 for invalid UUIDs")
    void testGetUuidVersionWithInvalidUuids() {
        // Given: Invalid UUIDs
        String nullUuid = null;
        String invalidUuid = "not-a-uuid";
        String emptyUuid = "";

        // When/Then: Should return -1 for invalid UUIDs
        assertEquals(-1, UuidValidator.getUuidVersion(nullUuid));
        assertEquals(-1, UuidValidator.getUuidVersion(invalidUuid));
        assertEquals(-1, UuidValidator.getUuidVersion(emptyUuid));
    }

    @Test
    @DisplayName("getUuidVersion should work with generated UUIDs")
    void testGetUuidVersionWithGeneratedUuids() {
        // Given: Generated UUIDs (should be version 4)
        for (int i = 0; i < 10; i++) {
            String generatedUuid = UUID.randomUUID().toString();
            
            // When/Then: Should return version 4
            assertEquals(4, UuidValidator.getUuidVersion(generatedUuid));
        }
    }

    @Test
    @DisplayName("private constructor should throw UnsupportedOperationException")
    void testPrivateConstructorThrowsException() throws NoSuchMethodException {
        // Given: Private constructor
        Constructor<UuidValidator> constructor = UuidValidator.class.getDeclaredConstructor();
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
        assertTrue(java.lang.reflect.Modifier.isFinal(UuidValidator.class.getModifiers()));
    }

    @Test
    @DisplayName("all methods should be static")
    void testAllMethodsAreStatic() {
        // Given: All public methods
        java.lang.reflect.Method[] methods = UuidValidator.class.getDeclaredMethods();
        
        // When/Then: All public methods should be static
        for (java.lang.reflect.Method method : methods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()), 
                    "Method " + method.getName() + " should be static");
            }
        }
    }

    @Test
    @DisplayName("validation methods should work with different field names")
    void testValidationMethodsWithDifferentFieldNames() {
        // Given: Valid UUID and different field names
        String validUuid = "123e4567-e89b-42d3-a456-426614174000";
        String invalidUuid = "invalid";

        // When/Then: Should include field name in exception message
        ValidationException exception1 = assertThrows(ValidationException.class,
            () -> UuidValidator.validateUuid(invalidUuid, "userId"));
        assertEquals("userId must be a valid UUID", exception1.getMessage());

        ValidationException exception2 = assertThrows(ValidationException.class,
            () -> UuidValidator.validateUuidV4(invalidUuid, "entityId"));
        assertEquals("entityId must be a valid UUID version 4", exception2.getMessage());

        ValidationException exception3 = assertThrows(ValidationException.class,
            () -> UuidValidator.validateUuid(null, "customField"));
        assertEquals("customField cannot be null", exception3.getMessage());
    }

    @Test
    @DisplayName("regex patterns should handle edge cases")
    void testRegexPatternsEdgeCases() {
        // Given: Edge case UUIDs
        String allZeros = "00000000-0000-4000-8000-000000000000"; // Valid V4
        String allFs = "ffffffff-ffff-4fff-bfff-ffffffffffff"; // Valid V4
        String mixedCase = "123E4567-e89B-42D3-A456-426614174000"; // Valid V4

        // When/Then: Should handle edge cases correctly
        assertTrue(UuidValidator.isValidUuidFormat(allZeros));
        assertTrue(UuidValidator.isValidUuidFormat(allFs));
        assertTrue(UuidValidator.isValidUuidFormat(mixedCase));
        
        assertTrue(UuidValidator.isValidUuidV4(allZeros));
        assertTrue(UuidValidator.isValidUuidV4(allFs));
        assertTrue(UuidValidator.isValidUuidV4(mixedCase));
    }

    @Test
    @DisplayName("methods should handle whitespace correctly")
    void testMethodsWithWhitespace() {
        // Given: UUIDs with whitespace
        String withSpaces = " 123e4567-e89b-42d3-a456-426614174000 ";
        String withTabs = "\t123e4567-e89b-42d3-a456-426614174000\t";

        // When/Then: Should return false (whitespace makes them invalid)
        assertFalse(UuidValidator.isValidUuid(withSpaces));
        assertFalse(UuidValidator.isValidUuid(withTabs));
        assertFalse(UuidValidator.isValidUuidFormat(withSpaces));
        assertFalse(UuidValidator.isValidUuidFormat(withTabs));
        assertFalse(UuidValidator.isValidUuidV4(withSpaces));
        assertFalse(UuidValidator.isValidUuidV4(withTabs));
    }

    @Test
    @DisplayName("methods should be consistent with each other")
    void testMethodConsistency() {
        // Given: Various UUIDs
        String validV4 = "123e4567-e89b-42d3-a456-426614174000";
        String validV1 = "123e4567-e89b-12d3-a456-426614174000";
        String invalidUuid = "not-a-uuid";

        // When/Then: Methods should be consistent
        // Valid V4 should pass all format checks
        assertTrue(UuidValidator.isValidUuid(validV4));
        assertTrue(UuidValidator.isValidUuidFormat(validV4));
        assertTrue(UuidValidator.isValidUuidV4(validV4));

        // Valid V1 should pass basic and format checks but not V4 check
        assertTrue(UuidValidator.isValidUuid(validV1));
        assertTrue(UuidValidator.isValidUuidFormat(validV1));
        assertFalse(UuidValidator.isValidUuidV4(validV1));

        // Invalid UUID should fail all checks
        assertFalse(UuidValidator.isValidUuid(invalidUuid));
        assertFalse(UuidValidator.isValidUuidFormat(invalidUuid));
        assertFalse(UuidValidator.isValidUuidV4(invalidUuid));
    }
}