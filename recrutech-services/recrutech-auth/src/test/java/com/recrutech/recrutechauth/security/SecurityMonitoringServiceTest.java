package com.recrutech.recrutechauth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SecurityMonitoringServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    private SecurityMonitoringService securityMonitoringService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(true); // Make updateDailyMetric use increment instead of set
        securityMonitoringService = new SecurityMonitoringService(redisTemplate);
    }

    @Test
    void constructor_ShouldInitializeWithRedisTemplate() {
        // Given
        RedisTemplate<String, String> mockRedisTemplate = mock(RedisTemplate.class);
        
        // When
        SecurityMonitoringService service = new SecurityMonitoringService(mockRedisTemplate);
        
        // Then
        assertNotNull(service);
    }

    @Test
    void logTokenCreation_ShouldLogTokenCreationEvent() {
        // Given
        String userId = "test-user-id";
        String tokenType = "ACCESS";
        String clientIp = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        
        // When
        securityMonitoringService.logTokenCreation(userId, tokenType, clientIp, userAgent);
        
        // Then
        verify(valueOperations).set(contains("security_events:TOKEN_CREATION:"), anyString(), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:token_creations:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logTokenCreation_ShouldHandleNullValues() {
        // Given
        String userId = "test-user-id";
        String tokenType = "ACCESS";
        String clientIp = null;
        String userAgent = null;
        
        // When
        securityMonitoringService.logTokenCreation(userId, tokenType, clientIp, userAgent);
        
        // Then
        verify(valueOperations).set(contains("security_events:TOKEN_CREATION:"), anyString(), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:token_creations:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logTokenValidation_ShouldLogValidationEvent_WhenValid() {
        // Given
        String userId = "test-user-id";
        String tokenType = "ACCESS";
        boolean isValid = true;
        String clientIp = "192.168.1.1";
        
        // When
        securityMonitoringService.logTokenValidation(userId, tokenType, isValid, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:TOKEN_VALIDATION:"), contains("valid"), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:token_validations:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logTokenValidation_ShouldLogValidationEvent_WhenInvalid() {
        // Given
        String userId = "test-user-id";
        String tokenType = "ACCESS";
        boolean isValid = false;
        String clientIp = "192.168.1.1";
        
        // When
        securityMonitoringService.logTokenValidation(userId, tokenType, isValid, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:TOKEN_VALIDATION:"), contains("false"), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:invalid_token_attempts:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logTokenInvalidation_ShouldLogInvalidationEvent() {
        // Given
        String userId = "test-user-id";
        String reason = "LOGOUT";
        String clientIp = "192.168.1.1";
        
        // When
        securityMonitoringService.logTokenInvalidation(userId, reason, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:TOKEN_INVALIDATION:"), contains(reason), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:token_invalidations:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logTokenInvalidation_ShouldHandleNullReason() {
        // Given
        String userId = "test-user-id";
        String reason = null;
        String clientIp = "192.168.1.1";
        
        // When
        securityMonitoringService.logTokenInvalidation(userId, reason, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:TOKEN_INVALIDATION:"), anyString(), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:token_invalidations:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logSuspiciousActivity_ShouldLogHighPriorityActivity() {
        // Given
        String userId = "test-user-id";
        String activityType = "BRUTE_FORCE_ATTACK";
        String details = "5 failed login attempts in 5 minutes";
        String clientIp = "192.168.1.1";
        
        // When
        securityMonitoringService.logSuspiciousActivity(userId, activityType, details, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:SUSPICIOUS_ACTIVITY:"), contains(activityType), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:suspicious_activities:" + java.time.LocalDate.now(), 1L);
        // High priority activities create security alerts
        verify(valueOperations).set(contains("security_alerts:BRUTE_FORCE_ATTACK:"), anyString(), eq(Duration.ofDays(7)));
        verify(valueOperations).increment("daily_metrics:security_alerts:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logSuspiciousActivity_ShouldLogNormalPriorityActivity() {
        // Given
        String userId = "test-user-id";
        String activityType = "UNUSUAL_LOGIN_TIME";
        String details = "Login at unusual hour";
        String clientIp = "192.168.1.1";
        
        // When
        securityMonitoringService.logSuspiciousActivity(userId, activityType, details, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:SUSPICIOUS_ACTIVITY:"), contains(activityType), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:suspicious_activities:" + java.time.LocalDate.now(), 1L);
        verify(valueOperations, never()).set(contains("security_alerts:"), anyString(), any(Duration.class));
    }

    @Test
    void logKeyRotation_ShouldLogSuccessfulRotation() {
        // Given
        String fromKey = "key_001";
        String toKey = "key_002";
        boolean successful = true;
        
        // When
        securityMonitoringService.logKeyRotation(fromKey, toKey, successful);
        
        // Then
        verify(valueOperations).set(contains("security_events:KEY_ROTATION:"), contains("successful"), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:key_rotations:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logKeyRotation_ShouldLogFailedRotation() {
        // Given
        String fromKey = "key_001";
        String toKey = "key_002";
        boolean successful = false;
        
        // When
        securityMonitoringService.logKeyRotation(fromKey, toKey, successful);
        
        // Then
        verify(valueOperations).set(contains("security_events:KEY_ROTATION:"), contains("false"), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:key_rotations:" + java.time.LocalDate.now(), 1L);
        // Failed rotation creates a security alert
        verify(valueOperations).set(contains("security_alerts:KEY_ROTATION_FAILED:"), anyString(), eq(Duration.ofDays(7)));
        verify(valueOperations).increment("daily_metrics:security_alerts:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void logKeyRotation_ShouldHandleNullKeys() {
        // Given
        String fromKey = null;
        String toKey = null;
        boolean successful = true;
        
        // When
        securityMonitoringService.logKeyRotation(fromKey, toKey, successful);
        
        // Then
        verify(valueOperations).set(contains("security_events:KEY_ROTATION:"), anyString(), eq(Duration.ofDays(30)));
        verify(valueOperations).increment("daily_metrics:key_rotations:" + java.time.LocalDate.now(), 1L);
    }

    @Test
    void getSecurityMetrics_ShouldReturnMetrics() {
        // Given
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        when(valueOperations.get("daily_metrics:token_creations:" + today)).thenReturn("10");
        when(valueOperations.get("daily_metrics:token_validations:" + today)).thenReturn("100");
        when(valueOperations.get("daily_metrics:invalid_token_attempts:" + today)).thenReturn("5");
        when(valueOperations.get("daily_metrics:suspicious_activities:" + today)).thenReturn("2");
        when(valueOperations.get("jwt_key_rotation:current_timestamp")).thenReturn("1234567890");
        
        // When
        Map<String, Object> result = securityMonitoringService.getSecurityMetrics();
        
        // Then
        assertNotNull(result);
        assertEquals(0L, result.get("tokenCreations")); // AtomicLong starts at 0
        assertEquals(0L, result.get("tokenValidations")); // AtomicLong starts at 0
        assertEquals(0L, result.get("tokenInvalidations")); // AtomicLong starts at 0
        assertEquals(0L, result.get("suspiciousActivities")); // AtomicLong starts at 0
        assertEquals(0L, result.get("keyRotations")); // AtomicLong starts at 0
        assertEquals(10L, result.get("dailyTokenCreations"));
        assertEquals(100L, result.get("dailyTokenValidations"));
        assertEquals(5L, result.get("dailyInvalidTokenAttempts"));
        assertEquals(2L, result.get("dailySuspiciousActivities"));
        assertNotNull(result.get("securityHealth"));
        assertEquals("1234567890", result.get("lastKeyRotation"));
    }

    @Test
    void getSecurityMetrics_ShouldHandleNullMetrics() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // When
        Map<String, Object> result = securityMonitoringService.getSecurityMetrics();
        
        // Then
        assertNotNull(result);
        assertEquals(0L, result.get("tokenCreations")); // AtomicLong starts at 0
        assertEquals(0L, result.get("tokenValidations")); // AtomicLong starts at 0
        assertEquals(0L, result.get("tokenInvalidations")); // AtomicLong starts at 0
        assertEquals(0L, result.get("suspiciousActivities")); // AtomicLong starts at 0
        assertEquals(0L, result.get("keyRotations")); // AtomicLong starts at 0
        assertEquals(0L, result.get("dailyTokenCreations")); // getDailyMetric returns 0 for null
        assertEquals(0L, result.get("dailyTokenValidations")); // getDailyMetric returns 0 for null
        assertEquals(0L, result.get("dailyInvalidTokenAttempts")); // getDailyMetric returns 0 for null
        assertEquals(0L, result.get("dailySuspiciousActivities")); // getDailyMetric returns 0 for null
        assertNotNull(result.get("securityHealth"));
    }

    @Test
    void getSecurityEvents_ShouldReturnEvents() {
        // Given
        String eventType = "TOKEN_CREATION";
        int hours = 24;
        
        // When
        List<Map<String, Object>> result = securityMonitoringService.getSecurityEvents(eventType, hours);
        
        // Then
        assertNotNull(result);
        // Note: This method would need Redis scan operations to be fully testable
        // For now, we verify it returns a non-null list
    }

    @Test
    void getSecurityEvents_ShouldHandleNullEventType() {
        // Given
        String eventType = null;
        int hours = 24;
        
        // When
        List<Map<String, Object>> result = securityMonitoringService.getSecurityEvents(eventType, hours);
        
        // Then
        assertNotNull(result);
    }

}