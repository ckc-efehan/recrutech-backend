package com.recrutech.recrutechauth.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Simplified company registration request record.
 * Features:
 * - Single form input for company registration
 * - Automatically creates admin and HR users from provided information
 * - Generates admin@domain.com and hr@domain.com emails automatically
 * - Uses the same password and personal details for both users
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyRegistrationRequest(
    
    @NotBlank(message = "Company name is required")
    @Size(
        min = 2, 
        max = 100, 
        message = "Company name must be between 2 and 100 characters"
    )
    @JsonProperty("name")
    String name,
    
    @NotBlank(message = "Location is required")
    @Size(
        min = 2, 
        max = 100, 
        message = "Location must be between 2 and 100 characters"
    )
    @JsonProperty("location")
    String location,
    
    @NotBlank(message = "Business email is required")
    @Email(
        message = "Please provide a valid business email address",
        regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    @Size(max = 254, message = "Business email cannot exceed 254 characters")
    @JsonProperty("businessEmail")
    String businessEmail,
    
    @NotBlank(message = "Telephone is required")
    @Pattern(
        regexp = "^[+]?[0-9\\s\\-\\(\\)]{7,20}$",
        message = "Please provide a valid telephone number"
    )
    @JsonProperty("telephone")
    String telephone,
    
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
    
    @NotBlank(message = "Password is required")
    @Size(
        min = 8, 
        max = 128, 
        message = "Password must be between 8 and 128 characters"
    )
    @JsonProperty("password")
    String password,
    
    @NotNull(message = "Agreement acceptance is required")
    @AssertTrue(message = "You must accept the terms and conditions")
    @JsonProperty("agreementAccepted")
    Boolean agreementAccepted
) {
    
    /**
     * Validates business rules for company registration
     */
    public void validateBusinessRules() {
        // Basic validation - ensure all required fields are present
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }
        if (businessEmail == null || businessEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Business email is required");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
    }
    
    /**
     * Generates admin email from business email domain
     */
    public String generateAdminEmail() {
        if (businessEmail == null || !businessEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid business email format");
        }
        String domain = businessEmail.substring(businessEmail.indexOf("@"));
        return "admin" + domain;
    }
    
    /**
     * Generates HR email from business email domain
     */
    public String generateHREmail() {
        if (businessEmail == null || !businessEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid business email format");
        }
        String domain = businessEmail.substring(businessEmail.indexOf("@"));
        return "hr" + domain;
    }
}