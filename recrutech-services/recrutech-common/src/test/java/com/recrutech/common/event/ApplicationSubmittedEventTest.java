package com.recrutech.common.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationSubmittedEventTest {

    @Test
    void constructor_initializesAllFields() {
        String applicationId = "11111111-1111-1111-1111-111111111111";
        String applicantId = "22222222-2222-2222-2222-222222222222";
        String jobPostingId = "33333333-3333-3333-3333-333333333333";
        String companyId = "44444444-4444-4444-4444-444444444444";

        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                applicationId,
                applicantId,
                jobPostingId,
                companyId
        );

        assertThat(event).isNotNull();
        assertThat(event.getApplicationId()).isEqualTo(applicationId);
        assertThat(event.getApplicantId()).isEqualTo(applicantId);
        assertThat(event.getJobPostingId()).isEqualTo(jobPostingId);
        assertThat(event.getCompanyId()).isEqualTo(companyId);
        assertThat(event.getEventType()).isEqualTo("APPLICATION_SUBMITTED");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void toString_containsAllFields() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                "app-id",
                "applicant-id",
                "job-id",
                "company-id"
        );

        String result = event.toString();

        assertThat(result).contains("ApplicationSubmittedEvent");
        assertThat(result).contains("app-id");
        assertThat(result).contains("applicant-id");
        assertThat(result).contains("job-id");
        assertThat(result).contains("company-id");
    }

    @Test
    void eventId_isUnique() {
        ApplicationSubmittedEvent event1 = new ApplicationSubmittedEvent(
                "app-id-1", "applicant-id", "job-id", "company-id"
        );
        ApplicationSubmittedEvent event2 = new ApplicationSubmittedEvent(
                "app-id-2", "applicant-id", "job-id", "company-id"
        );

        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }
}
