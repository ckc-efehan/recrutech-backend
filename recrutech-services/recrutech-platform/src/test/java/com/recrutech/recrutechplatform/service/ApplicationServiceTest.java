package com.recrutech.recrutechplatform.service;

import com.recrutech.common.dto.UserInfo;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.service.UserInfoService;
import com.recrutech.recrutechplatform.dto.application.ApplicationRequest;
import com.recrutech.recrutechplatform.dto.application.ApplicationResponse;
import com.recrutech.recrutechplatform.dto.application.ApplicationSummaryResponse;
import com.recrutech.recrutechplatform.dto.application.JobInfo;
import com.recrutech.recrutechplatform.enums.ApplicationStatus;
import com.recrutech.recrutechplatform.model.Application;
import com.recrutech.recrutechplatform.model.Job;
import com.recrutech.recrutechplatform.repository.ApplicationRepository;
import com.recrutech.recrutechplatform.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserInfoService userInfoService;

    @InjectMocks
    private ApplicationService applicationService;

    private Job job;
    private String jobId;
    private String userId;
    private String cvFileId;
    private ApplicationRequest applicationRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        jobId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();
        cvFileId = UUID.randomUUID().toString();

        job = new Job();
        job.setId(jobId);
        job.setTitle("Software Engineer");
        job.setDescription("Job description");
        job.setLocation("Berlin");

        applicationRequest = new ApplicationRequest(cvFileId);
    }

    @Test
    void createApplication_Success() {
        // Arrange
        Application savedApplication = new Application();
        savedApplication.setId(UUID.randomUUID().toString());
        savedApplication.setCvFileId(cvFileId);
        savedApplication.setUserId(userId);
        savedApplication.setStatus(ApplicationStatus.RECEIVED);
        savedApplication.setViewedByHr(false);
        savedApplication.setJob(job);
        savedApplication.setCreatedAt(LocalDateTime.now());

        UserInfo userInfo = new UserInfo(userId, "John", "Doe");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(userInfoService.createUserInfo(userId)).thenReturn(userInfo);
        when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);

        // Act
        ApplicationResponse response = applicationService.createApplication(jobId, applicationRequest, userId);

        // Assert
        assertNotNull(response);
        assertEquals(savedApplication.getId(), response.id());
        assertEquals(jobId, response.job().id());
        assertEquals(cvFileId, response.cvFileId());
        assertEquals(ApplicationStatus.RECEIVED, response.status());
        assertFalse(response.viewedByHr());
        assertNotNull(response.createdAt());
        assertEquals("John", response.user().firstName());
        assertEquals("Doe", response.user().lastName());

        verify(jobRepository).findById(jobId);
        verify(userInfoService).createUserInfo(userId);
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void createApplication_JobNotFound() {
        // Arrange
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            applicationService.createApplication(jobId, applicationRequest, userId);
        });

        assertEquals("Job not found with id: " + jobId, exception.getMessage());
        verify(jobRepository).findById(jobId);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_InvalidCvFileId() {
        // Arrange
        ApplicationRequest invalidRequest = new ApplicationRequest("invalid-uuid");

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            applicationService.createApplication(jobId, invalidRequest, userId);
        });

        assertTrue(exception.getMessage().contains("CV File ID"));
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_InvalidJobId() {
        // Arrange
        String invalidJobId = "invalid-uuid";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            applicationService.createApplication(invalidJobId, applicationRequest, userId);
        });

        assertTrue(exception.getMessage().contains("Job ID"));
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_InvalidUserId() {
        // Arrange
        String invalidUserId = "invalid-uuid";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            applicationService.createApplication(jobId, applicationRequest, invalidUserId);
        });

        assertTrue(exception.getMessage().contains("User ID"));
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_NullJobId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            applicationService.createApplication(null, applicationRequest, userId);
        });

        assertTrue(exception.getMessage().contains("Job ID"));
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_NullRequest() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            applicationService.createApplication(jobId, null, userId);
        });

        assertTrue(exception.getMessage().contains("request"));
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void getAllApplications_Success() {
        // Arrange
        Application app1 = createTestApplication("app1", "user1");
        Application app2 = createTestApplication("app2", "user2");
        List<Application> applications = Arrays.asList(app1, app2);

        UserInfo userInfo1 = new UserInfo("user1", "John", "Doe");
        UserInfo userInfo2 = new UserInfo("user2", "Jane", "Smith");

        when(applicationRepository.findAll()).thenReturn(applications);
        when(userInfoService.createUserInfo("user1")).thenReturn(userInfo1);
        when(userInfoService.createUserInfo("user2")).thenReturn(userInfo2);

        // Act
        List<ApplicationSummaryResponse> result = applicationService.getAllApplications();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        ApplicationSummaryResponse summary1 = result.get(0);
        assertEquals("app1", summary1.id());
        assertEquals(jobId, summary1.job().id());
        assertEquals("John", summary1.user().firstName());
        assertEquals("Doe", summary1.user().lastName());
        assertEquals("RECEIVED", summary1.status());

        ApplicationSummaryResponse summary2 = result.get(1);
        assertEquals("app2", summary2.id());
        assertEquals("Jane", summary2.user().firstName());
        assertEquals("Smith", summary2.user().lastName());

        verify(applicationRepository).findAll();
        verify(userInfoService).createUserInfo("user1");
        verify(userInfoService).createUserInfo("user2");
    }

    @Test
    void getAllApplications_EmptyList() {
        // Arrange
        when(applicationRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<ApplicationSummaryResponse> result = applicationService.getAllApplications();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(applicationRepository).findAll();
        verify(userInfoService, never()).createUserInfo(anyString());
    }

    @Test
    void getApplicationById_Success() {
        // Arrange
        String applicationId = UUID.randomUUID().toString();
        Application application = createTestApplication(applicationId, userId);
        UserInfo userInfo = new UserInfo(userId, "John", "Doe");

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(userInfoService.createUserInfo(userId)).thenReturn(userInfo);

        // Act
        ApplicationResponse result = applicationService.getApplicationById(applicationId);

        // Assert
        assertNotNull(result);
        assertEquals(applicationId, result.id());
        assertEquals(jobId, result.job().id());
        assertEquals("Software Engineer", result.job().title());
        assertEquals("Berlin", result.job().location());
        assertEquals("John", result.user().firstName());
        assertEquals("Doe", result.user().lastName());
        assertEquals(cvFileId, result.cvFileId());
        assertEquals(ApplicationStatus.RECEIVED, result.status());
        assertFalse(result.viewedByHr());
        assertNotNull(result.createdAt());

        verify(applicationRepository).findById(applicationId);
        verify(userInfoService).createUserInfo(userId);
    }

    @Test
    void getApplicationById_NotFound() {
        // Arrange
        String applicationId = UUID.randomUUID().toString();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            applicationService.getApplicationById(applicationId);
        });

        assertEquals("Application not found with id: " + applicationId, exception.getMessage());
        verify(applicationRepository).findById(applicationId);
        verify(userInfoService, never()).createUserInfo(anyString());
    }

    @Test
    void getApplicationById_InvalidId() {
        // Arrange
        String invalidId = "invalid-uuid";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            applicationService.getApplicationById(invalidId);
        });

        assertTrue(exception.getMessage().contains("Application ID"));
        verify(applicationRepository, never()).findById(anyString());
        verify(userInfoService, never()).createUserInfo(anyString());
    }

    @Test
    void getApplicationById_NullId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            applicationService.getApplicationById(null);
        });

        assertTrue(exception.getMessage().contains("Application ID"));
        verify(applicationRepository, never()).findById(anyString());
        verify(userInfoService, never()).createUserInfo(anyString());
    }

    private Application createTestApplication(String applicationId, String userId) {
        Application application = new Application();
        application.setId(applicationId);
        application.setCvFileId(cvFileId);
        application.setUserId(userId);
        application.setStatus(ApplicationStatus.RECEIVED);
        application.setViewedByHr(false);
        application.setJob(job);
        application.setCreatedAt(LocalDateTime.now());
        return application;
    }
}