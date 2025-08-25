package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.dto.gdpr.*;
import com.recrutech.recrutechauth.exception.GdprComplianceException;
import com.recrutech.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GDPR Compliance Service for handling data protection requirements.
 * Implements:
 * - Right to Deletion (Art. 17 GDPR)
 * - Right to Data Portability (Art. 20 GDPR)
 * - Right to Rectification (Art. 16 GDPR)
 * - Audit Logging for data processing activities
 * - Data retention policies
 */
@Service
@Transactional
public class GdprComplianceService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final HREmployeeRepository hrEmployeeRepository;
    private final ApplicantRepository applicantRepository;
    private final AuditLogService auditLogService;

    public GdprComplianceService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            HREmployeeRepository hrEmployeeRepository,
            ApplicantRepository applicantRepository,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.hrEmployeeRepository = hrEmployeeRepository;
        this.applicantRepository = applicantRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Right to Deletion (Art. 17 GDPR) - Delete all user data
     */
    public DeletionResponse deleteUserData(String userId, DeletionRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        // Log the deletion request
        auditLogService.logDataProcessing(userId, "DATA_DELETION_REQUESTED", 
            "User requested data deletion", request.reason());

        try {
            // Delete role-specific data first
            switch (user.getRole()) {
                case COMPANY_ADMIN -> deleteCompanyAdminData(userId);
                case HR -> deleteHREmployeeData(userId);
                case APPLICANT -> deleteApplicantData(userId);
            }

            // Anonymize user data instead of hard delete for audit trail
            anonymizeUserData(user);
            userRepository.save(user);

            auditLogService.logDataProcessing(userId, "DATA_DELETION_COMPLETED", 
                "User data successfully deleted/anonymized", null);

            return DeletionResponse.builder()
                .userId(userId)
                .deletionDate(LocalDateTime.now())
                .status("COMPLETED")
                .message("All personal data has been deleted or anonymized")
                .build();

        } catch (Exception e) {
            auditLogService.logDataProcessing(userId, "DATA_DELETION_FAILED", 
                "Failed to delete user data: " + e.getMessage(), null);
            throw new GdprComplianceException("Failed to delete user data", e);
        }
    }

    /**
     * Right to Data Portability (Art. 20 GDPR) - Export user data
     */
    public DataExportResponse exportUserData(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        auditLogService.logDataProcessing(userId, "DATA_EXPORT_REQUESTED", 
            "User requested data export", null);

        try {
            DataExportResponse.DataExportResponseBuilder responseBuilder = DataExportResponse.builder()
                .userId(userId)
                .exportDate(LocalDateTime.now())
                .personalData(buildPersonalDataExport(user));

            // Add role-specific data
            switch (user.getRole()) {
                case COMPANY_ADMIN -> responseBuilder.companyData(exportCompanyData(userId));
                case HR -> responseBuilder.hrData(exportHRData(userId));
                case APPLICANT -> responseBuilder.applicantData(exportApplicantData(userId));
            }

            DataExportResponse response = responseBuilder.build();

            auditLogService.logDataProcessing(userId, "DATA_EXPORT_COMPLETED", 
                "User data export completed", null);

            return response;

        } catch (Exception e) {
            auditLogService.logDataProcessing(userId, "DATA_EXPORT_FAILED", 
                "Failed to export user data: " + e.getMessage(), null);
            throw new GdprComplianceException("Failed to export user data", e);
        }
    }

    /**
     * Right to Rectification (Art. 16 GDPR) - Update user data
     */
    public RectificationResponse rectifyUserData(String userId, RectificationRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        auditLogService.logDataProcessing(userId, "DATA_RECTIFICATION_REQUESTED", 
            "User requested data rectification", request.toString());

        try {
            // Update personal data
            if (request.personalData() != null) {
                updatePersonalData(user, request.personalData());
            }

            // Update role-specific data
            if (request.roleSpecificData() != null) {
                updateRoleSpecificData(userId, user.getRole(), request.roleSpecificData());
            }

            userRepository.save(user);

            auditLogService.logDataProcessing(userId, "DATA_RECTIFICATION_COMPLETED", 
                "User data rectification completed", null);

            return RectificationResponse.builder()
                .userId(userId)
                .rectificationDate(LocalDateTime.now())
                .status("COMPLETED")
                .message("Personal data has been updated successfully")
                .build();

        } catch (Exception e) {
            auditLogService.logDataProcessing(userId, "DATA_RECTIFICATION_FAILED", 
                "Failed to rectify user data: " + e.getMessage(), null);
            throw new GdprComplianceException("Failed to rectify user data", e);
        }
    }

    /**
     * Get processing activities for a user (Art. 30 GDPR)
     */
    public ProcessingActivitiesResponse getProcessingActivities(String userId) {
        auditLogService.logDataProcessing(userId, "PROCESSING_ACTIVITIES_REQUESTED", 
            "User requested processing activities log", null);

        List<ProcessingActivity> activities = auditLogService.getProcessingActivities(userId);
        
        return ProcessingActivitiesResponse.createSuccessResponse(userId, activities);
    }

    // Private helper methods

    private void deleteCompanyAdminData(String userId) {
        companyRepository.findByAdminUserId(userId).ifPresent(company -> {
            // Anonymize company data or transfer ownership
            company.setAdminUserId(null);
            companyRepository.save(company);
        });
    }

    private void deleteHREmployeeData(String userId) {
        hrEmployeeRepository.findByUserId(userId).ifPresent(hrEmployeeRepository::delete);
    }

    private void deleteApplicantData(String userId) {
        applicantRepository.findByUserId(userId).ifPresent(applicantRepository::delete);
    }

    private void anonymizeUserData(User user) {
        user.setEmail("anonymized_" + user.getId() + "@deleted.local");
        user.setFirstName("DELETED");
        user.setLastName("USER");
        user.setPassword("ANONYMIZED");
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.clearTokens();
    }

    private PersonalDataExport buildPersonalDataExport(User user) {
        return PersonalDataExport.builder()
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().toString())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .emailVerified(user.isEmailVerified())
            .build();
    }

    private CompanyDataExport exportCompanyData(String userId) {
        return companyRepository.findByAdminUserId(userId)
            .map(company -> CompanyDataExport.builder()
                .name(company.getName())
                .location(company.getLocation())
                .businessEmail(company.getBusinessEmail())
                .telephone(company.getTelephone())
                .createdAt(company.getCreatedAt())
                .build())
            .orElse(null);
    }

    private HRDataExport exportHRData(String userId) {
        return hrEmployeeRepository.findByUserId(userId)
            .map(hr -> HRDataExport.builder()
                .department(hr.getDepartment())
                .position(hr.getPosition())
                .employeeId(hr.getEmployeeId())
                .companyId(hr.getCompanyId())
                .createdAt(hr.getCreatedAt())
                .build())
            .orElse(null);
    }

    private ApplicantDataExport exportApplicantData(String userId) {
        return applicantRepository.findByUserId(userId)
            .map(applicant -> ApplicantDataExport.builder()
                .phoneNumber(applicant.getPhoneNumber())
                .currentLocation(applicant.getCurrentLocation())
                .createdAt(applicant.getCreatedAt())
                .build())
            .orElse(null);
    }

    private void updatePersonalData(User user, PersonalDataUpdate personalData) {
        if (personalData.firstName() != null) {
            user.setFirstName(personalData.firstName());
        }
        if (personalData.lastName() != null) {
            user.setLastName(personalData.lastName());
        }
        if (personalData.email() != null) {
            user.setEmail(personalData.email());
        }
    }

    private void updateRoleSpecificData(String userId, UserRole role, Object roleSpecificData) {
        switch (role) {
            case HR -> updateHRData(userId, (HRDataUpdate) roleSpecificData);
            case APPLICANT -> updateApplicantData(userId, (ApplicantDataUpdate) roleSpecificData);
            case COMPANY_ADMIN -> updateCompanyData(userId, (CompanyDataUpdate) roleSpecificData);
        }
    }

    private void updateHRData(String userId, HRDataUpdate hrData) {
        hrEmployeeRepository.findByUserId(userId).ifPresent(hr -> {
            if (hrData.department() != null) hr.setDepartment(hrData.department());
            if (hrData.position() != null) hr.setPosition(hrData.position());
            hrEmployeeRepository.save(hr);
        });
    }

    private void updateApplicantData(String userId, ApplicantDataUpdate applicantData) {
        applicantRepository.findByUserId(userId).ifPresent(applicant -> {
            if (applicantData.phoneNumber() != null) applicant.setPhoneNumber(applicantData.phoneNumber());
            if (applicantData.currentLocation() != null) applicant.setCurrentLocation(applicantData.currentLocation());
            applicantRepository.save(applicant);
        });
    }

    private void updateCompanyData(String userId, CompanyDataUpdate companyData) {
        companyRepository.findByAdminUserId(userId).ifPresent(company -> {
            if (companyData.name() != null) company.setName(companyData.name());
            if (companyData.location() != null) company.setLocation(companyData.location());
            if (companyData.telephone() != null) company.setTelephone(companyData.telephone());
            companyRepository.save(company);
        });
    }
}