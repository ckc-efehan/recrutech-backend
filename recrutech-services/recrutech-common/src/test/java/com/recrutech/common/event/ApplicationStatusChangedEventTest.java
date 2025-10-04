package com.recrutech.common.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusChangedEventTest {

    @Test
    void constructor_initializesAllFields() {
        String applicationId = "11111111-1111-1111-1111-111111111111";
        String applicantId = "22222222-2222-2222-2222-222222222222";
        String jobPostingId = "33333333-3333-3333-3333-333333333333";
        String companyId = "44444444-4444-4444-4444-444444444444";
        String previousStatus = "SUBMITTED";
        String newStatus = "UNDER_REVIEW";
        String updatedByUserId = "55555555-5555-5555-5555-555555555555";

        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                applicationId,
                applicantId,
                jobPostingId,
                companyId,
                previousStatus,
                newStatus,
                updatedByUserId
        );

        assertThat(event).isNotNull();
        assertThat(event.getApplicationId()).isEqualTo(applicationId);
        assertThat(event.getApplicantId()).isEqualTo(applicantId);
        assertThat(event.getJobPostingId()).isEqualTo(jobPostingId);
        assertThat(event.getCompanyId()).isEqualTo(companyId);
        assertThat(event.getPreviousStatus()).isEqualTo(previousStatus);
        assertThat(event.getNewStatus()).isEqualTo(newStatus);
        assertThat(event.getUpdatedByUserId()).isEqualTo(updatedByUserId);
        assertThat(event.getEventType()).isEqualTo("APPLICATION_STATUS_CHANGED");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void toString_containsAllFields() {
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                "app-id",
                "applicant-id",
                "job-id",
                "company-id",
                "SUBMITTED",
                "UNDER_REVIEW",
                "user-id"
        );

        String result = event.toString();

        assertThat(result).contains("ApplicationStatusChangedEvent");
        assertThat(result).contains("app-id");
        assertThat(result).contains("applicant-id");
        assertThat(result).contains("job-id");
        assertThat(result).contains("company-id");
        assertThat(result).contains("SUBMITTED");
        assertThat(result).contains("UNDER_REVIEW");
        assertThat(result).contains("user-id");
    }

    @Test
    void eventId_isUnique() {
        ApplicationStatusChangedEvent event1 = new ApplicationStatusChangedEvent(
                "app-id-1", "applicant-id", "job-id", "company-id",
                "SUBMITTED", "UNDER_REVIEW", "user-id"
        );
        ApplicationStatusChangedEvent event2 = new ApplicationStatusChangedEvent(
                "app-id-2", "applicant-id", "job-id", "company-id",
                "SUBMITTED", "UNDER_REVIEW", "user-id"
        );

        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }
}
