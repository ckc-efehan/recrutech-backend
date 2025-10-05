package com.recrutech.recrutechplatform.application.mapper;

import com.recrutech.recrutechplatform.application.dto.ApplicationResponse;
import com.recrutech.recrutechplatform.application.model.Application;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationMapperTest {

    @Test
    void toResponse_mapsAllFields() {
        Application application = getApplication();

        ApplicationResponse response = ApplicationMapper.toResponse(application);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(response.applicantId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(response.jobPostingId()).isEqualTo("33333333-3333-3333-3333-333333333333");
        assertThat(response.coverLetterPath()).isEqualTo("applicant_coverLetter_123.pdf");
        assertThat(response.resumePath()).isEqualTo("applicant_resume_456.pdf");
        assertThat(response.portfolioPath()).isEqualTo("applicant_portfolio_789.pdf");
        assertThat(response.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(response.submittedAt()).isEqualTo(LocalDateTime.of(2025, 10, 1, 10, 0));
        assertThat(response.reviewedAt()).isEqualTo(LocalDateTime.of(2025, 10, 2, 14, 30));
        assertThat(response.interviewScheduledAt()).isNull();
        assertThat(response.offerExtendedAt()).isNull();
        assertThat(response.finalizedAt()).isNull();
        assertThat(response.hrNotes()).isEqualTo("Good candidate");
        assertThat(response.rejectionReason()).isNull();
        assertThat(response.isDeleted()).isFalse();
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2025, 10, 1, 9, 0));
        assertThat(response.createdByUserId()).isEqualTo("44444444-4444-4444-4444-444444444444");
        assertThat(response.updatedByUserId()).isEqualTo("55555555-5555-5555-5555-555555555555");
        assertThat(response.deletedByUserId()).isNull();
        assertThat(response.deletedAt()).isNull();
    }

    private static @NotNull Application getApplication() {
        Application application = new Application();
        application.setId("11111111-1111-1111-1111-111111111111");
        application.setApplicantId("22222222-2222-2222-2222-222222222222");
        application.setJobPostingId("33333333-3333-3333-3333-333333333333");
        application.setCoverLetterPath("applicant_coverLetter_123.pdf");
        application.setResumePath("applicant_resume_456.pdf");
        application.setPortfolioPath("applicant_portfolio_789.pdf");
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        application.setSubmittedAt(LocalDateTime.of(2025, 10, 1, 10, 0));
        application.setReviewedAt(LocalDateTime.of(2025, 10, 2, 14, 30));
        application.setInterviewScheduledAt(null);
        application.setOfferExtendedAt(null);
        application.setFinalizedAt(null);
        application.setHrNotes("Good candidate");
        application.setRejectionReason(null);
        application.setDeleted(false);
        application.setCreatedAt(LocalDateTime.of(2025, 10, 1, 9, 0));
        application.setCreatedByUserId("44444444-4444-4444-4444-444444444444");
        application.setUpdatedByUserId("55555555-5555-5555-5555-555555555555");
        application.setDeletedByUserId(null);
        application.setDeletedAt(null);
        return application;
    }

    @Test
    void toResponse_handlesNullOptionalFields() {
        Application application = new Application();
        application.setId("11111111-1111-1111-1111-111111111111");
        application.setApplicantId("22222222-2222-2222-2222-222222222222");
        application.setJobPostingId("33333333-3333-3333-3333-333333333333");
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());
        application.setDeleted(false);
        application.setCreatedAt(LocalDateTime.now());
        application.setCreatedByUserId("44444444-4444-4444-4444-444444444444");

        ApplicationResponse response = ApplicationMapper.toResponse(application);

        assertThat(response).isNotNull();
        assertThat(response.coverLetterPath()).isNull();
        assertThat(response.resumePath()).isNull();
        assertThat(response.portfolioPath()).isNull();
        assertThat(response.reviewedAt()).isNull();
        assertThat(response.hrNotes()).isNull();
        assertThat(response.rejectionReason()).isNull();
    }
}
