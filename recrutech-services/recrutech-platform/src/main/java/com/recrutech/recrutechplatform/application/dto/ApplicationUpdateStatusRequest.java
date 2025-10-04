package com.recrutech.recrutechplatform.application.dto;

import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating an application's status.
 * Used by HR/recruiters to move applications through the hiring workflow.
 */
public record ApplicationUpdateStatusRequest(
        @NotNull(message = "status is required")
        ApplicationStatus status,
        
        String hrNotes,
        
        String rejectionReason
) {
}
