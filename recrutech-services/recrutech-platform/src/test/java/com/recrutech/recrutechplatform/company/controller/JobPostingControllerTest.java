package com.recrutech.recrutechplatform.company.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recrutech.recrutechplatform.company.dto.JobPostingCreateRequest;
import com.recrutech.recrutechplatform.company.dto.JobPostingResponse;
import com.recrutech.recrutechplatform.company.dto.JobPostingUpdateRequest;
import com.recrutech.recrutechplatform.company.model.JobPostingStatus;
import com.recrutech.recrutechplatform.company.service.JobPostingService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobPostingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JobPostingControllerTest.TestConfig.class)
class JobPostingControllerTest {

    static class TestConfig {
        @Bean
        JobPostingService jobPostingService() {
            return org.mockito.Mockito.mock(JobPostingService.class);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JobPostingService service;

    private static final String BASE = "/api/companies/11111111-1111-1111-1111-111111111111/job-postings";

    private JobPostingResponse sampleResponse(String id) {
        return new JobPostingResponse(
                id,
                "11111111-1111-1111-1111-111111111111",
                "Senior Backend Engineer",
                "Beschreibung",
                "Berlin",
                "FULL_TIME",
                new BigDecimal("60000"),
                new BigDecimal("80000"),
                "EUR",
                JobPostingStatus.DRAFT,
                null,
                LocalDateTime.now().plusDays(30),
                false,
                LocalDateTime.now(),
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                null,
                null,
                null
        );
    }

    @Test
    void create_returns201_andDelegatesToService() throws Exception {
        JobPostingCreateRequest req = new JobPostingCreateRequest(
                "Senior Backend Engineer",
                "Beschreibung",
                "Berlin",
                "FULL_TIME",
                new BigDecimal("60000"),
                new BigDecimal("80000"),
                "EUR",
                LocalDateTime.now().plusDays(30)
        );

        when(service.create(any(), any(), any())).thenReturn(sampleResponse("22222222-2222-2222-2222-222222222222"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("22222222-2222-2222-2222-222222222222"));

        verify(service).create(eq("11111111-1111-1111-1111-111111111111"), eq("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), any());
    }

    @Test
    void create_missingUserHeader_returns400() throws Exception {
        JobPostingCreateRequest req = new JobPostingCreateRequest(
                "Senior Backend Engineer",
                "Beschreibung",
                "Berlin",
                "FULL_TIME",
                null,
                null,
                null,
                LocalDateTime.now().plusDays(30)
        );

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void getById_returns200() throws Exception {
        when(service.getById(any(), any())).thenReturn(sampleResponse("33333333-3333-3333-3333-333333333333"));

        mockMvc.perform(get(BASE + "/33333333-3333-3333-3333-333333333333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("33333333-3333-3333-3333-333333333333"));

        verify(service).getById(eq("11111111-1111-1111-1111-111111111111"), eq("33333333-3333-3333-3333-333333333333"));
    }

    @Test
    void list_usesPageableAndStatus() throws Exception {
        Page<JobPostingResponse> page = new PageImpl<>(List.of(sampleResponse("1")));
        when(service.list(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE)
                        .param("status", "PUBLISHED")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(eq("11111111-1111-1111-1111-111111111111"), eq(JobPostingStatus.PUBLISHED), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().iterator().next().isAscending()).isTrue();
    }

    @Test
    void update_returns200() throws Exception {
        JobPostingUpdateRequest req = new JobPostingUpdateRequest(
                "Senior Backend Engineer 2",
                "Neue Beschreibung",
                "Remote",
                "FULL_TIME",
                new BigDecimal("70000"),
                new BigDecimal("90000"),
                "EUR",
                LocalDateTime.now().plusDays(60)
        );
        when(service.update(any(), any(), any(), any())).thenReturn(sampleResponse("44444444-4444-4444-4444-444444444444"));

        mockMvc.perform(put(BASE + "/44444444-4444-4444-4444-444444444444")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("44444444-4444-4444-4444-444444444444"));

        verify(service).update(eq("11111111-1111-1111-1111-111111111111"), eq("44444444-4444-4444-4444-444444444444"), eq("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), any());
    }

    @Test
    void publish_returns200() throws Exception {
        when(service.publish(any(), any(), any())).thenReturn(sampleResponse("55555555-5555-5555-5555-555555555555"));

        mockMvc.perform(post(BASE + "/55555555-5555-5555-5555-555555555555/publish")
                        .header("X-User-Id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(status().isOk());

        verify(service).publish(eq("11111111-1111-1111-1111-111111111111"), eq("55555555-5555-5555-5555-555555555555"), eq("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    }

    @Test
    void close_returns200() throws Exception {
        when(service.close(any(), any(), any())).thenReturn(sampleResponse("66666666-6666-6666-6666-666666666666"));

        mockMvc.perform(post(BASE + "/66666666-6666-6666-6666-666666666666/close")
                        .header("X-User-Id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(status().isOk());

        verify(service).close(eq("11111111-1111-1111-1111-111111111111"), eq("66666666-6666-6666-6666-666666666666"), eq("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(service).softDelete(any(), any(), any());

        mockMvc.perform(delete(BASE + "/77777777-7777-7777-7777-777777777777")
                        .header("X-User-Id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(status().isNoContent());

        verify(service).softDelete(eq("11111111-1111-1111-1111-111111111111"), eq("77777777-7777-7777-7777-777777777777"), eq("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    }
}
