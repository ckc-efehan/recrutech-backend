package com.recrutech.recrutechauth.controller;

import com.recrutech.recrutechauth.dto.gdpr.*;
import com.recrutech.recrutechauth.service.GdprComplianceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for GDPR compliance endpoints.
 * Provides endpoints for users to exercise their GDPR rights:
 * - Right to Deletion (Art. 17)
 * - Right to Data Portability (Art. 20)
 * - Right to Rectification (Art. 16)
 * - Access to Processing Activities (Art. 30)
 */
@RestController
@RequestMapping("/api/gdpr")
public class GdprController {

    private final GdprComplianceService gdprComplianceService;

    public GdprController(GdprComplianceService gdprComplianceService) {
        this.gdprComplianceService = gdprComplianceService;
    }

    /**
     * Right to Deletion (Art. 17 GDPR)
     * Allows users to request deletion of their personal data
     */
    @PostMapping("/delete-data")
    @PreAuthorize("hasRole('USER') or hasRole('COMPANY_ADMIN') or hasRole('HR') or hasRole('APPLICANT')")
    public ResponseEntity<DeletionResponse> deleteUserData(
            @Valid @RequestBody DeletionRequest request,
            @RequestParam String userId,
            HttpServletRequest httpRequest) {
        
        try {
            // Validate business rules
            request.validateBusinessRules();
            
            // Process deletion request
            DeletionResponse response = gdprComplianceService.deleteUserData(userId, request);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(DeletionResponse.createFailureResponse(userId, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DeletionResponse.createFailureResponse(userId, "Internal server error occurred"));
        }
    }

    /**
     * Right to Data Portability (Art. 20 GDPR)
     * Allows users to export their personal data
     */
    @GetMapping("/export-data")
    @PreAuthorize("hasRole('USER') or hasRole('COMPANY_ADMIN') or hasRole('HR') or hasRole('APPLICANT')")
    public ResponseEntity<DataExportResponse> exportUserData(
            @RequestParam String userId,
            HttpServletRequest httpRequest) {
        
        try {
            DataExportResponse response = gdprComplianceService.exportUserData(userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DataExportResponse.builder()
                    .userId(userId)
                    .exportDate(java.time.LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Right to Rectification (Art. 16 GDPR)
     * Allows users to correct their personal data
     */
    @PutMapping("/rectify-data")
    @PreAuthorize("hasRole('USER') or hasRole('COMPANY_ADMIN') or hasRole('HR') or hasRole('APPLICANT')")
    public ResponseEntity<RectificationResponse> rectifyUserData(
            @Valid @RequestBody RectificationRequest request,
            @RequestParam String userId,
            HttpServletRequest httpRequest) {
        
        try {
            // Validate business rules
            request.validateBusinessRules();
            
            // Process rectification request
            RectificationResponse response = gdprComplianceService.rectifyUserData(userId, request);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(RectificationResponse.createFailureResponse(userId, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RectificationResponse.createFailureResponse(userId, "Internal server error occurred"));
        }
    }

    /**
     * Processing Activities (Art. 30 GDPR)
     * Allows users to view their data processing activities
     */
    @GetMapping("/processing-activities")
    @PreAuthorize("hasRole('USER') or hasRole('COMPANY_ADMIN') or hasRole('HR') or hasRole('APPLICANT')")
    public ResponseEntity<ProcessingActivitiesResponse> getProcessingActivities(
            @RequestParam String userId,
            HttpServletRequest httpRequest) {
        
        try {
            ProcessingActivitiesResponse response = gdprComplianceService.getProcessingActivities(userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProcessingActivitiesResponse.builder()
                    .userId(userId)
                    .requestDate(java.time.LocalDateTime.now())
                    .activities(java.util.List.of())
                    .totalActivities(0)
                    .build());
        }
    }

    /**
     * GDPR Information Endpoint
     * Provides information about GDPR rights and procedures
     */
    @GetMapping("/info")
    public ResponseEntity<GdprInfoResponse> getGdprInfo() {
        GdprInfoResponse response = GdprInfoResponse.builder()
            .rightToDeletion("You have the right to request deletion of your personal data under GDPR Article 17")
            .rightToPortability("You have the right to receive your personal data in a structured format under GDPR Article 20")
            .rightToRectification("You have the right to request correction of inaccurate personal data under GDPR Article 16")
            .processingActivities("You have the right to access information about how your data is processed under GDPR Article 30")
            .contactEmail("privacy@recrutech.com")
            .dataProtectionOfficer("DPO Team")
            .retentionPeriod("Personal data is retained for the duration of your account plus 30 days, unless longer retention is required by law")
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for GDPR service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("GDPR Compliance Service is operational");
    }
}
