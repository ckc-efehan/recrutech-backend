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
 * Phase 2 Refactored: Handles only User identity data (auth service responsibility).
 * 
 * Implements:
 * - Right to Deletion (Art. 17 GDPR) - User identity data only
 * - Right to Data Portability (Art. 20 GDPR) - User identity data only
 * - Right to Rectification (Art. 16 GDPR) - User identity data only
 * - Audit Logging for data processing activities
 * 
 * IMPORTANT: Full GDPR compliance requires coordination with platform service
 * for domain data (Applicant, Company, HREmployee). Platform service should
 * consume AccountDisabledEvent to handle domain data deletion.
 */
@Service
@Transactional
public class GdprComplianceService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public GdprComplianceService(
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Right to Deletion (Art. 17 GDPR) - Delete User identity data only.
     * Phase 2: Domain data deletion must be coordinated with platform service.
     */
    public DeletionResponse deleteUserData(String userId, DeletionRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        // Log the deletion request
        auditLogService.logDataProcessing(userId, "DATA_DELETION_REQUESTED", 
            "User requested data deletion", request.reason());

        try {
            // Anonymize user identity data instead of hard delete for audit trail
            anonymizeUserData(user);
            userRepository.save(user);

            auditLogService.logDataProcessing(userId, "DATA_DELETION_COMPLETED", 
                "User identity data successfully anonymized", null);

            return DeletionResponse.builder()
                .userId(userId)
                .success(true)
                .deletionDate(LocalDateTime.now())
                .status("COMPLETED")
                .message("User identity data has been anonymized. Domain data deletion requires platform service coordination.")
                .build();

        } catch (Exception e) {
            auditLogService.logDataProcessing(userId, "DATA_DELETION_FAILED", 
                "Failed to delete user data: " + e.getMessage(), null);
            throw new GdprComplianceException("Failed to delete user data", e);
        }
    }

    /**
     * Right to Data Portability (Art. 20 GDPR) - Export User identity data only.
     * Phase 2: Domain data export must be coordinated with platform service.
     */
    public DataExportResponse exportUserData(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        auditLogService.logDataProcessing(userId, "DATA_EXPORT_REQUESTED", 
            "User requested data export", null);

        try {
            DataExportResponse response = DataExportResponse.builder()
                .userId(userId)
                .exportDate(LocalDateTime.now())
                .personalData(buildPersonalDataExport(user))
                .companyData(null)  // Domain data in platform service
                .hrData(null)  // Domain data in platform service
                .applicantData(null)  // Domain data in platform service
                .build();

            auditLogService.logDataProcessing(userId, "DATA_EXPORT_COMPLETED", 
                "User identity data export completed", null);

            return response;

        } catch (Exception e) {
            auditLogService.logDataProcessing(userId, "DATA_EXPORT_FAILED", 
                "Failed to export user data: " + e.getMessage(), null);
            throw new GdprComplianceException("Failed to export user data", e);
        }
    }

    /**
     * Right to Rectification (Art. 16 GDPR) - Update User identity data only.
     * Phase 2: Domain data rectification must be coordinated with platform service.
     */
    public RectificationResponse rectifyUserData(String userId, RectificationRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        auditLogService.logDataProcessing(userId, "DATA_RECTIFICATION_REQUESTED", 
            "User requested data rectification", request.toString());

        try {
            // Update User identity personal data only
            if (request.personalData() != null) {
                updatePersonalData(user, (PersonalDataUpdate) request.personalData());
            }

            userRepository.save(user);

            auditLogService.logDataProcessing(userId, "DATA_RECTIFICATION_COMPLETED", 
                "User identity data rectification completed", null);

            return RectificationResponse.builder()
                .userId(userId)
                .success(true)
                .rectificationDate(LocalDateTime.now())
                .status("COMPLETED")
                .message("User identity data has been updated successfully. Domain data updates require platform service coordination.")
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

    // Private helper methods - Phase 2: Only User identity data operations

    /**
     * Anonymize User identity data for GDPR compliance.
     * Phase 2: Session cleared instead of tokens (tokens are in Redis).
     */
    private void anonymizeUserData(User user) {
        user.setEmail("anonymized_" + user.getId() + "@deleted.local");
        user.setFirstName("DELETED");
        user.setLastName("USER");
        user.setPassword("ANONYMIZED");
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.clearSession();  // Phase 2: clearSession instead of clearTokens
    }

    /**
     * Build personal data export for User identity information.
     */
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

    /**
     * Update User identity personal data.
     */
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
}