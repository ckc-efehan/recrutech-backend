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
 * Enterprise-grade company registration response record.
 * Features:
 * - Immutable design for thread safety
 * - Comprehensive response data
 * - Security-conscious field handling
 * - Audit trail information
 * - Status tracking capabilities
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyRegistrationResponse(
    
    @JsonProperty("companyId")
    @NotNull
    String companyId,
    
    @JsonProperty("adminUserId")
    @NotNull
    String adminUserId,
    
    @JsonProperty("hrUserId")
    @NotNull
    String hrUserId,
    
    @JsonProperty("tempPassword")
    String tempPassword,
    
    @JsonProperty("message")
    @NotNull
    String message,
    
    @JsonProperty("status")
    RegistrationStatus status,
    
    @JsonProperty("nextSteps")
    List<String> nextSteps,
    
    @JsonProperty("verificationRequired")
    Boolean verificationRequired,
    
    @JsonProperty("estimatedActivationTime")
    String estimatedActivationTime,
    
    @JsonProperty("supportContactInfo")
    SupportContactInfo supportContactInfo,
    
    @JsonProperty("registrationTimestamp")
    LocalDateTime registrationTimestamp
) {
    
    /**
     * Default constructor with default values
     */
    public CompanyRegistrationResponse {
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
        SUSPENDED,
        REJECTED
    }
    
    /**
     * Support contact information record
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SupportContactInfo(
        @JsonProperty("email")
        String email,
        
        @JsonProperty("phone")
        String phone,
        
        @JsonProperty("helpDeskUrl")
        String helpDeskUrl,
        
        @JsonProperty("businessHours")
        String businessHours
    ) {}
    
    /**
     * Creates a successful registration response with default next steps
     */
    public static CompanyRegistrationResponse createSuccessResponse(
            String companyId, String adminUserId, String hrUserId, String tempPassword) {
        return CompanyRegistrationResponse.builder()
            .companyId(companyId)
            .adminUserId(adminUserId)
            .hrUserId(hrUserId)
            .tempPassword(tempPassword)
            .message("Company registration completed successfully")
            .status(RegistrationStatus.PENDING_VERIFICATION)
            .verificationRequired(true)
            .estimatedActivationTime("24-48 hours")
            .nextSteps(List.of(
                "Check your email for verification instructions",
                "Complete company profile setup",
                "Verify business email address"
            ))
            .registrationTimestamp(LocalDateTime.now())
            .supportContactInfo(SupportContactInfo.builder()
                .email("support@recrutech.com")
                .phone("+1-800-RECRUTECH")
                .helpDeskUrl("https://help.recrutech.com")
                .businessHours("Monday-Friday 9AM-6PM EST")
                .build())
            .build();
    }
}