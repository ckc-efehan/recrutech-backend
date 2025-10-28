package com.recrutech.recrutechplatform.domain.company;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Entity representing a company in the system.
 * Companies are managed by admin users and can have multiple HR employees.
 * This is a domain entity that belongs in the Platform service.
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company extends BaseEntity {

    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String accountId; // Reference to User account in auth service (company admin)

    @Column(nullable = false)
    private String contactEmail; // Primary contact email from user registration

    @Column
    private String name;

    @Column
    private String location;
    
    @Column(unique = true)
    private String businessEmail;

    @Column
    private String contactFirstName;

    @Column
    private String contactLastName;

    @Column(unique = true)
    private String telephone;

    @Column(columnDefinition = "CHAR(36)")
    private String adminAccountId; // Reference to the admin User account in auth service

    @Column(nullable = false)
    private boolean verified = false;

    @Column
    private String verificationToken;

    @Column
    private LocalDateTime verificationExpiry;

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
        // No additional update logic needed at this time
    }
}
