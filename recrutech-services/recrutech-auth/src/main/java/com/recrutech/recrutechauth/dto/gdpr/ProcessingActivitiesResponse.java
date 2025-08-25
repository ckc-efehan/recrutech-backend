package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for GDPR Processing Activities (Art. 30).
 * Contains the record of processing activities for a user.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessingActivitiesResponse(
    
    String userId,
    
    LocalDateTime requestDate,
    
    List<ProcessingActivity> activities,
    
    Integer totalActivities,
    
    LocalDateTime earliestActivity,
    
    LocalDateTime latestActivity
) {
    
    /**
     * Creates a successful processing activities response
     */
    public static ProcessingActivitiesResponse createSuccessResponse(String userId, List<com.recrutech.recrutechauth.dto.gdpr.ProcessingActivity> standaloneActivities) {
        // Convert standalone ProcessingActivity to nested ProcessingActivity
        List<ProcessingActivity> activities = standaloneActivities.stream()
            .map(sa -> ProcessingActivity.builder()
                .activityType(sa.activityType())
                .purpose(sa.processingPurpose())
                .dataCategories(sa.dataCategories() != null ? List.of(sa.dataCategories().split(",")) : List.of())
                .legalBasis(sa.legalBasis())
                .build())
            .toList();
        
        // Calculate earliest and latest activity timestamps
        LocalDateTime earliest = standaloneActivities.stream()
            .map(com.recrutech.recrutechauth.dto.gdpr.ProcessingActivity::timestamp)
            .filter(timestamp -> timestamp != null)
            .min(LocalDateTime::compareTo)
            .orElse(null);
            
        LocalDateTime latest = standaloneActivities.stream()
            .map(com.recrutech.recrutechauth.dto.gdpr.ProcessingActivity::timestamp)
            .filter(timestamp -> timestamp != null)
            .max(LocalDateTime::compareTo)
            .orElse(null);
            
        return ProcessingActivitiesResponse.builder()
            .userId(userId)
            .requestDate(LocalDateTime.now())
            .activities(activities)
            .totalActivities(activities.size())
            .earliestActivity(earliest)
            .latestActivity(latest)
            .build();
    }
    
    /**
     * Nested ProcessingActivity class for test compatibility
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static record ProcessingActivity(
        
        String activityType,
        
        String purpose,
        
        List<String> dataCategories,
        
        String legalBasis
    ) {
    }
}
