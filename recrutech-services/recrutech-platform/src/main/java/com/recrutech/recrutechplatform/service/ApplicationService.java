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
     * Retrieves all applications for a specific job with summary information.
     * This method fetches applications with their associated job information
     * and user details from the user service.
     *
     * @param jobId the ID of the job to filter applications by
     * @return a list of applications for the specified job with summary information
     * @throws ValidationException if the job ID is invalid
     * @throws NotFoundException if the job is not found
     */
    @Transactional(readOnly = true)
    public List<ApplicationSummaryResponse> getAllApplications(String jobId) {
        log.debug("Retrieving all applications for job: {}", jobId);
        
        // Validate job ID
        UuidValidator.validateUuid(jobId, "Job ID");
        
        // Verify job exists
        findJobByIdOrThrow(jobId, "retrieving applications");
        
        List<Application> applications = applicationRepository.findByJob_Id(jobId);
        log.info("Retrieved {} applications for job: {}", applications.size(), jobId);
        
        return applications.stream()
                .map(this::mapToApplicationSummaryResponse)
                .toList();
    }

    /**
     * Retrieves an application by its ID and job ID with full details.
     * This ensures the application belongs to the specified job.
     *
     * @param applicationId the ID of the application to retrieve
     * @param jobId the ID of the job the application should belong to
     * @return the application response with full details
     * @throws ValidationException if the application ID or job ID is invalid
     * @throws NotFoundException if the application is not found or doesn't belong to the specified job
     */
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(String applicationId, String jobId) {
        log.debug("Retrieving application with id: {} for job: {}", applicationId, jobId);
        
        // Validate application ID and job ID
        UuidValidator.validateUuid(applicationId, "Application ID");
        UuidValidator.validateUuid(jobId, "Job ID");
        
        // Verify job exists
        findJobByIdOrThrow(jobId, "retrieving application");

        // Find application by ID and job ID
        Application application = applicationRepository.findByIdAndJob_Id(applicationId, jobId)
                .orElseThrow(() -> {
                    log.warn("Application with id {} not found for job {}", applicationId, jobId);
                    return new NotFoundException("Application not found with id: " + applicationId + " for job: " + jobId);
                });
        
        log.info("Retrieved application with id: {} for job: {}", applicationId, jobId);
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
