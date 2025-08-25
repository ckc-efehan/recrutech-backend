package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Response DTO for GDPR Right to Deletion (Art. 17).
 * Returned after processing a user's data deletion request.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeletionResponse(
    
    String userId,
    
    LocalDateTime deletionDate,
    
    String status, // COMPLETED, FAILED, PARTIAL
    
    String message,
    
    String confirmationId,
    
    LocalDateTime retentionEndDate // When audit logs will be deleted
) {
    
    /**
     * Creates a successful deletion response
     */
    public static DeletionResponse createSuccessResponse(String userId, String message) {
        return DeletionResponse.builder()
            .userId(userId)
            .deletionDate(LocalDateTime.now())
            .status("COMPLETED")
            .message(message)
            .confirmationId(generateConfirmationId())
            .retentionEndDate(LocalDateTime.now().plusYears(7)) // 7 years retention for audit
            .build();
    }
    
    /**
     * Creates a failed deletion response
     */
    public static DeletionResponse createFailureResponse(String userId, String errorMessage) {
        return DeletionResponse.builder()
            .userId(userId)
            .deletionDate(LocalDateTime.now())
            .status("FAILED")
            .message(errorMessage)
            .build();
    }
    
    /**
     * Creates a partial deletion response (some data couldn't be deleted)
     */
    public static DeletionResponse createPartialResponse(String userId, String message) {
        return DeletionResponse.builder()
            .userId(userId)
            .deletionDate(LocalDateTime.now())
            .status("PARTIAL")
            .message(message)
            .confirmationId(generateConfirmationId())
            .retentionEndDate(LocalDateTime.now().plusYears(7))
            .build();
    }
    
    private static String generateConfirmationId() {
        return "DEL-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
}