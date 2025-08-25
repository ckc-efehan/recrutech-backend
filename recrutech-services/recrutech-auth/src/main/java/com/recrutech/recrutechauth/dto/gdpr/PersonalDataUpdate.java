package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * DTO for personal data updates in rectification requests.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalDataUpdate(
    
    String firstName,
    
    String lastName,
    
    String email,
    
    String phoneNumber,
    
    String address
) {
}