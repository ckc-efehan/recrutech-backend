package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Request DTO for GDPR data deletion (Right to Erasure - Art. 17).
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeletionRequest(
    
    String reason,
    
    boolean confirmDeletion
) {
    
    /**
     * Validates business rules for deletion request
     */
    public void validateBusinessRules() {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Deletion reason is required");
        }
        if (!confirmDeletion) {
            throw new IllegalArgumentException("Deletion must be confirmed");
        }
    }
}