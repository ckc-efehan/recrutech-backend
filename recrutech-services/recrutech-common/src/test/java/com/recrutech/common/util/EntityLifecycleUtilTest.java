package com.recrutech.common.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EntityLifecycleUtil utility class.
 */
class EntityLifecycleUtilTest {

    @Test
    void generateId_ShouldReturnValidUuid() {
        // Act
        String id = EntityLifecycleUtil.generateId();
        
        // Assert
        assertNotNull(id);
        assertEquals(36, id.length());
        assertTrue(id.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }
    
    @Test
    void createTimestamp_ShouldReturnCurrentTime() {
        // Arranges
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        
        // Act
        LocalDateTime timestamp = EntityLifecycleUtil.createTimestamp();
        
        // Assert
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertNotNull(timestamp);
        assertTrue(timestamp.isAfter(before) || timestamp.isEqual(before));
        assertTrue(timestamp.isBefore(after) || timestamp.isEqual(after));
    }
    
    @Test
    void ensureId_WithNullId_ShouldGenerateNewId() {
        // Act
        String id = EntityLifecycleUtil.ensureId(null);
        
        // Assert
        assertNotNull(id);
        assertEquals(36, id.length());
        assertTrue(id.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }
    
    @Test
    void ensureId_WithExistingId_ShouldReturnSameId() {
        // Arrange
        String existingId = "123e4567-e89b-12d3-a456-426614174000";
        
        // Act
        String id = EntityLifecycleUtil.ensureId(existingId);
        
        // Assert
        assertEquals(existingId, id);
    }
    
    @Test
    void ensureTimestamp_WithNullTimestamp_ShouldGenerateNewTimestamp() {
        // Arrange
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        
        // Act
        LocalDateTime timestamp = EntityLifecycleUtil.ensureTimestamp(null);
        
        // Assert
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertNotNull(timestamp);
        assertTrue(timestamp.isAfter(before) || timestamp.isEqual(before));
        assertTrue(timestamp.isBefore(after) || timestamp.isEqual(after));
    }
    
    @Test
    void ensureTimestamp_WithExistingTimestamp_ShouldReturnSameTimestamp() {
        // Arrange
        LocalDateTime existingTimestamp = LocalDateTime.of(2023, 1, 1, 12, 0);
        
        // Act
        LocalDateTime timestamp = EntityLifecycleUtil.ensureTimestamp(existingTimestamp);
        
        // Assert
        assertEquals(existingTimestamp, timestamp);
    }
}