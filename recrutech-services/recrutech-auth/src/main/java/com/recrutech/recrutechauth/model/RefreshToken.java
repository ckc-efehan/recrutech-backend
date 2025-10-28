package com.recrutech.recrutechauth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RefreshToken entity stored in Redis.
 * Represents an opaque refresh token with rotation and family tracking capabilities.
 * Supports reuse detection to prevent token theft.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("refresh_tokens")
public class RefreshToken implements Serializable {

    /**
     * The opaque refresh token (UUID).
     * This is the primary identifier.
     */
    @Id
    private String token;

    /**
     * User ID this token belongs to.
     * Indexed for efficient lookup.
     */
    @Indexed
    private String userId;

    /**
     * Token family ID for rotation tracking.
     * All tokens in a rotation chain share the same family ID.
     * Used for invalidating entire chain when reuse is detected.
     */
    @Indexed
    private String familyId;

    /**
     * When this token was issued.
     */
    private LocalDateTime issuedAt;

    /**
     * When this token expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Whether this token has been revoked (e.g., after rotation or logout).
     */
    private boolean revoked;

    /**
     * If this token was rotated, contains the new token that replaced it.
     * Used for tracking rotation chain and detecting reuse.
     */
    private String replacedBy;

    /**
     * Client IP address when token was issued.
     * Used for security monitoring and anomaly detection.
     */
    private String clientIp;

    /**
     * User agent string when token was issued.
     * Used for security monitoring and device tracking.
     */
    private String userAgent;

    /**
     * Time-to-live in seconds for Redis expiration.
     * Redis will automatically delete expired tokens.
     */
    @TimeToLive
    private Long timeToLive;

    /**
     * Check if the token is expired.
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Check if the token is valid (not expired and not revoked).
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * Check if this token was already used (has been replaced).
     * Used for reuse detection.
     * @return true if token was rotated, false otherwise
     */
    public boolean wasRotated() {
        return replacedBy != null;
    }
}
