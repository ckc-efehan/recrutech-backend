package com.recrutech.recrutechplatform.interview.dto;

import com.recrutech.recrutechplatform.interview.model.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Request payload for scheduling a new interview.
 */
@Schema(description = "Request payload for scheduling a new interview")
public record InterviewCreateRequest(
        @Schema(description = "ID of the job application this interview is for", example = "11111111-1111-1111-1111-111111111111", required = true)
        @NotBlank(message = "applicationId is required")
        String applicationId,

        @Schema(description = "Scheduled date and time for the interview (must be in the future)", example = "2025-10-15T10:00:00", required = true)
        @NotNull(message = "scheduledAt is required")
        @Future(message = "scheduledAt must be in the future")
        LocalDateTime scheduledAt,

        @Schema(description = "Duration of the interview in minutes", example = "60")
        @Positive(message = "durationMinutes must be positive")
        Integer durationMinutes,

        @Schema(description = "Type of interview", example = "VIDEO", required = true)
        @NotNull(message = "interviewType is required")
        InterviewType interviewType,

        @Schema(description = "Physical location for onsite interviews", example = "Conference Room A, Building 2")
        @Size(max = 500, message = "location must not exceed 500 characters")
        String location,

        @Schema(description = "Video meeting link for remote interviews", example = "https://meet.example.com/abc-def-ghi")
        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        @Schema(description = "ID of the user who will conduct the interview", example = "22222222-2222-2222-2222-222222222222")
        String interviewerAccountId,

        @Schema(description = "Description of the interview purpose or topics", example = "Technical interview focusing on backend development")
        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description,

        @Schema(description = "Internal notes about the interview", example = "Candidate requested morning slot")
        @Size(max = 2000, message = "notes must not exceed 2000 characters")
        String notes
) {
}
