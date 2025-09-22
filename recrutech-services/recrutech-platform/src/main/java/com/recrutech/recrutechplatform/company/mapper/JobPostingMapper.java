package com.recrutech.recrutechplatform.company.mapper;

import com.recrutech.recrutechplatform.company.dto.JobPostingCreateRequest;
import com.recrutech.recrutechplatform.company.dto.JobPostingResponse;
import com.recrutech.recrutechplatform.company.dto.JobPostingUpdateRequest;
import com.recrutech.recrutechplatform.company.model.JobPosting;

public final class JobPostingMapper {

    private JobPostingMapper() {}

    public static JobPosting toEntity(JobPostingCreateRequest request) {
        JobPosting e = new JobPosting();
        e.setTitle(request.title());
        e.setDescription(request.description());
        e.setLocation(request.location());
        e.setEmploymentType(request.employmentType());
        e.setSalaryMin(request.salaryMin());
        e.setSalaryMax(request.salaryMax());
        e.setCurrency(request.currency());
        e.setExpiresAt(request.expiresAt());
        return e;
    }

    public static void updateEntity(JobPostingUpdateRequest request, JobPosting e) {
        e.setTitle(request.title());
        e.setDescription(request.description());
        e.setLocation(request.location());
        e.setEmploymentType(request.employmentType());
        e.setSalaryMin(request.salaryMin());
        e.setSalaryMax(request.salaryMax());
        e.setCurrency(request.currency());
        e.setExpiresAt(request.expiresAt());
    }

    public static JobPostingResponse toResponse(JobPosting e) {
        return new JobPostingResponse(
                e.getId(),
                e.getCompanyId(),
                e.getTitle(),
                e.getDescription(),
                e.getLocation(),
                e.getEmploymentType(),
                e.getSalaryMin(),
                e.getSalaryMax(),
                e.getCurrency(),
                e.getStatus(),
                e.getPublishedAt(),
                e.getExpiresAt(),
                e.isDeleted(),
                e.getCreatedAt(),
                e.getCreatedByUserId(),
                e.getUpdatedByUserId(),
                e.getDeletedByUserId(),
                e.getDeletedAt()
        );
    }
}
