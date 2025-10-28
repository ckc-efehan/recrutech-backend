package com.recrutech.recrutechplatform.domain.applicant;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an applicant in the system.
 * Applicants are job seekers linked to a User account (via accountId) for authentication.
 * This is a domain entity that belongs in the Platform service.
 */
@Entity
@Table(name = "applicants")
@Getter
@Setter
public class Applicant extends BaseEntity {

    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String accountId; // Reference to User account in auth service

    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;
    
    private LocalDate dateOfBirth;
    
    private String linkedinProfile;
    
    private String resumeUrl;
    
    private String currentLocation;

    @Column(nullable = false)
    private boolean profileComplete = false;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Initializes the entity by ensuring it has an ID and creation timestamp.
     * This method should be called before persisting the entity.
     */
    @PrePersist
    public void prePersist() {
        initializeEntity();
    }

    /**
     * Updates the entity before it's updated in the database.
     * This method is called automatically by JPA before an update operation.
     */
    @PreUpdate
    public void preUpdate() {
        // Update logic if needed
    }
}
