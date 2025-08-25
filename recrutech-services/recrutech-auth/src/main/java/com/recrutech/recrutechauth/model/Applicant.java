package com.recrutech.recrutechauth.model;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

/**
 * Entity representing an applicant in the system.
 * Applicants are job seekers linked to a User entity for authentication.
 */
@Entity
@Table(name = "applicants")
@Getter
@Setter
public class Applicant extends BaseEntity {

    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String userId; // Reference to User entity

    private String phoneNumber;
    
    private LocalDate dateOfBirth;
    
    private String linkedinProfile;
    
    private String resumeUrl;
    
    private String currentLocation;

    @Column(nullable = false)
    private boolean profileComplete = false;

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