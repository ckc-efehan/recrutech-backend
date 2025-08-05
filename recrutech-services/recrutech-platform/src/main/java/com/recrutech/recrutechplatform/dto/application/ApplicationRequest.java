package com.recrutech.recrutechplatform.dto.application;

/**
 * DTO for receiving application submission data from clients
 */
public record ApplicationRequest(String cvFileId, String userId, String firstName, String lastName) {
    // Add any other fields needed for application submission
}