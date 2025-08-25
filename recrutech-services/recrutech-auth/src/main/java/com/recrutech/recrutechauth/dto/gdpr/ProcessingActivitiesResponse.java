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
    public static ProcessingActivitiesResponse createSuccessResponse(String userId, List<ProcessingActivity> activities) {
        LocalDateTime earliest = activities.stream()
            .map(ProcessingActivity::timestamp)
            .min(LocalDateTime::compareTo)
            .orElse(null);
            
        LocalDateTime latest = activities.stream()
            .map(ProcessingActivity::timestamp)
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
}
