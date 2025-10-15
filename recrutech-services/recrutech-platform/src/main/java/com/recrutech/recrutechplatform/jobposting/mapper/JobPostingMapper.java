package com.recrutech.recrutechplatform.jobposting.mapper;

import com.recrutech.recrutechplatform.jobposting.dto.JobPostingCreateRequest;
import com.recrutech.recrutechplatform.jobposting.dto.JobPostingResponse;
import com.recrutech.recrutechplatform.jobposting.dto.JobPostingUpdateRequest;
import com.recrutech.recrutechplatform.jobposting.model.JobPosting;

/**
 * Mapper for converting between JobPosting entities and DTOs.
 */
public final class JobPostingMapper {

    private JobPostingMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a create request DTO to a JobPosting entity.
     *
     * @param request the create request
     * @return the new JobPosting entity
     */
    public static JobPosting toEntity(JobPostingCreateRequest request) {
        JobPosting entity = new JobPosting();
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setLocation(request.location());
        entity.setEmploymentType(request.employmentType());
        entity.setSalaryMin(request.salaryMin());
        entity.setSalaryMax(request.salaryMax());
        entity.setCurrency(request.currency());
        entity.setExpiresAt(request.expiresAt());
        return entity;
    }

    /**
     * Updates an existing JobPosting entity with data from an update request.
     *
     * @param request the update request
     * @param entity the entity to update
     */
    public static void updateEntity(JobPostingUpdateRequest request, JobPosting entity) {
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setLocation(request.location());
        entity.setEmploymentType(request.employmentType());
        entity.setSalaryMin(request.salaryMin());
        entity.setSalaryMax(request.salaryMax());
        entity.setCurrency(request.currency());
        entity.setExpiresAt(request.expiresAt());
    }

    /**
     * Converts a JobPosting entity to a response DTO.
     *
     * @param entity the entity to convert
     * @return the response DTO
     */
    public static JobPostingResponse toResponse(JobPosting entity) {
        return new JobPostingResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getEmploymentType(),
                entity.getSalaryMin(),
                entity.getSalaryMax(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getPublishedAt(),
                entity.getExpiresAt(),
                entity.isDeleted(),
                entity.getCreatedAt(),
                entity.getCreatedByUserId(),
                entity.getUpdatedByUserId(),
                entity.getDeletedByUserId(),
                entity.getDeletedAt()
        );
    }
}
