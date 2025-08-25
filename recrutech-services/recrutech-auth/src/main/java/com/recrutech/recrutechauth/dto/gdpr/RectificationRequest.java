package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Request DTO for GDPR data rectification (Right to Rectification - Art. 16).
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RectificationRequest(
    
    String fieldName,
    
    String oldValue,
    
    String newValue,
    
    String reason,
    
    Object personalData,
    
    Object roleSpecificData
) {
    
    /**
     * Validates business rules for rectification request
     */
    public void validateBusinessRules() {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rectification reason is required");
        }
        if (newValue == null || newValue.trim().isEmpty()) {
            throw new IllegalArgumentException("New value is required");
        }
    }
}