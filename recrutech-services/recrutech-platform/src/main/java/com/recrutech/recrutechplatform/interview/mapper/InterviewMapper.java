package com.recrutech.recrutechplatform.interview.mapper;

import com.recrutech.recrutechplatform.interview.dto.InterviewResponse;
import com.recrutech.recrutechplatform.interview.model.Interview;

/**
 * Mapper for converting between Interview entities and DTOs.
 */
public final class InterviewMapper {

    private InterviewMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts an Interview entity to a response DTO.
     *
     * @param entity the entity to convert
     * @return the response DTO
     */
    public static InterviewResponse toResponse(Interview entity) {
        return new InterviewResponse(
                entity.getId(),
                entity.getApplicationId(),
                entity.getInterviewType(),
                entity.getStatus(),
                entity.getScheduledAt(),
                entity.getDurationMinutes(),
                entity.getLocation(),
                entity.getMeetingLink(),
                entity.getDescription(),
                entity.getNotes(),
                entity.getInterviewerUserId(),
                entity.getFeedback(),
                entity.getRating(),
                entity.getCompletedAt(),
                entity.getCreatedByUserId(),
                entity.getUpdatedByUserId(),
                entity.getDeletedByUserId(),
                entity.isDeleted(),
                entity.getDeletedAt(),
                entity.getCreatedAt()
        );
    }
}
