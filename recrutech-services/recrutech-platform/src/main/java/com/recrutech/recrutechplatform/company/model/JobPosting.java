package com.recrutech.recrutechplatform.company.model;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Company-scoped job posting entity.
 * Best practices applied:
 * - companyId scoping (CHAR(36))
 * - audit references to users (createdBy/updatedBy/deletedBy)
 * - soft delete flags
 * - status lifecycle and publication dates
 */
@Entity
@Table(name = "job_postings")
@Getter
@Setter
public class JobPosting extends BaseEntity {

    // Scope & audit
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String companyId; // FK to companies.id

    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String createdByUserId; // FK to users.id

    @Column(columnDefinition = "CHAR(36)")
    private String updatedByUserId; // FK to users.id

    @Column(columnDefinition = "CHAR(36)")
    private String deletedByUserId; // FK to users.id

    // Content
    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(length = 200)
    private String location;

    @Column(length = 50)
    private String employmentType; // e.g., FULL_TIME, PART_TIME, INTERN

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    @Column(length = 3)
    private String currency; // ISO 4217

    // Publication & lifecycle
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status = JobPostingStatus.DRAFT;

    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;

    // Soft delete
    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        initializeEntity();
    }

    @PreUpdate
    public void preUpdate() {
        // hook for future audit fields
    }
}
