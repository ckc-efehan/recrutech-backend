package com.recrutech.recrutechauth.security;

import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Token Provider with automatic token generation capabilities.
 * Handles token creation, validation, refresh, and automatic generation.
 */
@Component
public class TokenProvider {

    private final String secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
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
            KeyRotationService keyRotationService,
            SecurityMonitoringService securityMonitoringService) {
        
        this.secretKey = secret;
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
        this.issuer = issuer;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.keyRotationService = keyRotationService;
        this.securityMonitoringService = securityMonitoringService;
    }

    /**
     * Creates a secure token pair (Access + Refresh) with automatic generation.
     * If user already has valid tokens, returns existing ones.
     * If tokens are expired or don't exist, generates new ones automatically.
     */
    public TokenPair createOrGetTokenPair(User user, String sessionId, String clientIp) {
        // Check if user already has valid tokens
        if (!user.needsNewToken()) {
            return new TokenPair(
                user.getCurrentAccessToken(), 
                user.getCurrentRefreshToken(), 
                Duration.between(LocalDateTime.now(), user.getTokenExpiresAt()).getSeconds()
            );
        }

        // Generate new token pair
        return createTokenPair(user, sessionId, clientIp);
    }

    /**
     * Creates a new secure token pair (Access + Refresh).
     */
    public TokenPair createTokenPair(User user, String sessionId, String clientIp) {
        Date now = new Date();
        String jti = UUID.randomUUID().toString(); // Unique Token ID
        
        // Access Token
        String accessToken = Jwts.builder()
            .id(jti)
            .subject(user.getEmail())
            .issuer(issuer)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + accessTokenValidityMs))
            .claim("userId", user.getId())
            .claim("role", user.getRole().getAuthority())
            .claim("sessionId", sessionId)
            .claim("clientIp", clientIp)
            .claim("tokenType", "ACCESS")
            .claim("twoFactorVerified", user.isTwoFactorEnabled())
            .signWith(Keys.hmacShaKeyFor(keyRotationService.getCurrentSigningKey().getBytes()))
            .compact();

        // Refresh Token
        String refreshJti = UUID.randomUUID().toString();
        String refreshToken = Jwts.builder()
            .id(refreshJti)
            .subject(user.getEmail())
            .issuer(issuer)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + refreshTokenValidityMs))
            .claim("userId", user.getId())
            .claim("sessionId", sessionId)
            .claim("tokenType", "REFRESH")
            .claim("accessTokenId", jti)
            .signWith(Keys.hmacShaKeyFor(keyRotationService.getCurrentSigningKey().getBytes()))
            .compact();

        // Store tokens in Redis for blacklisting and session management
        storeTokenInRedis(jti, accessToken, Duration.ofSeconds(accessTokenValidityMs / 1000));
        storeTokenInRedis(refreshJti, refreshToken, Duration.ofSeconds(refreshTokenValidityMs / 1000));
        
        // Store session mapping
        redisTemplate.opsForValue().set(
            "session:" + sessionId, 
            user.getId(), 
            Duration.ofSeconds(refreshTokenValidityMs / 1000)
        );

        // Update user with new tokens
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(accessTokenValidityMs / 1000);
        user.updateTokens(accessToken, refreshToken, expiresAt);
        userRepository.save(user);

        // Log token creation for security monitoring
        securityMonitoringService.logTokenCreation(user.getId(), "ACCESS", clientIp, "TokenProvider");
        securityMonitoringService.logTokenCreation(user.getId(), "REFRESH", clientIp, "TokenProvider");

        return new TokenPair(accessToken, refreshToken, accessTokenValidityMs / 1000);
    }



    /**
     * Refresh token with rotation and automatic generation.
     */
    public TokenPair refreshToken(String refreshToken, String clientIp) {
        try {
            Claims claims = parseToken(refreshToken);
            if (claims == null) {
                throw new IllegalArgumentException("Invalid refresh token");
            }

            String tokenType = claims.get("tokenType", String.class);
            if (!"REFRESH".equals(tokenType)) {
                throw new IllegalArgumentException("Invalid token type");
            }

            String userId = claims.get("userId", String.class);
            String sessionId = claims.get("sessionId", String.class);
            
            // Blacklist old access token
            String oldAccessTokenId = claims.get("accessTokenId", String.class);
            if (oldAccessTokenId != null) {
                blacklistTokenById(oldAccessTokenId);
            }

            // Blacklist old refresh token
            blacklistToken(refreshToken);

            // Load user and create new token pair
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            return createTokenPair(user, sessionId, clientIp);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }

    /**
     * Extracts user information from token.
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null ? claims.get("userId", String.class) : null;
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
        if (sessionKeys != null) {
            for (String sessionKey : sessionKeys) {
                String sessionUserId = redisTemplate.opsForValue().get(sessionKey);
                if (userId.equals(sessionUserId)) {
                    redisTemplate.delete(sessionKey);
                }
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
            String userId = claims.get("userId", String.class);
            if (userId != null) {
                String userInvalidationTime = redisTemplate.opsForValue().get("user_token_invalidation:" + userId);
                if (userInvalidationTime != null) {
                    long userInvalidationTimestamp = Long.parseLong(userInvalidationTime);
                    long tokenIssuedAt = claims.getIssuedAt().getTime();
                    if (tokenIssuedAt < userInvalidationTimestamp) {
                        return true;
                    }
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
                continue;
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
                if (isTokenGloballyInvalidated(token)) {
                    return true;
                }
                
                return false;
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
            
            String userId = claims.get("userId", String.class);
            String tokenType = claims.get("tokenType", String.class);
            
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
