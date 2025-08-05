package com.recrutech.recrutechplatform.service;

import com.recrutech.common.dto.UserInfo;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.service.UserInfoService;
import com.recrutech.common.util.UuidValidator;
import com.recrutech.common.validator.ApplicationValidator;
import com.recrutech.recrutechplatform.dto.application.ApplicationRequest;
import com.recrutech.recrutechplatform.dto.application.ApplicationResponse;
import com.recrutech.recrutechplatform.dto.application.ApplicationSummaryResponse;
import com.recrutech.recrutechplatform.dto.application.JobInfo;
import com.recrutech.recrutechplatform.enums.ApplicationStatus;
import com.recrutech.recrutechplatform.model.Application;
import com.recrutech.recrutechplatform.model.Job;
import com.recrutech.recrutechplatform.repository.ApplicationRepository;
import com.recrutech.recrutechplatform.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserInfoService userInfoService;

    public ApplicationService(ApplicationRepository applicationRepository, JobRepository jobRepository, UserInfoService userInfoService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userInfoService = userInfoService;
    }

    /**
     * Helper method to find a job by ID or throw NotFoundException.
     *
     * @param jobId the job ID to search for
     * @param operation the operation being performed (for logging)
     * @return the found job
     * @throws NotFoundException if the job is not found
     */
    private Job findJobByIdOrThrow(String jobId, String operation) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.warn("Job with id {} not found for {}", jobId, operation);
                    return new NotFoundException("Job not found with id: " + jobId);
                });
    }


    /**
     * Creates a new job application with automatic user data extraction.
     *
     * @param jobId the ID of the job to apply for
     * @param request the application request containing CV information
     * @param userId the user ID extracted from JWT token
     * @return the created application response
     * @throws ValidationException if the job ID or request data is invalid
     * @throws NotFoundException if the job is not found
     */
    @Transactional
    public ApplicationResponse createApplication(String jobId, ApplicationRequest request, String userId) {
        log.debug("Creating new application for job: {} by user: {}", jobId, userId);
        
        // Validate input parameters
        ApplicationValidator.validateCreateApplicationInput(jobId, request);
        UuidValidator.validateUuid(userId, "User ID");
        
        // Verify job exists
        Job job = findJobByIdOrThrow(jobId, "application creation");

        // Automatically get user information from UserInfoService
        UserInfo userInfo = userInfoService.createUserInfo(userId);

        // Create a new application
        Application application = new Application();
        application.setCvFileId(request.cvFileId());
        application.setUserId(userId);
        application.setStatus(ApplicationStatus.RECEIVED);
        application.setViewedByHr(false);
        application.setJob(job);

        // Save application
        Application savedApplication = applicationRepository.save(application);
        log.info("Application created successfully with id: {} for job: {} by user: {}", 
                savedApplication.getId(), jobId, userId);

        // Return response with automatically retrieved user information
        return new ApplicationResponse(
                savedApplication.getId(),
                new JobInfo(jobId, job.getTitle(), job.getLocation()),
                userInfo, // Automatically retrieved user information
                savedApplication.getCvFileId(),
                savedApplication.getStatus(),
                savedApplication.isViewedByHr(),
                savedApplication.getCreatedAt()
        );
    }

    /**
     * Retrieves all applications with summary information.
     * This method fetches applications with their associated job information
     * and user details from the user service.
     *
     * @return a list of all applications with summary information
     */
    @Transactional(readOnly = true)
    public List<ApplicationSummaryResponse> getAllApplications() {
        log.debug("Retrieving all applications");
        
        List<Application> applications = applicationRepository.findAll();
        log.info("Retrieved {} applications", applications.size());
        
        return applications.stream()
                .map(this::mapToApplicationSummaryResponse)
                .toList();
    }

    /**
     * Retrieves an application by its ID with full details.
     *
     * @param applicationId the ID of the application to retrieve
     * @return the application response with full details
     * @throws ValidationException if the application ID is invalid
     * @throws NotFoundException if the application is not found
     */
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(String applicationId) {
        log.debug("Retrieving application with id: {}", applicationId);
        
        // Validate application ID
        UuidValidator.validateUuid(applicationId, "Application ID");

        // Find application by ID
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> {
                    log.warn("Application with id {} not found", applicationId);
                    return new NotFoundException("Application not found with id: " + applicationId);
                });
        
        log.info("Retrieved application with id: {}", applicationId);
        return mapToApplicationResponse(application);
    }

    /**
     * Maps an Application entity to an ApplicationSummaryResponse DTO.
     * Uses cached user information to avoid N+1 query issues.
     *
     * @param application the application entity to map
     * @return the application summary response DTO
     */
    private ApplicationSummaryResponse mapToApplicationSummaryResponse(Application application) {
        return new ApplicationSummaryResponse(
                application.getId(),
                new JobInfo(application.getJob().getId(), application.getJob().getTitle(), application.getJob().getLocation()),
                userInfoService.createUserInfo(application.getUserId()),
                application.getStatus().toString()
        );
    }

    /**
     * Maps an Application entity to an ApplicationResponse DTO.
     * Uses cached user information to avoid N+1 query issues.
     *
     * @param application the application entity to map
     * @return the application response DTO
     */
    private ApplicationResponse mapToApplicationResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                new JobInfo(application.getJob().getId(), application.getJob().getTitle(), application.getJob().getLocation()),
                userInfoService.createUserInfo(application.getUserId()),
                application.getCvFileId(),
                application.getStatus(),
                application.isViewedByHr(),
                application.getCreatedAt()
        );
    }

}
