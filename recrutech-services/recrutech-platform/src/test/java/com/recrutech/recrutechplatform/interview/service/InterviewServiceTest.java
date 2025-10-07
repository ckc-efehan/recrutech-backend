package com.recrutech.recrutechplatform.interview.service;

import com.recrutech.common.exception.EntityReferenceNotFoundException;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import com.recrutech.recrutechplatform.application.service.ApplicationService;
import com.recrutech.recrutechplatform.interview.dto.InterviewCreateRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewFeedbackRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewResponse;
import com.recrutech.recrutechplatform.interview.dto.InterviewUpdateRequest;
import com.recrutech.recrutechplatform.interview.model.Interview;
import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import com.recrutech.recrutechplatform.interview.model.InterviewType;
import com.recrutech.recrutechplatform.interview.repository.InterviewRepository;
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
class InterviewServiceTest {

    @Mock
    private InterviewRepository repository;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private InterviewService service;

    private String applicationId;
    private String interviewId;
    private String userId;
    private String interviewerId;
    private Interview interview;
    private LocalDateTime futureDateTime;

    @BeforeEach
    void setUp() {
        applicationId = "11111111-1111-1111-1111-111111111111";
        interviewId = "22222222-2222-2222-2222-222222222222";
        userId = "33333333-3333-3333-3333-333333333333";
        interviewerId = "44444444-4444-4444-4444-444444444444";
        futureDateTime = LocalDateTime.now().plusDays(1);

        // Setup default interview entity
        interview = new Interview();
        interview.setId(interviewId);
        interview.setApplicationId(applicationId);
        interview.setScheduledAt(futureDateTime);
        interview.setDurationMinutes(60);
        interview.setInterviewType(InterviewType.VIDEO);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setInterviewerUserId(interviewerId);
        interview.setLocation("Office");
        interview.setMeetingLink("https://zoom.us/meeting");
        interview.setDescription("Technical interview");
        interview.setNotes("Prepare coding questions");
        interview.setCreatedByUserId(userId);
        interview.setCreatedAt(LocalDateTime.now());
    }

    // ========== Schedule Interview Tests ==========

    @Test
    void scheduleInterview_success() {
        InterviewCreateRequest request = new InterviewCreateRequest(
                applicationId,
                futureDateTime,
                60,
                InterviewType.VIDEO,
                "Office",
                "https://zoom.us/meeting",
                interviewerId,
                "Technical interview",
                "Prepare coding questions"
        );

        when(repository.applicationExists(applicationId)).thenReturn(true);
        when(repository.userExists(interviewerId)).thenReturn(true);
        when(repository.save(any(Interview.class))).thenAnswer(invocation -> {
            Interview saved = invocation.getArgument(0);
            saved.setId(interviewId);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        InterviewResponse result = service.scheduleInterview(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(interviewId);
        assertThat(result.applicationId()).isEqualTo(applicationId);
        assertThat(result.status()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(result.interviewType()).isEqualTo(InterviewType.VIDEO);
        assertThat(result.interviewerUserId()).isEqualTo(interviewerId);

        // Verify application status was updated
        verify(applicationService).updateStatus(
                eq(applicationId),
                eq(ApplicationStatus.INTERVIEW_SCHEDULED),
                eq(userId),
                isNull(),
                isNull()
        );

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(repository).save(captor.capture());
        Interview saved = captor.getValue();
        assertThat(saved.getApplicationId()).isEqualTo(applicationId);
        assertThat(saved.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(saved.getCreatedByUserId()).isEqualTo(userId);
    }

    @Test
    void scheduleInterview_throwsEntityReferenceNotFoundException_whenApplicationNotFound() {
        InterviewCreateRequest request = new InterviewCreateRequest(
                applicationId,
                futureDateTime,
                60,
                InterviewType.VIDEO,
                null,
                "https://zoom.us/meeting",
                null,
                null,
                null
        );

        when(repository.applicationExists(applicationId)).thenReturn(false);

        assertThatThrownBy(() -> service.scheduleInterview(request, userId))
                .isInstanceOf(EntityReferenceNotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(repository, never()).save(any());
        verify(applicationService, never()).updateStatus(any(), any(), any(), any(), any());
    }

    @Test
    void scheduleInterview_throwsEntityReferenceNotFoundException_whenInterviewerNotFound() {
        InterviewCreateRequest request = new InterviewCreateRequest(
                applicationId,
                futureDateTime,
                60,
                InterviewType.ONSITE,
                "Office Room 101",
                null,
                interviewerId,
                null,
                null
        );

        when(repository.applicationExists(applicationId)).thenReturn(true);
        when(repository.userExists(interviewerId)).thenReturn(false);

        assertThatThrownBy(() -> service.scheduleInterview(request, userId))
                .isInstanceOf(EntityReferenceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(repository, never()).save(any());
    }

    @Test
    void scheduleInterview_throwsValidationException_whenInvalidApplicationId() {
        InterviewCreateRequest request = new InterviewCreateRequest(
                "invalid-uuid",
                futureDateTime,
                60,
                InterviewType.PHONE,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.scheduleInterview(request, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid UUID format");

        verify(repository, never()).save(any());
    }

    @Test
    void scheduleInterview_success_withoutInterviewer() {
        InterviewCreateRequest request = new InterviewCreateRequest(
                applicationId,
                futureDateTime,
                30,
                InterviewType.PHONE,
                null,
                null,
                null,
                "Phone screening",
                null
        );

        when(repository.applicationExists(applicationId)).thenReturn(true);
        when(repository.save(any(Interview.class))).thenAnswer(invocation -> {
            Interview saved = invocation.getArgument(0);
            saved.setId(interviewId);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        InterviewResponse result = service.scheduleInterview(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.interviewerUserId()).isNull();
        assertThat(result.interviewType()).isEqualTo(InterviewType.PHONE);

        verify(repository).save(any(Interview.class));
        verify(applicationService).updateStatus(any(), any(), any(), any(), any());
    }

    // ========== Update Interview Tests ==========

    @Test
    void updateInterview_success() {
        InterviewUpdateRequest request = new InterviewUpdateRequest(
                futureDateTime.plusHours(2),
                90,
                InterviewType.ONSITE,
                "Conference Room A",
                null,
                interviewerId,
                "Updated description",
                "Updated notes"
        );

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));
        when(repository.userExists(interviewerId)).thenReturn(true);
        when(repository.save(any(Interview.class))).thenReturn(interview);

        InterviewResponse result = service.updateInterview(interviewId, request, userId);

        assertThat(result).isNotNull();
        verify(repository).save(any(Interview.class));

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(repository).save(captor.capture());
        Interview updated = captor.getValue();
        assertThat(updated.getUpdatedByUserId()).isEqualTo(userId);
        assertThat(updated.getDurationMinutes()).isEqualTo(90);
        assertThat(updated.getInterviewType()).isEqualTo(InterviewType.ONSITE);
    }

    @Test
    void updateInterview_throwsNotFoundException_whenInterviewNotFound() {
        InterviewUpdateRequest request = new InterviewUpdateRequest(
                futureDateTime,
                60,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateInterview(interviewId, request, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Interview not found");

        verify(repository, never()).save(any());
    }

    @Test
    void updateInterview_throwsValidationException_whenInterviewNotScheduled() {
        interview.setStatus(InterviewStatus.COMPLETED);
        InterviewUpdateRequest request = new InterviewUpdateRequest(
                futureDateTime,
                60,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.updateInterview(interviewId, request, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only scheduled interviews can be updated");

        verify(repository, never()).save(any());
    }

    @Test
    void updateInterview_throwsValidationException_whenInvalidInterviewerId() {
        InterviewUpdateRequest request = new InterviewUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                "invalid-uuid",
                null,
                null
        );

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.updateInterview(interviewId, request, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid UUID format");

        verify(repository, never()).save(any());
    }

    // ========== Cancel Interview Tests ==========

    @Test
    void cancelInterview_success() {
        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));
        when(repository.save(any(Interview.class))).thenReturn(interview);

        service.cancelInterview(interviewId, userId);

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(repository).save(captor.capture());
        Interview cancelled = captor.getValue();
        assertThat(cancelled.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(cancelled.isDeleted()).isTrue();
        assertThat(cancelled.getDeletedAt()).isNotNull();
        assertThat(cancelled.getDeletedByUserId()).isEqualTo(userId);
    }

    @Test
    void cancelInterview_throwsNotFoundException_whenInterviewNotFound() {
        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelInterview(interviewId, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Interview not found");

        verify(repository, never()).save(any());
    }

    // ========== Get Interview Tests ==========

    @Test
    void getInterview_success() {
        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));

        InterviewResponse result = service.getInterview(interviewId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(interviewId);
        assertThat(result.applicationId()).isEqualTo(applicationId);
    }

    @Test
    void getInterview_throwsNotFoundException_whenInterviewNotFound() {
        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInterview(interviewId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Interview not found");
    }

    @Test
    void getInterview_throwsValidationException_whenInvalidId() {
        assertThatThrownBy(() -> service.getInterview("invalid-uuid"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid UUID format");
    }

    // ========== Get Interviews By Application Tests ==========

    @Test
    void getInterviewsByApplication_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Interview> page = new PageImpl<>(List.of(interview));

        when(repository.findAllByApplicationIdAndIsDeletedFalse(applicationId, pageable))
                .thenReturn(page);

        Page<InterviewResponse> result = service.getInterviewsByApplication(applicationId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).applicationId()).isEqualTo(applicationId);
    }

    // ========== Get Interviews By Interviewer Tests ==========

    @Test
    void getInterviewsByInterviewer_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Interview> page = new PageImpl<>(List.of(interview));

        when(repository.findAllByInterviewerUserIdAndIsDeletedFalse(interviewerId, pageable))
                .thenReturn(page);

        Page<InterviewResponse> result = service.getInterviewsByInterviewer(interviewerId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).interviewerUserId()).isEqualTo(interviewerId);
    }

    // ========== Mark As Completed Tests ==========

    @Test
    void markAsCompleted_success() {
        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));
        when(repository.save(any(Interview.class))).thenReturn(interview);

        InterviewResponse result = service.markAsCompleted(interviewId, userId);

        assertThat(result).isNotNull();

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(repository).save(captor.capture());
        Interview completed = captor.getValue();
        assertThat(completed.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getUpdatedByUserId()).isEqualTo(userId);

        // Verify application status was updated
        verify(applicationService).updateStatus(
                eq(applicationId),
                eq(ApplicationStatus.INTERVIEWED),
                eq(userId),
                isNull(),
                isNull()
        );
    }

    @Test
    void markAsCompleted_throwsValidationException_whenInterviewNotScheduled() {
        interview.setStatus(InterviewStatus.CANCELLED);

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.markAsCompleted(interviewId, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only scheduled interviews can be marked as completed");

        verify(repository, never()).save(any());
        verify(applicationService, never()).updateStatus(any(), any(), any(), any(), any());
    }

    // ========== Mark As No Show Tests ==========

    @Test
    void markAsNoShow_success() {
        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));
        when(repository.save(any(Interview.class))).thenReturn(interview);

        InterviewResponse result = service.markAsNoShow(interviewId, userId);

        assertThat(result).isNotNull();

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(repository).save(captor.capture());
        Interview noShow = captor.getValue();
        assertThat(noShow.getStatus()).isEqualTo(InterviewStatus.NO_SHOW);
        assertThat(noShow.getCompletedAt()).isNotNull();
        assertThat(noShow.getUpdatedByUserId()).isEqualTo(userId);

        // Verify application status was updated to REJECTED
        verify(applicationService).updateStatus(
                eq(applicationId),
                eq(ApplicationStatus.REJECTED),
                eq(userId),
                anyString(),
                eq("No-show for interview")
        );
    }

    @Test
    void markAsNoShow_throwsValidationException_whenInterviewNotScheduled() {
        interview.setStatus(InterviewStatus.COMPLETED);

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.markAsNoShow(interviewId, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only scheduled interviews can be marked as no-show");

        verify(repository, never()).save(any());
    }

    // ========== Add Feedback Tests ==========

    @Test
    void addFeedback_success() {
        interview.setStatus(InterviewStatus.COMPLETED);
        InterviewFeedbackRequest request = new InterviewFeedbackRequest(
                "Excellent candidate, strong technical skills",
                5
        );

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));
        when(repository.save(any(Interview.class))).thenReturn(interview);

        InterviewResponse result = service.addFeedback(interviewId, request, userId);

        assertThat(result).isNotNull();

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(repository).save(captor.capture());
        Interview withFeedback = captor.getValue();
        assertThat(withFeedback.getFeedback()).isEqualTo("Excellent candidate, strong technical skills");
        assertThat(withFeedback.getRating()).isEqualTo(5);
        assertThat(withFeedback.getUpdatedByUserId()).isEqualTo(userId);
    }

    @Test
    void addFeedback_throwsValidationException_whenInterviewNotCompleted() {
        interview.setStatus(InterviewStatus.SCHEDULED);
        InterviewFeedbackRequest request = new InterviewFeedbackRequest(
                "Feedback",
                4
        );

        when(repository.findByIdAndIsDeletedFalse(interviewId)).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.addFeedback(interviewId, request, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Feedback can only be added to completed interviews");

        verify(repository, never()).save(any());
    }

    // ========== Get Upcoming Interviews Tests ==========

    @Test
    void getUpcomingInterviews_success() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(7);
        List<Interview> interviews = List.of(interview);

        when(repository.findByStatusAndScheduledAtBetweenAndIsDeletedFalse(
                InterviewStatus.SCHEDULED, from, to))
                .thenReturn(interviews);

        List<InterviewResponse> result = service.getUpcomingInterviews(from, to);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(InterviewStatus.SCHEDULED);
    }

    @Test
    void getUpcomingInterviews_throwsValidationException_whenFromAfterTo() {
        LocalDateTime from = LocalDateTime.now().plusDays(7);
        LocalDateTime to = LocalDateTime.now();

        assertThatThrownBy(() -> service.getUpcomingInterviews(from, to))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date must be before end date");
    }

    // ========== Get Today's Interviews Tests ==========

    @Test
    void getTodaysInterviews_success() {
        List<Interview> interviews = List.of(interview);

        when(repository.findByScheduledAtBetweenAndIsDeletedFalse(any(), any()))
                .thenReturn(interviews);

        List<InterviewResponse> result = service.getTodaysInterviews();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    // ========== Get Upcoming Interviews By Interviewer Tests ==========

    @Test
    void getUpcomingInterviewsByInterviewer_success() {
        List<Interview> interviews = List.of(interview);

        when(repository.findUpcomingInterviewsByInterviewer(eq(interviewerId), any()))
                .thenReturn(interviews);

        List<InterviewResponse> result = service.getUpcomingInterviewsByInterviewer(interviewerId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).interviewerUserId()).isEqualTo(interviewerId);
    }

    // ========== Get Today's Interviews By Interviewer Tests ==========

    @Test
    void getTodaysInterviewsByInterviewer_success() {
        List<Interview> interviews = List.of(interview);

        when(repository.findTodaysInterviewsByInterviewer(eq(interviewerId), any()))
                .thenReturn(interviews);

        List<InterviewResponse> result = service.getTodaysInterviewsByInterviewer(interviewerId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).interviewerUserId()).isEqualTo(interviewerId);
    }
}
