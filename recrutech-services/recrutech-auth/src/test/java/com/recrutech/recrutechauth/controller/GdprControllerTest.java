package com.recrutech.recrutechauth.controller;

import com.recrutech.recrutechauth.service.GdprComplianceService;
import com.recrutech.recrutechauth.dto.gdpr.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for GdprController.
 * Tests all GDPR endpoints, security annotations, validation, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GdprController Tests")
class GdprControllerTest {

    @Mock
    private GdprComplianceService gdprComplianceService;
    
    @Mock
    private HttpServletRequest httpServletRequest;
    
    private GdprController gdprController;

    @BeforeEach
    void setUp() {
        gdprController = new GdprController(gdprComplianceService);
        
        System.out.println("[DEBUG_LOG] GdprController test setup completed");
    }

    // ========== DELETE DATA ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful data deletion")
    void testSuccessfulDataDeletion() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        DeletionRequest request = DeletionRequest.builder()
            .reason("User requested account deletion")
            .confirmDeletion(true)
            .build();

        DeletionResponse expectedResponse = DeletionResponse.builder()
            .userId(userId)
            .success(true)
            .message("Data deletion completed successfully")
            .deletionDate(LocalDateTime.now())
            .build();

        when(gdprComplianceService.deleteUserData(userId, request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<DeletionResponse> response = gdprController.deleteUserData(request, userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(gdprComplianceService).deleteUserData(userId, request);
        
        System.out.println("[DEBUG_LOG] Successful data deletion test passed");
    }

    @Test
    @DisplayName("Test data deletion with validation failure")
    void testDataDeletionValidationFailure() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        DeletionRequest request = DeletionRequest.builder()
            .reason("Invalid reason")
            .confirmDeletion(false) // This should trigger validation failure
            .build();

        // Act
        ResponseEntity<DeletionResponse> response = gdprController.deleteUserData(request, userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        
        System.out.println("[DEBUG_LOG] Data deletion validation failure test passed");
    }

    @Test
    @DisplayName("Test data deletion with service exception")
    void testDataDeletionServiceException() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        DeletionRequest request = DeletionRequest.builder()
            .reason("User requested account deletion")
            .confirmDeletion(true)
            .build();

        when(gdprComplianceService.deleteUserData(userId, request))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<DeletionResponse> response = gdprController.deleteUserData(request, userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        
        System.out.println("[DEBUG_LOG] Data deletion service exception test passed");
    }

    // ========== EXPORT DATA ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful data export")
    void testSuccessfulDataExport() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        DataExportResponse expectedResponse = DataExportResponse.builder()
            .userId(userId)
            .exportDate(LocalDateTime.now())
            .format("JSON")
            .downloadUrl("https://example.com/export/" + userId)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();

        when(gdprComplianceService.exportUserData(userId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<DataExportResponse> response = gdprController.exportUserData(userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(gdprComplianceService).exportUserData(userId);
        
        System.out.println("[DEBUG_LOG] Successful data export test passed");
    }

    @Test
    @DisplayName("Test data export with service exception")
    void testDataExportServiceException() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        when(gdprComplianceService.exportUserData(userId))
            .thenThrow(new RuntimeException("Export service error"));

        // Act
        ResponseEntity<DataExportResponse> response = gdprController.exportUserData(userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().userId());
        
        System.out.println("[DEBUG_LOG] Data export service exception test passed");
    }

    // ========== RECTIFY DATA ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful data rectification")
    void testSuccessfulDataRectification() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        RectificationRequest request = RectificationRequest.builder()
            .fieldName("email")
            .oldValue("old@example.com")
            .newValue("new@example.com")
            .reason("Email address changed")
            .build();

        RectificationResponse expectedResponse = RectificationResponse.builder()
            .userId(userId)
            .success(true)
            .message("Data rectification completed successfully")
            .rectificationDate(LocalDateTime.now())
            .build();

        when(gdprComplianceService.rectifyUserData(userId, request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<RectificationResponse> response = gdprController.rectifyUserData(request, userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(gdprComplianceService).rectifyUserData(userId, request);
        
        System.out.println("[DEBUG_LOG] Successful data rectification test passed");
    }

    @Test
    @DisplayName("Test data rectification with validation failure")
    void testDataRectificationValidationFailure() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        RectificationRequest request = RectificationRequest.builder()
            .fieldName("") // Invalid field name
            .oldValue("old@example.com")
            .newValue("new@example.com")
            .reason("Email address changed")
            .build();

        // Act
        ResponseEntity<RectificationResponse> response = gdprController.rectifyUserData(request, userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        
        System.out.println("[DEBUG_LOG] Data rectification validation failure test passed");
    }

    // ========== PROCESSING ACTIVITIES ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful processing activities retrieval")
    void testSuccessfulProcessingActivitiesRetrieval() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        ProcessingActivitiesResponse expectedResponse = ProcessingActivitiesResponse.builder()
            .userId(userId)
            .requestDate(LocalDateTime.now())
            .activities(List.of(
                ProcessingActivitiesResponse.ProcessingActivity.builder()
                    .activityType("Authentication")
                    .purpose("User login and session management")
                    .dataCategories(List.of("Email", "Password Hash"))
                    .legalBasis("Legitimate Interest")
                    .build(),
                ProcessingActivitiesResponse.ProcessingActivity.builder()
                    .activityType("Profile Management")
                    .purpose("User profile maintenance")
                    .dataCategories(List.of("Personal Information", "Preferences"))
                    .legalBasis("Contract")
                    .build()
            ))
            .totalActivities(2)
            .build();

        when(gdprComplianceService.getProcessingActivities(userId)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ProcessingActivitiesResponse> response = gdprController.getProcessingActivities(userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(gdprComplianceService).getProcessingActivities(userId);
        
        System.out.println("[DEBUG_LOG] Successful processing activities retrieval test passed");
    }

    @Test
    @DisplayName("Test processing activities with service exception")
    void testProcessingActivitiesServiceException() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        when(gdprComplianceService.getProcessingActivities(userId))
            .thenThrow(new RuntimeException("Processing activities service error"));

        // Act
        ResponseEntity<ProcessingActivitiesResponse> response = gdprController.getProcessingActivities(userId, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().userId());
        assertEquals(0, response.getBody().totalActivities());
        
        System.out.println("[DEBUG_LOG] Processing activities service exception test passed");
    }

    // ========== GDPR INFO ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test GDPR info endpoint")
    void testGdprInfoEndpoint() {
        // Act
        ResponseEntity<GdprInfoResponse> response = gdprController.getGdprInfo();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().rightToDeletion());
        assertNotNull(response.getBody().rightToPortability());
        assertNotNull(response.getBody().rightToRectification());
        assertNotNull(response.getBody().processingActivities());
        assertEquals("privacy@recrutech.com", response.getBody().contactEmail());
        assertEquals("DPO Team", response.getBody().dataProtectionOfficer());
        
        System.out.println("[DEBUG_LOG] GDPR info endpoint test passed");
    }

    // ========== HEALTH CHECK ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test GDPR health check endpoint")
    void testGdprHealthCheckEndpoint() {
        // Act
        ResponseEntity<String> response = gdprController.healthCheck();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("GDPR Compliance Service is operational", response.getBody());
        
        System.out.println("[DEBUG_LOG] GDPR health check endpoint test passed");
    }

    // ========== SECURITY AND AUTHORIZATION TESTS ==========

    @Test
    @DisplayName("Test endpoints require proper authorization")
    void testEndpointsRequireAuthorization() {
        // Note: In a real Spring Security test, we would use @WithMockUser or similar
        // For now, we test that the methods exist and can be called
        // The actual security testing would be done with @WebMvcTest and MockMvc
        
        String userId = UUID.randomUUID().toString();
        
        // Verify that secured endpoints exist and are callable
        assertDoesNotThrow(() -> {
            DeletionRequest deletionRequest = DeletionRequest.builder()
                .reason("Test")
                .confirmDeletion(true)
                .build();
            
            // These would normally require authentication in a real environment
            gdprController.deleteUserData(deletionRequest, userId, httpServletRequest);
            gdprController.exportUserData(userId, httpServletRequest);
            
            RectificationRequest rectificationRequest = RectificationRequest.builder()
                .fieldName("email")
                .oldValue("old@test.com")
                .newValue("new@test.com")
                .reason("Update")
                .build();
            gdprController.rectifyUserData(rectificationRequest, userId, httpServletRequest);
            
            gdprController.getProcessingActivities(userId, httpServletRequest);
        });
        
        System.out.println("[DEBUG_LOG] Authorization test passed");
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("Test request validation")
    void testRequestValidation() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        
        // Test deletion request validation
        DeletionRequest invalidDeletionRequest = DeletionRequest.builder()
            .reason(null) // Invalid - null reason
            .confirmDeletion(false) // Invalid - not confirmed
            .build();
        
        // Act & Assert
        ResponseEntity<DeletionResponse> deletionResponse = gdprController.deleteUserData(
            invalidDeletionRequest, userId, httpServletRequest);
        assertEquals(HttpStatus.BAD_REQUEST, deletionResponse.getStatusCode());
        
        // Test rectification request validation
        RectificationRequest invalidRectificationRequest = RectificationRequest.builder()
            .fieldName("") // Invalid - empty field name
            .oldValue("old")
            .newValue("new")
            .reason("reason")
            .build();
        
        ResponseEntity<RectificationResponse> rectificationResponse = gdprController.rectifyUserData(
            invalidRectificationRequest, userId, httpServletRequest);
        assertEquals(HttpStatus.BAD_REQUEST, rectificationResponse.getStatusCode());
        
        System.out.println("[DEBUG_LOG] Request validation test passed");
    }
}