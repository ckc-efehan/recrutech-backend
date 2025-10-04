package com.recrutech.recrutechplatform.application.repository;

import com.recrutech.recrutechplatform.application.model.Application;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Application entity.
 * Provides data access methods for job applications with pagination and filtering.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {

    /**
     * Find an application by ID, ensuring it's not soft-deleted.
     */
    Optional<Application> findByIdAndIsDeletedFalse(String id);

    /**
     * Find all applications for a specific applicant.
     */
    Page<Application> findAllByApplicantIdAndIsDeletedFalse(String applicantId, Pageable pageable);

    /**
     * Find all applications for a specific applicant with status filter.
     */
    Page<Application> findAllByApplicantIdAndStatusAndIsDeletedFalse(
            String applicantId, 
            ApplicationStatus status, 
            Pageable pageable
    );

    /**
     * Find all applications for a specific job posting.
     */
    Page<Application> findAllByJobPostingIdAndIsDeletedFalse(String jobPostingId, Pageable pageable);

    /**
     * Find all applications for a specific job posting with status filter.
     */
    Page<Application> findAllByJobPostingIdAndStatusAndIsDeletedFalse(
            String jobPostingId, 
            ApplicationStatus status, 
            Pageable pageable
    );

    /**
     * Find all applications for job postings belonging to a specific company.
     * Uses a custom query to join with job_postings table.
     */
    @Query("SELECT a FROM Application a JOIN JobPosting jp ON a.jobPostingId = jp.id " +
           "WHERE jp.companyId = :companyId AND a.isDeleted = false AND jp.isDeleted = false")
    Page<Application> findAllByCompanyId(@Param("companyId") String companyId, Pageable pageable);

    /**
     * Find all applications for job postings belonging to a specific company with status filter.
     */
    @Query("SELECT a FROM Application a JOIN JobPosting jp ON a.jobPostingId = jp.id " +
           "WHERE jp.companyId = :companyId AND a.status = :status AND a.isDeleted = false AND jp.isDeleted = false")
    Page<Application> findAllByCompanyIdAndStatus(
            @Param("companyId") String companyId, 
            @Param("status") ApplicationStatus status, 
            Pageable pageable
    );

    /**
     * Check if an applicant has already applied to a specific job posting.
     * Useful to prevent duplicate applications.
     */
    boolean existsByApplicantIdAndJobPostingIdAndIsDeletedFalse(String applicantId, String jobPostingId);

}
