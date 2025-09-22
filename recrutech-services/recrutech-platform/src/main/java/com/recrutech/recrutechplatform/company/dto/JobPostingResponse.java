package com.recrutech.recrutechplatform.company.dto;

import com.recrutech.recrutechplatform.company.model.JobPostingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a job posting.
 */
public record JobPostingResponse(
        String id,
        String companyId,
        String title,
        String description,
        String location,
        String employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        JobPostingStatus status,
        LocalDateTime publishedAt,
        LocalDateTime expiresAt,
        boolean isDeleted,
        LocalDateTime createdAt,
        String createdByUserId,
        String updatedByUserId,
        String deletedByUserId,
        LocalDateTime deletedAt
) {
}
