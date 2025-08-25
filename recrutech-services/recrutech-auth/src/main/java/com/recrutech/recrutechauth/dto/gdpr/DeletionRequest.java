package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Request DTO for GDPR Right to Deletion (Art. 17).
 * Used when a user requests deletion of their personal data.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeletionRequest(
    
    @NotBlank(message = "Deletion reason is required")
    @Size(max = 500, message = "Deletion reason cannot exceed 500 characters")
    String reason,
    
    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    String additionalNotes,
    
    Boolean confirmDataLoss
) {
    
    /**
     * Default constructor with validation
     */
    public DeletionRequest {
        if (confirmDataLoss == null) {
            confirmDataLoss = false;
        }
    }
    
    /**
     * Validates the deletion request
     */
    public void validateBusinessRules() {
        if (!confirmDataLoss) {
            throw new IllegalArgumentException("User must confirm data loss before deletion");
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Deletion reason is required");
        }
    }
}