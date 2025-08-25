package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.gdpr.ProcessingActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuditLogServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private SetOperations<String, String> setOperations;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        auditLogService = new AuditLogService(redisTemplate);
    }

    @Test
    void constructor_ShouldInitializeWithRedisTemplate() {
        // Given
        RedisTemplate<String, String> mockRedisTemplate = mock(RedisTemplate.class);
        
        // When
        AuditLogService service = new AuditLogService(mockRedisTemplate);
        
        // Then
        assertNotNull(service);
    }

    @Test
    void logDataProcessing_ShouldLogActivity_WithBasicParameters() {
        // Given
        String userId = "test-user-id";
        String activityType = "LOGIN";
        String description = "User logged in successfully";
        String additionalDetails = "Additional login details";
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }

    @Test
    void logDataProcessing_ShouldLogActivity_WithExtendedParameters() {
        // Given
        String userId = "test-user-id";
        String activityType = "LOGIN";
        String description = "User logged in successfully";
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        String additionalDetails = "Additional login details";
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, ipAddress, userAgent, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }

    @Test
    void logDataProcessing_ShouldHandleNullValues_BasicParameters() {
        // Given
        String userId = "test-user-id";
        String activityType = "LOGIN";
        String description = null;
        String additionalDetails = null;
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }

    @Test
    void logDataProcessing_ShouldHandleNullValues_ExtendedParameters() {
        // Given
        String userId = "test-user-id";
        String activityType = "LOGIN";
        String description = "User logged in successfully";
        String ipAddress = null;
        String userAgent = null;
        String additionalDetails = null;
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, ipAddress, userAgent, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }

    @Test
    void logDataProcessing_ShouldHandleEmptyStrings() {
        // Given
        String userId = "test-user-id";
        String activityType = "";
        String description = "";
        String additionalDetails = "";
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }

    @Test
    void getProcessingActivities_ShouldReturnActivities_WhenActivitiesExist() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of(
            "activity1",
            "activity2"
        ));
        when(valueOperations.get("audit:activity1")).thenReturn(
            "{\"activityId\":\"activity1\",\"userId\":\"test-user-id\",\"activityType\":\"LOGIN\",\"description\":\"User login\",\"timestamp\":\"2023-12-01T10:00:00\",\"ipAddress\":\"192.168.1.1\",\"userAgent\":\"Mozilla/5.0\",\"legalBasis\":\"Legitimate Interest\",\"dataCategories\":\"Authentication Data\",\"processingPurpose\":\"User Authentication\",\"retentionPeriod\":\"7 years\",\"additionalDetails\":\"Login successful\"}"
        );
        when(valueOperations.get("audit:activity2")).thenReturn(
            "{\"activityId\":\"activity2\",\"userId\":\"test-user-id\",\"activityType\":\"LOGOUT\",\"description\":\"User logout\",\"timestamp\":\"2023-12-01T11:00:00\",\"ipAddress\":\"192.168.1.1\",\"userAgent\":\"Mozilla/5.0\",\"legalBasis\":\"Legitimate Interest\",\"dataCategories\":\"Authentication Data\",\"processingPurpose\":\"User Authentication\",\"retentionPeriod\":\"7 years\",\"additionalDetails\":\"Logout successful\"}"
        );
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        // Activities are sorted by timestamp (newest first), so activity2 (11:00) comes before activity1 (10:00)
        assertEquals("activity2", result.get(0).activityId());
        assertEquals("LOGOUT", result.get(0).activityType());
        assertEquals("activity1", result.get(1).activityId());
        assertEquals("LOGIN", result.get(1).activityType());
    }

    @Test
    void getProcessingActivities_ShouldReturnEmptyList_WhenNoActivitiesExist() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of());
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProcessingActivities_ShouldHandleNullKeys() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        when(setOperations.members(userActivitiesKey)).thenReturn(null);
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProcessingActivities_ShouldHandleInvalidActivityData() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of("activity1"));
        when(valueOperations.get("audit:activity1")).thenReturn("invalid-activity-data");
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Invalid data should be filtered out
    }

    @Test
    void getProcessingActivities_WithDateRange_ShouldFilterActivities() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        LocalDateTime from = LocalDateTime.of(2023, 12, 1, 9, 0);
        LocalDateTime to = LocalDateTime.of(2023, 12, 1, 12, 0);
        
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of(
            "activity1",
            "activity2",
            "activity3"
        ));
        when(valueOperations.get("audit:activity1")).thenReturn(
            "{\"activityId\":\"activity1\",\"userId\":\"test-user-id\",\"activityType\":\"LOGIN\",\"description\":\"User login\",\"timestamp\":\"2023-12-01T10:00:00\",\"ipAddress\":\"192.168.1.1\",\"userAgent\":\"Mozilla/5.0\",\"legalBasis\":\"Legitimate Interest\",\"dataCategories\":\"Authentication Data\",\"processingPurpose\":\"User Authentication\",\"retentionPeriod\":\"7 years\",\"additionalDetails\":\"Login successful\"}"
        );
        when(valueOperations.get("audit:activity2")).thenReturn(
            "{\"activityId\":\"activity2\",\"userId\":\"test-user-id\",\"activityType\":\"LOGOUT\",\"description\":\"User logout\",\"timestamp\":\"2023-12-01T11:00:00\",\"ipAddress\":\"192.168.1.1\",\"userAgent\":\"Mozilla/5.0\",\"legalBasis\":\"Legitimate Interest\",\"dataCategories\":\"Authentication Data\",\"processingPurpose\":\"User Authentication\",\"retentionPeriod\":\"7 years\",\"additionalDetails\":\"Logout successful\"}"
        );
        when(valueOperations.get("audit:activity3")).thenReturn(
            "{\"activityId\":\"activity3\",\"userId\":\"test-user-id\",\"activityType\":\"DATA_ACCESS\",\"description\":\"Data access\",\"timestamp\":\"2023-12-01T13:00:00\",\"ipAddress\":\"192.168.1.1\",\"userAgent\":\"Mozilla/5.0\",\"legalBasis\":\"Legitimate Interest\",\"dataCategories\":\"Personal Data\",\"processingPurpose\":\"Data Access\",\"retentionPeriod\":\"7 years\",\"additionalDetails\":\"Data accessed\"}"
        );
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId, from, to);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // Only activities 1 and 2 should be within the date range
        assertTrue(result.stream().anyMatch(a -> a.activityId().equals("activity1")));
        assertTrue(result.stream().anyMatch(a -> a.activityId().equals("activity2")));
    }

    @Test
    void getProcessingActivities_WithDateRange_ShouldHandleNullDates() {
        // Given
        String userId = "test-user-id";
        LocalDateTime from = null;
        LocalDateTime to = null;
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId, from, to);
        
        // Then
        assertNotNull(result);
        // Should return all activities when dates are null
    }

    @Test
    void deleteUserAuditLogs_ShouldDeleteAllUserLogs() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of(
            "activity1",
            "activity2", 
            "activity3"
        ));
        
        // When
        auditLogService.deleteUserAuditLogs(userId);
        
        // Then
        verify(redisTemplate).delete("audit:activity1");
        verify(redisTemplate).delete("audit:activity2");
        verify(redisTemplate).delete("audit:activity3");
        verify(redisTemplate).delete(userActivitiesKey);
    }

    @Test
    void deleteUserAuditLogs_ShouldHandleNoLogs() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of());
        
        // When
        auditLogService.deleteUserAuditLogs(userId);
        
        // Then
        verify(redisTemplate).delete(userActivitiesKey);
        verify(redisTemplate, never()).delete(startsWith("audit:"));
    }

    @Test
    void deleteUserAuditLogs_ShouldHandleNullKeys() {
        // Given
        String userId = "test-user-id";
        String userActivitiesKey = "user_activities:" + userId;
        when(setOperations.members(userActivitiesKey)).thenReturn(null);
        
        // When
        auditLogService.deleteUserAuditLogs(userId);
        
        // Then
        verify(redisTemplate).delete(userActivitiesKey);
        verify(redisTemplate, never()).delete(startsWith("audit:"));
    }

    @Test
    void logDataProcessing_ShouldHandleNullUserId() {
        // Given
        String userId = null;
        String activityType = "LOGIN";
        String description = "User logged in successfully";
        String additionalDetails = "Additional login details";
        
        // When & Then - should not throw exception, just log error
        assertDoesNotThrow(() -> 
            auditLogService.logDataProcessing(userId, activityType, description, additionalDetails));
        
        // Verify no Redis operations were performed
        verify(valueOperations, never()).set(anyString(), anyString());
        verify(setOperations, never()).add(anyString(), anyString());
    }

    @Test
    void logDataProcessing_ShouldHandleEmptyUserId() {
        // Given
        String userId = "";
        String activityType = "LOGIN";
        String description = "User logged in successfully";
        String additionalDetails = "Additional login details";
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }

    @Test
    void getProcessingActivities_ShouldHandleNullUserId() {
        // Given
        String userId = null;
        String userActivitiesKey = "user_activities:null";
        when(setOperations.members(userActivitiesKey)).thenReturn(null);
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId);
        
        // Then - should return empty list and not throw exception
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // Verify Redis operations were performed with null userId
        verify(setOperations).members(userActivitiesKey);
    }

    @Test
    void getProcessingActivities_ShouldHandleEmptyUserId() {
        // Given
        String userId = "";
        String userActivitiesKey = "user_activities:";
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of());
        
        // When
        List<ProcessingActivity> result = auditLogService.getProcessingActivities(userId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteUserAuditLogs_ShouldHandleNullUserId() {
        // Given
        String userId = null;
        String userActivitiesKey = "user_activities:null";
        when(setOperations.members(userActivitiesKey)).thenReturn(null);
        
        // When & Then - should not throw exception, just log error
        assertDoesNotThrow(() -> 
            auditLogService.deleteUserAuditLogs(userId));
        
        // Verify Redis operations were performed with null userId
        verify(setOperations).members(userActivitiesKey);
        verify(redisTemplate).delete(userActivitiesKey);
    }

    @Test
    void deleteUserAuditLogs_ShouldHandleEmptyUserId() {
        // Given
        String userId = "";
        String userActivitiesKey = "user_activities:";
        when(setOperations.members(userActivitiesKey)).thenReturn(Set.of());
        
        // When
        auditLogService.deleteUserAuditLogs(userId);
        
        // Then
        verify(redisTemplate).delete(userActivitiesKey);
        verify(redisTemplate, never()).delete(startsWith("audit:"));
    }

    @Test
    void logDataProcessing_ShouldCreateUniqueActivityIds() {
        // Given
        String userId = "test-user-id";
        String activityType = "LOGIN";
        String description = "User logged in successfully";
        String additionalDetails = "Additional login details";
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        
        // Then
        verify(valueOperations, times(2)).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate, times(2)).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
        // Each call should generate a unique activity ID
    }

    @Test
    void logDataProcessing_ShouldHandleLongStrings() {
        // Given
        String userId = "test-user-id";
        String activityType = "A".repeat(1000); // Very long activity type
        String description = "B".repeat(2000); // Very long description
        String additionalDetails = "C".repeat(3000); // Very long additional details
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }

    @Test
    void logDataProcessing_ShouldHandleSpecialCharacters() {
        // Given
        String userId = "test-user-id";
        String activityType = "LOGIN|SPECIAL";
        String description = "User logged in with special chars: |=&<>";
        String additionalDetails = "Details with pipes | and equals = signs";
        
        // When
        auditLogService.logDataProcessing(userId, activityType, description, additionalDetails);
        
        // Then
        verify(valueOperations).set(startsWith("audit:ACT-"), anyString());
        verify(redisTemplate).expire(startsWith("audit:ACT-"), eq(Duration.ofDays(2555)));
    }
}