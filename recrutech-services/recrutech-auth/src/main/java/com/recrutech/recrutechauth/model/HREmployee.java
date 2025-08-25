package com.recrutech.recrutechauth.model;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

/**
 * Entity representing an HR employee in the system.
 * HR employees are linked to both a User (for authentication) and a Company.
 */
@Entity
@Table(name = "hr_employees")
@Getter
@Setter
public class HREmployee extends BaseEntity {

    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String userId; // Reference to User entity

    @Column(nullable = false, columnDefinition = "CHAR(36)")
    private String companyId; // Reference to Company entity

    private String department;
    
    private String position;
    
    private LocalDate hireDate;
    
    private String employeeId;

    @Column(nullable = false)
    private boolean active = true;

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