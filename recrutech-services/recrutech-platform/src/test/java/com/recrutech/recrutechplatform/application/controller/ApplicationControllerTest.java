package com.recrutech.recrutechplatform.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recrutech.recrutechplatform.application.dto.ApplicationSubmitRequest;
import com.recrutech.recrutechplatform.application.dto.ApplicationUpdateStatusRequest;
import com.recrutech.recrutechplatform.application.model.Application;
import com.recrutech.recrutechplatform.application.model.ApplicationStatus;
import com.recrutech.recrutechplatform.application.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApplicationControllerTest.TestConfig.class)
class ApplicationControllerTest {

    static class TestConfig {
        @Bean
        ApplicationService applicationService() {
            return org.mockito.Mockito.mock(ApplicationService.class);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ApplicationService service;

    @BeforeEach
    void setUp() {
        reset(service);
    }

    private static final String BASE = "/applications";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String APPLICANT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String JOB_POSTING_ID = "33333333-3333-3333-3333-333333333333";
    private static final String COMPANY_ID = "44444444-4444-4444-4444-444444444444";
    private static final String APPLICATION_ID = "55555555-5555-5555-5555-555555555555";

    private Application sampleApplication() {
        Application app = new Application();
        app.setId(ApplicationControllerTest.APPLICATION_ID);
        app.setApplicantId(APPLICANT_ID);
        app.setJobPostingId(JOB_POSTING_ID);
        app.setCreatedByUserId(USER_ID);
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        app.setCoverLetter("I am very interested in this position");
        app.setResumeUrl("https://resume.example.com");
        app.setPortfolioUrl("https://portfolio.example.com");
        app.setCreatedAt(LocalDateTime.now());
        app.setDeleted(false);
        return app;
    }

    // ========== Submit Tests ==========

    @Test
    void submit_returns201_andDelegatesToService() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest(
                APPLICANT_ID,
                JOB_POSTING_ID,
                "I am very interested",
                "https://resume.url",
                "https://portfolio.url"
        );

        when(service.submit(any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleApplication());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(APPLICATION_ID))
                .andExpect(jsonPath("$.applicantId").value(APPLICANT_ID))
                .andExpect(jsonPath("$.jobPostingId").value(JOB_POSTING_ID))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(service).submit(
                eq(APPLICANT_ID),
                eq(JOB_POSTING_ID),
                eq(USER_ID),
                eq("I am very interested"),
                eq("https://resume.url"),
                eq("https://portfolio.url")
        );
    }

    @Test
    void submit_returns400_whenMissingUserHeader() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest(
                APPLICANT_ID,
                JOB_POSTING_ID,
                "Cover letter",
                null,
                null
        );

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).submit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void submit_returns400_whenMissingApplicantId() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest(
                null,
                JOB_POSTING_ID,
                "Cover letter",
                null,
                null
        );

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).submit(any(), any(), any(), any(), any(), any());
    }

    @Test
    void submit_returns400_whenMissingJobPostingId() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest(
                APPLICANT_ID,
                null,
                "Cover letter",
                null,
                null
        );

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).submit(any(), any(), any(), any(), any(), any());
    }

    // ========== Get By ID Tests ==========

    @Test
    void getById_returns200() throws Exception {
        when(service.getById(APPLICATION_ID)).thenReturn(sampleApplication());

        mockMvc.perform(get(BASE + "/" + APPLICATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APPLICATION_ID))
                .andExpect(jsonPath("$.applicantId").value(APPLICANT_ID));

        verify(service).getById(APPLICATION_ID);
    }

    // ========== Get By Applicant Tests ==========

    @Test
    void getByApplicant_returns200_withoutStatusFilter() throws Exception {
        Page<Application> page = new PageImpl<>(List.of(sampleApplication()));
        when(service.getByApplicant(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/applicant/" + APPLICANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(APPLICATION_ID));

        verify(service).getByApplicant(eq(APPLICANT_ID), eq(null), any(Pageable.class));
    }

    @Test
    void getByApplicant_returns200_withStatusFilter() throws Exception {
        Page<Application> page = new PageImpl<>(List.of(sampleApplication()));
        when(service.getByApplicant(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/applicant/" + APPLICANT_ID)
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).getByApplicant(eq(APPLICANT_ID), eq(ApplicationStatus.SUBMITTED), any(Pageable.class));
    }

    @Test
    void getByApplicant_usesPageableParameters() throws Exception {
        Page<Application> page = new PageImpl<>(List.of());
        when(service.getByApplicant(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/applicant/" + APPLICANT_ID)
                        .param("page", "2")
                        .param("size", "15")
                        .param("sort", "submittedAt,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getByApplicant(eq(APPLICANT_ID), eq(null), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort().iterator().next().isAscending()).isTrue();
    }

    // ========== Get By Job Posting Tests ==========

    @Test
    void getByJobPosting_returns200_withoutStatusFilter() throws Exception {
        Page<Application> page = new PageImpl<>(List.of(sampleApplication()));
        when(service.getByJobPosting(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/job-posting/" + JOB_POSTING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].jobPostingId").value(JOB_POSTING_ID));

        verify(service).getByJobPosting(eq(JOB_POSTING_ID), eq(null), any(Pageable.class));
    }

    @Test
    void getByJobPosting_returns200_withStatusFilter() throws Exception {
        Page<Application> page = new PageImpl<>(List.of(sampleApplication()));
        when(service.getByJobPosting(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/job-posting/" + JOB_POSTING_ID)
                        .param("status", "UNDER_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).getByJobPosting(eq(JOB_POSTING_ID), eq(ApplicationStatus.UNDER_REVIEW), any(Pageable.class));
    }

    // ========== Get By Company Tests ==========

    @Test
    void getByCompany_returns200_withoutStatusFilter() throws Exception {
        Page<Application> page = new PageImpl<>(List.of(sampleApplication()));
        when(service.getByCompany(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/company/" + COMPANY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).getByCompany(eq(COMPANY_ID), eq(null), any(Pageable.class));
    }

    @Test
    void getByCompany_returns200_withStatusFilter() throws Exception {
        Page<Application> page = new PageImpl<>(List.of(sampleApplication()));
        when(service.getByCompany(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/company/" + COMPANY_ID)
                        .param("status", "INTERVIEWED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).getByCompany(eq(COMPANY_ID), eq(ApplicationStatus.INTERVIEWED), any(Pageable.class));
    }

    @Test
    void getByCompany_usesPageableParameters() throws Exception {
        Page<Application> page = new PageImpl<>(List.of());
        when(service.getByCompany(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/company/" + COMPANY_ID)
                        .param("page", "1")
                        .param("size", "50")
                        .param("sort", "submittedAt,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getByCompany(eq(COMPANY_ID), eq(null), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort().iterator().next().isDescending()).isTrue();
    }

    // ========== Update Status Tests ==========

    @Test
    void updateStatus_returns200() throws Exception {
        ApplicationUpdateStatusRequest request = new ApplicationUpdateStatusRequest(
                ApplicationStatus.UNDER_REVIEW,
                "Reviewing candidate qualifications",
                null
        );

        Application updated = sampleApplication();
        updated.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(service.updateStatus(any(), any(), any(), any(), any())).thenReturn(updated);

        mockMvc.perform(put(BASE + "/" + APPLICATION_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APPLICATION_ID))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        verify(service).updateStatus(
                eq(APPLICATION_ID),
                eq(ApplicationStatus.UNDER_REVIEW),
                eq(USER_ID),
                eq("Reviewing candidate qualifications"),
                eq(null)
        );
    }

    @Test
    void updateStatus_returns400_whenMissingUserHeader() throws Exception {
        ApplicationUpdateStatusRequest request = new ApplicationUpdateStatusRequest(
                ApplicationStatus.UNDER_REVIEW,
                null,
                null
        );

        mockMvc.perform(put(BASE + "/" + APPLICATION_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).updateStatus(any(), any(), any(), any(), any());
    }

    @Test
    void updateStatus_returns400_whenMissingStatus() throws Exception {
        ApplicationUpdateStatusRequest request = new ApplicationUpdateStatusRequest(
                null,
                "Some notes",
                null
        );

        mockMvc.perform(put(BASE + "/" + APPLICATION_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).updateStatus(any(), any(), any(), any(), any());
    }

    // ========== Withdraw Tests ==========

    @Test
    void withdraw_returns200() throws Exception {
        Application withdrawn = sampleApplication();
        withdrawn.setStatus(ApplicationStatus.WITHDRAWN);
        when(service.withdraw(any(), any(), any())).thenReturn(withdrawn);

        mockMvc.perform(post(BASE + "/" + APPLICATION_ID + "/withdraw")
                        .param("applicantId", APPLICANT_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APPLICATION_ID))
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        verify(service).withdraw(APPLICATION_ID, APPLICANT_ID, USER_ID);
    }

    @Test
    void withdraw_returns400_whenMissingUserHeader() throws Exception {
        mockMvc.perform(post(BASE + "/" + APPLICATION_ID + "/withdraw")
                        .param("applicantId", APPLICANT_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdraw_returns400_whenMissingApplicantId() throws Exception {
        mockMvc.perform(post(BASE + "/" + APPLICATION_ID + "/withdraw")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());
    }

    // ========== Delete Tests ==========

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(service).softDelete(any(), any());

        mockMvc.perform(delete(BASE + "/" + APPLICATION_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());

        verify(service).softDelete(APPLICATION_ID, USER_ID);
    }

    @Test
    void delete_returns400_whenMissingUserHeader() throws Exception {
        mockMvc.perform(delete(BASE + "/" + APPLICATION_ID))
                .andExpect(status().isBadRequest());

        verify(service, never()).softDelete(any(), any());
    }
}
