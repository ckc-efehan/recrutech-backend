package com.recrutech.recrutechplatform.interview.dto;

import com.recrutech.recrutechplatform.interview.model.InterviewType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Request payload for scheduling a new interview.
 */
public record InterviewCreateRequest(
        @NotBlank(message = "applicationId is required")
        String applicationId,

        @NotNull(message = "scheduledAt is required")
        @Future(message = "scheduledAt must be in the future")
        LocalDateTime scheduledAt,

        @Positive(message = "durationMinutes must be positive")
        Integer durationMinutes,

        @NotNull(message = "interviewType is required")
        InterviewType interviewType,

        @Size(max = 500, message = "location must not exceed 500 characters")
        String location,

        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        String interviewerUserId,

        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description,

        @Size(max = 2000, message = "notes must not exceed 2000 characters")
        String notes
) {
}
