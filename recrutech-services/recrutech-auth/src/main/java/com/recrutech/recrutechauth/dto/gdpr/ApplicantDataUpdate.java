package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Applicant data update fields
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicantDataUpdate(
    
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    String phoneNumber,
    
    @Size(max = 200, message = "Current location cannot exceed 200 characters")
    String currentLocation
) {}