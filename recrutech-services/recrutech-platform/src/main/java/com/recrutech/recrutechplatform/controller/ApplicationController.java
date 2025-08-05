package com.recrutech.recrutechplatform.controller;

import com.recrutech.recrutechplatform.dto.application.ApplicationRequest;
import com.recrutech.recrutechplatform.dto.application.ApplicationResponse;
import com.recrutech.recrutechplatform.dto.application.ApplicationSummaryResponse;
import com.recrutech.recrutechplatform.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for handling application-related endpoints
 */
@RestController
@PreAuthorize("hasRole('USER') or hasRole('HR')") // Only applicants and HR can access these endpoints
@RequestMapping("/api/v1")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Endpoint for submitting a job application.
     * User information is automatically extracted from JWT token for security.
     * 
     * @param jobId The ID of the job to apply for
     * @param applicationRequest The application data (only cvFileId required)
     * @param authentication The JWT authentication containing user information
     * @return The created application
     */
    @PostMapping("/jobs/{jobId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse submitApplication(@PathVariable String jobId,
            @RequestBody @Valid ApplicationRequest applicationRequest,
            Authentication authentication) {
        
        // Extract userId from JWT token's "sub" claim for security
        String userId = ((JwtAuthenticationToken) authentication).getToken().getSubject();
        
        return applicationService.createApplication(jobId, applicationRequest, userId);
    }

    @GetMapping("/jobs/{jobId}/applications")
    @ResponseStatus(HttpStatus.OK)
    public List<ApplicationSummaryResponse> getAllApplications(@PathVariable String jobId) {
        return applicationService.getAllApplications();
    }

    @GetMapping("/jobs/{jobId}/applications/{applicationId}")
    @ResponseStatus(HttpStatus.OK)
    public ApplicationResponse getApplicationById(@PathVariable String applicationId, @PathVariable String jobId) {
        return applicationService.getApplicationById(applicationId);
    }
}