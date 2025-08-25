package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Response DTO for GDPR Right to Data Portability (Art. 20).
 * Contains all personal data associated with a user account.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DataExportResponse(
    
    String userId,
    
    LocalDateTime exportDate,
    
    PersonalDataExport personalData,
    
    CompanyDataExport companyData,
    
    HRDataExport hrData,
    
    ApplicantDataExport applicantData,
    
    String exportFormat, // JSON, XML, CSV
    
    String downloadUrl,
    
    LocalDateTime expiresAt // When download link expires
) {
    
    /**
     * Creates a successful export response
     */
    public static DataExportResponse createSuccessResponse(String userId, PersonalDataExport personalData) {
        return DataExportResponse.builder()
            .userId(userId)
            .exportDate(LocalDateTime.now())
            .personalData(personalData)
            .exportFormat("JSON")
            .expiresAt(LocalDateTime.now().plusDays(7)) // Download link expires in 7 days
            .build();
    }
}