package com.recrutech.recrutechplatform.application.model;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a job application in the system.
 * Links applicants to job postings and tracks the application lifecycle.
 * Best practices applied:
 * - Foreign key relationships to applicants and job_postings
 * - Audit trail (createdBy/updatedBy)
 * - Status lifecycle management
 * - Application metadata (cover letter, resume)
 */
@Entity
@Table(name = "applications")
@Getter
@Setter
public class Application extends BaseEntity {

    // Core relationships
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String applicantId; // FK to applicants.id
    
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String jobPostingId; // FK to job_postings.id
    
    // Audit fields
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String createdByUserId; // FK to users.id (applicant's user)
    
    @Column(columnDefinition = "CHAR(36)")
    private String updatedByUserId; // FK to users.id (HR who updated status)
    
    // Application content
    @Column(columnDefinition = "TEXT")
    private String coverLetter;
    
    @Column(length = 500)
    private String resumeUrl; // URL to uploaded resume document
    
    @Column(length = 500)
    private String portfolioUrl; // Optional portfolio link
    
    // Status and lifecycle
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;
    
    private LocalDateTime submittedAt;
    
    private LocalDateTime reviewedAt;
    
    private LocalDateTime interviewScheduledAt;
    
    private LocalDateTime offerExtendedAt;
    
    private LocalDateTime finalizedAt; // When status became ACCEPTED, REJECTED, or WITHDRAWN
    
    // Additional notes
    @Column(columnDefinition = "TEXT")
    private String hrNotes; // Internal notes from HR/recruiter
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason; // Optional reason if rejected
    
    // Soft delete
    @Column(nullable = false)
    private boolean isDeleted = false;
    
    private LocalDateTime deletedAt;
    
    @Column(columnDefinition = "CHAR(36)")
    private String deletedByUserId; // FK to users.id

    @PrePersist
    public void prePersist() {
        initializeEntity();
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        // Hook for future audit logic
    }
}
