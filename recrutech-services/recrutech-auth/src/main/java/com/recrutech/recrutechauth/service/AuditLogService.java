package com.recrutech.recrutechauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.recrutech.recrutechauth.dto.gdpr.ProcessingActivity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final ObjectMapper objectMapper;

    public AuditLogService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // Configure ObjectMapper with JavaTimeModule to handle LocalDateTime as ISO-8601
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Logs a data processing activity for GDPR compliance
     */
    public void logDataProcessing(String userId, String activityType, String description, String additionalDetails) {
        if (userId == null) {
            // Silently ignore logging when userId is null
            return;
        }
        if (activityType == null) {
            // Silently ignore invalid activityType
            return;
        }
        
        try {
            ProcessingActivity activity;
            switch (activityType) {
                case "DATA_EXPORT" -> {
                    ProcessingActivity base = ProcessingActivity.createDataExportActivity(userId);
                    activity = getProcessingActivity(additionalDetails, base);
                }
                case "DATA_DELETION" -> activity = ProcessingActivity.createDataDeletionActivity(userId, additionalDetails);
                default -> activity = ProcessingActivity.builder()
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
            }

            if (activity == null) {
                // Defensive guard: should not happen due to switch logic
                return;
            }
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
            // Silently ignore logging when userId is null
            return;
        }
        if (activityType == null || activityType.isBlank()) {
            // Silently ignore invalid activityType
            return;
        }
        
        try {
            ProcessingActivity activity;
            if ("LOGIN".equals(activityType)) {
                ProcessingActivity base = ProcessingActivity.createLoginActivity(userId, ipAddress, userAgent);
                activity = getProcessingActivity(additionalDetails, base);
            } else {
                activity = ProcessingActivity.builder()
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
            }

            storeActivity(activity);
            
        } catch (Exception e) {
            // Log the error but don't fail the main operation
            System.err.println("[AUDIT_LOG_ERROR] Failed to log activity for user " + userId + ": " + e.getMessage());
        }
    }

    private ProcessingActivity getProcessingActivity(String additionalDetails, ProcessingActivity base) {
        ProcessingActivity activity;
        if (additionalDetails != null && !additionalDetails.isEmpty()) {
            activity = ProcessingActivity.builder()
                    .activityId(base.activityId())
                    .userId(base.userId())
                    .activityType(base.activityType())
                    .description(base.description())
                    .timestamp(base.timestamp())
                    .ipAddress(base.ipAddress())
                    .userAgent(base.userAgent())
                    .legalBasis(base.legalBasis())
                    .dataCategories(base.dataCategories())
                    .processingPurpose(base.processingPurpose())
                    .retentionPeriod(base.retentionPeriod())
                    .additionalDetails(additionalDetails)
                    .build();
        } else {
            activity = base;
        }
        return activity;
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
        
        // Serialize activity to JSON string using ObjectMapper
        String activityJson = serializeActivity(activity);
        if (activityJson == null) {
            System.err.println("[AUDIT_LOG_ERROR] Skipping store due to serialization failure for activity " + activity.activityId());
            return;
        }
        
        // Store activity
        redisTemplate.opsForValue().set(activityKey, activityJson);
        
        // Add activity ID to user's activity set
        redisTemplate.opsForSet().add(userActivitiesKey, activity.activityId());
        
        // Set expiration (7 years for GDPR compliance)
        redisTemplate.expire(activityKey, Duration.ofDays(7 * 365));
        redisTemplate.expire(userActivitiesKey, Duration.ofDays(7 * 365));
    }

    private String serializeActivity(ProcessingActivity activity) {
        try {
            return objectMapper.writeValueAsString(activity);
        } catch (Exception e) {
            System.err.println("[AUDIT_LOG_ERROR] Failed to serialize activity: " + e.getMessage());
            return null;
        }
    }

    private ProcessingActivity deserializeActivity(String activityJson) {
        try {
            return objectMapper.readValue(activityJson, ProcessingActivity.class);
        } catch (Exception e) {
            System.err.println("[AUDIT_LOG_ERROR] Failed to deserialize activity: " + e.getMessage());
            return null;
        }
    }


    private String generateActivityId() {
        return "ACT-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }

    private String determineLegalBasis(String activityType) {
        return switch (activityType) {
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