package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.gdpr.*;
import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.exception.GdprComplianceException;
import com.recrutech.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Simplified test suite for GdprComplianceService.
 * Tests main GDPR compliance functionality without accessing internal DTO fields.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GDPR Compliance Service Tests")
class GdprComplianceServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private HREmployeeRepository hrEmployeeRepository;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private AuditLogService auditLogService;

    private GdprComplianceService gdprComplianceService;
    private User testUser;

    @BeforeEach
    void setUp() {
        gdprComplianceService = new GdprComplianceService(
            userRepository,
            companyRepository,
            hrEmployeeRepository,
            applicantRepository,
            auditLogService
        );

        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.APPLICANT);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
    }

    @Test
    @DisplayName("deleteUserData should successfully process deletion request")
    void testDeleteUserDataSuccess() {
        // Given
        DeletionRequest request = DeletionRequest.builder()
            .reason("No longer need the service")
            .confirmDataLoss(true)
            .build();

        Applicant applicant = new Applicant();
        applicant.setUserId("user-123");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(applicantRepository.findByUserId("user-123")).thenReturn(Optional.of(applicant));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        DeletionResponse response = gdprComplianceService.deleteUserData("user-123", request);

        // Then
        assertNotNull(response);
        assertEquals("user-123", response.userId());
        assertEquals("COMPLETED", response.status());
        assertNotNull(response.deletionDate());
        assertTrue(response.message().contains("deleted or anonymized"));

        verify(auditLogService, times(2)).logDataProcessing(eq("user-123"), anyString(), anyString(), any());
        verify(applicantRepository).delete(applicant);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("deleteUserData should throw NotFoundException for non-existent user")
    void testDeleteUserDataUserNotFound() {
        // Given
        DeletionRequest request = DeletionRequest.builder()
            .reason("Test deletion")
            .confirmDataLoss(true)
            .build();

        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When/Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> gdprComplianceService.deleteUserData("nonexistent", request));
        
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("deleteUserData should handle exceptions and throw GdprComplianceException")
    void testDeleteUserDataException() {
        // Given
        DeletionRequest request = DeletionRequest.builder()
            .reason("Test deletion")
            .confirmDataLoss(true)
            .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(applicantRepository.findByUserId("user-123")).thenThrow(new RuntimeException("Database error"));

        // When/Then
        GdprComplianceException exception = assertThrows(GdprComplianceException.class,
            () -> gdprComplianceService.deleteUserData("user-123", request));
        
        assertEquals("Failed to delete user data", exception.getMessage());
        verify(auditLogService).logDataProcessing(eq("user-123"), eq("DATA_DELETION_FAILED"), anyString(), isNull());
    }

    @Test
    @DisplayName("exportUserData should successfully export user data")
    void testExportUserDataSuccess() {
        // Given
        Applicant applicant = new Applicant();
        applicant.setUserId("user-123");
        applicant.setPhoneNumber("1234567890");
        applicant.setCurrentLocation("Test City");
        applicant.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(applicantRepository.findByUserId("user-123")).thenReturn(Optional.of(applicant));

        // When
        DataExportResponse response = gdprComplianceService.exportUserData("user-123");

        // Then
        assertNotNull(response);
        assertEquals("user-123", response.userId());
        assertNotNull(response.exportDate());
        assertNotNull(response.personalData());
        assertNotNull(response.applicantData());
        assertNull(response.companyData());
        assertNull(response.hrData());

        verify(auditLogService, times(2)).logDataProcessing(eq("user-123"), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("exportUserData should handle exceptions and throw GdprComplianceException")
    void testExportUserDataException() {
        // Given
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(applicantRepository.findByUserId("user-123")).thenThrow(new RuntimeException("Database error"));

        // When/Then
        GdprComplianceException exception = assertThrows(GdprComplianceException.class,
            () -> gdprComplianceService.exportUserData("user-123"));
        
        assertEquals("Failed to export user data", exception.getMessage());
        verify(auditLogService).logDataProcessing(eq("user-123"), eq("DATA_EXPORT_FAILED"), anyString(), isNull());
    }

    @Test
    @DisplayName("rectifyUserData should successfully process rectification request")
    void testRectifyUserDataSuccess() {
        // Given
        RectificationRequest request = RectificationRequest.builder()
            .personalData(PersonalDataUpdate.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build())
            .reason("Name change after marriage")
            .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        RectificationResponse response = gdprComplianceService.rectifyUserData("user-123", request);

        // Then
        assertNotNull(response);
        assertEquals("user-123", response.userId());
        assertEquals("COMPLETED", response.status());
        assertNotNull(response.rectificationDate());
        assertEquals("Personal data has been updated successfully", response.message());

        verify(auditLogService, times(2)).logDataProcessing(eq("user-123"), anyString(), anyString(), any());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("rectifyUserData should handle exceptions and throw GdprComplianceException")
    void testRectifyUserDataException() {
        // Given
        RectificationRequest request = RectificationRequest.builder()
            .reason("Test rectification")
            .build();

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // When/Then
        GdprComplianceException exception = assertThrows(GdprComplianceException.class,
            () -> gdprComplianceService.rectifyUserData("user-123", request));
        
        assertEquals("Failed to rectify user data", exception.getMessage());
        verify(auditLogService).logDataProcessing(eq("user-123"), eq("DATA_RECTIFICATION_FAILED"), anyString(), isNull());
    }

    @Test
    @DisplayName("getProcessingActivities should return user activities")
    void testGetProcessingActivities() {
        // Given
        ProcessingActivity activity1 = ProcessingActivity.builder()
            .activityId("act-1")
            .userId("user-123")
            .activityType("LOGIN")
            .description("User login")
            .timestamp(LocalDateTime.now())
            .build();

        ProcessingActivity activity2 = ProcessingActivity.builder()
            .activityId("act-2")
            .userId("user-123")
            .activityType("DATA_EXPORT")
            .description("Data export")
            .timestamp(LocalDateTime.now().minusHours(1))
            .build();

        List<ProcessingActivity> activities = List.of(activity1, activity2);
        when(auditLogService.getProcessingActivities("user-123")).thenReturn(activities);

        // When
        ProcessingActivitiesResponse response = gdprComplianceService.getProcessingActivities("user-123");

        // Then
        assertNotNull(response);
        assertEquals("user-123", response.userId());
        assertNotNull(response.requestDate());
        assertEquals(2, response.activities().size());
        assertEquals(2, response.totalActivities());
        assertNotNull(response.earliestActivity());
        assertNotNull(response.latestActivity());

        verify(auditLogService).logDataProcessing(eq("user-123"), eq("PROCESSING_ACTIVITIES_REQUESTED"), anyString(), isNull());
        verify(auditLogService).getProcessingActivities("user-123");
    }

    @Test
    @DisplayName("deleteUserData should handle different user roles correctly")
    void testDeleteUserDataDifferentRoles() {
        // Test HR role
        testUser.setRole(UserRole.HR);
        DeletionRequest request = DeletionRequest.builder()
            .reason("Leaving company")
            .confirmDataLoss(true)
            .build();

        HREmployee hrEmployee = new HREmployee();
        hrEmployee.setUserId("user-123");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(hrEmployeeRepository.findByUserId("user-123")).thenReturn(Optional.of(hrEmployee));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        DeletionResponse response = gdprComplianceService.deleteUserData("user-123", request);

        // Then
        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        verify(hrEmployeeRepository).delete(hrEmployee);
    }

    @Test
    @DisplayName("exportUserData should handle different user roles correctly")
    void testExportUserDataDifferentRoles() {
        // Test HR role
        testUser.setRole(UserRole.HR);
        HREmployee hrEmployee = new HREmployee();
        hrEmployee.setUserId("user-123");
        hrEmployee.setDepartment("Human Resources");
        hrEmployee.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(hrEmployeeRepository.findByUserId("user-123")).thenReturn(Optional.of(hrEmployee));

        // When
        DataExportResponse response = gdprComplianceService.exportUserData("user-123");

        // Then
        assertNotNull(response);
        assertNotNull(response.hrData());
        assertNull(response.applicantData());
        assertNull(response.companyData());
        
        verify(auditLogService, times(2)).logDataProcessing(eq("user-123"), anyString(), anyString(), any());
    }
}