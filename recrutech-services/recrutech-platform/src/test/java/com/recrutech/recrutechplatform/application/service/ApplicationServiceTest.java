package com.recrutech.recrutechplatform.application.service;

import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.recrutechplatform.application.model.Application;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import com.recrutech.recrutechplatform.application.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository repository;

    @InjectMocks
    private ApplicationService service;

    private String applicantId;
    private String jobPostingId;
    private String userId;
    private String applicationId;

    @BeforeEach
    void setUp() {
        applicantId = "11111111-1111-1111-1111-111111111111";
        jobPostingId = "22222222-2222-2222-2222-222222222222";
        userId = "33333333-3333-3333-3333-333333333333";
        applicationId = "44444444-4444-4444-4444-444444444444";
    }

    // ========== Submit Tests ==========

    @Test
    void submit_success() {
        when(repository.existsByApplicantIdAndJobPostingIdAndIsDeletedFalse(applicantId, jobPostingId))
                .thenReturn(false);
        when(repository.save(any(Application.class))).thenAnswer(invocation -> {
            Application app = invocation.getArgument(0);
            app.setId(applicationId);
            app.setCreatedAt(LocalDateTime.now());
            return app;
        });

        Application result = service.submit(
                applicantId,
                jobPostingId,
                userId,
                "I am interested in this position",
                "https://resume.url",
                "https://portfolio.url"
        );

        assertThat(result).isNotNull();
        assertThat(result.getApplicantId()).isEqualTo(applicantId);
        assertThat(result.getJobPostingId()).isEqualTo(jobPostingId);
        assertThat(result.getCreatedByUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
        assertThat(result.getCoverLetter()).isEqualTo("I am interested in this position");
        assertThat(result.getResumeUrl()).isEqualTo("https://resume.url");
        assertThat(result.getPortfolioUrl()).isEqualTo("https://portfolio.url");

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(repository).save(captor.capture());
        Application saved = captor.getValue();
        assertThat(saved.getApplicantId()).isEqualTo(applicantId);
        assertThat(saved.getJobPostingId()).isEqualTo(jobPostingId);
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void submit_throwsValidationException_whenDuplicateApplication() {
        when(repository.existsByApplicantIdAndJobPostingIdAndIsDeletedFalse(applicantId, jobPostingId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submit(
                applicantId,
                jobPostingId,
                userId,
                "Cover letter",
                "https://resume.url",
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("You have already applied to this job posting");

        verify(repository, never()).save(any());
    }

    @Test
    void submit_throwsValidationException_whenInvalidApplicantId() {
        assertThatThrownBy(() -> service.submit(
                "invalid-uuid",
                jobPostingId,
                userId,
                "Cover letter",
                null,
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("applicantId");
    }

    @Test
    void submit_throwsValidationException_whenInvalidJobPostingId() {
        assertThatThrownBy(() -> service.submit(
                applicantId,
                "invalid-uuid",
                userId,
                "Cover letter",
                null,
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("jobPostingId");
    }

    @Test
    void submit_throwsValidationException_whenInvalidUserId() {
        assertThatThrownBy(() -> service.submit(
                applicantId,
                jobPostingId,
                "invalid-uuid",
                "Cover letter",
                null,
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("userId");
    }

    // ========== Get By ID Tests ==========

    @Test
    void getById_success() {
        Application application = createApplication();
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        Application result = service.getById(applicationId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(applicationId);
        verify(repository).findByIdAndIsDeletedFalse(applicationId);
    }

    @Test
    void getById_throwsNotFoundException_whenNotFound() {
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(applicationId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void getById_throwsValidationException_whenInvalidId() {
        assertThatThrownBy(() -> service.getById("invalid-uuid"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("id");
    }

    // ========== Get By Applicant Tests ==========

    @Test
    void getByApplicant_success_withoutStatusFilter() {
        Application app = createApplication();
        Page<Application> page = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAllByApplicantIdAndIsDeletedFalse(applicantId, pageable))
                .thenReturn(page);

        Page<Application> result = service.getByApplicant(applicantId, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getApplicantId()).isEqualTo(applicantId);
        verify(repository).findAllByApplicantIdAndIsDeletedFalse(applicantId, pageable);
    }

    @Test
    void getByApplicant_success_withStatusFilter() {
        Application app = createApplication();
        Page<Application> page = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAllByApplicantIdAndStatusAndIsDeletedFalse(
                applicantId, ApplicationStatus.SUBMITTED, pageable))
                .thenReturn(page);

        Page<Application> result = service.getByApplicant(
                applicantId, ApplicationStatus.SUBMITTED, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAllByApplicantIdAndStatusAndIsDeletedFalse(
                applicantId, ApplicationStatus.SUBMITTED, pageable);
    }

    // ========== Get By Job Posting Tests ==========

    @Test
    void getByJobPosting_success_withoutStatusFilter() {
        Application app = createApplication();
        Page<Application> page = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAllByJobPostingIdAndIsDeletedFalse(jobPostingId, pageable))
                .thenReturn(page);

        Page<Application> result = service.getByJobPosting(jobPostingId, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAllByJobPostingIdAndIsDeletedFalse(jobPostingId, pageable);
    }

    @Test
    void getByJobPosting_success_withStatusFilter() {
        Application app = createApplication();
        Page<Application> page = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAllByJobPostingIdAndStatusAndIsDeletedFalse(
                jobPostingId, ApplicationStatus.UNDER_REVIEW, pageable))
                .thenReturn(page);

        Page<Application> result = service.getByJobPosting(
                jobPostingId, ApplicationStatus.UNDER_REVIEW, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAllByJobPostingIdAndStatusAndIsDeletedFalse(
                jobPostingId, ApplicationStatus.UNDER_REVIEW, pageable);
    }

    // ========== Get By Company Tests ==========

    @Test
    void getByCompany_success_withoutStatusFilter() {
        String companyId = "55555555-5555-5555-5555-555555555555";
        Application app = createApplication();
        Page<Application> page = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAllByCompanyId(companyId, pageable)).thenReturn(page);

        Page<Application> result = service.getByCompany(companyId, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAllByCompanyId(companyId, pageable);
    }

    @Test
    void getByCompany_success_withStatusFilter() {
        String companyId = "55555555-5555-5555-5555-555555555555";
        Application app = createApplication();
        Page<Application> page = new PageImpl<>(List.of(app));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAllByCompanyIdAndStatus(
                companyId, ApplicationStatus.INTERVIEWED, pageable))
                .thenReturn(page);

        Page<Application> result = service.getByCompany(
                companyId, ApplicationStatus.INTERVIEWED, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAllByCompanyIdAndStatus(
                companyId, ApplicationStatus.INTERVIEWED, pageable);
    }

    // ========== Update Status Tests ==========

    @Test
    void updateStatus_success_submittedToUnderReview() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.updateStatus(
                applicationId,
                ApplicationStatus.UNDER_REVIEW,
                userId,
                "Initial review in progress",
                null
        );

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(result.getReviewedAt()).isNotNull();
        assertThat(result.getUpdatedByUserId()).isEqualTo(userId);
        assertThat(result.getHrNotes()).isEqualTo("Initial review in progress");
        verify(repository).save(application);
    }

    @Test
    void updateStatus_success_underReviewToInterviewScheduled() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.updateStatus(
                applicationId,
                ApplicationStatus.INTERVIEW_SCHEDULED,
                userId,
                null,
                null
        );

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);
        assertThat(result.getInterviewScheduledAt()).isNotNull();
        verify(repository).save(application);
    }

    @Test
    void updateStatus_success_interviewScheduledToInterviewed() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.updateStatus(
                applicationId,
                ApplicationStatus.INTERVIEWED,
                userId,
                null,
                null
        );

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.INTERVIEWED);
        verify(repository).save(application);
    }

    @Test
    void updateStatus_success_interviewedToOfferExtended() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.INTERVIEWED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.updateStatus(
                applicationId,
                ApplicationStatus.OFFER_EXTENDED,
                userId,
                null,
                null
        );

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.OFFER_EXTENDED);
        assertThat(result.getOfferExtendedAt()).isNotNull();
        verify(repository).save(application);
    }

    @Test
    void updateStatus_success_offerExtendedToAccepted() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.OFFER_EXTENDED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.updateStatus(
                applicationId,
                ApplicationStatus.ACCEPTED,
                userId,
                null,
                null
        );

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(result.getFinalizedAt()).isNotNull();
        verify(repository).save(application);
    }

    @Test
    void updateStatus_success_submittedToRejected() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.updateStatus(
                applicationId,
                ApplicationStatus.REJECTED,
                userId,
                null,
                "Does not meet minimum requirements"
        );

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(result.getFinalizedAt()).isNotNull();
        assertThat(result.getRejectionReason()).isEqualTo("Does not meet minimum requirements");
        verify(repository).save(application);
    }

    @Test
    void updateStatus_idempotent_whenSameStatus() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.updateStatus(
                applicationId,
                ApplicationStatus.UNDER_REVIEW,
                userId,
                null,
                null
        );

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        verify(repository).save(application);
    }

    @Test
    void updateStatus_throwsValidationException_whenInvalidTransition() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateStatus(
                applicationId,
                ApplicationStatus.OFFER_EXTENDED,
                userId,
                null,
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid status transition from SUBMITTED to OFFER_EXTENDED");

        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_throwsValidationException_whenApplicationFinalized() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.ACCEPTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateStatus(
                applicationId,
                ApplicationStatus.REJECTED,
                userId,
                null,
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot modify an application that has been finalized");

        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_throwsValidationException_whenApplicationRejected() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.REJECTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateStatus(
                applicationId,
                ApplicationStatus.UNDER_REVIEW,
                userId,
                null,
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot modify an application that has been finalized");

        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_throwsValidationException_whenApplicationWithdrawn() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.WITHDRAWN);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateStatus(
                applicationId,
                ApplicationStatus.UNDER_REVIEW,
                userId,
                null,
                null
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot modify an application that has been finalized");

        verify(repository, never()).save(any());
    }

    // ========== Withdraw Tests ==========

    @Test
    void withdraw_success() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = service.withdraw(applicationId, applicantId, userId);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(result.getFinalizedAt()).isNotNull();
        assertThat(result.getUpdatedByUserId()).isEqualTo(userId);
        verify(repository).save(application);
    }

    @Test
    void withdraw_throwsValidationException_whenNotOwner() {
        String differentApplicantId = "99999999-9999-9999-9999-999999999999";
        Application application = createApplication();
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.withdraw(applicationId, differentApplicantId, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("You can only withdraw your own applications");

        verify(repository, never()).save(any());
    }

    @Test
    void withdraw_throwsValidationException_whenAlreadyFinalized() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.ACCEPTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.withdraw(applicationId, applicantId, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot modify an application that has been finalized");

        verify(repository, never()).save(any());
    }

    // ========== Soft Delete Tests ==========

    @Test
    void softDelete_success() {
        Application application = createApplication();
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        service.softDelete(applicationId, userId);

        assertThat(application.isDeleted()).isTrue();
        assertThat(application.getDeletedAt()).isNotNull();
        assertThat(application.getDeletedByUserId()).isEqualTo(userId);
        verify(repository).save(application);
    }

    @Test
    void softDelete_throwsNotFoundException_whenNotFound() {
        when(repository.findByIdAndIsDeletedFalse(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(applicationId, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(repository, never()).save(any());
    }

    // ========== Helper Methods ==========

    private Application createApplication() {
        Application application = new Application();
        application.setId(applicationId);
        application.setApplicantId(applicantId);
        application.setJobPostingId(jobPostingId);
        application.setCreatedByUserId(userId);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());
        application.setCoverLetter("Cover letter");
        application.setResumeUrl("https://resume.url");
        application.setCreatedAt(LocalDateTime.now());
        return application;
    }
}
