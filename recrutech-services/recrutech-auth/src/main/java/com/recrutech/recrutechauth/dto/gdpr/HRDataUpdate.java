package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * HR employee data update fields
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HRDataUpdate(
    
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    String department,
    
    @Size(max = 100, message = "Position cannot exceed 100 characters")
    String position
) {}