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

import java.time.LocalDateTime;

@Service
@Transactional
public class JobPostingService {

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
        JobPosting entity = repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new NotFoundException("Job posting not found"));
        return JobPostingMapper.toResponse(entity);
    }

    public JobPostingResponse update(String companyId, String id, String userId, JobPostingUpdateRequest request) {
        validateIds(companyId, userId);
        UuidValidator.validateUuid(id, "id");
        validateBusiness(request.salaryMin(), request.salaryMax(), request.currency(), request.expiresAt());

        JobPosting entity = repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new NotFoundException("Job posting not found"));

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
        JobPosting entity = repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new NotFoundException("Job posting not found"));
        if (entity.getStatus() == JobPostingStatus.PUBLISHED) {
            return JobPostingMapper.toResponse(entity); // idempotent
        }
        if (entity.getStatus() == JobPostingStatus.ARCHIVED) {
            throw new ValidationException("Cannot publish an archived job posting");
        }
        entity.setStatus(JobPostingStatus.PUBLISHED);
        entity.setPublishedAt(LocalDateTime.now());
        entity.setUpdatedByUserId(userId);
        return JobPostingMapper.toResponse(repository.save(entity));
    }

    public JobPostingResponse close(String companyId, String id, String userId) {
        validateIds(companyId, userId);
        UuidValidator.validateUuid(id, "id");
        JobPosting entity = repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new NotFoundException("Job posting not found"));
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
        JobPosting entity = repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new NotFoundException("Job posting not found"));
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedByUserId(userId);
        repository.save(entity);
    }

    private void validateIds(String companyId, String userId) {
        UuidValidator.validateUuid(companyId, "companyId");
        UuidValidator.validateUuid(userId, "userId");
    }

    private void validateBusiness(java.math.BigDecimal min, java.math.BigDecimal max, String currency, LocalDateTime expiresAt) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ValidationException("salaryMin cannot be greater than salaryMax");
        }
        if ((min != null || max != null) && currency == null) {
            throw new ValidationException("currency is required when salaryMin or salaryMax is provided");
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new ValidationException("expiresAt must be in the future");
        }
    }
}
