package com.recrutech.recrutechplatform.application.service;

import com.recrutech.common.exception.EntityReferenceNotFoundException;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.util.UuidValidator;
import com.recrutech.recrutechplatform.application.model.Application;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import com.recrutech.recrutechplatform.application.repository.ApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing job applications.
 * Handles application lifecycle from submission to final decision.
 * Best practices applied:
 * - Duplicate application prevention
 * - Status transition validation
 * - Audit trail maintenance
 * - Business rule enforcement
 */
@Service
@Transactional
public class ApplicationService {

    private static final String APPLICATION_NOT_FOUND_MESSAGE = "Application not found";
    private static final String DUPLICATE_APPLICATION_MESSAGE = "You have already applied to this job posting";
    private static final String INVALID_STATUS_TRANSITION_MESSAGE = "Invalid status transition from %s to %s";
    private static final String CANNOT_MODIFY_FINALIZED_MESSAGE = "Cannot modify an application that has been finalized (ACCEPTED, REJECTED, or WITHDRAWN)";

    private final ApplicationRepository repository;
    private final MinioStorageService storageService;

    public ApplicationService(ApplicationRepository repository, MinioStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    /**
     * Submit a new application to a job posting.
     * Validates that the applicant hasn't already applied to this job.
     * Validates that referenced entities (applicant, job posting) exist.
     */
    public Application submit(String applicantId, String jobPostingId, String userId, 
                               String coverLetterPath, String resumePath, String portfolioPath) {
        validateIds(applicantId, jobPostingId, userId);
        
        // Validate that applicant exists before attempting to create application
        if (!repository.applicantExists(applicantId)) {
            throw new EntityReferenceNotFoundException(
                "Applicant profile does not exist. Please create an applicant profile before submitting an application.");
        }
        
        // Validate that job posting exists and is not deleted
        if (!repository.jobPostingExists(jobPostingId)) {
            throw new EntityReferenceNotFoundException(
                "Job posting does not exist or has been deleted.");
        }
        
        // Check for duplicate application
        if (repository.existsByApplicantIdAndJobPostingIdAndIsDeletedFalse(applicantId, jobPostingId)) {
            throw new ValidationException(DUPLICATE_APPLICATION_MESSAGE);
        }

        Application application = new Application();
        application.setApplicantId(applicantId);
        application.setJobPostingId(jobPostingId);
        application.setCreatedByUserId(userId);
        application.setCoverLetterPath(coverLetterPath);
        application.setResumePath(resumePath);
        application.setPortfolioPath(portfolioPath);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());

        return repository.save(application);
    }

    /**
     * Get application by ID.
     */
    @Transactional(readOnly = true)
    public Application getById(String id) {
        UuidValidator.validateUuid(id, "id");
        return findApplicationById(id);
    }

    /**
     * Get all applications for an applicant.
     */
    @Transactional(readOnly = true)
    public Page<Application> getByApplicant(String applicantId, ApplicationStatus status, Pageable pageable) {
        UuidValidator.validateUuid(applicantId, "applicantId");
        return (status == null)
                ? repository.findAllByApplicantIdAndIsDeletedFalse(applicantId, pageable)
                : repository.findAllByApplicantIdAndStatusAndIsDeletedFalse(applicantId, status, pageable);
    }

    /**
     * Get all applications for a job posting.
     */
    @Transactional(readOnly = true)
    public Page<Application> getByJobPosting(String jobPostingId, ApplicationStatus status, Pageable pageable) {
        UuidValidator.validateUuid(jobPostingId, "jobPostingId");
        return (status == null)
                ? repository.findAllByJobPostingIdAndIsDeletedFalse(jobPostingId, pageable)
                : repository.findAllByJobPostingIdAndStatusAndIsDeletedFalse(jobPostingId, status, pageable);
    }

    /**
     * Get all applications for a company's job postings.
     */
    @Transactional(readOnly = true)
    public Page<Application> getByCompany(String companyId, ApplicationStatus status, Pageable pageable) {
        UuidValidator.validateUuid(companyId, "companyId");
        return (status == null)
                ? repository.findAllByCompanyId(companyId, pageable)
                : repository.findAllByCompanyIdAndStatus(companyId, status, pageable);
    }

    /**
     * Update application status by HR/recruiter.
     * Validates status transitions and updates relevant timestamps.
     */
    public Application updateStatus(String id, ApplicationStatus newStatus, String userId, 
                                     String hrNotes, String rejectionReason) {
        UuidValidator.validateUuid(id, "id");
        UuidValidator.validateUuid(userId, "userId");

        Application application = findApplicationById(id);
        
        // Prevent modification of finalized applications
        if (isFinalized(application.getStatus())) {
            throw new ValidationException(CANNOT_MODIFY_FINALIZED_MESSAGE);
        }

        // Validate status transition
        validateStatusTransition(application.getStatus(), newStatus);

        application.setStatus(newStatus);
        application.setUpdatedByUserId(userId);
        
        if (hrNotes != null) {
            application.setHrNotes(hrNotes);
        }
        
        if (rejectionReason != null) {
            application.setRejectionReason(rejectionReason);
        }

        // Update lifecycle timestamps
        updateLifecycleTimestamps(application, newStatus);

        return repository.save(application);
    }

    /**
     * Withdraw application by applicant.
     */
    public Application withdraw(String id, String applicantId, String userId) {
        validateIds(applicantId, userId);
        UuidValidator.validateUuid(id, "id");

        Application application = findApplicationById(id);
        
        // Verify ownership
        if (!application.getApplicantId().equals(applicantId)) {
            throw new ValidationException("You can only withdraw your own applications");
        }
        
        // Prevent withdrawal of finalized applications
        if (isFinalized(application.getStatus())) {
            throw new ValidationException(CANNOT_MODIFY_FINALIZED_MESSAGE);
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setUpdatedByUserId(userId);
        application.setFinalizedAt(LocalDateTime.now());

        return repository.save(application);
    }

    /**
     * Soft delete an application.
     * Also cleans up associated files from MinIO storage to prevent resource leaks.
     */
    public void softDelete(String id, String userId) {
        UuidValidator.validateUuid(id, "id");
        UuidValidator.validateUuid(userId, "userId");

        Application application = findApplicationById(id);
        
        // Clean up files from MinIO storage before deleting the application
        deleteApplicationFiles(application);
        
        application.setDeleted(true);
        application.setDeletedAt(LocalDateTime.now());
        application.setDeletedByUserId(userId);
        
        repository.save(application);
    }

    /**
     * Deletes all associated files for an application from MinIO storage.
     * Handles missing or null file paths gracefully.
     * 
     * @param application the application whose files should be deleted
     */
    private void deleteApplicationFiles(Application application) {
        // Delete cover letter
        if (application.getCoverLetterPath() != null) {
            storageService.deleteFile(application.getCoverLetterPath());
        }
        
        // Delete resume
        if (application.getResumePath() != null) {
            storageService.deleteFile(application.getResumePath());
        }
        
        // Delete portfolio if it exists
        if (application.getPortfolioPath() != null) {
            storageService.deleteFile(application.getPortfolioPath());
        }
    }

    /**
     * Generates a temporary presigned URL for accessing an application document.
     * The URL is valid for the specified duration and provides secure, temporary access
     * without requiring authentication headers.
     * 
     * @param id the application ID
     * @param documentType the type of document (coverLetter, resume, portfolio)
     * @param expiryMinutes how long the URL should remain valid (in minutes)
     * @return the presigned URL for the requested document
     * @throws NotFoundException if the application or document is not found
     * @throws ValidationException if the document type is invalid
     */
    @Transactional(readOnly = true)
    public String generatePresignedUrl(String id, String documentType, int expiryMinutes) {
        UuidValidator.validateUuid(id, "id");
        Application application = findApplicationById(id);
        
        String objectKey = switch (documentType.toLowerCase()) {
            case "coverletter", "cover-letter" -> application.getCoverLetterPath();
            case "resume" -> application.getResumePath();
            case "portfolio" -> application.getPortfolioPath();
            default -> throw new ValidationException("Invalid document type: " + documentType + 
                    ". Valid types are: coverLetter, resume, portfolio");
        };
        
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new NotFoundException("Document not found for type: " + documentType);
        }
        
        return storageService.generatePresignedUrl(objectKey, expiryMinutes);
    }

    /**
     * Update lifecycle timestamps based on status changes.
     */
    private void updateLifecycleTimestamps(Application application, ApplicationStatus newStatus) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (newStatus) {
            case UNDER_REVIEW:
                if (application.getReviewedAt() == null) {
                    application.setReviewedAt(now);
                }
                break;
            case INTERVIEW_SCHEDULED:
                if (application.getInterviewScheduledAt() == null) {
                    application.setInterviewScheduledAt(now);
                }
                break;
            case OFFER_EXTENDED:
                if (application.getOfferExtendedAt() == null) {
                    application.setOfferExtendedAt(now);
                }
                break;
            case ACCEPTED:
            case REJECTED:
            case WITHDRAWN:
                if (application.getFinalizedAt() == null) {
                    application.setFinalizedAt(now);
                }
                break;
        }
    }

    /**
     * Validate status transition follows allowed paths.
     */
    private void validateStatusTransition(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        if (currentStatus == newStatus) {
            return; // Idempotent
        }

        // Define valid transitions
        boolean isValid = switch (currentStatus) {
            case SUBMITTED -> 
                newStatus == ApplicationStatus.UNDER_REVIEW || 
                newStatus == ApplicationStatus.REJECTED ||
                newStatus == ApplicationStatus.WITHDRAWN;
            case UNDER_REVIEW -> 
                newStatus == ApplicationStatus.INTERVIEW_SCHEDULED || 
                newStatus == ApplicationStatus.REJECTED ||
                newStatus == ApplicationStatus.WITHDRAWN;
            case INTERVIEW_SCHEDULED -> 
                newStatus == ApplicationStatus.INTERVIEWED || 
                newStatus == ApplicationStatus.REJECTED ||
                newStatus == ApplicationStatus.WITHDRAWN;
            case INTERVIEWED -> 
                newStatus == ApplicationStatus.OFFER_EXTENDED || 
                newStatus == ApplicationStatus.REJECTED ||
                newStatus == ApplicationStatus.WITHDRAWN;
            case OFFER_EXTENDED -> 
                newStatus == ApplicationStatus.ACCEPTED || 
                newStatus == ApplicationStatus.REJECTED ||
                newStatus == ApplicationStatus.WITHDRAWN;
            default -> false; // Cannot transition from final states
        };

        if (!isValid) {
            throw new ValidationException(
                String.format(INVALID_STATUS_TRANSITION_MESSAGE, currentStatus, newStatus)
            );
        }
    }

    /**
     * Check if status is finalized (no further changes allowed).
     */
    private boolean isFinalized(ApplicationStatus status) {
        return status == ApplicationStatus.ACCEPTED || 
               status == ApplicationStatus.REJECTED || 
               status == ApplicationStatus.WITHDRAWN;
    }

    /**
     * Find application by ID, ensuring it's not deleted.
     */
    private Application findApplicationById(String id) {
        return repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(APPLICATION_NOT_FOUND_MESSAGE));
    }

    /**
     * Validate multiple IDs at once.
     */
    private void validateIds(String... ids) {
        String[] fieldNames = {"applicantId", "jobPostingId", "userId"};
        for (int i = 0; i < ids.length && i < fieldNames.length; i++) {
            UuidValidator.validateUuid(ids[i], fieldNames[i]);
        }
    }
}
