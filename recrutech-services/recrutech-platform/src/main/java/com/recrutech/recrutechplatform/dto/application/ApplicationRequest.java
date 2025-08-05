package com.recrutech.recrutechplatform.dto.application;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for receiving application submission data from clients.
 * User information (userId, firstName, lastName) is automatically extracted from JWT token.
 */
public record ApplicationRequest(
    @NotBlank(message = "CV file ID is required")
    String cvFileId
) {
    // User information is automatically extracted from JWT authentication
}