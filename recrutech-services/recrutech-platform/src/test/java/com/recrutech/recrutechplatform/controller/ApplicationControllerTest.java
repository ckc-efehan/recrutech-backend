package com.recrutech.recrutechplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recrutech.common.dto.UserInfo;
import com.recrutech.recrutechplatform.dto.application.ApplicationRequest;
import com.recrutech.recrutechplatform.dto.application.ApplicationResponse;
import com.recrutech.recrutechplatform.dto.application.ApplicationSummaryResponse;
import com.recrutech.recrutechplatform.dto.application.JobInfo;
import com.recrutech.recrutechplatform.enums.ApplicationStatus;
import com.recrutech.common.exception.GlobalExceptionHandler;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.recrutechplatform.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationService applicationService;

    private ApplicationController applicationController;

    private ApplicationRequest applicationRequest;
    private ApplicationResponse applicationResponse;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        applicationController = new ApplicationController(applicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(applicationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        testDateTime = LocalDateTime.now();

        applicationRequest = new ApplicationRequest(
                "123e4567-e89b-12d3-a456-426614174000"
        );

        applicationResponse = new ApplicationResponse(
                "app-id-123",
                new JobInfo("job-id-456", "Software Engineer", "Berlin"),
                new UserInfo("user-id-123", "John", "Doe"),
                "123e4567-e89b-12d3-a456-426614174000",
                ApplicationStatus.RECEIVED,
                false,
                testDateTime
        );
    }

    @Test
    void submitApplication_ShouldReturnCreatedApplicationResponse() throws Exception {
        // Arrange
        String jobId = "job-id-456";
        String userId = "user-id-123";
        
        // Create mock JWT token with subject claim
        Jwt mockJwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject(userId)
                .build();
        Authentication mockAuth = new JwtAuthenticationToken(mockJwt);
        
        when(applicationService.createApplication(any(String.class), any(ApplicationRequest.class), any(String.class)))
                .thenReturn(applicationResponse);

        // Act - Direct method call to test authentication handling
        ApplicationResponse result = applicationController.submitApplication(jobId, applicationRequest, mockAuth);

        // Assert
        assertNotNull(result);
        assertEquals("app-id-123", result.id());
        assertEquals("job-id-456", result.job().id());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.cvFileId());
        assertEquals(ApplicationStatus.RECEIVED, result.status());
        assertFalse(result.viewedByHr());

        verify(applicationService, times(1)).createApplication(eq(jobId), any(ApplicationRequest.class), eq(userId));
    }

    @Test
    void submitApplication_WhenJobDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        String jobId = "non-existent-job-id";
        String userId = "user-id-123";
        
        // Create mock JWT token with subject claim
        Jwt mockJwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject(userId)
                .build();
        Authentication mockAuth = new JwtAuthenticationToken(mockJwt);
        
        when(applicationService.createApplication(any(String.class), any(ApplicationRequest.class), any(String.class)))
                .thenThrow(new NotFoundException("Job not found with id: " + jobId));

        // Act & Assert - Direct method call to test authentication handling
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            applicationController.submitApplication(jobId, applicationRequest, mockAuth);
        });

        assertEquals("Job not found with id: " + jobId, exception.getMessage());
        verify(applicationService, times(1)).createApplication(eq(jobId), any(ApplicationRequest.class), eq(userId));
    }

    @Test
    void getAllApplications_ShouldReturnListOfApplicationResponses() throws Exception {
        // Arrange
        String jobId = "job-id-456";
        ApplicationSummaryResponse applicationSummaryResponse1 = new ApplicationSummaryResponse(
                "app-id-123", new JobInfo("job-id-456", "Software Engineer", "Berlin"), new UserInfo("user-id-1", "John", "Doe"), "RECEIVED");
        ApplicationSummaryResponse applicationSummaryResponse2 = new ApplicationSummaryResponse(
                "app-id-789", new JobInfo("job-id-456", "Software Engineer", "Berlin"), new UserInfo("user-id-2", "Jane", "Smith"), "UNDER_REVIEW");

        List<ApplicationSummaryResponse> applicationResponses = List.of(applicationSummaryResponse1, applicationSummaryResponse2);
        when(applicationService.getAllApplications(jobId)).thenReturn(applicationResponses);

        // Act & Assert
        mockMvc.perform(get("/api/v1/jobs/{jobId}/applications", jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("app-id-123")))
                .andExpect(jsonPath("$[0].job.id", is("job-id-456")))
                .andExpect(jsonPath("$[0].job.title", is("Software Engineer")))
                .andExpect(jsonPath("$[0].job.location", is("Berlin")))
                .andExpect(jsonPath("$[0].status", is("RECEIVED")))
                .andExpect(jsonPath("$[1].id", is("app-id-789")))
                .andExpect(jsonPath("$[1].job.id", is("job-id-456")))
                .andExpect(jsonPath("$[1].job.title", is("Software Engineer")))
                .andExpect(jsonPath("$[1].job.location", is("Berlin")))
                .andExpect(jsonPath("$[1].status", is("UNDER_REVIEW")));

        verify(applicationService, times(1)).getAllApplications(jobId);
    }

    @Test
    void getAllApplications_WhenNoApplications_ShouldReturnEmptyList() throws Exception {
        // Arrange
        String jobId = "job-id-456";
        when(applicationService.getAllApplications(jobId)).thenReturn(List.<ApplicationSummaryResponse>of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/jobs/{jobId}/applications", jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(applicationService, times(1)).getAllApplications(jobId);
    }

    @Test
    void getApplicationById_WhenApplicationExists_ShouldReturnApplicationResponse() throws Exception {
        // Arrange
        String jobId = "job-id-456";
        String applicationId = "app-id-123";
        when(applicationService.getApplicationById(applicationId, jobId)).thenReturn(applicationResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/jobs/{jobId}/applications/{applicationId}", jobId, applicationId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("app-id-123")))
                .andExpect(jsonPath("$.job.id", is("job-id-456")))
                .andExpect(jsonPath("$.job.title", is("Software Engineer")))
                .andExpect(jsonPath("$.job.location", is("Berlin")))
                .andExpect(jsonPath("$.cvFileId", is("123e4567-e89b-12d3-a456-426614174000")))
                .andExpect(jsonPath("$.status", is("RECEIVED")))
                .andExpect(jsonPath("$.viewedByHr", is(false)));

        verify(applicationService, times(1)).getApplicationById(applicationId, jobId);
    }

    @Test
    void getApplicationById_WhenApplicationDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        String jobId = "job-id-456";
        String applicationId = "non-existent-app-id";
        when(applicationService.getApplicationById(applicationId, jobId))
                .thenThrow(new NotFoundException("Application not found with id: " + applicationId + " for job: " + jobId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/jobs/{jobId}/applications/{applicationId}", jobId, applicationId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Application not found with id: " + applicationId + " for job: " + jobId)));

        verify(applicationService, times(1)).getApplicationById(applicationId, jobId);
    }

    @Test
    void submitApplication_WithDifferentStatus_ShouldReturnCorrectStatus() throws Exception {
        // Arrange
        String jobId = "job-id-456";
        String userId = "user-id-123";
        
        // Create mock JWT token with subject claim
        Jwt mockJwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject(userId)
                .build();
        Authentication mockAuth = new JwtAuthenticationToken(mockJwt);
        ApplicationResponse reviewResponse = new ApplicationResponse(
                "app-id-123",
                new JobInfo(jobId, "Software Engineer", "Berlin"),
                new UserInfo("user-id-123", "John", "Doe"),
                "123e4567-e89b-12d3-a456-426614174000",
                ApplicationStatus.UNDER_REVIEW,
                true,
                testDateTime
        );

        when(applicationService.createApplication(any(String.class), any(ApplicationRequest.class), any(String.class)))
                .thenReturn(reviewResponse);

        // Act - Direct method call to test authentication handling
        ApplicationResponse result = applicationController.submitApplication(jobId, applicationRequest, mockAuth);

        // Assert
        assertNotNull(result);
        assertEquals("app-id-123", result.id());
        assertEquals(jobId, result.job().id());
        assertEquals(ApplicationStatus.UNDER_REVIEW, result.status());
        assertTrue(result.viewedByHr());

        verify(applicationService, times(1)).createApplication(eq(jobId), any(ApplicationRequest.class), eq(userId));
    }

    @Test
    void getAllApplications_WithDifferentStatuses_ShouldReturnAllStatuses() throws Exception {
        // Arrange
        String jobId = "job-id-456";
        ApplicationSummaryResponse receivedApp = new ApplicationSummaryResponse(
                "app-id-1", new JobInfo("job-id-456", "Frontend Developer", "Hamburg"), new UserInfo("user-id-1", "Alice", "Johnson"), "RECEIVED");
        ApplicationSummaryResponse invitedApp = new ApplicationSummaryResponse(
                "app-id-2", new JobInfo("job-id-456", "Backend Developer", "Frankfurt"), new UserInfo("user-id-2", "Bob", "Wilson"), "INVITED");
        ApplicationSummaryResponse rejectedApp = new ApplicationSummaryResponse(
                "app-id-3", new JobInfo("job-id-456", "DevOps Engineer", "Cologne"), new UserInfo("user-id-3", "Charlie", "Brown"), "REJECTED");

        List<ApplicationSummaryResponse> applicationResponses = List.of(receivedApp, invitedApp, rejectedApp);
        when(applicationService.getAllApplications(jobId)).thenReturn(applicationResponses);

        // Act & Assert
        mockMvc.perform(get("/api/v1/jobs/{jobId}/applications", jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status", is("RECEIVED")))
                .andExpect(jsonPath("$[1].status", is("INVITED")))
                .andExpect(jsonPath("$[2].status", is("REJECTED")));

        verify(applicationService, times(1)).getAllApplications(jobId);
    }
}