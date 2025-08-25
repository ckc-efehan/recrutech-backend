package com.recrutech.recrutechauth.security;

import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.model.UserRole;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SecurityServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private SetOperations<String, String> setOperations;

    private SecurityService securityService;
    private User testUser;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        securityService = new SecurityService(redisTemplate);
        
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.APPLICANT);
        testUser.setEnabled(true);
        testUser.setFailedLoginAttempts(0);
    }

    @Test
    void isRateLimited_ShouldReturnFalse_WhenFirstAttempt() {
        // Given
        String identifier = "192.168.1.1";
        when(valueOperations.get("rate_limit:" + identifier)).thenReturn(null);
        
        // When
        boolean result = securityService.isRateLimited(identifier);
        
        // Then
        assertFalse(result);
        verify(valueOperations).set(eq("rate_limit:" + identifier), eq("1"), eq(Duration.ofMinutes(15)));
    }

    @Test
    void isRateLimited_ShouldReturnFalse_WhenUnderLimit() {
        // Given
        String identifier = "192.168.1.1";
        when(valueOperations.get("rate_limit:" + identifier)).thenReturn("3");
        
        // When
        boolean result = securityService.isRateLimited(identifier);
        
        // Then
        assertFalse(result);
        verify(valueOperations).increment("rate_limit:" + identifier);
    }

    @Test
    void isRateLimited_ShouldReturnTrue_WhenOverLimit() {
        // Given
        String identifier = "192.168.1.1";
        when(valueOperations.get("rate_limit:" + identifier)).thenReturn("5");
        
        // When
        boolean result = securityService.isRateLimited(identifier);
        
        // Then
        assertTrue(result);
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void clearRateLimit_ShouldDeleteRateLimitKey() {
        // Given
        String identifier = "192.168.1.1";
        
        // When
        securityService.clearRateLimit(identifier);
        
        // Then
        verify(redisTemplate).delete("rate_limit:" + identifier);
    }

    @Test
    void recordFailedAttempt_ShouldIncrementFailedAttempts() {
        // Given
        testUser.setFailedLoginAttempts(2);
        
        // When
        securityService.recordFailedAttempt(testUser);
        
        // Then
        assertEquals(3, testUser.getFailedLoginAttempts());
        assertNotNull(testUser.getLastFailedLogin());
        assertNull(testUser.getAccountLockedUntil()); // Not locked yet
    }

    @Test
    void recordFailedAttempt_ShouldLockAccount_WhenMaxAttemptsReached() {
        // Given
        testUser.setFailedLoginAttempts(4); // One less than max
        
        // When
        securityService.recordFailedAttempt(testUser);
        
        // Then
        assertEquals(5, testUser.getFailedLoginAttempts());
        assertNotNull(testUser.getLastFailedLogin());
        assertNotNull(testUser.getAccountLockedUntil());
        assertTrue(testUser.getAccountLockedUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    void clearFailedAttempts_ShouldResetFailedAttempts() {
        // Given
        testUser.setFailedLoginAttempts(3);
        testUser.setLastFailedLogin(LocalDateTime.now());
        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
        
        // When
        securityService.clearFailedAttempts(testUser);
        
        // Then
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertNull(testUser.getLastFailedLogin());
        assertNull(testUser.getAccountLockedUntil());
    }

    @Test
    void isSuspiciousActivity_ShouldReturnFalse_WhenNormalActivity() {
        // Given
        String userId = "test-user-id";
        String clientIp = "192.168.1.1";
        when(setOperations.members("recent_ips:" + userId)).thenReturn(Set.of("192.168.1.1"));
        when(valueOperations.get("request_count:" + userId)).thenReturn(null);
        
        // When
        boolean result = securityService.isSuspiciousActivity(userId, clientIp);
        
        // Then
        assertFalse(result);
        verify(setOperations).add("recent_ips:" + userId, clientIp);
        verify(redisTemplate).expire("recent_ips:" + userId, Duration.ofHours(1));
        verify(valueOperations).set("request_count:" + userId, "1", Duration.ofMinutes(5));
    }

    @Test
    void isSuspiciousActivity_ShouldReturnTrue_WhenTooManyIPs() {
        // Given
        String userId = "test-user-id";
        String clientIp = "192.168.1.1";
        Set<String> manyIps = Set.of("192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4");
        when(setOperations.members("recent_ips:" + userId)).thenReturn(manyIps);
        
        // When
        boolean result = securityService.isSuspiciousActivity(userId, clientIp);
        
        // Then
        assertTrue(result);
    }

    @Test
    void isSuspiciousActivity_ShouldReturnTrue_WhenTooManyRequests() {
        // Given
        String userId = "test-user-id";
        String clientIp = "192.168.1.1";
        when(setOperations.members("recent_ips:" + userId)).thenReturn(Set.of("192.168.1.1"));
        when(valueOperations.get("request_count:" + userId)).thenReturn("51");
        
        // When
        boolean result = securityService.isSuspiciousActivity(userId, clientIp);
        
        // Then
        assertTrue(result);
        verify(valueOperations).increment("request_count:" + userId);
    }

    @Test
    void isSuspiciousActivity_ShouldReturnFalse_WhenRequestCountUnderLimit() {
        // Given
        String userId = "test-user-id";
        String clientIp = "192.168.1.1";
        when(setOperations.members("recent_ips:" + userId)).thenReturn(Set.of("192.168.1.1"));
        when(valueOperations.get("request_count:" + userId)).thenReturn("25");
        
        // When
        boolean result = securityService.isSuspiciousActivity(userId, clientIp);
        
        // Then
        assertFalse(result);
        verify(valueOperations).increment("request_count:" + userId);
    }

    @Test
    void generateDeviceFingerprint_ShouldGenerateConsistentFingerprint() {
        // Given
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        String acceptLanguage = "en-US,en;q=0.9";
        String clientIp = "192.168.1.1";
        
        // When
        String fingerprint1 = securityService.generateDeviceFingerprint(userAgent, acceptLanguage, clientIp);
        String fingerprint2 = securityService.generateDeviceFingerprint(userAgent, acceptLanguage, clientIp);
        
        // Then
        assertNotNull(fingerprint1);
        assertNotNull(fingerprint2);
        assertEquals(fingerprint1, fingerprint2);
    }

    @Test
    void generateDeviceFingerprint_ShouldGenerateDifferentFingerprints_ForDifferentInputs() {
        // Given
        String userAgent1 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        String userAgent2 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
        String acceptLanguage = "en-US,en;q=0.9";
        String clientIp = "192.168.1.1";
        
        // When
        String fingerprint1 = securityService.generateDeviceFingerprint(userAgent1, acceptLanguage, clientIp);
        String fingerprint2 = securityService.generateDeviceFingerprint(userAgent2, acceptLanguage, clientIp);
        
        // Then
        assertNotNull(fingerprint1);
        assertNotNull(fingerprint2);
        assertNotEquals(fingerprint1, fingerprint2);
    }

    @Test
    void isKnownDevice_ShouldReturnTrue_WhenDeviceIsKnown() {
        // Given
        String userId = "test-user-id";
        String deviceFingerprint = "device123";
        when(setOperations.isMember("known_devices:" + userId, deviceFingerprint)).thenReturn(true);
        
        // When
        boolean result = securityService.isKnownDevice(userId, deviceFingerprint);
        
        // Then
        assertTrue(result);
    }

    @Test
    void isKnownDevice_ShouldReturnFalse_WhenDeviceIsUnknown() {
        // Given
        String userId = "test-user-id";
        String deviceFingerprint = "device123";
        when(setOperations.isMember("known_devices:" + userId, deviceFingerprint)).thenReturn(false);
        
        // When
        boolean result = securityService.isKnownDevice(userId, deviceFingerprint);
        
        // Then
        assertFalse(result);
    }

    @Test
    void registerDevice_ShouldAddDeviceToKnownDevices() {
        // Given
        String userId = "test-user-id";
        String deviceFingerprint = "device123";
        
        // When
        securityService.registerDevice(userId, deviceFingerprint);
        
        // Then
        verify(setOperations).add("known_devices:" + userId, deviceFingerprint);
        verify(redisTemplate).expire("known_devices:" + userId, Duration.ofDays(90));
    }

    @Test
    void removeActiveSession_ShouldRemoveSessionFromActiveSet() {
        // Given
        String userId = "test-user-id";
        String sessionId = "session123";
        
        // When
        securityService.removeActiveSession(userId, sessionId);
        
        // Then
        verify(setOperations).remove("active_sessions:" + userId, sessionId);
    }

    @Test
    void logSecurityEvent_ShouldStoreSecurityEvent() {
        // Given
        String userId = "test-user-id";
        String event = "LOGIN_ATTEMPT";
        String details = "Failed login from suspicious IP";
        String clientIp = "192.168.1.1";
        
        // When
        securityService.logSecurityEvent(userId, event, details, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:" + userId), contains(event), eq(Duration.ofDays(30)));
        verify(valueOperations).set(contains("security_events:" + userId), contains(details), eq(Duration.ofDays(30)));
        verify(valueOperations).set(contains("security_events:" + userId), contains(clientIp), eq(Duration.ofDays(30)));
    }

    @Test
    void logSecurityEvent_ShouldHandleNullValues() {
        // Given
        String userId = "test-user-id";
        String event = "LOGIN_ATTEMPT";
        String details = null;
        String clientIp = null;
        
        // When
        securityService.logSecurityEvent(userId, event, details, clientIp);
        
        // Then
        verify(valueOperations).set(contains("security_events:" + userId), anyString(), eq(Duration.ofDays(30)));
    }

    @Test
    void constructor_ShouldInitializeWithRedisTemplate() {
        // Given
        RedisTemplate<String, String> mockRedisTemplate = mock(RedisTemplate.class);
        
        // When
        SecurityService service = new SecurityService(mockRedisTemplate);
        
        // Then
        assertNotNull(service);
    }

    @Test
    void recordFailedAttempt_ShouldHandleNullUser() {
        // Given
        User nullUser = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            securityService.recordFailedAttempt(nullUser));
    }

    @Test
    void clearFailedAttempts_ShouldHandleNullUser() {
        // Given
        User nullUser = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            securityService.clearFailedAttempts(nullUser));
    }

    @Test
    void generateDeviceFingerprint_ShouldHandleNullInputs() {
        // Given
        String userAgent = null;
        String acceptLanguage = null;
        String clientIp = null;
        
        // When
        String result = securityService.generateDeviceFingerprint(userAgent, acceptLanguage, clientIp);
        
        // Then
        assertNotNull(result);
        assertEquals("null|null|null".hashCode(), Integer.parseInt(result));
    }

    @Test
    void isSuspiciousActivity_ShouldHandleNullRedisResponse() {
        // Given
        String userId = "test-user-id";
        String clientIp = "192.168.1.1";
        when(setOperations.members("recent_ips:" + userId)).thenReturn(null);
        when(valueOperations.get("request_count:" + userId)).thenReturn(null);
        
        // When
        boolean result = securityService.isSuspiciousActivity(userId, clientIp);
        
        // Then
        assertFalse(result);
    }
}