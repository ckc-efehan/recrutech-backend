package com.recrutech.recrutechauth.security;

import com.recrutech.recrutechauth.model.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Security service for handling rate limiting, account lockout, and security monitoring.
 */
@Service
public class SecurityService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;

    public SecurityService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Rate limiting for login attempts.
     */
    public boolean isRateLimited(String identifier) {
        String key = "rate_limit:" + identifier;
        String attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(RATE_LIMIT_WINDOW_MINUTES));
            return false;
        }
        
        int currentAttempts = Integer.parseInt(attempts);
        if (currentAttempts >= MAX_LOGIN_ATTEMPTS) {
            return true;
        }
        
        redisTemplate.opsForValue().increment(key);
        return false;
    }

    /**
     * Clear rate limiting for successful login.
     */
    public void clearRateLimit(String identifier) {
        String key = "rate_limit:" + identifier;
        redisTemplate.delete(key);
    }

    /**
     * Record failed login attempt and potentially lock account.
     */
    public void recordFailedAttempt(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        user.setLastFailedLogin(LocalDateTime.now());
        
        if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
        }
    }

    /**
     * Clear failed login attempts after successful login.
     */
    public void clearFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLastFailedLogin(null);
        user.setAccountLockedUntil(null);
    }

    /**
     * Check for suspicious activity patterns.
     */
    public boolean isSuspiciousActivity(String userId, String clientIp) {
        // Check for multiple IPs in short time
        String recentIpsKey = "recent_ips:" + userId;
        redisTemplate.opsForSet().add(recentIpsKey, clientIp);
        redisTemplate.expire(recentIpsKey, Duration.ofHours(1));
        
        Set<String> recentIps = redisTemplate.opsForSet().members(recentIpsKey);
        if (recentIps != null && recentIps.size() > 3) {
            return true;
        }

        // Check for too many requests in short time
        String requestCountKey = "request_count:" + userId;
        String count = redisTemplate.opsForValue().get(requestCountKey);
        if (count == null) {
            redisTemplate.opsForValue().set(requestCountKey, "1", Duration.ofMinutes(5));
            return false;
        }
        
        int requestCount = Integer.parseInt(count);
        redisTemplate.opsForValue().increment(requestCountKey);
        
        return requestCount > 50; // 50 requests in 5 minutes
    }

    /**
     * Generate device fingerprint for device tracking.
     */
    public String generateDeviceFingerprint(String userAgent, String acceptLanguage, String clientIp) {
        String combined = userAgent + "|" + acceptLanguage + "|" + clientIp;
        return Integer.toString(combined.hashCode());
    }

    /**
     * Check if device is known for this user.
     */
    public boolean isKnownDevice(String userId, String deviceFingerprint) {
        String key = "known_devices:" + userId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, deviceFingerprint));
    }

    /**
     * Register a new device for the user.
     */
    public void registerDevice(String userId, String deviceFingerprint) {
        String key = "known_devices:" + userId;
        redisTemplate.opsForSet().add(key, deviceFingerprint);
        redisTemplate.expire(key, Duration.ofDays(90));
    }



    /**
     * Remove active session for user.
     */
    public void removeActiveSession(String userId, String sessionId) {
        String sessionKey = "active_sessions:" + userId;
        redisTemplate.opsForSet().remove(sessionKey, sessionId);
    }



    /**
     * Log security event for monitoring.
     */
    public void logSecurityEvent(String userId, String event, String details, String clientIp) {
        String eventKey = "security_events:" + userId + ":" + System.currentTimeMillis();
        String eventData = String.format("{\"event\":\"%s\",\"details\":\"%s\",\"ip\":\"%s\",\"timestamp\":\"%s\"}", 
            event, details, clientIp, LocalDateTime.now());
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
    }


}