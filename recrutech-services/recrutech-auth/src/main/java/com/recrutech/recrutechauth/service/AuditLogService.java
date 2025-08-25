package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.gdpr.ProcessingActivity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Audit Log Service for GDPR compliance.
 * Handles logging of all data processing activities as required by GDPR Art. 30.
 * Uses Redis for fast storage and retrieval of audit logs.
 */
@Service
public class AuditLogService {

    private static final String AUDIT_LOG_PREFIX = "audit:";
    private static final String USER_ACTIVITIES_PREFIX = "user_activities:";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RedisTemplate<String, String> redisTemplate;

    public AuditLogService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Logs a data processing activity for GDPR compliance
     */
    public void logDataProcessing(String userId, String activityType, String description, String additionalDetails) {
        if (userId == null) {
            System.err.println("[AUDIT_LOG_ERROR] Cannot log activity for null userId");
            return;
        }
        
        try {
            ProcessingActivity activity = ProcessingActivity.builder()
                .activityId(generateActivityId())
                .userId(userId)
                .activityType(activityType)
                .description(description)
                .timestamp(LocalDateTime.now())
                .legalBasis(determineLegalBasis(activityType))
                .dataCategories(determineDataCategories(activityType))
                .processingPurpose(determineProcessingPurpose(activityType))
                .retentionPeriod(determineRetentionPeriod(activityType))
                .additionalDetails(additionalDetails)
                .build();

            storeActivity(activity);
            
        } catch (Exception e) {
            // Log the error but don't fail the main operation
            System.err.println("[AUDIT_LOG_ERROR] Failed to log activity for user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Logs a data processing activity with IP and user agent information
     */
    public void logDataProcessing(String userId, String activityType, String description, 
                                String ipAddress, String userAgent, String additionalDetails) {
        if (userId == null) {
            System.err.println("[AUDIT_LOG_ERROR] Cannot log activity for null userId");
            return;
        }
        
        try {
            ProcessingActivity activity = ProcessingActivity.builder()
                .activityId(generateActivityId())
                .userId(userId)
                .activityType(activityType)
                .description(description)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .legalBasis(determineLegalBasis(activityType))
                .dataCategories(determineDataCategories(activityType))
                .processingPurpose(determineProcessingPurpose(activityType))
                .retentionPeriod(determineRetentionPeriod(activityType))
                .additionalDetails(additionalDetails)
                .build();

            storeActivity(activity);
            
        } catch (Exception e) {
            // Log the error but don't fail the main operation
            System.err.println("[AUDIT_LOG_ERROR] Failed to log activity for user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves all processing activities for a specific user
     */
    public List<ProcessingActivity> getProcessingActivities(String userId) {
        try {
            String userActivitiesKey = USER_ACTIVITIES_PREFIX + userId;
            Set<String> activityIds = redisTemplate.opsForSet().members(userActivitiesKey);
            
            List<ProcessingActivity> activities = new ArrayList<>();
            if (activityIds != null) {
                for (String activityId : activityIds) {
                    String activityKey = AUDIT_LOG_PREFIX + activityId;
                    String activityJson = redisTemplate.opsForValue().get(activityKey);
                    if (activityJson != null) {
                        ProcessingActivity activity = deserializeActivity(activityJson);
                        if (activity != null) {
                            activities.add(activity);
                        }
                    }
                }
            }
            
            // Sort by timestamp (newest first)
            activities.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
            return activities;
            
        } catch (Exception e) {
            System.err.println("[AUDIT_LOG_ERROR] Failed to retrieve activities for user " + userId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves processing activities for a user within a date range
     */
    public List<ProcessingActivity> getProcessingActivities(String userId, LocalDateTime from, LocalDateTime to) {
        List<ProcessingActivity> allActivities = getProcessingActivities(userId);
        return allActivities.stream()
            .filter(activity -> !activity.timestamp().isBefore(from) && !activity.timestamp().isAfter(to))
            .toList();
    }

    /**
     * Deletes audit logs for a user (used during data deletion)
     * Note: This should only be called after the legal retention period
     */
    public void deleteUserAuditLogs(String userId) {
        try {
            String userActivitiesKey = USER_ACTIVITIES_PREFIX + userId;
            Set<String> activityIds = redisTemplate.opsForSet().members(userActivitiesKey);
            
            if (activityIds != null) {
                for (String activityId : activityIds) {
                    String activityKey = AUDIT_LOG_PREFIX + activityId;
                    redisTemplate.delete(activityKey);
                }
            }
            
            redisTemplate.delete(userActivitiesKey);
            
        } catch (Exception e) {
            System.err.println("[AUDIT_LOG_ERROR] Failed to delete audit logs for user " + userId + ": " + e.getMessage());
        }
    }

    // Private helper methods

    private void storeActivity(ProcessingActivity activity) {
        String activityKey = AUDIT_LOG_PREFIX + activity.activityId();
        String userActivitiesKey = USER_ACTIVITIES_PREFIX + activity.userId();
        
        // Serialize activity to JSON-like string
        String activityJson = serializeActivity(activity);
        
        // Store activity
        redisTemplate.opsForValue().set(activityKey, activityJson);
        
        // Add activity ID to user's activity set
        redisTemplate.opsForSet().add(userActivitiesKey, activity.activityId());
        
        // Set expiration (7 years for GDPR compliance)
        redisTemplate.expire(activityKey, java.time.Duration.ofDays(7 * 365));
        redisTemplate.expire(userActivitiesKey, java.time.Duration.ofDays(7 * 365));
    }

    private String serializeActivity(ProcessingActivity activity) {
        // Simple JSON-like serialization (in production, use proper JSON library)
        return String.format(
            "{\"activityId\":\"%s\",\"userId\":\"%s\",\"activityType\":\"%s\",\"description\":\"%s\"," +
            "\"timestamp\":\"%s\",\"ipAddress\":\"%s\",\"userAgent\":\"%s\",\"legalBasis\":\"%s\"," +
            "\"dataCategories\":\"%s\",\"processingPurpose\":\"%s\",\"retentionPeriod\":\"%s\"," +
            "\"additionalDetails\":\"%s\"}",
            activity.activityId(), activity.userId(), activity.activityType(), activity.description(),
            activity.timestamp().format(TIMESTAMP_FORMAT),
            activity.ipAddress() != null ? activity.ipAddress() : "",
            activity.userAgent() != null ? activity.userAgent() : "",
            activity.legalBasis(), activity.dataCategories(), activity.processingPurpose(),
            activity.retentionPeriod(),
            activity.additionalDetails() != null ? activity.additionalDetails() : ""
        );
    }

    private ProcessingActivity deserializeActivity(String activityJson) {
        try {
            // Simple JSON-like deserialization (in production, use proper JSON library)
            // This is a simplified implementation for demonstration
            String[] parts = activityJson.replace("{", "").replace("}", "").split(",");
            
            String activityId = extractValue(parts[0]);
            String userId = extractValue(parts[1]);
            String activityType = extractValue(parts[2]);
            String description = extractValue(parts[3]);
            String timestampStr = extractValue(parts[4]);
            String ipAddress = extractValue(parts[5]);
            String userAgent = extractValue(parts[6]);
            String legalBasis = extractValue(parts[7]);
            String dataCategories = extractValue(parts[8]);
            String processingPurpose = extractValue(parts[9]);
            String retentionPeriod = extractValue(parts[10]);
            String additionalDetails = extractValue(parts[11]);
            
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, TIMESTAMP_FORMAT);
            
            return ProcessingActivity.builder()
                .activityId(activityId)
                .userId(userId)
                .activityType(activityType)
                .description(description)
                .timestamp(timestamp)
                .ipAddress(ipAddress.isEmpty() ? null : ipAddress)
                .userAgent(userAgent.isEmpty() ? null : userAgent)
                .legalBasis(legalBasis)
                .dataCategories(dataCategories)
                .processingPurpose(processingPurpose)
                .retentionPeriod(retentionPeriod)
                .additionalDetails(additionalDetails.isEmpty() ? null : additionalDetails)
                .build();
                
        } catch (Exception e) {
            System.err.println("[AUDIT_LOG_ERROR] Failed to deserialize activity: " + e.getMessage());
            return null;
        }
    }

    private String extractValue(String keyValuePair) {
        String[] parts = keyValuePair.split(":", 2); // Split only on first colon
        if (parts.length >= 2) {
            return parts[1].replace("\"", "").trim();
        }
        return "";
    }

    private String generateActivityId() {
        return "ACT-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }

    private String determineLegalBasis(String activityType) {
        return switch (activityType) {
            case "LOGIN", "LOGOUT", "TOKEN_REFRESH" -> "CONTRACT";
            case "REGISTRATION" -> "CONSENT";
            case "DATA_EXPORT", "DATA_DELETION", "DATA_RECTIFICATION" -> "LEGAL_OBLIGATION";
            case "SECURITY_EVENT", "SUSPICIOUS_ACTIVITY" -> "LEGITIMATE_INTEREST";
            default -> "CONTRACT";
        };
    }

    private String determineDataCategories(String activityType) {
        return switch (activityType) {
            case "LOGIN", "LOGOUT", "TOKEN_REFRESH" -> "AUTHENTICATION_DATA";
            case "REGISTRATION" -> "PERSONAL_DATA,CONTACT_DATA";
            case "DATA_EXPORT", "DATA_DELETION", "DATA_RECTIFICATION" -> "ALL_PERSONAL_DATA";
            case "SECURITY_EVENT", "SUSPICIOUS_ACTIVITY" -> "SECURITY_DATA";
            default -> "PERSONAL_DATA";
        };
    }

    private String determineProcessingPurpose(String activityType) {
        return switch (activityType) {
            case "LOGIN", "LOGOUT", "TOKEN_REFRESH" -> "User authentication and access control";
            case "REGISTRATION" -> "Account creation and user onboarding";
            case "DATA_EXPORT" -> "GDPR compliance - Right to Data Portability";
            case "DATA_DELETION" -> "GDPR compliance - Right to Erasure";
            case "DATA_RECTIFICATION" -> "GDPR compliance - Right to Rectification";
            case "SECURITY_EVENT", "SUSPICIOUS_ACTIVITY" -> "Security monitoring and fraud prevention";
            default -> "Service provision and user management";
        };
    }

    private String determineRetentionPeriod(String activityType) {
        return switch (activityType) {
            case "LOGIN", "LOGOUT", "TOKEN_REFRESH" -> "Session duration + 30 days for security logs";
            case "REGISTRATION" -> "Account lifetime + 30 days";
            case "DATA_EXPORT", "DATA_DELETION", "DATA_RECTIFICATION" -> "7 years for audit purposes";
            case "SECURITY_EVENT", "SUSPICIOUS_ACTIVITY" -> "2 years for security analysis";
            default -> "Account lifetime";
        };
    }
}