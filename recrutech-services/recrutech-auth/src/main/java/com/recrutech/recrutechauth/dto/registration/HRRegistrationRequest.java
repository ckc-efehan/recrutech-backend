package com.recrutech.recrutechauth.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Enterprise-grade HR registration request record.
 * Features:
 * - Immutable design with comprehensive validation
 * - Invitation-based registration security
 * - Employee profile information
 * - Business rule validation
 * - Audit trail support
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HRRegistrationRequest(
    
    @NotBlank(message = "Email address is required and cannot be empty")
    @Email(
        message = "Please provide a valid email address format",
        regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    @Size(max = 254, message = "Email address cannot exceed 254 characters")
    @JsonProperty("email")
    String email,
    
    @NotBlank(message = "Password is required and cannot be empty")
    @Size(
        min = 8, 
        max = 128, 
        message = "Password must be between 8 and 128 characters"
    )
    @JsonProperty("password")
    String password,
    
    @NotBlank(message = "First name is required")
    @Size(
        min = 1, 
        max = 50, 
        message = "First name must be between 1 and 50 characters"
    )
    @Pattern(
        regexp = "^[a-zA-Z\\s\\-']+$",
        message = "First name contains invalid characters"
    )
    @JsonProperty("firstName")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(
        min = 1, 
        max = 50, 
        message = "Last name must be between 1 and 50 characters"
    )
    @Pattern(
        regexp = "^[a-zA-Z\\s\\-']+$",
        message = "Last name contains invalid characters"
    )
    @JsonProperty("lastName")
    String lastName,
    
    @NotBlank(message = "Company ID is required")
    @Size(max = 36, message = "Company ID cannot exceed 36 characters")
    @JsonProperty("companyId")
    String companyId,
    
    @NotBlank(message = "Invitation token is required for HR registration")
    @Size(max = 500, message = "Invitation token cannot exceed 500 characters")
    @JsonProperty("invitationToken")
    String invitationToken,
    
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    @JsonProperty("department")
    String department,
    
    @Size(max = 100, message = "Position cannot exceed 100 characters")
    @JsonProperty("position")
    String position,
    
    @Size(max = 50, message = "Employee ID cannot exceed 50 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9\\-_]+$",
        message = "Employee ID can only contain letters, numbers, hyphens, and underscores"
    )
    @JsonProperty("employeeId")
    String employeeId,
    
    @JsonProperty("startDate")
    String startDate, // ISO date format
    
    @JsonProperty("workLocation")
    @Size(max = 100, message = "Work location cannot exceed 100 characters")
    String workLocation,
    
    @JsonProperty("phoneNumber")
    @Pattern(
        regexp = "^[+]?[0-9\\s\\-()]{7,20}$",
        message = "Please provide a valid phone number"
    )
    String phoneNumber,
    
    @JsonProperty("emergencyContact")
    @Valid
    EmergencyContact emergencyContact,
    
    @JsonProperty("agreementAccepted")
    @NotNull(message = "Employee agreement must be accepted")
    @AssertTrue(message = "You must accept the employee agreement to proceed")
    Boolean agreementAccepted
) {
    
    /**
     * Default constructor with default values
     */
    public HRRegistrationRequest {
        if (department == null || department.trim().isEmpty()) {
            department = "Human Resources";
        }
        if (position == null || position.trim().isEmpty()) {
            position = "HR Specialist";
        }
    }
    
    /**
     * Emergency contact information record
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmergencyContact(
        @Size(max = 100, message = "Emergency contact name cannot exceed 100 characters")
        @JsonProperty("name")
        String name,
        
        @Pattern(
            regexp = "^[+]?[0-9\\s\\-()]{7,20}$",
            message = "Please provide a valid emergency contact phone number"
        )
        @JsonProperty("phoneNumber")
        String phoneNumber,
        
        @Size(max = 50, message = "Relationship cannot exceed 50 characters")
        @JsonProperty("relationship")
        String relationship
    ) {}
    
    /**
     * Validates business rules for HR registration
     */
    public void validateBusinessRules() {
        // Validate invitation token format (simplified)
        if (invitationToken != null && !invitationToken.matches("^[A-Za-z0-9\\-_]{20,}$")) {
            throw new IllegalArgumentException("Invalid invitation token format");
        }
        
        // Validate employee ID uniqueness constraint will be handled at service level
        if (employeeId != null && employeeId.length() < 3) {
            throw new IllegalArgumentException("Employee ID must be at least 3 characters long");
        }
        
        // Validate start date if provided
        if (startDate != null && !startDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new IllegalArgumentException("Start date must be in YYYY-MM-DD format");
        }
    }
    
    /**
     * Gets the full name for display purposes
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return String.format("%s %s", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : "").trim();
    }
    
    /**
     * Checks if this is a self-registration (has invitation token)
     */
    public boolean isSelfRegistration() {
        return invitationToken != null && !invitationToken.trim().isEmpty();
    }
}