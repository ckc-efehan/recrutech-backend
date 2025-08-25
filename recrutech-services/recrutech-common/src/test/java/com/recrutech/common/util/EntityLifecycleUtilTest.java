package com.recrutech.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for EntityLifecycleUtil class.
 * Tests:
 * - ID generation methods
 * - Timestamp creation methods
 * - Ensure methods (ID and timestamp)
 * - Private constructor throws exception
 * - Edge cases with null values
 */
@DisplayName("EntityLifecycleUtil Tests")
class EntityLifecycleUtilTest {

    @Test
    @DisplayName("generateId should return valid UUID")
    void testGenerateId() {
        // When: Generate ID
        String id = EntityLifecycleUtil.generateId();

        // Then: Should return valid UUID
        assertNotNull(id);
        assertEquals(36, id.length()); // UUID length with hyphens
        assertTrue(id.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }

    @Test
    @DisplayName("generateId should return different UUIDs on multiple calls")
    void testGenerateIdUniqueness() {
        // When: Generate multiple IDs
        String id1 = EntityLifecycleUtil.generateId();
        String id2 = EntityLifecycleUtil.generateId();
        String id3 = EntityLifecycleUtil.generateId();

        // Then: Should all be different
        assertNotEquals(id1, id2);
        assertNotEquals(id2, id3);
        assertNotEquals(id1, id3);
    }

    @Test
    @DisplayName("createTimestamp should return current timestamp")
    void testCreateTimestamp() {
        // Given: Current time before call
        LocalDateTime before = LocalDateTime.now();

        // When: Create timestamp
        LocalDateTime timestamp = EntityLifecycleUtil.createTimestamp();

        // Given: Current time after call
        LocalDateTime after = LocalDateTime.now();

        // Then: Should be between before and after
        assertNotNull(timestamp);
        assertTrue(timestamp.isAfter(before.minusSeconds(1)) || timestamp.isEqual(before.minusSeconds(1)));
        assertTrue(timestamp.isBefore(after.plusSeconds(1)) || timestamp.isEqual(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("ensureId should return existing ID when not null")
    void testEnsureIdWithExistingId() {
        // Given: Existing ID
        String existingId = "existing-id-123";

        // When: Ensure ID
        String result = EntityLifecycleUtil.ensureId(existingId);

        // Then: Should return the same ID
        assertEquals(existingId, result);
    }

    @Test
    @DisplayName("ensureId should generate new ID when null")
    void testEnsureIdWithNullId() {
        // When: Ensure ID
        String result = EntityLifecycleUtil.ensureId(null);

        // Then: Should generate new valid UUID
        assertNotNull(result);
        assertEquals(36, result.length());
        assertTrue(result.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }

    @Test
    @DisplayName("ensureId should handle empty string as existing ID")
    void testEnsureIdWithEmptyString() {
        // Given: Empty string ID
        String emptyId = "";

        // When: Ensure ID
        String result = EntityLifecycleUtil.ensureId(emptyId);

        // Then: Should return the empty string (not null, so preserved)
        assertEquals(emptyId, result);
    }

    @Test
    @DisplayName("ensureTimestamp should return existing timestamp when not null")
    void testEnsureTimestampWithExistingTimestamp() {
        // Given: Existing timestamp
        LocalDateTime existingTimestamp = LocalDateTime.of(2023, 6, 15, 10, 30, 45);

        // When: Ensure timestamp
        LocalDateTime result = EntityLifecycleUtil.ensureTimestamp(existingTimestamp);

        // Then: Should return the same timestamp
        assertEquals(existingTimestamp, result);
    }

    @Test
    @DisplayName("ensureTimestamp should generate new timestamp when null")
    void testEnsureTimestampWithNullTimestamp() {
        // Given: Null timestamp
        LocalDateTime before = LocalDateTime.now();

        // When: Ensure timestamp
        LocalDateTime result = EntityLifecycleUtil.ensureTimestamp(null);

        // Given: Current time after call
        LocalDateTime after = LocalDateTime.now();

        // Then: Should generate new current timestamp
        assertNotNull(result);
        assertTrue(result.isAfter(before.minusSeconds(1)) || result.isEqual(before.minusSeconds(1)));
        assertTrue(result.isBefore(after.plusSeconds(1)) || result.isEqual(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("private constructor should throw UnsupportedOperationException")
    void testPrivateConstructorThrowsException() throws NoSuchMethodException {
        // Given: Private constructor
        Constructor<EntityLifecycleUtil> constructor = EntityLifecycleUtil.class.getDeclaredConstructor();
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
        assertTrue(java.lang.reflect.Modifier.isFinal(EntityLifecycleUtil.class.getModifiers()));
    }

    @Test
    @DisplayName("all methods should be static")
    void testAllMethodsAreStatic() {
        // Given: All public methods
        java.lang.reflect.Method[] methods = EntityLifecycleUtil.class.getDeclaredMethods();
        
        // When/Then: All public methods should be static
        for (java.lang.reflect.Method method : methods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()), 
                    "Method " + method.getName() + " should be static");
            }
        }
    }

    @Test
    @DisplayName("ensureId and ensureTimestamp should work together")
    void testEnsureMethodsTogether() {
        // Given: Null values

        // When: Ensure both
        String resultId = EntityLifecycleUtil.ensureId(null);
        LocalDateTime resultTimestamp = EntityLifecycleUtil.ensureTimestamp(null);

        // Then: Both should be generated
        assertNotNull(resultId);
        assertNotNull(resultTimestamp);
        assertEquals(36, resultId.length());
        assertTrue(resultTimestamp.isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("methods should handle multiple rapid calls correctly")
    void testRapidCalls() {
        // When: Make rapid calls
        String id1 = EntityLifecycleUtil.generateId();
        String id2 = EntityLifecycleUtil.generateId();
        LocalDateTime time1 = EntityLifecycleUtil.createTimestamp();
        LocalDateTime time2 = EntityLifecycleUtil.createTimestamp();

        // Then: Should handle rapid calls without issues
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotNull(time1);
        assertNotNull(time2);
        assertNotEquals(id1, id2); // UUIDs should be different
        // Timestamps might be the same due to rapid execution, which is acceptable
    }
}