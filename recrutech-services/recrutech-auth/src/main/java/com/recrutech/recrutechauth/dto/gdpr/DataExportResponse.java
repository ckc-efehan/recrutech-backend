package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Response DTO for GDPR data export (Right to Data Portability - Art. 20).
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DataExportResponse(
    
    String userId,
    
    LocalDateTime exportDate,
    
    String format,
    
    String downloadUrl,
    
    LocalDateTime expiresAt,
    
    Object personalData,
    
    Object companyData,
    
    Object hrData,
    
    Object applicantData
) {
}