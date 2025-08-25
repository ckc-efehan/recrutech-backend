package com.recrutech.recrutechauth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing JWT key rotation and multiple active keys.
 * Implements automatic key rotation with configurable intervals and overlap periods.
 */
@Service
public class KeyRotationService {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyRotationService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final boolean rotationEnabled;
    private final int rotationIntervalHours;
    private final int overlapHours;
    private final int maxKeys;
    
    // Environment-based secrets (fallback to config if not set)
    private final String primarySecret;
    private final String secondarySecret;
    private final String tertiarySecret;
    
    // Lazy initialization flag to avoid Redis connection during startup
    private volatile boolean initialized = false;
    private volatile boolean redisAvailable = true;

    public KeyRotationService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${jwt.rotation.enabled:true}") boolean rotationEnabled,
            @Value("${jwt.rotation.interval-hours:168}") int rotationIntervalHours,
            @Value("${jwt.rotation.overlap-hours:24}") int overlapHours,
            @Value("${jwt.rotation.max-keys:3}") int maxKeys,
            @Value("${jwt.secret}") String defaultSecret,
            @Value("${JWT_SECRET_PRIMARY:#{null}}") String envPrimarySecret,
            @Value("${JWT_SECRET_SECONDARY:#{null}}") String envSecondarySecret,
            @Value("${JWT_SECRET_TERTIARY:#{null}}") String envTertiarySecret) {
        
        this.redisTemplate = redisTemplate;
        this.rotationEnabled = rotationEnabled;
        this.rotationIntervalHours = rotationIntervalHours;
        this.overlapHours = overlapHours;
        this.maxKeys = maxKeys;
        
        // Use environment variables if available, otherwise use default
        this.primarySecret = envPrimarySecret != null ? envPrimarySecret : defaultSecret;
        this.secondarySecret = envSecondarySecret != null ? envSecondarySecret : generateSecureSecret();
        this.tertiarySecret = envTertiarySecret != null ? envTertiarySecret : generateSecureSecret();
        
        // Key rotation system will be initialized lazily on first use to avoid Redis connection during startup
        logger.info("KeyRotationService initialized with rotation enabled: {}", rotationEnabled);
    }

    /**
     * Ensure the key rotation system is initialized (lazy initialization).
     */
    private void ensureInitialized() {
        if (initialized || !rotationEnabled) {
            return;
        }
        
        synchronized (this) {
            if (initialized) {
                return;
            }
            
            try {
                initializeKeyRotation();
                initialized = true;
                redisAvailable = true;
                logger.info("Key rotation system successfully initialized");
            } catch (Exception e) {
                redisAvailable = false;
                logger.warn("Redis is not available for key rotation. Falling back to static key mode: {}", e.getMessage());
                // Mark as initialized even if Redis is not available to avoid repeated attempts
                initialized = true;
            }
        }
    }

    /**
     * Initialize the key rotation system with current keys.
     */
    private void initializeKeyRotation() {
        if (!rotationEnabled) {
            return;
        }
        
        // Check if rotation system is already initialized
        if (!redisTemplate.hasKey("jwt_key_rotation:initialized")) {
            // Set up initial key rotation state
            String currentTime = String.valueOf(System.currentTimeMillis());
            
            // Store current active key
            redisTemplate.opsForValue().set("jwt_key_rotation:current", "primary");
            redisTemplate.opsForValue().set("jwt_key_rotation:current_timestamp", currentTime);
            
            // Store next rotation time
            long nextRotationTime = System.currentTimeMillis() + (rotationIntervalHours * 3600000L);
            redisTemplate.opsForValue().set("jwt_key_rotation:next_rotation", String.valueOf(nextRotationTime));
            
            // Mark as initialized
            redisTemplate.opsForValue().set("jwt_key_rotation:initialized", "true");
            
            logKeyRotationEvent("KEY_ROTATION_INITIALIZED", "Key rotation system initialized");
        }
    }

    /**
     * Get the current active signing key.
     */
    public String getCurrentSigningKey() {
        if (!rotationEnabled) {
            return primarySecret;
        }
        
        ensureInitialized();
        
        if (!redisAvailable) {
            logger.debug("Redis not available, using primary secret for signing");
            return primarySecret;
        }
        
        try {
            String currentKeyId = redisTemplate.opsForValue().get("jwt_key_rotation:current");
            return getKeyById(currentKeyId != null ? currentKeyId : "primary");
        } catch (Exception e) {
            logger.warn("Failed to get current signing key from Redis, falling back to primary secret: {}", e.getMessage());
            redisAvailable = false;
            return primarySecret;
        }
    }

    /**
     * Get all valid verification keys (current + overlap period keys).
     */
    public List<String> getValidVerificationKeys() {
        List<String> keys = new ArrayList<>();
        
        if (!rotationEnabled) {
            keys.add(primarySecret);
            return keys;
        }
        
        ensureInitialized();
        
        if (!redisAvailable) {
            logger.debug("Redis not available, using primary secret for verification");
            keys.add(primarySecret);
            return keys;
        }
        
        // Always include current key
        keys.add(getCurrentSigningKey());
        
        try {
            // Include keys from overlap period
            String previousKeyId = redisTemplate.opsForValue().get("jwt_key_rotation:previous");
            if (previousKeyId != null) {
                String previousTimestamp = redisTemplate.opsForValue().get("jwt_key_rotation:previous_timestamp");
                if (previousTimestamp != null) {
                    long previousTime = Long.parseLong(previousTimestamp);
                    long overlapEndTime = previousTime + (overlapHours * 3600000L);
                    
                    if (System.currentTimeMillis() < overlapEndTime) {
                        String previousKey = getKeyById(previousKeyId);
                        if (!keys.contains(previousKey)) {
                            keys.add(previousKey);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get verification keys from Redis, using current key only: {}", e.getMessage());
            redisAvailable = false;
        }
        
        return keys;
    }

    /**
     * Perform key rotation (scheduled task).
     */
    @Scheduled(fixedRate = 3600000) // Check every hour
    public void checkAndRotateKeys() {
        if (!rotationEnabled) {
            return;
        }
        
        ensureInitialized();
        
        if (!redisAvailable) {
            logger.debug("Redis not available, skipping key rotation check");
            return;
        }
        
        try {
            String nextRotationTimeStr = redisTemplate.opsForValue().get("jwt_key_rotation:next_rotation");
            if (nextRotationTimeStr != null) {
                long nextRotationTime = Long.parseLong(nextRotationTimeStr);
                if (System.currentTimeMillis() >= nextRotationTime) {
                    performKeyRotation();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to check key rotation schedule from Redis: {}", e.getMessage());
            redisAvailable = false;
        }
    }

    /**
     * Manually trigger key rotation.
     */
    public void forceKeyRotation() {
        if (rotationEnabled) {
            performKeyRotation();
            logKeyRotationEvent("MANUAL_KEY_ROTATION", "Key rotation manually triggered");
        }
    }

    /**
     * Perform the actual key rotation.
     */
    private void performKeyRotation() {
        try {
            String currentKeyId = redisTemplate.opsForValue().get("jwt_key_rotation:current");
            String currentTimestamp = redisTemplate.opsForValue().get("jwt_key_rotation:current_timestamp");
            
            // Move current to previous
            if (currentKeyId != null && currentTimestamp != null) {
                redisTemplate.opsForValue().set("jwt_key_rotation:previous", currentKeyId);
                redisTemplate.opsForValue().set("jwt_key_rotation:previous_timestamp", currentTimestamp);
            }
            
            // Set new current key
            assert currentKeyId != null;
            String newKeyId = getNextKeyId(currentKeyId);
            String newTimestamp = String.valueOf(System.currentTimeMillis());
            
            redisTemplate.opsForValue().set("jwt_key_rotation:current", newKeyId);
            redisTemplate.opsForValue().set("jwt_key_rotation:current_timestamp", newTimestamp);
            
            // Set next rotation time
            long nextRotationTime = System.currentTimeMillis() + (rotationIntervalHours * 3600000L);
            redisTemplate.opsForValue().set("jwt_key_rotation:next_rotation", String.valueOf(nextRotationTime));
            
            logKeyRotationEvent("KEY_ROTATION_COMPLETED", 
                String.format("Key rotated from %s to %s", currentKeyId, newKeyId));
            
        } catch (Exception e) {
            logKeyRotationEvent("KEY_ROTATION_ERROR", 
                "Key rotation failed: " + e.getMessage());
        }
    }

    /**
     * Get key by ID.
     */
    private String getKeyById(String keyId) {
        return switch (keyId) {
            case "primary" -> primarySecret;
            case "secondary" -> secondarySecret;
            case "tertiary" -> tertiarySecret;
            default -> primarySecret;
        };
    }

    /**
     * Get next key ID in rotation.
     */
    private String getNextKeyId(String currentKeyId) {
        return switch (currentKeyId) {
            case "primary" -> "secondary";
            case "secondary" -> "tertiary";
            case "tertiary" -> "primary";
            default -> "secondary";
        };
    }

    /**
     * Generate a secure secret key.
     */
    private String generateSecureSecret() {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * Log key rotation events.
     */
    private void logKeyRotationEvent(String event, String details) {
        String eventKey = "security_events:KEY_ROTATION:" + System.currentTimeMillis();
        String eventData = String.format("{\"event\":\"%s\",\"details\":\"%s\",\"timestamp\":\"%s\"}", 
            event, details, LocalDateTime.now());
        redisTemplate.opsForValue().set(eventKey, eventData, Duration.ofDays(30));
    }

    /**
     * Get key rotation status information.
     */
    public Map<String, Object> getRotationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", rotationEnabled);
        status.put("intervalHours", rotationIntervalHours);
        status.put("overlapHours", overlapHours);
        status.put("maxKeys", maxKeys);
        
        if (rotationEnabled) {
            status.put("currentKey", redisTemplate.opsForValue().get("jwt_key_rotation:current"));
            status.put("nextRotation", redisTemplate.opsForValue().get("jwt_key_rotation:next_rotation"));
            status.put("validKeysCount", getValidVerificationKeys().size());
        }
        
        return status;
    }
}