package com.recrutech.recrutechauth.security;

import com.recrutech.recrutechauth.model.RefreshToken;
import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.repository.RefreshTokenRepository;
import com.recrutech.recrutechauth.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Token Provider with JWT access tokens and opaque refresh tokens.
 * Implements proper JWT claims structure and refresh token rotation with reuse detection.
 */
@Component
public class TokenProvider {

    private final String secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String issuer;
    private final KeyRotationService keyRotationService;
    private final SecurityMonitoringService securityMonitoringService;

    public TokenProvider(
            @Value("${jwt.secret:YourVeryLongAndSecureBase64EncodedSecretKeyThatIsAtLeast512BitsLong}") String secret,
            @Value("${jwt.access-token-validity:900000}") long accessTokenValidityMs, // 15 min
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityMs, // 7 days
            @Value("${jwt.issuer:recrutech}") String issuer,
            RedisTemplate<String, String> redisTemplate,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            KeyRotationService keyRotationService,
            SecurityMonitoringService securityMonitoringService) {
        
        this.secretKey = secret;
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
        this.issuer = issuer;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.keyRotationService = keyRotationService;
        this.securityMonitoringService = securityMonitoringService;
    }

    /**
     * Creates a secure token pair (Access + Refresh).
     * Note: Tokens are no longer stored in database, always generates new pair.
     * Token validation and session management is handled via Redis.
     */
    public TokenPair createOrGetTokenPair(User user, String sessionId, String clientIp) {
        // Always generate new token pair (tokens not stored in DB anymore)
        return createTokenPair(user, sessionId, clientIp);
    }

    /**
     * Creates a new secure token pair (Access JWT + Opaque Refresh Token).
     * Implements proper JWT claims and opaque refresh token with family tracking.
     */
    public TokenPair createTokenPair(User user, String sessionId, String clientIp) {
        return createTokenPair(user, sessionId, clientIp, null, null);
    }

    /**
     * Creates a new secure token pair with optional family ID for rotation.
     * @param user the user
     * @param sessionId the session ID
     * @param clientIp the client IP
     * @param userAgent the user agent (optional)
     * @param familyId the family ID for rotation chain (null for new family)
     */
    private TokenPair createTokenPair(User user, String sessionId, String clientIp, String userAgent, String familyId) {
        Date now = new Date();
        Date notBefore = now; // Token valid immediately
        Date expiration = new Date(now.getTime() + accessTokenValidityMs);
        
        String jti = UUID.randomUUID().toString(); // Unique Token ID
        
        // Build JWT Access Token with proper claims per Phase 3 requirements
        String accessToken = Jwts.builder()
            .id(jti)
            .subject(user.getId()) // Subject is userId (account ID)
            .issuer(issuer)
            .audience().add("recrutech-platform").add("recrutech-notification").and() // Audience claim
            .issuedAt(now)
            .notBefore(notBefore) // Not before claim
            .expiration(expiration)
            .claim("roles", Collections.singletonList(user.getRole().getAuthority())) // Roles array
            .claim("scope", determineScopes(user)) // Permission scopes
            .claim("tenant", "default") // Multi-tenancy support
            .claim("email", user.getEmail())
            .claim("emailVerified", user.isEmailVerified())
            .claim("mfaVerified", user.isTwoFactorEnabled())
            .signWith(Keys.hmacShaKeyFor(keyRotationService.getCurrentSigningKey().getBytes()))
            .compact();

        // Generate Opaque Refresh Token (UUID, not JWT)
        String refreshToken = UUID.randomUUID().toString();
        String effectiveFamilyId = familyId != null ? familyId : UUID.randomUUID().toString();
        
        // Create RefreshToken entity for Redis storage
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plus(refreshTokenValidityMs, ChronoUnit.MILLIS);
        long ttlSeconds = refreshTokenValidityMs / 1000;
        
        RefreshToken rt = new RefreshToken(
            refreshToken,
            user.getId(),
            effectiveFamilyId,
            issuedAt,
            expiresAt,
            false, // not revoked
            null, // not replaced yet
            clientIp,
            userAgent,
            ttlSeconds
        );
        
        // Store refresh token in Redis
        refreshTokenRepository.save(rt);
        
        // Store access token JTI in Redis for blacklisting capability
        storeTokenInRedis(jti, accessToken, Duration.ofSeconds(accessTokenValidityMs / 1000));
        
        // Store session mapping
        redisTemplate.opsForValue().set(
            "session:" + sessionId, 
            user.getId(), 
            Duration.ofSeconds(refreshTokenValidityMs / 1000)
        );

        // Log token creation for security monitoring
        securityMonitoringService.logTokenCreation(user.getId(), "ACCESS", clientIp, "TokenProvider");
        securityMonitoringService.logTokenCreation(user.getId(), "REFRESH", clientIp, "TokenProvider");

        return new TokenPair(accessToken, refreshToken, refreshTokenValidityMs / 1000);
    }

    /**
     * Determine permission scopes based on user role.
     * @param user the user
     * @return list of scopes
     */
    private List<String> determineScopes(User user) {
        List<String> scopes = new ArrayList<>();
        
        switch (user.getRole()) {
            case COMPANY_ADMIN:
                scopes.add("company:read");
                scopes.add("company:write");
                scopes.add("company:manage");
                scopes.add("hr:read");
                scopes.add("hr:write");
                scopes.add("hr:manage");
                scopes.add("application:read");
                scopes.add("application:write");
                scopes.add("application:manage");
                scopes.add("jobposting:read");
                scopes.add("jobposting:write");
                scopes.add("jobposting:manage");
                scopes.add("interview:read");
                scopes.add("interview:write");
                scopes.add("interview:manage");
                break;
            case HR:
                scopes.add("company:read");
                scopes.add("application:read");
                scopes.add("application:write");
                scopes.add("jobposting:read");
                scopes.add("jobposting:write");
                scopes.add("interview:read");
                scopes.add("interview:write");
                scopes.add("interview:manage");
                break;
            case APPLICANT:
                scopes.add("profile:read");
                scopes.add("profile:write");
                scopes.add("application:create");
                scopes.add("application:read:own");
                scopes.add("jobposting:read");
                scopes.add("interview:read:own");
                break;
        }
        
        return scopes;
    }



    /**
     * Refresh token with rotation and reuse detection.
     * Implements proper refresh token rotation per Phase 3 requirements:
     * - Opaque refresh tokens (UUID, not JWT)
     * - Token family tracking
     * - Automatic reuse detection and family revocation
     */
    public TokenPair refreshToken(String refreshToken, String clientIp) {
        return refreshToken(refreshToken, clientIp, null);
    }

    /**
     * Refresh token with rotation and reuse detection.
     * @param refreshToken the opaque refresh token
     * @param clientIp the client IP address
     * @param userAgent the user agent (optional)
     * @return new token pair
     */
    public TokenPair refreshToken(String refreshToken, String clientIp, String userAgent) {
        // 1. Look up refresh token in Redis
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findById(refreshToken);
        
        if (rtOpt.isEmpty()) {
            securityMonitoringService.logSecurityEvent("UNKNOWN", "REFRESH_TOKEN_NOT_FOUND", clientIp, "Unknown or expired refresh token");
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        
        RefreshToken rt = rtOpt.get();
        
        // 2. Check if token is expired
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteById(refreshToken);
            securityMonitoringService.logSecurityEvent(rt.getUserId(), "REFRESH_TOKEN_EXPIRED", clientIp, "Expired refresh token used");
            throw new IllegalArgumentException("Refresh token expired");
        }
        
        // 3. REUSE DETECTION: Check if token was already used (has replacedBy value)
        if (rt.getReplacedBy() != null) {
            // Token reuse detected! Revoke entire family
            securityMonitoringService.logSecurityEvent(rt.getUserId(), "REFRESH_TOKEN_REUSE_DETECTED", clientIp, 
                "Refresh token reuse detected - revoking family: " + rt.getFamilyId());
            
            // Revoke all tokens in this family
            revokeTokenFamily(rt.getFamilyId());
            
            throw new SecurityException("Refresh token reuse detected - session terminated");
        }
        
        // 4. Check if token is revoked
        if (rt.isRevoked()) {
            securityMonitoringService.logSecurityEvent(rt.getUserId(), "REVOKED_TOKEN_USED", clientIp, "Revoked refresh token used");
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        
        // 5. Load user
        User user = userRepository.findById(rt.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // 6. Create new token pair (with same familyId for rotation chain)
        String sessionId = UUID.randomUUID().toString(); // New session for new token pair
        TokenPair newTokenPair = createTokenPair(user, sessionId, clientIp, userAgent, rt.getFamilyId());
        
        // 7. Mark old refresh token as replaced
        rt.setReplacedBy(newTokenPair.refreshToken());
        refreshTokenRepository.save(rt);
        
        // 8. Log successful rotation
        securityMonitoringService.logTokenRotation(user.getId(), rt.getToken(), newTokenPair.refreshToken(), clientIp);
        
        return newTokenPair;
    }
    
    /**
     * Revokes all refresh tokens in a token family.
     * Called when token reuse is detected.
     * @param familyId the family ID
     */
    private void revokeTokenFamily(String familyId) {
        List<RefreshToken> familyTokens = refreshTokenRepository.findByFamilyId(familyId);
        for (RefreshToken token : familyTokens) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
        securityMonitoringService.logSecurityEvent("SYSTEM", "TOKEN_FAMILY_REVOKED", "SYSTEM", 
            "Revoked token family: " + familyId + " (" + familyTokens.size() + " tokens)");
    }

    /**
     * Extracts user information from token.
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null ? claims.getSubject() : null;
        } catch (Exception e) {
            return null;
        }
    }



    /**
     * Adds token to blacklist.
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims != null) {
                String jti = claims.getId();
                Date expiration = claims.getExpiration();
                
                if (jti != null && expiration.after(new Date())) {
                    Duration ttl = Duration.between(
                        LocalDateTime.now(), 
                        expiration.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                    );
                    redisTemplate.opsForValue().set("blacklist:" + jti, "true", ttl);
                }
            }
        } catch (Exception e) {
            // Fallback: Token hash to blacklist
            String tokenHash = Integer.toString(token.hashCode());
            redisTemplate.opsForValue().set("blacklist:hash:" + tokenHash, "true", Duration.ofDays(1));
        }
    }

    /**
     * Blacklists token by ID.
     */
    public void blacklistTokenById(String jti) {
        redisTemplate.opsForValue().set("blacklist:" + jti, "true", Duration.ofDays(1));
    }

    /**
     * Invalidates user session.
     */
    public void invalidateSession(String sessionId) {
        redisTemplate.delete("session:" + sessionId);
    }

    /**
     * Invalidates all existing tokens by setting a global invalidation timestamp.
     * All tokens issued before this timestamp will be considered invalid.
     */
    public void invalidateAllTokens() {
        String invalidationTime = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set("global_token_invalidation", invalidationTime);
        
        // Log security event to Redis for monitoring
        String eventKey = "security_events:SYSTEM:" + System.currentTimeMillis();
        String eventData = String.format("{\"event\":\"GLOBAL_TOKEN_INVALIDATION\",\"details\":\"All tokens invalidated due to security update\",\"ip\":\"SYSTEM\",\"timestamp\":\"%s\"}", 
            LocalDateTime.now());
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        
        // Log invalidation for security monitoring
        securityMonitoringService.logTokenInvalidation("ALL_USERS", "GLOBAL_INVALIDATION", "SYSTEM");
    }

    /**
     * Invalidates all tokens for a specific user.
     */
    public void invalidateAllUserTokens(String userId) {
        String userInvalidationKey = "user_token_invalidation:" + userId;
        String invalidationTime = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set(userInvalidationKey, invalidationTime, Duration.ofDays(30));
        
        // Also clear all active sessions for this user
        String sessionPattern = "session:*";
        Set<String> sessionKeys = redisTemplate.keys(sessionPattern);
        for (String sessionKey : sessionKeys) {
            String sessionUserId = redisTemplate.opsForValue().get(sessionKey);
            if (userId.equals(sessionUserId)) {
                redisTemplate.delete(sessionKey);
            }
        }

        // Log security event to Redis for monitoring
        String eventKey = "security_events:" + userId + ":" + System.currentTimeMillis();
        String eventData = String.format("{\"event\":\"USER_TOKEN_INVALIDATION\",\"details\":\"All user tokens invalidated\",\"ip\":\"SYSTEM\",\"timestamp\":\"%s\"}", 
            LocalDateTime.now());
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
    }

    /**
     * Checks if a token is globally invalidated.
     */
    public boolean isTokenGloballyInvalidated(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) return true;
            
            // Check global invalidation
            String globalInvalidationTime = redisTemplate.opsForValue().get("global_token_invalidation");
            if (globalInvalidationTime != null) {
                long invalidationTimestamp = Long.parseLong(globalInvalidationTime);
                long tokenIssuedAt = claims.getIssuedAt().getTime();
                if (tokenIssuedAt < invalidationTimestamp) {
                    return true;
                }
            }
            
            // Check user-specific invalidation
            String userId = claims.getSubject();
            if (userId != null) {
                String userInvalidationTime = redisTemplate.opsForValue().get("user_token_invalidation:" + userId);
                if (userInvalidationTime != null) {
                    long userInvalidationTimestamp = Long.parseLong(userInvalidationTime);
                    long tokenIssuedAt = claims.getIssuedAt().getTime();
                    return tokenIssuedAt < userInvalidationTimestamp;
                }
            }
            
            return false;
        } catch (Exception e) {
            return true; // If we can't parse the token, consider it invalid
        }
    }


    /**
     * Generates a secure random token for various purposes.
     * Uses URL-safe Base64 encoding to avoid URL encoding issues.
     */
    public String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }


    // Private helper methods
    
    /**
     * Parse token using multiple keys for rotation support.
     */
    private Claims parseToken(String token) {
        // Try all valid verification keys (current + overlap period keys)
        List<String> validKeys = keyRotationService.getValidVerificationKeys();
        
        for (String key : validKeys) {
            try {
                return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(key.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            } catch (Exception e) {
                // Continue to next key if this one fails
            }
        }
        
        // If all keys fail, try the fallback secret key for backward compatibility
        try {
            return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
    
    private void storeTokenInRedis(String jti, String token, Duration ttl) {
        redisTemplate.opsForValue().set("token:" + jti, "valid", ttl);
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims != null) {
                String jti = claims.getId();
                
                // Check individual token blacklist
                if (jti != null && redisTemplate.hasKey("blacklist:" + jti)) {
                    return true;
                }
                
                // Check global and user-specific invalidation
                return isTokenGloballyInvalidated(token);
            }
            return true; // If we can't parse claims, consider it blacklisted
        } catch (Exception e) {
            return true; // If there's an error, consider it blacklisted for security
        }
    }

    /**
     * Comprehensive token validation including blacklist, global invalidation, and expiration checks.
     */
    public boolean isTokenValid(String token) {
        return isTokenValid(token, "UNKNOWN");
    }

    /**
     * Comprehensive token validation with client IP for monitoring.
     */
    public boolean isTokenValid(String token, String clientIp) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                securityMonitoringService.logTokenValidation("UNKNOWN", "UNKNOWN", false, clientIp);
                return false;
            }
            
            String userId = claims.getSubject();
            String tokenType = "ACCESS"; // Access tokens are validated here
            
            // Check expiration
            if (claims.getExpiration().before(new Date())) {
                securityMonitoringService.logTokenValidation(userId, tokenType, false, clientIp);
                return false;
            }
            
            // Check if token is blacklisted or globally invalidated
            if (isTokenBlacklisted(token)) {
                securityMonitoringService.logTokenValidation(userId, tokenType, false, clientIp);
                return false;
            }
            
            // Log successful validation
            securityMonitoringService.logTokenValidation(userId, tokenType, true, clientIp);
            return true;
        } catch (Exception e) {
            securityMonitoringService.logTokenValidation("UNKNOWN", "UNKNOWN", false, clientIp);
            return false;
        }
    }

    /**
     * Check if session is valid (utilizing previously unused method).
     */
    public boolean isSessionValid(String sessionId) {
        return redisTemplate.hasKey("session:" + sessionId);
    }
}
