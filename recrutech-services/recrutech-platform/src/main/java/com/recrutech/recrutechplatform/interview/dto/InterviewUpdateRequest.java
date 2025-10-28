package com.recrutech.recrutechplatform.interview.dto;

import com.recrutech.recrutechplatform.interview.model.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request payload for updating an existing interview.
 * All fields are optional to allow partial updates.
 */
@Schema(description = "Request payload for updating an existing interview. All fields are optional for partial updates.")
public record InterviewUpdateRequest(
        @Schema(description = "New scheduled date and time for the interview (must be in the future)", example = "2025-10-16T14:00:00")
        @Future(message = "scheduledAt must be in the future")
        LocalDateTime scheduledAt,

        @Schema(description = "New duration of the interview in minutes", example = "90")
        @Positive(message = "durationMinutes must be positive")
        Integer durationMinutes,

        @Schema(description = "New interview type", example = "ONSITE")
        InterviewType interviewType,

        @Schema(description = "New physical location for the interview", example = "Conference Room B, Building 1")
        @Size(max = 500, message = "location must not exceed 500 characters")
        String location,

        @Schema(description = "New video meeting link", example = "https://meet.example.com/xyz-123-456")
        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        @Schema(description = "New interviewer user ID", example = "33333333-3333-3333-3333-333333333333")
        String interviewerAccountId,

        @Schema(description = "Updated description of the interview", example = "System design interview")
        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description,

        @Schema(description = "Updated internal notes", example = "Rescheduled due to candidate request")
        @Size(max = 2000, message = "notes must not exceed 2000 characters")
        String notes
) {
}
