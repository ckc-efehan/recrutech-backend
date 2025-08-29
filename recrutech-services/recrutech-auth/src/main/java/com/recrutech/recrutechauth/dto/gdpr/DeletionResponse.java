package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Response DTO for GDPR data deletion (Right to Erasure - Art. 17).
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeletionResponse(
    
    String userId,
    
    boolean success,
    
    String message,
    
    LocalDateTime deletionDate,
    
    String status
) {
    
    /**
     * Creates a failure response for deletion request
     */
    public static DeletionResponse createFailureResponse(String userId, String errorMessage) {
        return DeletionResponse.builder()
            .userId(userId)
            .success(false)
            .status("FAILED")
            .message(errorMessage)
            .deletionDate(null)
            .build();
    }
}