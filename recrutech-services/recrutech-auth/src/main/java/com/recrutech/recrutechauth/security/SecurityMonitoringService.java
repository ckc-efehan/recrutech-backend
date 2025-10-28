package com.recrutech.recrutechauth.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for comprehensive security monitoring and metrics collection.
 * Tracks token operations, security events, and provides real-time security metrics.
 */
@Service
public class SecurityMonitoringService {

    private final RedisTemplate<String, String> redisTemplate;
    
    // In-memory counters for real-time metrics
    private final AtomicLong tokenCreationCount = new AtomicLong(0);
    private final AtomicLong tokenValidationCount = new AtomicLong(0);
    private final AtomicLong tokenInvalidationCount = new AtomicLong(0);
    private final AtomicLong suspiciousActivityCount = new AtomicLong(0);
    private final AtomicLong keyRotationCount = new AtomicLong(0);

    public SecurityMonitoringService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Log token creation event.
     */
    public void logTokenCreation(String userId, String tokenType, String clientIp, String userAgent) {
        tokenCreationCount.incrementAndGet();
        
        String eventKey = "security_events:TOKEN_CREATION:" + System.currentTimeMillis();
        String eventData = String.format(
            "{\"event\":\"TOKEN_CREATION\",\"userId\":\"%s\",\"tokenType\":\"%s\",\"ip\":\"%s\",\"userAgent\":\"%s\",\"timestamp\":\"%s\"}", 
            userId, tokenType, clientIp, sanitizeUserAgent(userAgent), LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        
        // Update daily metrics
        updateDailyMetric("token_creations");
    }

    /**
     * Log token validation event.
     */
    public void logTokenValidation(String userId, String tokenType, boolean isValid, String clientIp) {
        tokenValidationCount.incrementAndGet();
        
        String eventKey = "security_events:TOKEN_VALIDATION:" + System.currentTimeMillis();
        String eventData = String.format(
            "{\"event\":\"TOKEN_VALIDATION\",\"userId\":\"%s\",\"tokenType\":\"%s\",\"valid\":%b,\"ip\":\"%s\",\"timestamp\":\"%s\"}", 
            userId, tokenType, isValid, clientIp, LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        
        if (!isValid) {
            updateDailyMetric("invalid_token_attempts");
            checkForSuspiciousActivity(userId, clientIp);
        }
        
        updateDailyMetric("token_validations");
    }

    /**
     * Log token invalidation event.
     */
    public void logTokenInvalidation(String userId, String reason, String clientIp) {
        tokenInvalidationCount.incrementAndGet();
        
        String eventKey = "security_events:TOKEN_INVALIDATION:" + System.currentTimeMillis();
        String eventData = String.format(
            "{\"event\":\"TOKEN_INVALIDATION\",\"userId\":\"%s\",\"reason\":\"%s\",\"ip\":\"%s\",\"timestamp\":\"%s\"}", 
            userId, reason, clientIp, LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        updateDailyMetric("token_invalidations");
    }

    /**
     * Log suspicious activity.
     */
    public void logSuspiciousActivity(String userId, String activityType, String details, String clientIp) {
        suspiciousActivityCount.incrementAndGet();
        
        String eventKey = "security_events:SUSPICIOUS_ACTIVITY:" + System.currentTimeMillis();
        String eventData = String.format(
            "{\"event\":\"SUSPICIOUS_ACTIVITY\",\"userId\":\"%s\",\"activityType\":\"%s\",\"details\":\"%s\",\"ip\":\"%s\",\"timestamp\":\"%s\"}", 
            userId, activityType, details, clientIp, LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        updateDailyMetric("suspicious_activities");
        
        // Create alert for high-priority suspicious activities
        if (isHighPrioritySuspiciousActivity(activityType)) {
            createSecurityAlert(userId, activityType, details, clientIp);
        }
    }

    /**
     * Log key rotation event.
     */
    public void logKeyRotation(String fromKey, String toKey, boolean successful) {
        keyRotationCount.incrementAndGet();
        
        String eventKey = "security_events:KEY_ROTATION:" + System.currentTimeMillis();
        String eventData = String.format(
            "{\"event\":\"KEY_ROTATION\",\"fromKey\":\"%s\",\"toKey\":\"%s\",\"successful\":%b,\"timestamp\":\"%s\"}", 
            fromKey, toKey, successful, LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        updateDailyMetric("key_rotations");
        
        if (!successful) {
            createSecurityAlert("SYSTEM", "KEY_ROTATION_FAILED", 
                "Key rotation failed from " + fromKey + " to " + toKey, "SYSTEM");
        }
    }

    /**
     * Log generic security event.
     * Used for various security-related events that don't fit specific categories.
     */
    public void logSecurityEvent(String userId, String eventType, String clientIp, String details) {
        String eventKey = "security_events:" + eventType + ":" + System.currentTimeMillis();
        String eventData = String.format(
            "{\"event\":\"%s\",\"userId\":\"%s\",\"details\":\"%s\",\"ip\":\"%s\",\"timestamp\":\"%s\"}", 
            eventType, userId, details, clientIp, LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        updateDailyMetric("security_events");
    }

    /**
     * Log refresh token rotation event.
     * Tracks refresh token rotation for security monitoring.
     */
    public void logTokenRotation(String userId, String oldToken, String newToken, String clientIp) {
        String eventKey = "security_events:TOKEN_ROTATION:" + System.currentTimeMillis();
        String eventData = String.format(
            "{\"event\":\"TOKEN_ROTATION\",\"userId\":\"%s\",\"oldTokenHash\":\"%s\",\"newTokenHash\":\"%s\",\"ip\":\"%s\",\"timestamp\":\"%s\"}", 
            userId, 
            Integer.toHexString(oldToken.hashCode()), 
            Integer.toHexString(newToken.hashCode()), 
            clientIp, 
            LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
        updateDailyMetric("token_rotations");
    }

    /**
     * Get real-time security metrics.
     */
    public Map<String, Object> getSecurityMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Real-time counters
        metrics.put("tokenCreations", tokenCreationCount.get());
        metrics.put("tokenValidations", tokenValidationCount.get());
        metrics.put("tokenInvalidations", tokenInvalidationCount.get());
        metrics.put("suspiciousActivities", suspiciousActivityCount.get());
        metrics.put("keyRotations", keyRotationCount.get());
        
        // Daily metrics
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        metrics.put("dailyTokenCreations", getDailyMetric("token_creations", today));
        metrics.put("dailyTokenValidations", getDailyMetric("token_validations", today));
        metrics.put("dailyInvalidTokenAttempts", getDailyMetric("invalid_token_attempts", today));
        metrics.put("dailySuspiciousActivities", getDailyMetric("suspicious_activities", today));
        
        // Security health indicators
        metrics.put("securityHealth", calculateSecurityHealth());
        metrics.put("lastKeyRotation", redisTemplate.opsForValue().get("jwt_key_rotation:current_timestamp"));
        
        return metrics;
    }

    /**
     * Get security events for a specific time period.
     */
    public List<Map<String, Object>> getSecurityEvents(String eventType, int hours) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        long startTime = System.currentTimeMillis() - (hours * 3600000L);
        String pattern = "security_events:" + (eventType != null ? eventType : "*") + ":*";
        
        Set<String> eventKeys = redisTemplate.keys(pattern);
        for (String key : eventKeys) {
            String[] parts = key.split(":");
            if (parts.length >= 3) {
                try {
                    long eventTime = Long.parseLong(parts[2]);
                    if (eventTime >= startTime) {
                        String eventData = redisTemplate.opsForValue().get(key);
                        if (eventData != null) {
                            Map<String, Object> event = new HashMap<>();
                            event.put("key", key);
                            event.put("data", eventData);
                            event.put("timestamp", eventTime);
                            events.add(event);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid timestamp
                }
            }
        }

        // Sort by timestamp descending
        events.sort((a, b) -> Long.compare((Long) b.get("timestamp"), (Long) a.get("timestamp")));
        
        return events;
    }

    /**
     * Create security alert.
     */
    private void createSecurityAlert(String userId, String alertType, String details, String clientIp) {
        String alertKey = "security_alerts:" + alertType + ":" + System.currentTimeMillis();
        String alertData = String.format(
            "{\"alertType\":\"%s\",\"userId\":\"%s\",\"details\":\"%s\",\"ip\":\"%s\",\"timestamp\":\"%s\",\"severity\":\"HIGH\"}", 
            alertType, userId, details, clientIp, LocalDateTime.now()
        );
        
        redisTemplate.opsForValue().set(alertKey, alertData, Duration.ofDays(7));
        
        // Increment alert counter
        updateDailyMetric("security_alerts");
    }

    /**
     * Check for suspicious activity patterns.
     */
    private void checkForSuspiciousActivity(String userId, String clientIp) {
        String activityKey = "suspicious_pattern:" + userId + ":" + "INVALID_TOKEN_ATTEMPT";
        String count = redisTemplate.opsForValue().get(activityKey);
        
        if (count == null) {
            redisTemplate.opsForValue().set(activityKey, "1", Duration.ofMinutes(15));
        } else {
            int currentCount = Integer.parseInt(count);
            redisTemplate.opsForValue().increment(activityKey);
            
            // Trigger alert if threshold exceeded
            if (currentCount >= 5) { // 5 attempts in 15 minutes
                logSuspiciousActivity(userId, "REPEATED_" + "INVALID_TOKEN_ATTEMPT",
                    "Multiple " + "INVALID_TOKEN_ATTEMPT" + " attempts detected", clientIp);
            }
        }
    }

    /**
     * Update daily metric counter.
     */
    private void updateDailyMetric(String metricName) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String metricKey = "daily_metrics:" + metricName + ":" + today;
        
        if (redisTemplate.hasKey(metricKey)) {
            redisTemplate.opsForValue().increment(metricKey, 1);
        } else {
            redisTemplate.opsForValue().set(metricKey, String.valueOf(1), Duration.ofDays(30));
        }
    }

    /**
     * Get daily metric value.
     */
    private long getDailyMetric(String metricName, String date) {
        String metricKey = "daily_metrics:" + metricName + ":" + date;
        String value = redisTemplate.opsForValue().get(metricKey);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * Calculate overall security health score.
     */
    private String calculateSecurityHealth() {
        long invalidAttempts = getDailyMetric("invalid_token_attempts", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        long suspiciousActivities = getDailyMetric("suspicious_activities", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        if (invalidAttempts > 100 || suspiciousActivities > 50) {
            return "CRITICAL";
        } else if (invalidAttempts > 50 || suspiciousActivities > 20) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * Check if activity type requires high priority alert.
     */
    private boolean isHighPrioritySuspiciousActivity(String activityType) {
        return activityType.contains("BRUTE_FORCE") || 
               activityType.contains("MULTIPLE_IP") || 
               activityType.contains("TOKEN_MANIPULATION") ||
               activityType.contains("REPEATED_INVALID_TOKEN");
    }

    /**
     * Sanitize user agent string for logging.
     */
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null) return "UNKNOWN";
        // Remove potentially malicious characters and limit length
        return userAgent.replaceAll("[<>\"'&]", "").substring(0, Math.min(userAgent.length(), 200));
    }
}