package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Personal data export containing basic user information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalDataExport(
    String email,
    String firstName,
    String lastName,
    String role,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt,
    Boolean emailVerified
) {
    // Manually implemented public builder to avoid Lombok builder visibility issues
    public static PersonalDataExportBuilder builder() {
        return new PersonalDataExportBuilder();
    }

    public static final class PersonalDataExportBuilder {
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private Boolean emailVerified;

        public PersonalDataExportBuilder email(String email) {
            this.email = email; return this;
        }
        public PersonalDataExportBuilder firstName(String firstName) {
            this.firstName = firstName; return this;
        }
        public PersonalDataExportBuilder lastName(String lastName) {
            this.lastName = lastName; return this;
        }
        public PersonalDataExportBuilder role(String role) {
            this.role = role; return this;
        }
        public PersonalDataExportBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt; return this;
        }
        public PersonalDataExportBuilder lastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt; return this;
        }
        public PersonalDataExportBuilder emailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified; return this;
        }
        public PersonalDataExport build() {
            return new PersonalDataExport(email, firstName, lastName, role, createdAt, lastLoginAt, emailVerified);
        }
    }
}