package com.recrutech.recrutechplatform.interview.mapper;

import com.recrutech.recrutechplatform.interview.dto.InterviewResponse;
import com.recrutech.recrutechplatform.interview.model.Interview;
import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import com.recrutech.recrutechplatform.interview.model.InterviewType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewMapperTest {

    @Test
    void toResponse_mapsAllFields() {
        Interview interview = createFullInterview();

        InterviewResponse response = InterviewMapper.toResponse(interview);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(response.applicationId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(response.interviewType()).isEqualTo(InterviewType.VIDEO);
        assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(response.scheduledAt()).isEqualTo(LocalDateTime.of(2025, 10, 10, 14, 0));
        assertThat(response.durationMinutes()).isEqualTo(60);
        assertThat(response.location()).isEqualTo("Office Room 101");
        assertThat(response.meetingLink()).isEqualTo("https://zoom.us/meeting");
        assertThat(response.description()).isEqualTo("Technical interview");
        assertThat(response.notes()).isEqualTo("Prepare coding questions");
        assertThat(response.interviewerUserId()).isEqualTo("33333333-3333-3333-3333-333333333333");
        assertThat(response.feedback()).isEqualTo("Excellent candidate, strong technical skills");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.completedAt()).isEqualTo(LocalDateTime.of(2025, 10, 10, 15, 30));
        assertThat(response.createdByUserId()).isEqualTo("44444444-4444-4444-4444-444444444444");
        assertThat(response.updatedByUserId()).isEqualTo("55555555-5555-5555-5555-555555555555");
        assertThat(response.deletedByUserId()).isNull();
        assertThat(response.isDeleted()).isFalse();
        assertThat(response.deletedAt()).isNull();
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2025, 10, 1, 9, 0));
    }

    @Test
    void toResponse_handlesNullOptionalFields() {
        Interview interview = new Interview();
        interview.setId("11111111-1111-1111-1111-111111111111");
        interview.setApplicationId("22222222-2222-2222-2222-222222222222");
        interview.setInterviewType(InterviewType.PHONE);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setScheduledAt(LocalDateTime.of(2025, 10, 15, 10, 0));
        interview.setCreatedByUserId("44444444-4444-4444-4444-444444444444");
        interview.setCreatedAt(LocalDateTime.now());
        interview.setDeleted(false);

        InterviewResponse response = InterviewMapper.toResponse(interview);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(response.applicationId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(response.interviewType()).isEqualTo(InterviewType.PHONE);
        assertThat(response.status()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(response.durationMinutes()).isNull();
        assertThat(response.location()).isNull();
        assertThat(response.meetingLink()).isNull();
        assertThat(response.description()).isNull();
        assertThat(response.notes()).isNull();
        assertThat(response.interviewerUserId()).isNull();
        assertThat(response.feedback()).isNull();
        assertThat(response.rating()).isNull();
        assertThat(response.completedAt()).isNull();
        assertThat(response.updatedByUserId()).isNull();
        assertThat(response.deletedByUserId()).isNull();
        assertThat(response.deletedAt()).isNull();
    }

    @Test
    void toResponse_mapsScheduledInterview() {
        Interview interview = new Interview();
        interview.setId("11111111-1111-1111-1111-111111111111");
        interview.setApplicationId("22222222-2222-2222-2222-222222222222");
        interview.setInterviewType(InterviewType.ONSITE);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setScheduledAt(LocalDateTime.of(2025, 10, 20, 9, 0));
        interview.setDurationMinutes(90);
        interview.setLocation("Conference Room A");
        interview.setInterviewerUserId("33333333-3333-3333-3333-333333333333");
        interview.setDescription("Panel interview with team leads");
        interview.setNotes("Bring portfolio");
        interview.setCreatedByUserId("44444444-4444-4444-4444-444444444444");
        interview.setCreatedAt(LocalDateTime.now());
        interview.setDeleted(false);

        InterviewResponse response = InterviewMapper.toResponse(interview);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(response.interviewType()).isEqualTo(InterviewType.ONSITE);
        assertThat(response.location()).isEqualTo("Conference Room A");
        assertThat(response.meetingLink()).isNull();
        assertThat(response.feedback()).isNull();
        assertThat(response.rating()).isNull();
        assertThat(response.completedAt()).isNull();
    }

    @Test
    void toResponse_mapsCompletedInterviewWithFeedback() {
        Interview interview = new Interview();
        interview.setId("11111111-1111-1111-1111-111111111111");
        interview.setApplicationId("22222222-2222-2222-2222-222222222222");
        interview.setInterviewType(InterviewType.VIDEO);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setScheduledAt(LocalDateTime.of(2025, 10, 5, 14, 0));
        interview.setDurationMinutes(45);
        interview.setMeetingLink("https://meet.google.com/abc-defg-hij");
        interview.setInterviewerUserId("33333333-3333-3333-3333-333333333333");
        interview.setFeedback("Strong communication skills, needs more technical depth");
        interview.setRating(3);
        interview.setCompletedAt(LocalDateTime.of(2025, 10, 5, 14, 45));
        interview.setCreatedByUserId("44444444-4444-4444-4444-444444444444");
        interview.setUpdatedByUserId("33333333-3333-3333-3333-333333333333");
        interview.setCreatedAt(LocalDateTime.now());
        interview.setDeleted(false);

        InterviewResponse response = InterviewMapper.toResponse(interview);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(response.feedback()).isEqualTo("Strong communication skills, needs more technical depth");
        assertThat(response.rating()).isEqualTo(3);
        assertThat(response.completedAt()).isNotNull();
        assertThat(response.updatedByUserId()).isEqualTo("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void toResponse_mapsCancelledInterview() {
        Interview interview = new Interview();
        interview.setId("11111111-1111-1111-1111-111111111111");
        interview.setApplicationId("22222222-2222-2222-2222-222222222222");
        interview.setInterviewType(InterviewType.PHONE);
        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setScheduledAt(LocalDateTime.of(2025, 10, 12, 11, 0));
        interview.setCreatedByUserId("44444444-4444-4444-4444-444444444444");
        interview.setCreatedAt(LocalDateTime.now());
        interview.setDeleted(true);
        interview.setDeletedAt(LocalDateTime.of(2025, 10, 10, 16, 0));
        interview.setDeletedByUserId("44444444-4444-4444-4444-444444444444");

        InterviewResponse response = InterviewMapper.toResponse(interview);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(response.isDeleted()).isTrue();
        assertThat(response.deletedAt()).isNotNull();
        assertThat(response.deletedByUserId()).isEqualTo("44444444-4444-4444-4444-444444444444");
    }

    @Test
    void toResponse_mapsNoShowInterview() {
        Interview interview = new Interview();
        interview.setId("11111111-1111-1111-1111-111111111111");
        interview.setApplicationId("22222222-2222-2222-2222-222222222222");
        interview.setInterviewType(InterviewType.VIDEO);
        interview.setStatus(InterviewStatus.NO_SHOW);
        interview.setScheduledAt(LocalDateTime.of(2025, 10, 8, 10, 0));
        interview.setDurationMinutes(60);
        interview.setMeetingLink("https://zoom.us/meeting");
        interview.setInterviewerUserId("33333333-3333-3333-3333-333333333333");
        interview.setCompletedAt(LocalDateTime.of(2025, 10, 8, 10, 15));
        interview.setCreatedByUserId("44444444-4444-4444-4444-444444444444");
        interview.setUpdatedByUserId("33333333-3333-3333-3333-333333333333");
        interview.setCreatedAt(LocalDateTime.now());
        interview.setDeleted(false);

        InterviewResponse response = InterviewMapper.toResponse(interview);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(InterviewStatus.NO_SHOW);
        assertThat(response.completedAt()).isNotNull();
        assertThat(response.feedback()).isNull();
        assertThat(response.rating()).isNull();
    }

    private Interview createFullInterview() {
        Interview interview = new Interview();
        interview.setId("11111111-1111-1111-1111-111111111111");
        interview.setApplicationId("22222222-2222-2222-2222-222222222222");
        interview.setInterviewType(InterviewType.VIDEO);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setScheduledAt(LocalDateTime.of(2025, 10, 10, 14, 0));
        interview.setDurationMinutes(60);
        interview.setLocation("Office Room 101");
        interview.setMeetingLink("https://zoom.us/meeting");
        interview.setDescription("Technical interview");
        interview.setNotes("Prepare coding questions");
        interview.setInterviewerUserId("33333333-3333-3333-3333-333333333333");
        interview.setFeedback("Excellent candidate, strong technical skills");
        interview.setRating(5);
        interview.setCompletedAt(LocalDateTime.of(2025, 10, 10, 15, 30));
        interview.setCreatedByUserId("44444444-4444-4444-4444-444444444444");
        interview.setUpdatedByUserId("55555555-5555-5555-5555-555555555555");
        interview.setCreatedAt(LocalDateTime.of(2025, 10, 1, 9, 0));
        interview.setDeleted(false);
        return interview;
    }
}
