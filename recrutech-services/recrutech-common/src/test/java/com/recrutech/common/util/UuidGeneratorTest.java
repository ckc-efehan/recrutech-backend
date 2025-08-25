package com.recrutech.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for UuidGenerator class.
 * Tests:
 * - UUID generation methods
 * - UUID validation methods
 * - Private constructor throws exception
 * - Edge cases with invalid UUIDs
 * - Utility class structure validation
 */
@DisplayName("UuidGenerator Tests")
class UuidGeneratorTest {

    @Test
    @DisplayName("generateUuid should return valid UUID")
    void testGenerateUuid() {
        // When: Generate UUID
        String uuid = UuidGenerator.generateUuid();

        // Then: Should return valid UUID
        assertNotNull(uuid);
        assertEquals(36, uuid.length()); // UUID length with hyphens
        assertTrue(uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
        
        // Should be parseable as UUID
        assertDoesNotThrow(() -> UUID.fromString(uuid));
    }

    @Test
    @DisplayName("generateUuid should return different UUIDs on multiple calls")
    void testGenerateUuidUniqueness() {
        // When: Generate multiple UUIDs
        String uuid1 = UuidGenerator.generateUuid();
        String uuid2 = UuidGenerator.generateUuid();
        String uuid3 = UuidGenerator.generateUuid();

        // Then: Should all be different
        assertNotEquals(uuid1, uuid2);
        assertNotEquals(uuid2, uuid3);
        assertNotEquals(uuid1, uuid3);
    }

    @Test
    @DisplayName("isValidUuid should return true for valid UUID")
    void testIsValidUuidWithValidUuid() {
        // Given: Valid UUID
        String validUuid = "123e4567-e89b-12d3-a456-426614174000";

        // When/Then: Should return true
        assertTrue(UuidGenerator.isValidUuid(validUuid));
    }

    @Test
    @DisplayName("isValidUuid should return true for generated UUID")
    void testIsValidUuidWithGeneratedUuid() {
        // Given: Generated UUID
        String generatedUuid = UuidGenerator.generateUuid();

        // When/Then: Should return true
        assertTrue(UuidGenerator.isValidUuid(generatedUuid));
    }

    @Test
    @DisplayName("isValidUuid should return false for null")
    void testIsValidUuidWithNull() {
        assertFalse(UuidGenerator.isValidUuid(null));
    }

    @Test
    @DisplayName("isValidUuid should return false for empty string")
    void testIsValidUuidWithEmptyString() {
        // Given: Empty string
        String emptyUuid = "";

        // When/Then: Should return false
        assertFalse(UuidGenerator.isValidUuid(emptyUuid));
    }

    @Test
    @DisplayName("isValidUuid should return false for too short string")
    void testIsValidUuidWithTooShortString() {
        // Given: Too short string
        String shortUuid = "123e4567-e89b-12d3-a456";

        // When/Then: Should return false
        assertFalse(UuidGenerator.isValidUuid(shortUuid));
    }

    @Test
    @DisplayName("isValidUuid should return false for too long string")
    void testIsValidUuidWithTooLongString() {
        // Given: Too long string
        String longUuid = "123e4567-e89b-12d3-a456-426614174000-extra";

        // When/Then: Should return false
        assertFalse(UuidGenerator.isValidUuid(longUuid));
    }

    @Test
    @DisplayName("isValidUuid should return false for invalid format")
    void testIsValidUuidWithInvalidFormat() {
        // Given: Invalid format strings
        String invalidFormat1 = "123e4567_e89b_12d3_a456_426614174000"; // Underscores instead of hyphens
        String invalidFormat2 = "123e4567-e89b-12d3-a456-42661417400g"; // Invalid character 'g'
        String invalidFormat3 = "not-a-uuid-at-all-just-random-text"; // Random text

        // When/Then: Should return false for all
        assertFalse(UuidGenerator.isValidUuid(invalidFormat1));
        assertFalse(UuidGenerator.isValidUuid(invalidFormat2));
        assertFalse(UuidGenerator.isValidUuid(invalidFormat3));
    }

    @Test
    @DisplayName("isValidUuid should return false for correct length but invalid format")
    void testIsValidUuidWithCorrectLengthButInvalidFormat() {
        // Given: 36 character string but invalid format
        String invalidUuid = "123456789012345678901234567890123456"; // 36 chars but no hyphens

        // When/Then: Should return false
        assertFalse(UuidGenerator.isValidUuid(invalidUuid));
    }

    @Test
    @DisplayName("isValidUuid should handle uppercase and lowercase")
    void testIsValidUuidWithDifferentCases() {
        // Given: UUIDs with different cases
        String uppercaseUuid = "123E4567-E89B-12D3-A456-426614174000";
        String lowercaseUuid = "123e4567-e89b-12d3-a456-426614174000";
        String mixedCaseUuid = "123E4567-e89b-12D3-a456-426614174000";

        // When/Then: Should return true for all valid formats
        assertTrue(UuidGenerator.isValidUuid(uppercaseUuid));
        assertTrue(UuidGenerator.isValidUuid(lowercaseUuid));
        assertTrue(UuidGenerator.isValidUuid(mixedCaseUuid));
    }

    @Test
    @DisplayName("private constructor should throw UnsupportedOperationException")
    void testPrivateConstructorThrowsException() throws NoSuchMethodException {
        // Given: Private constructor
        Constructor<UuidGenerator> constructor = UuidGenerator.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When/Then: Should throw UnsupportedOperationException
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, 
            constructor::newInstance);

        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("This is a utility class and cannot be instantiated", 
            exception.getCause().getMessage());
    }

    @Test
    @DisplayName("utility class should be final")
    void testUtilityClassIsFinal() {
        // When/Then: Class should be final
        assertTrue(java.lang.reflect.Modifier.isFinal(UuidGenerator.class.getModifiers()));
    }

    @Test
    @DisplayName("all methods should be static")
    void testAllMethodsAreStatic() {
        // Given: All public methods
        java.lang.reflect.Method[] methods = UuidGenerator.class.getDeclaredMethods();
        
        // When/Then: All public methods should be static
        for (java.lang.reflect.Method method : methods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()), 
                    "Method " + method.getName() + " should be static");
            }
        }
    }

    @Test
    @DisplayName("isValidUuid should handle edge case with special characters")
    void testIsValidUuidWithSpecialCharacters() {
        // Given: Strings with special characters
        String specialChars1 = "123e4567-e89b-12d3-a456-42661417400@";
        String specialChars2 = "123e4567-e89b-12d3-a456-42661417400#";
        String specialChars3 = "123e4567-e89b-12d3-a456-42661417400$";

        // When/Then: Should return false for all
        assertFalse(UuidGenerator.isValidUuid(specialChars1));
        assertFalse(UuidGenerator.isValidUuid(specialChars2));
        assertFalse(UuidGenerator.isValidUuid(specialChars3));
    }

    @Test
    @DisplayName("isValidUuid should handle whitespace")
    void testIsValidUuidWithWhitespace() {
        // Given: UUIDs with whitespace
        String withSpaces = " 123e4567-e89b-12d3-a456-426614174000 ";
        String withTabs = "\t123e4567-e89b-12d3-a456-426614174000\t";
        String withNewlines = "\n123e4567-e89b-12d3-a456-426614174000\n";

        // When/Then: Should return false (whitespace makes them invalid)
        assertFalse(UuidGenerator.isValidUuid(withSpaces));
        assertFalse(UuidGenerator.isValidUuid(withTabs));
        assertFalse(UuidGenerator.isValidUuid(withNewlines));
    }

    @Test
    @DisplayName("generateUuid should produce version 4 UUIDs")
    void testGenerateUuidProducesVersion4() {
        // When: Generate multiple UUIDs
        for (int i = 0; i < 10; i++) {
            String uuid = UuidGenerator.generateUuid();
            UUID parsedUuid = UUID.fromString(uuid);
            
            // Then: Should be version 4 (random)
            assertEquals(4, parsedUuid.version(), "Generated UUID should be version 4");
        }
    }

    @Test
    @DisplayName("methods should work correctly under concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        // Given: Multiple threads
        int threadCount = 10;
        int uuidsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        String[][] results = new String[threadCount][uuidsPerThread];

        // When: Generate UUIDs concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < uuidsPerThread; j++) {
                    results[threadIndex][j] = UuidGenerator.generateUuid();
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then: All UUIDs should be valid and unique
        java.util.Set<String> allUuids = new java.util.HashSet<>();
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < uuidsPerThread; j++) {
                String uuid = results[i][j];
                assertNotNull(uuid);
                assertTrue(UuidGenerator.isValidUuid(uuid));
                assertTrue(allUuids.add(uuid), "UUID should be unique: " + uuid);
            }
        }

        assertEquals(threadCount * uuidsPerThread, allUuids.size(), "All UUIDs should be unique");
    }
}