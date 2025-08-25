package com.recrutech.recrutechauth.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enterprise-grade user registration response record.
 * Features:
 * - Immutable design for thread safety
 * - Comprehensive registration feedback
 * - Next steps guidance
 * - Support information
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserRegistrationResponse(
    
    @JsonProperty("userId")
    @NotNull
    String userId,
    
    @JsonProperty("message")
    @NotNull
    String message,
    
    @JsonProperty("status")
    RegistrationStatus status,
    
    @JsonProperty("nextSteps")
    List<String> nextSteps,
    
    @JsonProperty("verificationRequired")
    Boolean verificationRequired,
    
    @JsonProperty("profileCompletionPercentage")
    Integer profileCompletionPercentage,
    
    @JsonProperty("registrationTimestamp")
    LocalDateTime registrationTimestamp
) {
    
    /**
     * Default constructor with default values
     */
    public UserRegistrationResponse {
        if (status == null) {
            status = RegistrationStatus.PENDING_VERIFICATION;
        }
        if (verificationRequired == null) {
            verificationRequired = true;
        }
    }
    
    /**
     * Registration status enumeration
     */
    public enum RegistrationStatus {
        PENDING_VERIFICATION,
        VERIFIED,
        ACTIVE,
        INCOMPLETE,
        SUSPENDED
    }
    
    /**
     * Creates a successful registration response with default guidance
     */
    public static UserRegistrationResponse createSuccessResponse(String userId, String userType) {
        List<String> defaultNextSteps = switch (userType.toLowerCase()) {
            case "hr" -> List.of(
                "Check your email for verification instructions",
                "Complete your HR profile setup",
                "Familiarize yourself with company policies",
                "Set up your workspace preferences"
            );
            case "applicant" -> List.of(
                "Verify your email address",
                "Complete your professional profile",
                "Upload your resume and portfolio",
                "Set your job preferences",
                "Start browsing available positions"
            );
            default -> List.of(
                "Check your email for verification instructions",
                "Complete your profile setup"
            );
        };
        
        return UserRegistrationResponse.builder()
            .userId(userId)
            .message(String.format("%s registration completed successfully", 
                userType.substring(0, 1).toUpperCase() + userType.substring(1)))
            .status(RegistrationStatus.PENDING_VERIFICATION)
            .nextSteps(defaultNextSteps)
            .verificationRequired(true)
            .profileCompletionPercentage(25)
            .registrationTimestamp(LocalDateTime.now())
            .build();
    }
}