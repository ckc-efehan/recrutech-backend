package com.recrutech.common.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for BaseEntity class.
 * Tests:
 * - Entity initialization with ID and timestamp generation
 * - Equals and hashCode methods based on ID
 * - Getter and setter functionality
 * - Edge cases with null values
 */
@DisplayName("BaseEntity Tests")
class BaseEntityTest {

    private TestEntity entity1;
    private TestEntity entity2;

    /**
     * Concrete implementation of BaseEntity for testing purposes.
     */
    private static class TestEntity extends BaseEntity {
        // Simple concrete implementation for testing
    }

    @BeforeEach
    void setUp() {
        entity1 = new TestEntity();
        entity2 = new TestEntity();
    }

    @Test
    @DisplayName("initializeEntity should generate ID when null")
    void testInitializeEntityGeneratesId() {
        // Given: Entity with null ID
        assertNull(entity1.getId());
        assertNull(entity1.getCreatedAt());

        // When: Initialize entity
        entity1.initializeEntity();

        // Then: Should have generated ID and timestamp
        assertNotNull(entity1.getId());
        assertNotNull(entity1.getCreatedAt());
        assertEquals(36, entity1.getId().length()); // UUID length
        assertTrue(entity1.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("initializeEntity should preserve existing ID")
    void testInitializeEntityPreservesExistingId() {
        // Given: Entity with existing ID
        String existingId = "existing-id-123";
        entity1.setId(existingId);

        // When: Initialize entity
        entity1.initializeEntity();

        // Then: Should preserve existing ID but generate timestamp
        assertEquals(existingId, entity1.getId());
        assertNotNull(entity1.getCreatedAt());
    }

    @Test
    @DisplayName("initializeEntity should preserve existing timestamp")
    void testInitializeEntityPreservesExistingTimestamp() {
        // Given: Entity with existing timestamp
        LocalDateTime existingTimestamp = LocalDateTime.of(2023, 1, 1, 12, 0);
        entity1.setCreatedAt(existingTimestamp);

        // When: Initialize entity
        entity1.initializeEntity();

        // Then: Should preserve existing timestamp but generate ID
        assertEquals(existingTimestamp, entity1.getCreatedAt());
        assertNotNull(entity1.getId());
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void testEqualsSameInstance() {
        // Given: Same instance
        // When/Then: Should be equal to itself
        assertEquals(entity1, entity1);
        assertTrue(entity1.equals(entity1));
    }

    @Test
    @DisplayName("equals should return true for entities with same ID")
    void testEqualsSameId() {
        // Given: Two entities with same ID
        String sameId = "same-id-123";
        entity1.setId(sameId);
        entity2.setId(sameId);

        // When/Then: Should be equal
        assertEquals(entity1, entity2);
        assertEquals(entity2, entity1);
    }

    @Test
    @DisplayName("equals should return false for entities with different IDs")
    void testEqualsDifferentIds() {
        // Given: Two entities with different IDs
        entity1.setId("id-1");
        entity2.setId("id-2");

        // When/Then: Should not be equal
        assertNotEquals(entity1, entity2);
        assertNotEquals(entity2, entity1);
    }

    @Test
    @DisplayName("equals should return false for null")
    void testEqualsNull() {
        // Given: Entity and null
        entity1.setId("some-id");

        // When/Then: Should not be equal to null
        assertNotEquals(entity1, null);
        assertFalse(entity1.equals(null));
    }

    @Test
    @DisplayName("equals should return false for different class")
    void testEqualsDifferentClass() {
        // Given: Entity and different object type
        entity1.setId("some-id");
        String differentObject = "not an entity";

        // When/Then: Should not be equal
        assertNotEquals(entity1, differentObject);
        assertFalse(entity1.equals(differentObject));
    }

    @Test
    @DisplayName("equals should handle null IDs correctly")
    void testEqualsNullIds() {
        // Given: Two entities with null IDs
        entity1.setId(null);
        entity2.setId(null);

        // When/Then: Should be equal (both have null IDs)
        assertEquals(entity1, entity2);

        // Given: One entity with null ID, one with actual ID
        entity2.setId("some-id");

        // When/Then: Should not be equal
        assertNotEquals(entity1, entity2);
        assertNotEquals(entity2, entity1);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCodeConsistentWithEquals() {
        // Given: Two entities with same ID
        String sameId = "same-id-123";
        entity1.setId(sameId);
        entity2.setId(sameId);

        // When/Then: Should have same hash code
        assertEquals(entity1.hashCode(), entity2.hashCode());

        // Given: Entities with different IDs
        entity2.setId("different-id");

        // When/Then: Should likely have different hash codes
        assertNotEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    @DisplayName("hashCode should handle null ID")
    void testHashCodeNullId() {
        // Given: Entity with null ID
        entity1.setId(null);

        // When/Then: Should not throw exception
        assertDoesNotThrow(() -> entity1.hashCode());
        
        // Hash code should be consistent
        int hashCode1 = entity1.hashCode();
        int hashCode2 = entity1.hashCode();
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    @DisplayName("getters and setters should work correctly")
    void testGettersAndSetters() {
        // Given: Test values
        String testId = "test-id-123";
        LocalDateTime testTimestamp = LocalDateTime.of(2023, 6, 15, 10, 30);

        // When: Set values
        entity1.setId(testId);
        entity1.setCreatedAt(testTimestamp);

        // Then: Should retrieve correct values
        assertEquals(testId, entity1.getId());
        assertEquals(testTimestamp, entity1.getCreatedAt());
    }

    @Test
    @DisplayName("entity should handle null values gracefully")
    void testNullValues() {
        // Given: Entity with null values
        entity1.setId(null);
        entity1.setCreatedAt(null);

        // When/Then: Should handle null values without exceptions
        assertNull(entity1.getId());
        assertNull(entity1.getCreatedAt());
        assertDoesNotThrow(() -> entity1.hashCode());
        assertDoesNotThrow(() -> entity1.equals(entity2));
    }
}