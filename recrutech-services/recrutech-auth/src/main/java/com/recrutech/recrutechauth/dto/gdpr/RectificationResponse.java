package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for GDPR Right to Rectification (Art. 16).
 * Returned after processing a user's data rectification request.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RectificationResponse(
    
    String userId,
    
    LocalDateTime rectificationDate,
    
    String status, // COMPLETED, FAILED, PARTIAL
    
    String message,
    
    List<String> updatedFields,
    
    List<String> failedFields,
    
    String confirmationId
) {
    
    /**
     * Creates a successful rectification response
     */
    public static RectificationResponse createSuccessResponse(String userId, List<String> updatedFields) {
        return RectificationResponse.builder()
            .userId(userId)
            .rectificationDate(LocalDateTime.now())
            .status("COMPLETED")
            .message("Personal data has been successfully updated")
            .updatedFields(updatedFields)
            .confirmationId(generateConfirmationId())
            .build();
    }
    
    /**
     * Creates a failed rectification response
     */
    public static RectificationResponse createFailureResponse(String userId, String errorMessage) {
        return RectificationResponse.builder()
            .userId(userId)
            .rectificationDate(LocalDateTime.now())
            .status("FAILED")
            .message(errorMessage)
            .build();
    }
    
    /**
     * Creates a partial rectification response (some fields couldn't be updated)
     */
    public static RectificationResponse createPartialResponse(String userId, 
                                                            List<String> updatedFields, 
                                                            List<String> failedFields,
                                                            String message) {
        return RectificationResponse.builder()
            .userId(userId)
            .rectificationDate(LocalDateTime.now())
            .status("PARTIAL")
            .message(message)
            .updatedFields(updatedFields)
            .failedFields(failedFields)
            .confirmationId(generateConfirmationId())
            .build();
    }
    
    private static String generateConfirmationId() {
        return "RECT-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
}