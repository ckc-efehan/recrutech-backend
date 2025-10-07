package com.recrutech.recrutechplatform.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for adding feedback to a completed interview.
 */
@Schema(description = "Request payload for adding feedback to a completed interview")
public record InterviewFeedbackRequest(
        @Schema(description = "Interviewer's feedback about the candidate's performance", 
                example = "The candidate demonstrated strong technical skills and excellent problem-solving abilities. Communication was clear and concise.", 
                required = true)
        @NotBlank(message = "feedback is required")
        @Size(max = 5000, message = "feedback must not exceed 5000 characters")
        String feedback,

        @Schema(description = "Overall rating of the candidate (1-5 scale, where 1 is poor and 5 is excellent)", 
                example = "4", 
                minimum = "1", 
                maximum = "5")
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must not exceed 5")
        Integer rating
) {
}
