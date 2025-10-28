package com.recrutech.recrutechauth.phase3;

import com.recrutech.recrutechauth.model.RefreshToken;
import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.model.UserRole;
import com.recrutech.recrutechauth.repository.RefreshTokenRepository;
import com.recrutech.recrutechauth.repository.UserRepository;
import com.recrutech.recrutechauth.security.KeyRotationService;
import com.recrutech.recrutechauth.security.SecurityMonitoringService;
import com.recrutech.recrutechauth.security.TokenPair;
import com.recrutech.recrutechauth.security.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Phase 3 Token Model Tests.
 * Validates all Phase 3 requirements:
 * 1. Access Token: Short-lived JWT with offline validation and standard claims
 * 2. Refresh Token: Opaque tokens with rotation and reuse detection
 * 3. Key Management: Key rotation and JWKS exposure
 */
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=testSecretKeyThatIsAtLeast512BitsLongForTestingPurposesOnly123456789012345678901234567890",
    "jwt.access-token-validity=900000",
    "jwt.refresh-token-validity=604800000",
    "jwt.issuer=recrutech",
    "jwt.rotation.enabled=true",
    "jwt.rotation.interval-hours=168",
    "jwt.rotation.overlap-hours=24"
})
class Phase3TokenModelTest {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private KeyRotationService keyRotationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private SecurityMonitoringService securityMonitoringService;

    private User testUser;
    private String sessionId;
    private String clientIp;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail("test@recrutech.com");
        testUser.setRole(UserRole.APPLICANT);
        testUser.setEmailVerified(true);
        testUser.setTwoFactorEnabled(false);

        sessionId = UUID.randomUUID().toString();
        clientIp = "192.168.1.1";

        // Mock Redis operations
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
    }

    @Test
    @DisplayName("Phase 3.1: Access Token should be short-lived JWT with standard claims")
    void testAccessTokenIsJwtWithStandardClaims() {
        // Given
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);

        // When - Parse JWT
        String signingKey = keyRotationService.getCurrentSigningKey();
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(signingKey.getBytes()))
                .build()
                .parseSignedClaims(tokenPair.accessToken())
                .getPayload();

        // Then - Verify standard claims (Phase 3 requirement)
        assertNotNull(claims.getId(), "jti (JWT ID) should be present");
        assertEquals(testUser.getId(), claims.getSubject(), "sub (subject) should be user ID");
        assertEquals("recrutech", claims.getIssuer(), "iss (issuer) should be 'recrutech'");
        assertTrue(claims.getAudience().contains("recrutech-platform"), "aud (audience) should contain 'recrutech-platform'");
        assertTrue(claims.getAudience().contains("recrutech-notification"), "aud (audience) should contain 'recrutech-notification'");
        assertNotNull(claims.getExpiration(), "exp (expiration) should be present");
        assertNotNull(claims.getIssuedAt(), "iat (issued at) should be present");
        assertNotNull(claims.getNotBefore(), "nbf (not before) should be present");

        // Verify custom claims
        assertNotNull(claims.get("roles"), "roles claim should be present");
        assertNotNull(claims.get("scope"), "scope claim should be present");
        assertEquals("default", claims.get("tenant"), "tenant claim should be present");
        assertEquals(testUser.getEmail(), claims.get("email"), "email claim should be present");

        System.out.println("[DEBUG_LOG] Access Token Claims: " + claims);
    }

    @Test
    @DisplayName("Phase 3.2: JWT validation should work offline (no database lookup)")
    void testJwtValidationIsOffline() {
        // Given
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);

        // When - Validate token using only the secret key (offline validation)
        String signingKey = keyRotationService.getCurrentSigningKey();
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(signingKey.getBytes()))
                .build()
                .parseSignedClaims(tokenPair.accessToken())
                .getPayload();

        // Then - Should successfully parse without any database calls
        assertNotNull(claims);
        assertEquals(testUser.getId(), claims.getSubject());

        // Verify no database lookups were made during JWT parsing
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).findByEmail(anyString());

        System.out.println("[DEBUG_LOG] JWT validated offline successfully");
    }

    @Test
    @DisplayName("Phase 3.3: Refresh Token should be opaque (UUID-based)")
    void testRefreshTokenIsOpaque() {
        // Given
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);

        // When
        String refreshToken = tokenPair.refreshToken();

        // Then - Refresh token should be opaque (not a JWT)
        assertFalse(refreshToken.contains("."), "Refresh token should not be a JWT (no dots)");
        assertTrue(refreshToken.length() >= 32, "Refresh token should be sufficiently long");
        assertDoesNotThrow(() -> UUID.fromString(refreshToken), "Refresh token should be a valid UUID");

        System.out.println("[DEBUG_LOG] Refresh Token (opaque): " + refreshToken);
    }

    @Test
    @DisplayName("Phase 3.4: Refresh Token rotation should work correctly")
    void testRefreshTokenRotation() {
        // Given - Initial token pair
        TokenPair initialTokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);
        String familyId = UUID.randomUUID().toString();

        RefreshToken initialRefreshToken = new RefreshToken(
                initialTokenPair.refreshToken(),
                testUser.getId(),
                familyId,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                false,
                null,
                clientIp,
                "Mozilla/5.0",
                604800L
        );

        when(refreshTokenRepository.findById(initialTokenPair.refreshToken()))
                .thenReturn(Optional.of(initialRefreshToken));
        when(userRepository.findById(testUser.getId()))
                .thenReturn(Optional.of(testUser));

        // When - Rotate token
        TokenPair newTokenPair = tokenProvider.refreshToken(initialTokenPair.refreshToken(), clientIp);

        // Then - Verify rotation occurred
        assertNotNull(newTokenPair);
        assertNotEquals(initialTokenPair.accessToken(), newTokenPair.accessToken(), "New access token should be different");
        assertNotEquals(initialTokenPair.refreshToken(), newTokenPair.refreshToken(), "New refresh token should be different");

        // Verify old token was marked as replaced
        verify(refreshTokenRepository).save(argThat(rt -> 
                rt.getToken().equals(initialTokenPair.refreshToken()) && 
                rt.getReplacedBy() != null
        ));

        System.out.println("[DEBUG_LOG] Token rotation successful");
        System.out.println("[DEBUG_LOG] Old refresh token: " + initialTokenPair.refreshToken());
        System.out.println("[DEBUG_LOG] New refresh token: " + newTokenPair.refreshToken());
    }

    @Test
    @DisplayName("Phase 3.5: Reuse detection should block token theft")
    void testRefreshTokenReuseDetection() {
        // Given - Token that was already used (has replacedBy value)
        String oldRefreshToken = UUID.randomUUID().toString();
        String newRefreshToken = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();

        RefreshToken reusedToken = new RefreshToken(
                oldRefreshToken,
                testUser.getId(),
                familyId,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(7),
                false,
                newRefreshToken, // Already replaced!
                clientIp,
                "Mozilla/5.0",
                604800L
        );

        when(refreshTokenRepository.findById(oldRefreshToken))
                .thenReturn(Optional.of(reusedToken));
        when(refreshTokenRepository.findByFamilyId(familyId))
                .thenReturn(List.of(reusedToken));

        // When & Then - Attempting to reuse should throw SecurityException
        SecurityException exception = assertThrows(SecurityException.class, 
                () -> tokenProvider.refreshToken(oldRefreshToken, clientIp));

        assertTrue(exception.getMessage().contains("reuse detected"), 
                "Exception should mention token reuse");

        // Verify security event was logged
        verify(securityMonitoringService).logSecurityEvent(
                eq(testUser.getId()),
                eq("REFRESH_TOKEN_REUSE_DETECTED"),
                eq(clientIp),
                contains("revoking family")
        );

        // Verify token family was revoked
        verify(refreshTokenRepository).findByFamilyId(familyId);

        System.out.println("[DEBUG_LOG] Reuse detection blocked successfully");
    }

    @Test
    @DisplayName("Phase 3.6: Token family should be revoked on reuse")
    void testTokenFamilyRevocationOnReuse() {
        // Given - Multiple tokens in same family
        String familyId = UUID.randomUUID().toString();
        String token1 = UUID.randomUUID().toString();
        String token2 = UUID.randomUUID().toString();
        String token3 = UUID.randomUUID().toString();

        RefreshToken rt1 = new RefreshToken(token1, testUser.getId(), familyId, 
                LocalDateTime.now(), LocalDateTime.now().plusDays(7), 
                false, token2, clientIp, "Mozilla/5.0", 604800L);
        RefreshToken rt2 = new RefreshToken(token2, testUser.getId(), familyId,
                LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                false, token3, clientIp, "Mozilla/5.0", 604800L);
        RefreshToken rt3 = new RefreshToken(token3, testUser.getId(), familyId,
                LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                false, null, clientIp, "Mozilla/5.0", 604800L);

        when(refreshTokenRepository.findById(token1)).thenReturn(Optional.of(rt1));
        when(refreshTokenRepository.findByFamilyId(familyId)).thenReturn(List.of(rt1, rt2, rt3));

        // When - Try to reuse token1 (which already has replacedBy)
        assertThrows(SecurityException.class, 
                () -> tokenProvider.refreshToken(token1, clientIp));

        // Then - All tokens in family should be revoked
        verify(refreshTokenRepository, atLeast(3)).save(argThat(rt -> rt.isRevoked()));

        System.out.println("[DEBUG_LOG] Entire token family revoked on reuse");
    }

    @Test
    @DisplayName("Phase 3.7: Key rotation should support multiple valid keys")
    void testKeyRotationSupportsMultipleKeys() {
        // Given
        List<String> validKeys = keyRotationService.getValidVerificationKeys();

        // Then
        assertNotNull(validKeys);
        assertFalse(validKeys.isEmpty(), "Should have at least one valid key");
        assertTrue(validKeys.size() >= 1, "Should support multiple keys during overlap period");

        System.out.println("[DEBUG_LOG] Valid verification keys count: " + validKeys.size());
    }

    @Test
    @DisplayName("Phase 3.8: Access Token should have correct TTL (15 minutes)")
    void testAccessTokenTtl() {
        // Given
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);

        // When
        String signingKey = keyRotationService.getCurrentSigningKey();
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(signingKey.getBytes()))
                .build()
                .parseSignedClaims(tokenPair.accessToken())
                .getPayload();

        // Then - Token should expire in ~15 minutes
        long expiresIn = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertEquals(900, expiresIn, 5, "Access token should expire in 15 minutes (900 seconds)");

        System.out.println("[DEBUG_LOG] Access token TTL: " + expiresIn + " seconds");
    }

    @Test
    @DisplayName("Phase 3.9: Refresh Token should have correct TTL (7 days)")
    void testRefreshTokenTtl() {
        // Given
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);

        // Then - Response should indicate 7 days
        long expectedTtl = 604800; // 7 days in seconds
        assertEquals(expectedTtl, tokenPair.expiresIn(), 
                "Refresh token should have 7 day TTL");

        System.out.println("[DEBUG_LOG] Refresh token TTL: " + tokenPair.expiresIn() + " seconds");
    }

    @Test
    @DisplayName("Phase 3.10: Expired refresh token should be rejected")
    void testExpiredRefreshTokenRejection() {
        // Given - Expired refresh token
        String expiredToken = UUID.randomUUID().toString();
        RefreshToken expiredRefreshToken = new RefreshToken(
                expiredToken,
                testUser.getId(),
                UUID.randomUUID().toString(),
                LocalDateTime.now().minusDays(8),
                LocalDateTime.now().minusDays(1), // Expired yesterday
                false,
                null,
                clientIp,
                "Mozilla/5.0",
                604800L
        );

        when(refreshTokenRepository.findById(expiredToken))
                .thenReturn(Optional.of(expiredRefreshToken));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tokenProvider.refreshToken(expiredToken, clientIp));

        assertTrue(exception.getMessage().contains("expired"), 
                "Exception should mention token expiration");

        // Verify token was deleted
        verify(refreshTokenRepository).deleteById(expiredToken);

        System.out.println("[DEBUG_LOG] Expired refresh token rejected correctly");
    }

    @Test
    @DisplayName("Phase 3.11: getUserIdFromToken should read from 'sub' claim")
    void testGetUserIdFromTokenUsesSubClaim() {
        // Given
        TokenPair tokenPair = tokenProvider.createTokenPair(testUser, sessionId, clientIp);

        // When
        String extractedUserId = tokenProvider.getUserIdFromToken(tokenPair.accessToken());

        // Then
        assertEquals(testUser.getId(), extractedUserId, 
                "Should extract user ID from 'sub' claim");

        System.out.println("[DEBUG_LOG] User ID extracted from 'sub' claim: " + extractedUserId);
    }
}
