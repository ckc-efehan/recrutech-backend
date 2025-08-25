package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Company data update fields
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyDataUpdate(
    
    @Size(max = 200, message = "Company name cannot exceed 200 characters")
    String name,
    
    @Size(max = 300, message = "Location cannot exceed 300 characters")
    String location,
    
    @Size(max = 20, message = "Telephone cannot exceed 20 characters")
    String telephone
) {}