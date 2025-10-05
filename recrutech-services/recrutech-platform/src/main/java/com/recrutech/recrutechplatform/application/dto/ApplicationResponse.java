package com.recrutech.recrutechplatform.application.dto;

import com.recrutech.recrutechplatform.application.model.ApplicationStatus;

import java.time.LocalDateTime;

/**
 * Response payload representing a job application.
 */
public record ApplicationResponse(
        String id,
        String applicantId,
        String jobPostingId,
        String coverLetterPath,
        String resumePath,
        String portfolioPath,
        ApplicationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime interviewScheduledAt,
        LocalDateTime offerExtendedAt,
        LocalDateTime finalizedAt,
        String hrNotes,
        String rejectionReason,
        boolean isDeleted,
        LocalDateTime createdAt,
        String createdByUserId,
        String updatedByUserId,
        String deletedByUserId,
        LocalDateTime deletedAt
) {
}
