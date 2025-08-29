package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Individual processing activity record for GDPR compliance.
 * Represents a single data processing activity that must be logged
 * according to GDPR Article 30 (Records of processing activities).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessingActivity(
    
    String activityId,
    
    String userId,
    
    String activityType, // LOGIN, REGISTRATION, DATA_EXPORT, DATA_DELETION, etc.
    
    String description,
    
    LocalDateTime timestamp,
    
    String ipAddress,
    
    String userAgent,
    
    String legalBasis, // CONSENT, CONTRACT, LEGITIMATE_INTEREST, etc.
    
    String dataCategories, // PERSONAL_DATA, CONTACT_DATA, EMPLOYMENT_DATA, etc.
    
    String processingPurpose,
    
    String retentionPeriod,
    
    String additionalDetails
) {
    
    // Manually implemented public builder to avoid Lombok builder visibility issues
    public static ProcessingActivityBuilder builder() {
        return new ProcessingActivityBuilder();
    }
    
    public static final class ProcessingActivityBuilder {
        private String activityId;
        private String userId;
        private String activityType;
        private String description;
        private LocalDateTime timestamp;
        private String ipAddress;
        private String userAgent;
        private String legalBasis;
        private String dataCategories;
        private String processingPurpose;
        private String retentionPeriod;
        private String additionalDetails;
        
        public ProcessingActivityBuilder activityId(String activityId) {
            this.activityId = activityId; return this;
        }
        public ProcessingActivityBuilder userId(String userId) {
            this.userId = userId; return this;
        }
        public ProcessingActivityBuilder activityType(String activityType) {
            this.activityType = activityType; return this;
        }
        public ProcessingActivityBuilder description(String description) {
            this.description = description; return this;
        }
        public ProcessingActivityBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp; return this;
        }
        public ProcessingActivityBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress; return this;
        }
        public ProcessingActivityBuilder userAgent(String userAgent) {
            this.userAgent = userAgent; return this;
        }
        public ProcessingActivityBuilder legalBasis(String legalBasis) {
            this.legalBasis = legalBasis; return this;
        }
        public ProcessingActivityBuilder dataCategories(String dataCategories) {
            this.dataCategories = dataCategories; return this;
        }
        public ProcessingActivityBuilder processingPurpose(String processingPurpose) {
            this.processingPurpose = processingPurpose; return this;
        }
        public ProcessingActivityBuilder retentionPeriod(String retentionPeriod) {
            this.retentionPeriod = retentionPeriod; return this;
        }
        public ProcessingActivityBuilder additionalDetails(String additionalDetails) {
            this.additionalDetails = additionalDetails; return this;
        }
        public ProcessingActivity build() {
            return new ProcessingActivity(activityId, userId, activityType, description, timestamp,
                    ipAddress, userAgent, legalBasis, dataCategories, processingPurpose, retentionPeriod, additionalDetails);
        }
    }
    
    /**
     * Creates a processing activity for login events
     */
    public static ProcessingActivity createLoginActivity(String userId, String ipAddress, String userAgent) {
        return ProcessingActivity.builder()
            .activityId(generateActivityId())
            .userId(userId)
            .activityType("LOGIN")
            .description("User authentication and session creation")
            .timestamp(LocalDateTime.now())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .legalBasis("CONTRACT")
            .dataCategories("AUTHENTICATION_DATA")
            .processingPurpose("User authentication and access control")
            .retentionPeriod("Session duration + 30 days for security logs")
            .build();
    }
    
    /**
     * Creates a processing activity for data export events
     */
    public static ProcessingActivity createDataExportActivity(String userId) {
        return ProcessingActivity.builder()
            .activityId(generateActivityId())
            .userId(userId)
            .activityType("DATA_EXPORT")
            .description("User requested data export under GDPR Art. 20")
            .timestamp(LocalDateTime.now())
            .legalBasis("LEGAL_OBLIGATION")
            .dataCategories("ALL_PERSONAL_DATA")
            .processingPurpose("GDPR compliance - Right to Data Portability")
            .retentionPeriod("7 years for audit purposes")
            .build();
    }
    
    /**
     * Creates a processing activity for data deletion events
     */
    public static ProcessingActivity createDataDeletionActivity(String userId, String reason) {
        return ProcessingActivity.builder()
            .activityId(generateActivityId())
            .userId(userId)
            .activityType("DATA_DELETION")
            .description("User requested data deletion under GDPR Art. 17")
            .timestamp(LocalDateTime.now())
            .legalBasis("LEGAL_OBLIGATION")
            .dataCategories("ALL_PERSONAL_DATA")
            .processingPurpose("GDPR compliance - Right to Erasure")
            .retentionPeriod("7 years for audit purposes")
            .additionalDetails("Deletion reason: " + reason)
            .build();
    }
    
    private static String generateActivityId() {
        return "ACT-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
}