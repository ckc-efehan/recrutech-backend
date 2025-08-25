package com.recrutech.recrutechauth.model;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Entity representing a company in the system.
 * Companies are managed by admin users and can have multiple HR employees.
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false, unique = true)
    private String businessEmail;

    @Column(nullable = false)
    private String contactFirstName;

    @Column(nullable = false)
    private String contactLastName;

    @Column(nullable = false, unique = true)
    private String telephone;

    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String adminUserId; // Reference to the admin User

    @Column(nullable = false)
    private boolean verified = false;

    @Column
    private String verificationToken;

    @Column
    private LocalDateTime verificationExpiry;

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
        // No additional update logic needed at this time
    }
}
