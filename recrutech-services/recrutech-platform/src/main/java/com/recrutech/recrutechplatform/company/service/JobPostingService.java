package com.recrutech.recrutechplatform.company.service;

import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.util.UuidValidator;
import com.recrutech.recrutechplatform.company.dto.JobPostingCreateRequest;
import com.recrutech.recrutechplatform.company.dto.JobPostingResponse;
import com.recrutech.recrutechplatform.company.dto.JobPostingUpdateRequest;
import com.recrutech.recrutechplatform.company.mapper.JobPostingMapper;
import com.recrutech.recrutechplatform.company.model.JobPosting;
import com.recrutech.recrutechplatform.company.model.JobPostingStatus;
import com.recrutech.recrutechplatform.company.repository.JobPostingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for managing job postings.
 * Handles CRUD operations, status transitions, and business validations.
 */
@Service
@Transactional
public class JobPostingService {

    private static final String JOB_POSTING_NOT_FOUND_MESSAGE = "Job posting not found";
    private static final String SALARY_MIN_GREATER_THAN_MAX_MESSAGE = "salaryMin cannot be greater than salaryMax";
    private static final String CURRENCY_REQUIRED_MESSAGE = "currency is required when salaryMin or salaryMax is provided";
    private static final String EXPIRES_AT_MUST_BE_FUTURE_MESSAGE = "expiresAt must be in the future";
    private static final String CANNOT_PUBLISH_ARCHIVED_MESSAGE = "Cannot publish an archived job posting";

    private final JobPostingRepository repository;

    public JobPostingService(JobPostingRepository repository) {
        this.repository = repository;
    }

    public JobPostingResponse create(String companyId, String userId, JobPostingCreateRequest request) {
        validateIds(companyId, userId);
        validateBusiness(request.salaryMin(), request.salaryMax(), request.currency(), request.expiresAt());

        JobPosting entity = JobPostingMapper.toEntity(request);
        entity.setCompanyId(companyId);
        entity.setCreatedByUserId(userId);

        JobPosting saved = repository.save(entity);
        return JobPostingMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JobPostingResponse getById(String companyId, String id) {
        UuidValidator.validateUuid(companyId, "companyId");
        UuidValidator.validateUuid(id, "id");
        JobPosting entity = findJobPostingByIdAndCompanyId(id, companyId);
        return JobPostingMapper.toResponse(entity);
    }

    public JobPostingResponse update(String companyId, String id, String userId, JobPostingUpdateRequest request) {
        validateIds(companyId, userId);
        UuidValidator.validateUuid(id, "id");
        validateBusiness(request.salaryMin(), request.salaryMax(), request.currency(), request.expiresAt());

        JobPosting entity = findJobPostingByIdAndCompanyId(id, companyId);

        JobPostingMapper.updateEntity(request, entity);
        entity.setUpdatedByUserId(userId);

        return JobPostingMapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<JobPostingResponse> list(String companyId, JobPostingStatus status, Pageable pageable) {
        UuidValidator.validateUuid(companyId, "companyId");
        Page<JobPosting> page = (status == null)
                ? repository.findAllByCompanyIdAndIsDeletedFalse(companyId, pageable)
                : repository.findAllByCompanyIdAndStatusAndIsDeletedFalse(companyId, status, pageable);
        return page.map(JobPostingMapper::toResponse);
    }

    public JobPostingResponse publish(String companyId, String id, String userId) {
        validateIds(companyId, userId);
        UuidValidator.validateUuid(id, "id");
        JobPosting entity = findJobPostingByIdAndCompanyId(id, companyId);
        
        if (entity.getStatus() == JobPostingStatus.PUBLISHED) {
            return JobPostingMapper.toResponse(entity); // idempotent
        }
        if (entity.getStatus() == JobPostingStatus.ARCHIVED) {
            throw new ValidationException(CANNOT_PUBLISH_ARCHIVED_MESSAGE);
        }
        
        entity.setStatus(JobPostingStatus.PUBLISHED);
        entity.setPublishedAt(LocalDateTime.now());
        entity.setUpdatedByUserId(userId);
        return JobPostingMapper.toResponse(repository.save(entity));
    }

    public JobPostingResponse close(String companyId, String id, String userId) {
        validateIds(companyId, userId);
        UuidValidator.validateUuid(id, "id");
        JobPosting entity = findJobPostingByIdAndCompanyId(id, companyId);
        
        if (entity.getStatus() == JobPostingStatus.ARCHIVED) {
            return JobPostingMapper.toResponse(entity); // idempotent
        }
        
        entity.setStatus(JobPostingStatus.ARCHIVED);
        entity.setUpdatedByUserId(userId);
        return JobPostingMapper.toResponse(repository.save(entity));
    }

    public void softDelete(String companyId, String id, String userId) {
        validateIds(companyId, userId);
        UuidValidator.validateUuid(id, "id");
        JobPosting entity = findJobPostingByIdAndCompanyId(id, companyId);
        
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedByUserId(userId);
        repository.save(entity);
    }

    private void validateIds(String companyId, String userId) {
        UuidValidator.validateUuid(companyId, "companyId");
        UuidValidator.validateUuid(userId, "userId");
    }

    private void validateBusiness(BigDecimal min, BigDecimal max, String currency, LocalDateTime expiresAt) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ValidationException(SALARY_MIN_GREATER_THAN_MAX_MESSAGE);
        }
        if ((min != null || max != null) && currency == null) {
            throw new ValidationException(CURRENCY_REQUIRED_MESSAGE);
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new ValidationException(EXPIRES_AT_MUST_BE_FUTURE_MESSAGE);
        }
    }

    /**
     * Finds a job posting by ID and company ID, ensuring it's not deleted.
     *
     * @param id the job posting ID
     * @param companyId the company ID
     * @return the found job posting
     * @throws NotFoundException if the job posting is not found
     */
    private JobPosting findJobPostingByIdAndCompanyId(String id, String companyId) {
        return repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new NotFoundException(JOB_POSTING_NOT_FOUND_MESSAGE));
    }
}
