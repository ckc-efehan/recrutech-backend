package com.recrutech.recrutechplatform.application.mapper;

import com.recrutech.recrutechplatform.application.dto.ApplicationResponse;
import com.recrutech.recrutechplatform.application.model.Application;

/**
 * Mapper for converting between Application entities and DTOs.
 */
public final class ApplicationMapper {

    private ApplicationMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts an Application entity to a response DTO.
     *
     * @param entity the entity to convert
     * @return the response DTO
     */
    public static ApplicationResponse toResponse(Application entity) {
        return new ApplicationResponse(
                entity.getId(),
                entity.getApplicantId(),
                entity.getJobPostingId(),
                entity.getCoverLetter(),
                entity.getResumeUrl(),
                entity.getPortfolioUrl(),
                entity.getStatus(),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getInterviewScheduledAt(),
                entity.getOfferExtendedAt(),
                entity.getFinalizedAt(),
                entity.getHrNotes(),
                entity.getRejectionReason(),
                entity.isDeleted(),
                entity.getCreatedAt(),
                entity.getCreatedByUserId(),
                entity.getUpdatedByUserId(),
                entity.getDeletedByUserId(),
                entity.getDeletedAt()
        );
    }
}
