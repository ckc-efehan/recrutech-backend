package com.recrutech.recrutechauth.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Enterprise-grade applicant registration request record.
 * Features:
 * - Immutable design with comprehensive validation
 * - Professional profile information capture
 * - Career preferences and skills tracking
 * - Privacy and consent management
 * - Social media integration support
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicantRegistrationRequest(
    
    @Valid
    @NotNull(message = "Personal information is required")
    @JsonProperty("personalInfo")
    PersonalInfo personalInfo,
    
    @Valid
    @JsonProperty("professionalInfo")
    ProfessionalInfo professionalInfo,
    
    @Valid
    @JsonProperty("preferences")
    JobPreferences preferences,
    
    @JsonProperty("privacyConsent")
    @NotNull(message = "Privacy consent is required")
    @AssertTrue(message = "You must consent to privacy policy to proceed")
    Boolean privacyConsent,
    
    @JsonProperty("marketingConsent")
    Boolean marketingConsent,
    
    @JsonProperty("referralSource")
    @Size(max = 100, message = "Referral source cannot exceed 100 characters")
    String referralSource
) {
    
    /**
     * Default constructor with default values
     */
    public ApplicantRegistrationRequest {
        if (marketingConsent == null) {
            marketingConsent = false;
        }
    }
    
    /**
     * Personal information nested record
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonalInfo(
        
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
        
        @Pattern(
            regexp = "^[+]?[0-9\\s\\-()]{7,20}$",
            message = "Please provide a valid phone number"
        )
        @JsonProperty("phoneNumber")
        String phoneNumber,
        
        @Size(max = 100, message = "Current location cannot exceed 100 characters")
        @JsonProperty("currentLocation")
        String currentLocation,
        
        @JsonProperty("dateOfBirth")
        String dateOfBirth, // ISO date format
        
        @Size(max = 10, message = "Gender cannot exceed 10 characters")
        @JsonProperty("gender")
        String gender,
        
        @Size(max = 100, message = "Nationality cannot exceed 100 characters")
        @JsonProperty("nationality")
        String nationality,
        
        @JsonProperty("workAuthorization")
        WorkAuthorization workAuthorization
    ) {}
    
    /**
     * Professional information nested record
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfessionalInfo(
        
        @Size(max = 100, message = "Current job title cannot exceed 100 characters")
        @JsonProperty("currentJobTitle")
        String currentJobTitle,
        
        @Size(max = 100, message = "Current company cannot exceed 100 characters")
        @JsonProperty("currentCompany")
        String currentCompany,
        
        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 50, message = "Years of experience cannot exceed 50")
        @JsonProperty("yearsOfExperience")
        Integer yearsOfExperience,
        
        @Size(max = 100, message = "Industry cannot exceed 100 characters")
        @JsonProperty("industry")
        String industry,
        
        @Size(max = 500, message = "Skills cannot exceed 500 characters")
        @JsonProperty("skills")
        String skills,
        
        @Size(max = 100, message = "Education level cannot exceed 100 characters")
        @JsonProperty("educationLevel")
        String educationLevel,
        
        @Size(max = 100, message = "Field of study cannot exceed 100 characters")
        @JsonProperty("fieldOfStudy")
        String fieldOfStudy,
        
        @Size(max = 500, message = "Resume URL cannot exceed 500 characters")
        @Pattern(
            regexp = "^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$",
            message = "Please provide a valid URL for resume"
        )
        @JsonProperty("resumeUrl")
        String resumeUrl,
        
        @Size(max = 500, message = "LinkedIn profile cannot exceed 500 characters")
        @Pattern(
            regexp = "^(https?://)?(www\\.)?linkedin\\.com/in/[\\w\\-]+/?$",
            message = "Please provide a valid LinkedIn profile URL"
        )
        @JsonProperty("linkedinProfile")
        String linkedinProfile,
        
        @Size(max = 500, message = "Portfolio URL cannot exceed 500 characters")
        @Pattern(
            regexp = "^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$",
            message = "Please provide a valid portfolio URL"
        )
        @JsonProperty("portfolioUrl")
        String portfolioUrl
    ) {}
    
    /**
     * Job preferences nested record
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JobPreferences(
        
        @Size(max = 100, message = "Desired job title cannot exceed 100 characters")
        @JsonProperty("desiredJobTitle")
        String desiredJobTitle,
        
        @Size(max = 200, message = "Preferred locations cannot exceed 200 characters")
        @JsonProperty("preferredLocations")
        String preferredLocations,
        
        @JsonProperty("remoteWorkPreference")
        RemoteWorkPreference remoteWorkPreference,
        
        @JsonProperty("employmentType")
        EmploymentType employmentType,
        
        @Min(value = 0, message = "Minimum salary cannot be negative")
        @JsonProperty("minimumSalary")
        Integer minimumSalary,
        
        @Size(max = 10, message = "Salary currency cannot exceed 10 characters")
        @JsonProperty("salaryCurrency")
        String salaryCurrency,
        
        @JsonProperty("availabilityDate")
        String availabilityDate, // ISO date format
        
        @JsonProperty("willingToRelocate")
        Boolean willingToRelocate
    ) {
        /**
         * Default constructor with default values
         */
        public JobPreferences {
            if (salaryCurrency == null || salaryCurrency.trim().isEmpty()) {
                salaryCurrency = "USD";
            }
            if (willingToRelocate == null) {
                willingToRelocate = false;
            }
        }
    }
    
    /**
     * Work authorization information record
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkAuthorization(
        @JsonProperty("authorized")
        Boolean authorized,
        
        @Size(max = 50, message = "Authorization type cannot exceed 50 characters")
        @JsonProperty("authorizationType")
        String authorizationType,
        
        @JsonProperty("requiresSponsorship")
        Boolean requiresSponsorship
    ) {
        /**
         * Default constructor with default values
         */
        public WorkAuthorization {
            if (authorized == null) {
                authorized = true;
            }
            if (requiresSponsorship == null) {
                requiresSponsorship = false;
            }
        }
    }
    
    /**
     * Remote work preference enumeration
     */
    public enum RemoteWorkPreference {
        REMOTE_ONLY,
        HYBRID,
        ON_SITE_ONLY,
        NO_PREFERENCE
    }
    
    /**
     * Employment type enumeration
     */
    public enum EmploymentType {
        FULL_TIME,
        PART_TIME,
        CONTRACT,
        FREELANCE,
        INTERNSHIP,
        NO_PREFERENCE
    }
    
    /**
     * Validates business rules for applicant registration
     */
    public void validateBusinessRules() {
        // Validate age if date of birth is provided
        if (personalInfo != null && personalInfo.dateOfBirth() != null) {
            if (!personalInfo.dateOfBirth().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                throw new IllegalArgumentException("Date of birth must be in YYYY-MM-DD format");
            }
        }
        
        // Validate salary range
        if (preferences != null && preferences.minimumSalary() != null) {
            if (preferences.minimumSalary() < 0 || preferences.minimumSalary() > 10000000) {
                throw new IllegalArgumentException("Minimum salary must be between 0 and 10,000,000");
            }
        }
        
        // Validate availability date
        if (preferences != null && preferences.availabilityDate() != null) {
            if (!preferences.availabilityDate().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                throw new IllegalArgumentException("Availability date must be in YYYY-MM-DD format");
            }
        }
    }
    
    /**
     * Gets the full name for display purposes
     */
    public String getFullName() {
        if (personalInfo == null) {
            return null;
        }
        String firstName = personalInfo.firstName();
        String lastName = personalInfo.lastName();
        if (firstName == null && lastName == null) {
            return null;
        }
        return String.format("%s %s", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : "").trim();
    }
    
    /**
     * Checks if the applicant has professional experience
     */
    public boolean hasExperience() {
        return professionalInfo != null && 
               professionalInfo.yearsOfExperience() != null && 
               professionalInfo.yearsOfExperience() > 0;
    }
    
    /**
     * Gets the primary email address
     */
    public String getEmail() {
        return personalInfo != null ? personalInfo.email() : null;
    }
}