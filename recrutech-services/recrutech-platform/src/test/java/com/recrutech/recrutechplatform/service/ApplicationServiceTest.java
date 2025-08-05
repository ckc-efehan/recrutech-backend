package com.recrutech.recrutechplatform.service;

import com.recrutech.common.dto.UserInfo;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.service.UserInfoService;
import com.recrutech.recrutechplatform.dto.application.ApplicationRequest;
import com.recrutech.recrutechplatform.dto.application.ApplicationResponse;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}