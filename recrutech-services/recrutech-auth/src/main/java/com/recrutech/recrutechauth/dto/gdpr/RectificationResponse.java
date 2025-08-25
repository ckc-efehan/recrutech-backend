package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Response DTO for GDPR data rectification (Right to Rectification - Art. 16).
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RectificationResponse(
    
    String userId,
    
    boolean success,
    
    String message,
    
    LocalDateTime rectificationDate,
    
    String status
) {
    
    /**
     * Creates a failure response for rectification request
     */
    public static RectificationResponse createFailureResponse(String userId, String errorMessage) {
        return RectificationResponse.builder()
            .userId(userId)
            .success(false)
            .message(errorMessage)
            .rectificationDate(null)
            .build();
    }
}