package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Request DTO for GDPR Right to Rectification (Art. 16).
 * Used when a user requests correction of their personal data.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RectificationRequest(
    
    PersonalDataUpdate personalData,
    
    Object roleSpecificData, // Can be HRDataUpdate, ApplicantDataUpdate, or CompanyDataUpdate
    
    @Size(max = 1000, message = "Rectification reason cannot exceed 1000 characters")
    String reason
) {
    
    /**
     * Validates the rectification request
     */
    public void validateBusinessRules() {
        if (personalData == null && roleSpecificData == null) {
            throw new IllegalArgumentException("At least one data field must be provided for rectification");
        }
        
        if (personalData != null) {
            personalData.validateBusinessRules();
        }
    }
}