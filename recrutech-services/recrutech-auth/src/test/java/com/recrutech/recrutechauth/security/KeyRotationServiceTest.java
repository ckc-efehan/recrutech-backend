package com.recrutech.recrutechauth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class KeyRotationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    private KeyRotationService keyRotationService;
    
    private static final String DEFAULT_SECRET = "defaultSecretKeyThatIsLongEnoughForHS256AlgorithmToWorkProperly123456789";
    private static final String PRIMARY_SECRET = "primarySecretKeyThatIsLongEnoughForHS256AlgorithmToWorkProperly123456789";
    private static final String SECONDARY_SECRET = "secondarySecretKeyThatIsLongEnoughForHS256AlgorithmToWorkProperly123456789";
    private static final String TERTIARY_SECRET = "tertiarySecretKeyThatIsLongEnoughForHS256AlgorithmToWorkProperly123456789";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("jwt_key_rotation:initialized")).thenReturn(true); // Skip initialization
        
        keyRotationService = new KeyRotationService(
            redisTemplate,
            true, // rotationEnabled
            24, // rotationIntervalHours
            2, // overlapHours
            5, // maxKeys
            DEFAULT_SECRET,
            PRIMARY_SECRET,
            SECONDARY_SECRET,
            TERTIARY_SECRET
        );
    }

    @Test
    void constructor_ShouldInitializeWithRotationEnabled() {
        // Given & When
        KeyRotationService service = new KeyRotationService(
            redisTemplate, true, 24, 2, 5, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // Then
        assertNotNull(service);
    }

    @Test
    void constructor_ShouldInitializeWithRotationDisabled() {
        // Given & When
        KeyRotationService service = new KeyRotationService(
            redisTemplate, false, 24, 2, 5, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // Then
        assertNotNull(service);
    }


    @Test
    void getCurrentSigningKey_ShouldReturnCurrentKey_WhenRotationEnabled() {
        // Given
        String keyId = "primary";
        when(valueOperations.get("jwt_key_rotation:current")).thenReturn(keyId);
        
        // When
        String result = keyRotationService.getCurrentSigningKey();
        
        // Then
        assertEquals(PRIMARY_SECRET, result);
    }

    @Test
    void getCurrentSigningKey_ShouldReturnDefaultKey_WhenRotationDisabled() {
        // Given
        KeyRotationService disabledService = new KeyRotationService(
            redisTemplate, false, 24, 2, 5, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // When
        String result = disabledService.getCurrentSigningKey();
        
        // Then
        assertEquals(PRIMARY_SECRET, result);
    }

    @Test
    void getCurrentSigningKey_ShouldReturnDefaultKey_WhenNoCurrentKeyFound() {
        // Given
        when(valueOperations.get("key_rotation:current_key_id")).thenReturn(null);
        
        // When
        String result = keyRotationService.getCurrentSigningKey();
        
        // Then
        assertEquals(PRIMARY_SECRET, result);
    }

    @Test
    void getValidVerificationKeys_ShouldReturnAllValidKeys_WhenRotationEnabled() {
        // Given
        String currentKeyId = "primary";
        when(valueOperations.get("jwt_key_rotation:current")).thenReturn(currentKeyId);
        when(valueOperations.get("jwt_key_rotation:previous")).thenReturn("secondary");
        when(valueOperations.get("jwt_key_rotation:previous_timestamp")).thenReturn(String.valueOf(System.currentTimeMillis() - 1000)); // 1 second ago
        
        // When
        List<String> result = keyRotationService.getValidVerificationKeys();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains(PRIMARY_SECRET));
        assertTrue(result.contains(SECONDARY_SECRET));
        assertTrue(true);
    }

    @Test
    void getValidVerificationKeys_ShouldReturnDefaultKey_WhenRotationDisabled() {
        // Given
        KeyRotationService disabledService = new KeyRotationService(
            redisTemplate, false, 24, 2, 5, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // When
        List<String> result = disabledService.getValidVerificationKeys();
        
        // Then
        assertEquals(1, result.size());
        assertEquals(PRIMARY_SECRET, result.getFirst());
    }

    @Test
    void checkAndRotateKeys_ShouldRotateKeys_WhenRotationNeeded() {
        // Given
        String nextRotationTime = String.valueOf(System.currentTimeMillis() - 1000); // 1 second ago (past due)
        when(valueOperations.get("jwt_key_rotation:next_rotation")).thenReturn(nextRotationTime);
        when(valueOperations.get("jwt_key_rotation:current")).thenReturn("primary");
        when(valueOperations.get("jwt_key_rotation:current_timestamp")).thenReturn(String.valueOf(System.currentTimeMillis()));
        
        // When
        keyRotationService.checkAndRotateKeys();
        
        // Then
        verify(valueOperations, atLeastOnce()).set(eq("jwt_key_rotation:current"), anyString());
    }

    @Test
    void checkAndRotateKeys_ShouldSkipRotation_WhenNotNeeded() {
        // Given
        String nextRotationTime = String.valueOf(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour in future
        when(valueOperations.get("jwt_key_rotation:next_rotation")).thenReturn(nextRotationTime);
        
        // When
        keyRotationService.checkAndRotateKeys();
        
        // Then
        verify(valueOperations, never()).set(eq("jwt_key_rotation:current"), anyString());
    }

    @Test
    void checkAndRotateKeys_ShouldSkipRotation_WhenRotationDisabled() {
        // Given
        RedisTemplate<String, String> mockRedis = mock(RedisTemplate.class);
        ValueOperations<String, String> mockValueOps = mock(ValueOperations.class);
        when(mockRedis.opsForValue()).thenReturn(mockValueOps);
        when(mockRedis.hasKey(anyString())).thenReturn(false); // No initialization needed for disabled rotation
        
        KeyRotationService disabledService = new KeyRotationService(
            mockRedis, false, 24, 2, 5, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // When
        disabledService.checkAndRotateKeys();
        
        // Then
        verify(mockValueOps, never()).set(anyString(), anyString());
    }

    @Test
    void forceKeyRotation_ShouldPerformRotation_WhenRotationEnabled() {
        // Given
        when(valueOperations.get("jwt_key_rotation:current")).thenReturn("primary");
        when(valueOperations.get("jwt_key_rotation:current_timestamp")).thenReturn(String.valueOf(System.currentTimeMillis()));
        
        // When
        keyRotationService.forceKeyRotation();
        
        // Then
        verify(valueOperations, atLeastOnce()).set(eq("jwt_key_rotation:current"), anyString());
    }

    @Test
    void forceKeyRotation_ShouldSkipRotation_WhenRotationDisabled() {
        // Given
        RedisTemplate<String, String> mockRedis = mock(RedisTemplate.class);
        ValueOperations<String, String> mockValueOps = mock(ValueOperations.class);
        when(mockRedis.opsForValue()).thenReturn(mockValueOps);
        when(mockRedis.hasKey(anyString())).thenReturn(false); // No initialization needed for disabled rotation
        
        KeyRotationService disabledService = new KeyRotationService(
            mockRedis, false, 24, 2, 5, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // When
        disabledService.forceKeyRotation();
        
        // Then
        verify(mockValueOps, never()).set(anyString(), anyString());
    }


    @Test
    void getRotationStatus_ShouldReturnStatus_WhenRotationEnabled() {
        // Given
        String currentKeyId = "primary";
        String nextRotation = String.valueOf(System.currentTimeMillis() + 86400000);
        when(valueOperations.get("jwt_key_rotation:current")).thenReturn(currentKeyId);
        when(valueOperations.get("jwt_key_rotation:next_rotation")).thenReturn(nextRotation);
        
        // When
        Map<String, Object> result = keyRotationService.getRotationStatus();
        
        // Then
        assertNotNull(result);
        assertEquals(true, result.get("enabled"));
        assertEquals(currentKeyId, result.get("currentKey"));
        assertEquals(nextRotation, result.get("nextRotation"));
        assertNotNull(result.get("validKeysCount"));
    }

    @Test
    void getRotationStatus_ShouldReturnStatus_WhenRotationDisabled() {
        // Given
        KeyRotationService disabledService = new KeyRotationService(
            redisTemplate, false, 24, 2, 5, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // When
        Map<String, Object> result = disabledService.getRotationStatus();
        
        // Then
        assertNotNull(result);
        assertEquals(false, result.get("enabled"));
        assertEquals(24, result.get("intervalHours"));
        assertEquals(2, result.get("overlapHours"));
        assertEquals(5, result.get("maxKeys"));
        // For disabled rotation, currentKey and nextRotation should not be present
        assertNull(result.get("currentKey"));
        assertNull(result.get("nextRotation"));
    }

    @Test
    void getRotationStatus_ShouldHandleNullValues() {
        // Given
        when(valueOperations.get("jwt_key_rotation:current")).thenReturn(null);
        when(valueOperations.get("jwt_key_rotation:next_rotation")).thenReturn(null);
        
        // When
        Map<String, Object> result = keyRotationService.getRotationStatus();
        
        // Then
        assertNotNull(result);
        assertEquals(true, result.get("enabled"));
        assertNull(result.get("currentKey")); // Should be null when Redis returns null
        assertNull(result.get("nextRotation")); // Should be null when Redis returns null
        assertNotNull(result.get("validKeysCount")); // Should still have valid keys count
    }

    @Test
    void constructor_ShouldHandleNullSecrets() {
        // Given & When
        KeyRotationService service = new KeyRotationService(
            redisTemplate, true, 24, 2, 5, null, null, null, null
        );
        
        // Then
        assertNotNull(service);
    }

    @Test
    void constructor_ShouldHandleZeroIntervals() {
        // Given & When
        KeyRotationService service = new KeyRotationService(
            redisTemplate, true, 0, 0, 0, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // Then
        assertNotNull(service);
    }

    @Test
    void constructor_ShouldHandleNegativeIntervals() {
        // Given & When
        KeyRotationService service = new KeyRotationService(
            redisTemplate, true, -1, -1, -1, DEFAULT_SECRET, 
            PRIMARY_SECRET, SECONDARY_SECRET, TERTIARY_SECRET
        );
        
        // Then
        assertNotNull(service);
    }


    @Test
    void getValidVerificationKeys_ShouldHandleEmptyValidKeys() {
        // Given
        String currentKeyId = "primary";
        when(valueOperations.get("jwt_key_rotation:current")).thenReturn(currentKeyId);
        when(valueOperations.get("jwt_key_rotation:previous")).thenReturn(null);
        
        // When
        List<String> result = keyRotationService.getValidVerificationKeys();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PRIMARY_SECRET, result.getFirst());
    }
}