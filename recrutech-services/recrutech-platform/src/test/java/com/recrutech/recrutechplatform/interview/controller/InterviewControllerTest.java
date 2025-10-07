package com.recrutech.recrutechplatform.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recrutech.common.exception.EntityReferenceNotFoundException;
import com.recrutech.common.exception.GlobalExceptionHandler;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.recrutechplatform.interview.dto.InterviewCreateRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewFeedbackRequest;
import com.recrutech.recrutechplatform.interview.dto.InterviewResponse;
import com.recrutech.recrutechplatform.interview.dto.InterviewUpdateRequest;
import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import com.recrutech.recrutechplatform.interview.model.InterviewType;
import com.recrutech.recrutechplatform.interview.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InterviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({InterviewControllerTest.TestConfig.class, GlobalExceptionHandler.class})
class InterviewControllerTest {

    static class TestConfig {
        @Bean
        InterviewService interviewService() {
            return mock(InterviewService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InterviewService service;

    private static final String BASE = "/api/interviews";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String APPLICATION_ID = "22222222-2222-2222-2222-222222222222";
    private static final String INTERVIEW_ID = "33333333-3333-3333-3333-333333333333";
    private static final String INTERVIEWER_ID = "44444444-4444-4444-4444-444444444444";

    private InterviewResponse sampleResponse;
    private LocalDateTime futureDateTime;

    @BeforeEach
    void setUp() {
        reset(service);
        futureDateTime = LocalDateTime.now().plusDays(1);

        sampleResponse = new InterviewResponse(
                INTERVIEW_ID,
                APPLICATION_ID,
                InterviewType.VIDEO,
                InterviewStatus.SCHEDULED,
                futureDateTime,
                60,
                "Office Room 101",
                "https://zoom.us/meeting",
                "Technical interview",
                "Prepare coding questions",
                INTERVIEWER_ID,
                null,
                null,
                null,
                USER_ID,
                null,
                null,
                false,
                null,
                LocalDateTime.now()
        );
    }

    // ========== Schedule Interview Tests ==========

    @Test
    void scheduleInterview_returns201_andCreatesInterview() throws Exception {
        InterviewCreateRequest request = new InterviewCreateRequest(
                APPLICATION_ID,
                futureDateTime,
                60,
                InterviewType.VIDEO,
                "Office Room 101",
                "https://zoom.us/meeting",
                INTERVIEWER_ID,
                "Technical interview",
                "Prepare coding questions"
        );

        when(service.scheduleInterview(any(InterviewCreateRequest.class), eq(USER_ID)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post(BASE)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.interviewType").value("VIDEO"))
                .andExpect(jsonPath("$.interviewerUserId").value(INTERVIEWER_ID));

        verify(service).scheduleInterview(any(InterviewCreateRequest.class), eq(USER_ID));
    }

    @Test
    void scheduleInterview_returns404_whenApplicationNotFound() throws Exception {
        InterviewCreateRequest request = new InterviewCreateRequest(
                APPLICATION_ID,
                futureDateTime,
                60,
                InterviewType.VIDEO,
                null,
                "https://zoom.us/meeting",
                null,
                null,
                null
        );

        when(service.scheduleInterview(any(InterviewCreateRequest.class), eq(USER_ID)))
                .thenThrow(new EntityReferenceNotFoundException("Application not found"));

        mockMvc.perform(post(BASE)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(service).scheduleInterview(any(InterviewCreateRequest.class), eq(USER_ID));
    }

    @Test
    void scheduleInterview_returns400_whenValidationFails() throws Exception {
        InterviewCreateRequest request = new InterviewCreateRequest(
                "invalid-uuid",
                futureDateTime,
                60,
                InterviewType.VIDEO,
                null,
                null,
                null,
                null,
                null
        );

        when(service.scheduleInterview(any(InterviewCreateRequest.class), eq(USER_ID)))
                .thenThrow(new ValidationException("Invalid UUID format"));

        mockMvc.perform(post(BASE)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service).scheduleInterview(any(InterviewCreateRequest.class), eq(USER_ID));
    }

    // ========== Get Interview Tests ==========

    @Test
    void getInterview_returns200_andInterviewData() throws Exception {
        when(service.getInterview(INTERVIEW_ID))
                .thenReturn(sampleResponse);

        mockMvc.perform(get(BASE + "/{id}", INTERVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(service).getInterview(INTERVIEW_ID);
    }

    @Test
    void getInterview_returns404_whenNotFound() throws Exception {
        when(service.getInterview(INTERVIEW_ID))
                .thenThrow(new NotFoundException("Interview not found"));

        mockMvc.perform(get(BASE + "/{id}", INTERVIEW_ID))
                .andExpect(status().isNotFound());

        verify(service).getInterview(INTERVIEW_ID);
    }

    // ========== Update Interview Tests ==========

    @Test
    void updateInterview_returns200_andUpdatedInterview() throws Exception {
        InterviewUpdateRequest request = new InterviewUpdateRequest(
                futureDateTime.plusHours(2),
                90,
                InterviewType.ONSITE,
                "Conference Room A",
                null,
                INTERVIEWER_ID,
                "Updated description",
                "Updated notes"
        );

        when(service.updateInterview(eq(INTERVIEW_ID), any(InterviewUpdateRequest.class), eq(USER_ID)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put(BASE + "/{id}", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERVIEW_ID));

        verify(service).updateInterview(eq(INTERVIEW_ID), any(InterviewUpdateRequest.class), eq(USER_ID));
    }

    @Test
    void updateInterview_returns404_whenNotFound() throws Exception {
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

        when(service.updateInterview(eq(INTERVIEW_ID), any(InterviewUpdateRequest.class), eq(USER_ID)))
                .thenThrow(new NotFoundException("Interview not found"));

        mockMvc.perform(put(BASE + "/{id}", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(service).updateInterview(eq(INTERVIEW_ID), any(InterviewUpdateRequest.class), eq(USER_ID));
    }

    // ========== Cancel Interview Tests ==========

    @Test
    void cancelInterview_returns204() throws Exception {
        doNothing().when(service).cancelInterview(INTERVIEW_ID, USER_ID);

        mockMvc.perform(delete(BASE + "/{id}", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());

        verify(service).cancelInterview(INTERVIEW_ID, USER_ID);
    }

    @Test
    void cancelInterview_returns404_whenNotFound() throws Exception {
        doThrow(new NotFoundException("Interview not found"))
                .when(service).cancelInterview(INTERVIEW_ID, USER_ID);

        mockMvc.perform(delete(BASE + "/{id}", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());

        verify(service).cancelInterview(INTERVIEW_ID, USER_ID);
    }

    // ========== Get Interviews By Application Tests ==========

    @Test
    void getInterviewsByApplication_returns200_andPagedResults() throws Exception {
        Page<InterviewResponse> page = new PageImpl<>(List.of(sampleResponse));

        when(service.getInterviewsByApplication(eq(APPLICATION_ID), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get(BASE + "/application/{applicationId}", APPLICATION_ID)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "scheduledAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).getInterviewsByApplication(eq(APPLICATION_ID), any(Pageable.class));
    }

    // ========== Get Interviews By Interviewer Tests ==========

    @Test
    void getInterviewsByInterviewer_returns200_andPagedResults() throws Exception {
        Page<InterviewResponse> page = new PageImpl<>(List.of(sampleResponse));

        when(service.getInterviewsByInterviewer(eq(INTERVIEWER_ID), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get(BASE + "/interviewer/{interviewerId}", INTERVIEWER_ID)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "scheduledAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].interviewerUserId").value(INTERVIEWER_ID))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).getInterviewsByInterviewer(eq(INTERVIEWER_ID), any(Pageable.class));
    }

    // ========== Mark As Completed Tests ==========

    @Test
    void markAsCompleted_returns200_andUpdatedInterview() throws Exception {
        InterviewResponse completedResponse = new InterviewResponse(
                INTERVIEW_ID,
                APPLICATION_ID,
                InterviewType.VIDEO,
                InterviewStatus.COMPLETED,
                futureDateTime,
                60,
                "Office",
                "https://zoom.us/meeting",
                "Technical interview",
                "Notes",
                INTERVIEWER_ID,
                null,
                null,
                LocalDateTime.now(),
                USER_ID,
                USER_ID,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(service.markAsCompleted(INTERVIEW_ID, USER_ID))
                .thenReturn(completedResponse);

        mockMvc.perform(post(BASE + "/{id}/complete", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(service).markAsCompleted(INTERVIEW_ID, USER_ID);
    }

    @Test
    void markAsCompleted_returns400_whenInvalidStatus() throws Exception {
        when(service.markAsCompleted(INTERVIEW_ID, USER_ID))
                .thenThrow(new ValidationException("Only scheduled interviews can be marked as completed"));

        mockMvc.perform(post(BASE + "/{id}/complete", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());

        verify(service).markAsCompleted(INTERVIEW_ID, USER_ID);
    }

    // ========== Mark As No Show Tests ==========

    @Test
    void markAsNoShow_returns200_andUpdatedInterview() throws Exception {
        InterviewResponse noShowResponse = new InterviewResponse(
                INTERVIEW_ID,
                APPLICATION_ID,
                InterviewType.VIDEO,
                InterviewStatus.NO_SHOW,
                futureDateTime,
                60,
                "Office",
                "https://zoom.us/meeting",
                "Technical interview",
                "Notes",
                INTERVIEWER_ID,
                null,
                null,
                LocalDateTime.now(),
                USER_ID,
                USER_ID,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(service.markAsNoShow(INTERVIEW_ID, USER_ID))
                .thenReturn(noShowResponse);

        mockMvc.perform(post(BASE + "/{id}/no-show", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        verify(service).markAsNoShow(INTERVIEW_ID, USER_ID);
    }

    // ========== Add Feedback Tests ==========

    @Test
    void addFeedback_returns200_andUpdatedInterview() throws Exception {
        InterviewFeedbackRequest request = new InterviewFeedbackRequest(
                "Excellent candidate, strong technical skills",
                5
        );

        InterviewResponse withFeedback = new InterviewResponse(
                INTERVIEW_ID,
                APPLICATION_ID,
                InterviewType.VIDEO,
                InterviewStatus.COMPLETED,
                futureDateTime,
                60,
                "Office",
                "https://zoom.us/meeting",
                "Technical interview",
                "Notes",
                INTERVIEWER_ID,
                "Excellent candidate, strong technical skills",
                5,
                LocalDateTime.now(),
                USER_ID,
                USER_ID,
                null,
                false,
                null,
                LocalDateTime.now()
        );

        when(service.addFeedback(eq(INTERVIEW_ID), any(InterviewFeedbackRequest.class), eq(USER_ID)))
                .thenReturn(withFeedback);

        mockMvc.perform(post(BASE + "/{id}/feedback", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERVIEW_ID))
                .andExpect(jsonPath("$.feedback").value("Excellent candidate, strong technical skills"))
                .andExpect(jsonPath("$.rating").value(5));

        verify(service).addFeedback(eq(INTERVIEW_ID), any(InterviewFeedbackRequest.class), eq(USER_ID));
    }

    @Test
    void addFeedback_returns400_whenInterviewNotCompleted() throws Exception {
        InterviewFeedbackRequest request = new InterviewFeedbackRequest(
                "Feedback",
                4
        );

        when(service.addFeedback(eq(INTERVIEW_ID), any(InterviewFeedbackRequest.class), eq(USER_ID)))
                .thenThrow(new ValidationException("Feedback can only be added to completed interviews"));

        mockMvc.perform(post(BASE + "/{id}/feedback", INTERVIEW_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service).addFeedback(eq(INTERVIEW_ID), any(InterviewFeedbackRequest.class), eq(USER_ID));
    }

    // ========== Get Upcoming Interviews Tests ==========

    @Test
    void getUpcomingInterviews_returns200_andListOfInterviews() throws Exception {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(7);

        when(service.getUpcomingInterviews(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get(BASE + "/upcoming")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(INTERVIEW_ID))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(service).getUpcomingInterviews(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // ========== Get Today's Interviews Tests ==========

    @Test
    void getTodaysInterviews_returns200_andListOfInterviews() throws Exception {
        when(service.getTodaysInterviews())
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get(BASE + "/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(INTERVIEW_ID));

        verify(service).getTodaysInterviews();
    }

    // ========== Get Upcoming Interviews By Interviewer Tests ==========

    @Test
    void getUpcomingInterviewsByInterviewer_returns200_andListOfInterviews() throws Exception {
        when(service.getUpcomingInterviewsByInterviewer(INTERVIEWER_ID))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get(BASE + "/interviewer/{interviewerId}/upcoming", INTERVIEWER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].interviewerUserId").value(INTERVIEWER_ID));

        verify(service).getUpcomingInterviewsByInterviewer(INTERVIEWER_ID);
    }

    // ========== Get Today's Interviews By Interviewer Tests ==========

    @Test
    void getTodaysInterviewsByInterviewer_returns200_andListOfInterviews() throws Exception {
        when(service.getTodaysInterviewsByInterviewer(INTERVIEWER_ID))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get(BASE + "/interviewer/{interviewerId}/today", INTERVIEWER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].interviewerUserId").value(INTERVIEWER_ID));

        verify(service).getTodaysInterviewsByInterviewer(INTERVIEWER_ID);
    }

    // ========== Pagination and Sorting Tests ==========

    @Test
    void getInterviewsByApplication_supportsPaginationParameters() throws Exception {
        Page<InterviewResponse> page = new PageImpl<>(List.of(sampleResponse));

        when(service.getInterviewsByApplication(eq(APPLICATION_ID), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get(BASE + "/application/{applicationId}", APPLICATION_ID)
                        .param("page", "1")
                        .param("size", "20")
                        .param("sort", "scheduledAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).getInterviewsByApplication(eq(APPLICATION_ID), any(Pageable.class));
    }
}
