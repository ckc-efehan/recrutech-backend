package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Response DTO for GDPR information endpoint.
 * Provides information about GDPR rights and contact details.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GdprInfoResponse(
    
    String rightToDeletion,
    
    String rightToPortability,
    
    String rightToRectification,
    
    String processingActivities,
    
    String contactEmail,
    
    String dataProtectionOfficer,
    
    String retentionPeriod
) {
    
    /**
     * Creates a default GDPR info response
     */
    public static GdprInfoResponse createDefault() {
        return GdprInfoResponse.builder()
            .rightToDeletion("You have the right to request deletion of your personal data under GDPR Article 17")
            .rightToPortability("You have the right to receive your personal data in a portable format under GDPR Article 20")
            .rightToRectification("You have the right to request correction of inaccurate personal data under GDPR Article 16")
            .processingActivities("You can request information about how your data is processed under GDPR Article 30")
            .contactEmail("privacy@recrutech.com")
            .dataProtectionOfficer("DPO Team")
            .build();
    }
}