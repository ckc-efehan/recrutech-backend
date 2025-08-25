package com.recrutech.recrutechauth.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Immutable login request record with comprehensive validation.
 * Features:
 * - Immutable design prevents accidental modification
 * - Builder pattern for flexible construction
 * - Comprehensive validation with custom messages
 * - JSON serialization optimizations
 * - Security-focused field handling
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginRequest(
    
    @NotBlank(message = "Email address is required and cannot be empty")
    @Email(
        message = "Please provide a valid email address format",
        regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    @Size(
        max = 254, 
        message = "Email address cannot exceed 254 characters"
    )
    @JsonProperty("email")
    String email,
    
    @NotBlank(message = "Password is required and cannot be empty")
    @Size(
        min = 1, 
        max = 128, 
        message = "Password must be between 1 and 128 characters"
    )
    @JsonProperty("password")
    String password,
    
    @JsonProperty("rememberMe")
    Boolean rememberMe,
    
    @JsonProperty("deviceInfo")
    DeviceInfo deviceInfo
) {
    
    /**
     * Default constructor with default values
     */
    public LoginRequest {
        if (rememberMe == null) {
            rememberMe = false;
        }
    }
    
    /**
     * Device information for enhanced security tracking
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeviceInfo(
        @Size(max = 500, message = "User agent cannot exceed 500 characters")
        String userAgent,
        
        @Size(max = 100, message = "Device type cannot exceed 100 characters")
        String deviceType,
        
        @Size(max = 50, message = "Operating system cannot exceed 50 characters")
        String operatingSystem,
        
        @Size(max = 100, message = "Browser cannot exceed 100 characters")
        String browser
    ) {}
    
    /**
     * Validates the login request with business rules
     */
    public void validateBusinessRules() {
        if (email != null && email.toLowerCase().contains("test") && 
            password != null && password.equals("password")) {
            throw new IllegalArgumentException("Test credentials are not allowed in production");
        }
    }
}