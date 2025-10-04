package com.recrutech.recrutechplatform.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for submitting a new job application.
 */
public record ApplicationSubmitRequest(
        @NotBlank(message = "applicantId is required")
        String applicantId,
        
        @NotBlank(message = "jobPostingId is required")
        String jobPostingId,
        
        String coverLetter,
        
        String resumeUrl,
        
        String portfolioUrl
) {
}
