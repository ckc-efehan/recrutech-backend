package com.recrutech.recrutechplatform.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for adding feedback to a completed interview.
 */
public record InterviewFeedbackRequest(
        @NotBlank(message = "feedback is required")
        @Size(max = 5000, message = "feedback must not exceed 5000 characters")
        String feedback,

        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must not exceed 5")
        Integer rating
) {
}
