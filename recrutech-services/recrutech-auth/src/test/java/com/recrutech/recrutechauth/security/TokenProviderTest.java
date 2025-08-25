package com.recrutech.recrutechauth.security;

import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.model.UserRole;
import com.recrutech.recrutechauth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TokenProviderTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private SetOperations<String, String> setOperations;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private KeyRotationService keyRotationService;
    
    @Mock
    private SecurityMonitoringService securityMonitoringService;

    private TokenProvider tokenProvider;
    private User testUser;
    
    private static final String TEST_SECRET = "testSecretKeyThatIsLongEnoughForHS256AlgorithmToWorkProperly123456789";
    private static final long ACCESS_TOKEN_VALIDITY = 900000; // 15 minutes
    private static final long REFRESH_TOKEN_VALIDITY = 86400000; // 24 hours
    private static final String TEST_ISSUER = "recrutech-auth";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(keyRotationService.getCurrentSigningKey()).thenReturn(TEST_SECRET);
        when(keyRotationService.getValidVerificationKeys()).thenReturn(java.util.List.of(TEST_SECRET));
        
        tokenProvider = new TokenProvider(
            TEST_SECRET,
            ACCESS_TOKEN_VALIDITY,
            REFRESH_TOKEN_VALIDITY,
            TEST_ISSUER,
            redisTemplate,
            userRepository,
            keyRotationService,
            securityMonitoringService
        );
        
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.APPLICANT);
        testUser.setEnabled(true);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @Test
    void createOrGetTokenPair_ShouldCreateNewTokenPair_WhenNoExistingToken() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        when(valueOperations.get("session:" + sessionId)).thenReturn(null);
        
        // When
        TokenPair result = tokenProvider.createOrGetTokenPair(testUser, sessionId, clientIp);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        verify(securityMonitoringService).logTokenCreation(testUser.getId(), "ACCESS", clientIp, "TokenProvider");
        verify(securityMonitoringService).logTokenCreation(testUser.getId(), "REFRESH", clientIp, "TokenProvider");
    }

    @Test
    void createOrGetTokenPair_ShouldReturnExistingTokenPair_WhenValidTokenExists() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair existingTokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        String tokenPairJson = String.format("{\"accessToken\":\"%s\",\"refreshToken\":\"%s\"}", 
            existingTokenPair.accessToken(), existingTokenPair.refreshToken());
        when(valueOperations.get("session_token:" + sessionId)).thenReturn(tokenPairJson);
        
        // When
        TokenPair result = tokenProvider.createOrGetTokenPair(testUser, sessionId, clientIp);
        
        // Then
        assertNotNull(result);
        assertEquals(existingTokenPair.accessToken(), result.accessToken());
        assertEquals(existingTokenPair.refreshToken(), result.refreshToken());
    }

    @Test
    void createTokenPair_ShouldCreateValidTokenPair() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        
        // When
        TokenPair result = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        
        // Verify tokens are valid
        assertTrue(tokenProvider.isTokenValid(result.accessToken()));
        assertEquals(testUser.getId(), tokenProvider.getUserIdFromToken(result.accessToken()));
    }

    @Test
    void refreshToken_ShouldCreateNewTokenPair_WhenValidRefreshToken() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair originalTokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(valueOperations.get(anyString())).thenReturn(null); // Token not blacklisted
        
        // When
        TokenPair result = tokenProvider.refreshToken(originalTokenPair.refreshToken(), clientIp);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        assertNotEquals(originalTokenPair.accessToken(), result.accessToken());
        verify(securityMonitoringService, times(2)).logTokenCreation(testUser.getId(), "ACCESS", clientIp, "TokenProvider");
        verify(securityMonitoringService, times(2)).logTokenCreation(testUser.getId(), "REFRESH", clientIp, "TokenProvider");
    }

    @Test
    void refreshToken_ShouldThrowException_WhenUserNotFound() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair originalTokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            tokenProvider.refreshToken(originalTokenPair.refreshToken(), clientIp));
    }

    @Test
    void getUserIdFromToken_ShouldReturnUserId_WhenValidToken() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        
        // When
        String result = tokenProvider.getUserIdFromToken(tokenPair.accessToken());
        
        // Then
        assertEquals(testUser.getId(), result);
    }

    @Test
    void getUserIdFromToken_ShouldReturnNull_WhenInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        String result = tokenProvider.getUserIdFromToken(invalidToken);
        
        // Then
        assertNull(result);
    }

    @Test
    void blacklistToken_ShouldBlacklistToken() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        
        // When
        tokenProvider.blacklistToken(tokenPair.accessToken());
        
        // Then
        verify(valueOperations).set(contains("blacklist:"), eq("true"), any(Duration.class));
    }

    @Test
    void blacklistTokenById_ShouldBlacklistTokenById() {
        // Given
        String jti = "test-jti";
        
        // When
        tokenProvider.blacklistTokenById(jti);
        
        // Then
        verify(valueOperations).set(eq("blacklist:" + jti), eq("true"), any(Duration.class));
    }

    @Test
    void invalidateSession_ShouldInvalidateSession() {
        // Given
        String sessionId = "test-session";
        
        // When
        tokenProvider.invalidateSession(sessionId);
        
        // Then
        verify(redisTemplate).delete("session:" + sessionId);
    }

    @Test
    void invalidateAllTokens_ShouldInvalidateAllTokens() {
        // When
        tokenProvider.invalidateAllTokens();
        
        // Then
        verify(valueOperations).set(eq("global_token_invalidation"), anyString());
    }

    @Test
    void invalidateAllUserTokens_ShouldInvalidateUserTokens() {
        // Given
        String userId = "test-user-id";
        when(redisTemplate.keys("session:*")).thenReturn(Set.of("session:session1", "session:session2"));
        when(valueOperations.get("session:session1")).thenReturn(userId);
        when(valueOperations.get("session:session2")).thenReturn(userId);
        
        // When
        tokenProvider.invalidateAllUserTokens(userId);
        
        // Then
        verify(valueOperations).set(eq("user_token_invalidation:" + userId), anyString(), any(Duration.class));
        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    void isTokenGloballyInvalidated_ShouldReturnTrue_WhenGloballyInvalidated() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        when(valueOperations.get("global_token_invalidation")).thenReturn(String.valueOf(System.currentTimeMillis() + 1000));
        
        // When
        boolean result = tokenProvider.isTokenGloballyInvalidated(tokenPair.accessToken());
        
        // Then
        assertTrue(result);
    }

    @Test
    void isTokenGloballyInvalidated_ShouldReturnFalse_WhenNotGloballyInvalidated() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        when(valueOperations.get("global_token_invalidation")).thenReturn(null);
        when(valueOperations.get("user_token_invalidation:" + testUser.getId())).thenReturn(null);
        
        // When
        boolean result = tokenProvider.isTokenGloballyInvalidated(tokenPair.accessToken());
        
        // Then
        assertFalse(result);
    }

    @Test
    void generateSecureToken_ShouldGenerateUniqueTokens() {
        // When
        String token1 = tokenProvider.generateSecureToken();
        String token2 = tokenProvider.generateSecureToken();
        
        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertEquals(44, token1.length());
        assertEquals(44, token2.length());
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenExpiredToken() {
        // Given - Create a token provider with very short validity
        TokenProvider shortLivedTokenProvider = new TokenProvider(
            TEST_SECRET, 1, 1, TEST_ISSUER, redisTemplate, userRepository, 
            keyRotationService, securityMonitoringService
        );
        TokenPair tokenPair = shortLivedTokenProvider.createTokenPair(testUser, "session", "127.0.0.1");
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When & Then
        assertFalse(tokenProvider.isTokenValid(tokenPair.accessToken()));
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenValid() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        when(valueOperations.get(anyString())).thenReturn(null); // Not blacklisted or invalidated
        
        // When
        boolean result = tokenProvider.isTokenValid(tokenPair.accessToken());
        
        // Then
        assertTrue(result);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenBlacklisted() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        
        // Blacklist the token first
        tokenProvider.blacklistToken(tokenPair.accessToken());
        
        // Mock hasKey to return true for blacklist check
        when(redisTemplate.hasKey(startsWith("blacklist:"))).thenReturn(true);
        when(valueOperations.get("global_token_invalidation")).thenReturn(null);
        when(valueOperations.get("user_token_invalidation:" + testUser.getId())).thenReturn(null);
        
        // When
        boolean result = tokenProvider.isTokenValid(tokenPair.accessToken());
        
        // Then
        assertFalse(result);
    }

    @Test
    void isTokenValidWithClientIp_ShouldReturnTrue_WhenValidTokenAndIp() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        
        // When
        boolean result = tokenProvider.isTokenValid(tokenPair.accessToken(), clientIp);
        
        // Then
        assertTrue(result);
        verify(securityMonitoringService).logTokenValidation(testUser.getId(), "ACCESS", true, clientIp);
    }

    @Test
    void isTokenValidWithClientIp_ShouldReturnTrue_WhenUserNotFoundButTokenValid() {
        // Given
        String sessionId = "test-session";
        String clientIp = "192.168.1.1";
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());
        
        // When
        boolean result = tokenProvider.isTokenValid(tokenPair.accessToken(), clientIp);
        
        // Then
        assertTrue(result); // Token validation doesn't check user existence
        verify(securityMonitoringService).logTokenValidation(testUser.getId(), "ACCESS", true, clientIp);
    }

    @Test
    void isSessionValid_ShouldReturnTrue_WhenSessionExists() {
        // Given
        String sessionId = "test-session";
        when(redisTemplate.hasKey("session:" + sessionId)).thenReturn(true);
        
        // When
        boolean result = tokenProvider.isSessionValid(sessionId);
        
        // Then
        assertTrue(result);
    }

    @Test
    void isSessionValid_ShouldReturnFalse_WhenSessionNotExists() {
        // Given
        String sessionId = "test-session";
        when(redisTemplate.hasKey("session:" + sessionId)).thenReturn(false);
        
        // When
        boolean result = tokenProvider.isSessionValid(sessionId);
        
        // Then
        assertFalse(result);
    }

}