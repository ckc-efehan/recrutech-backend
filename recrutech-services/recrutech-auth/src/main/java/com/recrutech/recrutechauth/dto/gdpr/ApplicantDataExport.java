package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Applicant data export
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicantDataExport(
    String phoneNumber,
    String currentLocation,
    LocalDateTime createdAt
) {
    // Manually implemented public builder to avoid Lombok builder visibility issues
    public static ApplicantDataExportBuilder builder() {
        return new ApplicantDataExportBuilder();
    }

    public static final class ApplicantDataExportBuilder {
        private String phoneNumber;
        private String currentLocation;
        private LocalDateTime createdAt;

        public ApplicantDataExportBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber; return this;
        }
        public ApplicantDataExportBuilder currentLocation(String currentLocation) {
            this.currentLocation = currentLocation; return this;
        }
        public ApplicantDataExportBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt; return this;
        }
        public ApplicantDataExport build() {
            return new ApplicantDataExport(phoneNumber, currentLocation, createdAt);
        }
    }
}