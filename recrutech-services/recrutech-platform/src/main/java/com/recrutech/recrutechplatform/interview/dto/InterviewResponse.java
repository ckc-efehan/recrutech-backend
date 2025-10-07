package com.recrutech.recrutechplatform.interview.dto;

import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import com.recrutech.recrutechplatform.interview.model.InterviewType;

import java.time.LocalDateTime;

/**
 * Response payload representing an interview.
 */
public record InterviewResponse(
        String id,
        String applicationId,
        InterviewType interviewType,
        InterviewStatus status,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        String location,
        String meetingLink,
        String description,
        String notes,
        String interviewerUserId,
        String feedback,
        Integer rating,
        LocalDateTime completedAt,
        String createdByUserId,
        String updatedByUserId,
        String deletedByUserId,
        boolean isDeleted,
        LocalDateTime deletedAt,
        LocalDateTime createdAt
) {
}
