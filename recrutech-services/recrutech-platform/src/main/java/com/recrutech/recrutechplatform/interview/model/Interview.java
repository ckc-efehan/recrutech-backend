package com.recrutech.recrutechplatform.interview.model;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing an interview for a job application.
 * Links to applications and tracks interview details, scheduling, and status.
 * Best practices applied:
 * - Foreign key relationship to applications
 * - Audit trail (createdBy/updatedBy)
 * - Status lifecycle management
 * - Flexible scheduling with location or meeting link
 */
@Entity
@Table(name = "interviews")
@Getter
@Setter
public class Interview extends BaseEntity {

    // Core relationship
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String applicationId; // FK to applications.id
    
    // Interview details
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewType interviewType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewStatus status = InterviewStatus.SCHEDULED;
    
    // Scheduling information
    @Column(nullable = false)
    private LocalDateTime scheduledAt;
    
    @Column
    private Integer durationMinutes;
    
    // Location or meeting details
    @Column(length = 500)
    private String location; // Physical location for ONSITE interviews
    
    @Column(length = 500)
    private String meetingLink; // Video conference link for VIDEO interviews
    
    // Additional information
    @Column(columnDefinition = "TEXT")
    private String description; // Details about the interview
    
    @Column(columnDefinition = "TEXT")
    private String notes; // Internal notes about the interview
    
    // Interviewer information (optional)
    @Column(columnDefinition = "CHAR(36)")
    private String interviewerUserId; // FK to users.id
    
    // Feedback after interview
    @Column(columnDefinition = "TEXT")
    private String feedback; // Feedback from interviewer
    
    @Column
    private Integer rating; // Rating (e.g., 1-5 or 1-10)
    
    // Completion tracking
    private LocalDateTime completedAt; // When interview was completed
    
    // Audit fields
    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String createdByUserId; // FK to users.id (HR who created interview)
    
    @Column(columnDefinition = "CHAR(36)")
    private String updatedByUserId; // FK to users.id (who updated interview)
    
    // Soft delete
    @Column(nullable = false)
    private boolean isDeleted = false;
    
    private LocalDateTime deletedAt;
    
    @Column(columnDefinition = "CHAR(36)")
    private String deletedByUserId; // FK to users.id

    @PrePersist
    public void prePersist() {
        initializeEntity();
    }

    @PreUpdate
    public void preUpdate() {
        // Hook for future audit logic
    }
}
