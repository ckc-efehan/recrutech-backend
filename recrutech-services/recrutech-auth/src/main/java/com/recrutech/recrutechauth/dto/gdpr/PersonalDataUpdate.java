package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Personal data update fields
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalDataUpdate(
    
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    String firstName,
    
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    String lastName,
    
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email cannot exceed 254 characters")
    String email
) {
    
    /**
     * Validates personal data updates
     */
    public void validateBusinessRules() {
        if (firstName != null && firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        
        if (lastName != null && lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
        
        if (email != null && email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
    }
}