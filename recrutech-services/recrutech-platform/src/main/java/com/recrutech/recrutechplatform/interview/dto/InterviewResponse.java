package com.recrutech.recrutechplatform.interview.dto;

import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import com.recrutech.recrutechplatform.interview.model.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response payload representing an interview.
 */
@Schema(description = "Response payload representing an interview with all its details")
public record InterviewResponse(
        @Schema(description = "Unique identifier of the interview", example = "12345678-1234-1234-1234-123456789012")
        String id,
        
        @Schema(description = "ID of the job application this interview is associated with", example = "11111111-1111-1111-1111-111111111111")
        String applicationId,
        
        @Schema(description = "Type of the interview (PHONE, VIDEO, ONSITE)", example = "VIDEO")
        InterviewType interviewType,
        
        @Schema(description = "Current status of the interview", example = "SCHEDULED")
        InterviewStatus status,
        
        @Schema(description = "Scheduled date and time for the interview", example = "2025-10-15T10:00:00")
        LocalDateTime scheduledAt,
        
        @Schema(description = "Duration of the interview in minutes", example = "60")
        Integer durationMinutes,
        
        @Schema(description = "Physical location for onsite interviews", example = "Conference Room A, Building 2")
        String location,
        
        @Schema(description = "Video meeting link for remote interviews", example = "https://meet.example.com/abc-def-ghi")
        String meetingLink,
        
        @Schema(description = "Description of the interview purpose or topics", example = "Technical interview focusing on backend development")
        String description,
        
        @Schema(description = "Internal notes about the interview", example = "Candidate requested morning slot")
        String notes,
        
        @Schema(description = "ID of the user conducting the interview", example = "22222222-2222-2222-2222-222222222222")
        String interviewerUserId,
        
        @Schema(description = "Interviewer's feedback (populated after interview completion)", example = "Strong technical skills demonstrated")
        String feedback,
        
        @Schema(description = "Rating given by the interviewer (1-5 scale)", example = "4", minimum = "1", maximum = "5")
        Integer rating,
        
        @Schema(description = "Date and time when the interview was completed", example = "2025-10-15T11:00:00")
        LocalDateTime completedAt,
        
        @Schema(description = "ID of the user who created the interview", example = "33333333-3333-3333-3333-333333333333")
        String createdByUserId,
        
        @Schema(description = "ID of the user who last updated the interview", example = "33333333-3333-3333-3333-333333333333")
        String updatedByUserId,
        
        @Schema(description = "ID of the user who deleted/cancelled the interview", example = "33333333-3333-3333-3333-333333333333")
        String deletedByUserId,
        
        @Schema(description = "Flag indicating if the interview has been cancelled/deleted", example = "false")
        boolean isDeleted,
        
        @Schema(description = "Date and time when the interview was deleted/cancelled", example = "2025-10-14T16:00:00")
        LocalDateTime deletedAt,
        
        @Schema(description = "Date and time when the interview was created", example = "2025-10-07T09:00:00")
        LocalDateTime createdAt
) {
}
