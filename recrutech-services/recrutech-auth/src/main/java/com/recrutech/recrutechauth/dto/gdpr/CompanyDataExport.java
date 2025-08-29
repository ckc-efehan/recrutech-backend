package com.recrutech.recrutechauth.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Company data export for company admin users
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyDataExport(
    String name,
    String location,
    String businessEmail,
    String telephone,
    LocalDateTime createdAt
) {
    // Manually implemented public builder to avoid Lombok builder visibility issues
    public static CompanyDataExportBuilder builder() {
        return new CompanyDataExportBuilder();
    }

    public static final class CompanyDataExportBuilder {
        private String name;
        private String location;
        private String businessEmail;
        private String telephone;
        private LocalDateTime createdAt;

        public CompanyDataExportBuilder name(String name) {
            this.name = name; return this;
        }
        public CompanyDataExportBuilder location(String location) {
            this.location = location; return this;
        }
        public CompanyDataExportBuilder businessEmail(String businessEmail) {
            this.businessEmail = businessEmail; return this;
        }
        public CompanyDataExportBuilder telephone(String telephone) {
            this.telephone = telephone; return this;
        }
        public CompanyDataExportBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt; return this;
        }
        public CompanyDataExport build() {
            return new CompanyDataExport(name, location, businessEmail, telephone, createdAt);
        }
    }
}