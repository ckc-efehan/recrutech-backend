package com.recrutech.recrutechplatform.service;

import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.util.UuidValidator;
import com.recrutech.recrutechplatform.dto.application.ApplicationRequest;
import com.recrutech.recrutechplatform.dto.application.ApplicationResponse;
import com.recrutech.recrutechplatform.dto.application.ApplicationSummaryResponse;
import com.recrutech.recrutechplatform.dto.application.JobInfo;
import com.recrutech.recrutechplatform.dto.application.UserInfo;
import com.recrutech.recrutechplatform.enums.ApplicationStatus;
import com.recrutech.recrutechplatform.model.Application;
import com.recrutech.recrutechplatform.model.Job;
import com.recrutech.recrutechplatform.repository.ApplicationRepository;
import com.recrutech.recrutechplatform.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public ApplicationService(ApplicationRepository applicationRepository, JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public ApplicationResponse createApplication(String jobId, ApplicationRequest request) {
        // Verify job exists
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found with id: " + jobId));

        // Validate cvFileId is a valid UUID
        String cvFileId = request.cvFileId();
        UuidValidator.validateUuid(cvFileId, "CV File ID");

        // Create new application
        Application application = new Application();
        application.setCvFileId(cvFileId);
        application.setUserId(request.userId());
        application.setStatus(ApplicationStatus.RECEIVED);
        application.setViewedByHr(false);
        application.setJob(job);

        // Save application
        Application savedApplication = applicationRepository.save(application);

        // Return response
        return new ApplicationResponse(
                savedApplication.getId(),
                new JobInfo(jobId, job.getTitle(), job.getLocation()),
                new UserInfo(request.userId(), request.firstName(), request.lastName()),
                savedApplication.getCvFileId(),
                savedApplication.getStatus(),
                savedApplication.isViewedByHr(),
                savedApplication.getCreatedAt()
        );
    }

    public List<ApplicationSummaryResponse> getAllApplications() {
        List<Application> applications = applicationRepository.findAll();
        return applications.stream()
                .map(app -> new ApplicationSummaryResponse(
                        app.getId(),
                        new JobInfo(app.getJob().getId(), app.getJob().getTitle(), app.getJob().getLocation()),
                        new UserInfo(app.getUserId(), getUserFirstName(app.getUserId()), getUserLastName(app.getUserId())),
                        app.getStatus().toString()
                ))
                .toList();
    }

    public ApplicationResponse getApplicationById(String applicationId) {
        // Validate application ID
        UuidValidator.validateUuid(applicationId, "Application ID");

        // Find application by ID
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found with id: " + applicationId));
        // Return response
        return new ApplicationResponse(
                application.getId(),
                new JobInfo(application.getJob().getId(), application.getJob().getTitle(), application.getJob().getLocation()),
                new UserInfo(application.getUserId(), getUserFirstName(application.getUserId()), getUserLastName(application.getUserId())),
                application.getCvFileId(),
                application.getStatus(),
                application.isViewedByHr(),
                application.getCreatedAt()
        );
    }

    // Helper methods to fetch user information
    // TODO: Implement proper user service integration
    private String getUserFirstName(String userId) {
        if (userId == null) return "Unknown";
        // Placeholder - in real implementation, call auth service to get user info
        return "FirstName";
    }

    private String getUserLastName(String userId) {
        if (userId == null) return "Unknown";
        // Placeholder - in real implementation, call auth service to get user info
        return "LastName";
    }
}
