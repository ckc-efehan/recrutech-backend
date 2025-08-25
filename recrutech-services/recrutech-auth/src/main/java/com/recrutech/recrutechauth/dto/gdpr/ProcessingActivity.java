package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Individual processing activity record for GDPR compliance.
 * Represents a single data processing activity that must be logged
 * according to GDPR Article 30 (Records of processing activities).
 */
@Builder
@Jacksonized
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