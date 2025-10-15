package com.recrutech.recrutechplatform.jobposting.service;

import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.recrutechplatform.jobposting.dto.JobPostingCreateRequest;
import com.recrutech.recrutechplatform.jobposting.dto.JobPostingResponse;
import com.recrutech.recrutechplatform.jobposting.dto.JobPostingUpdateRequest;
import com.recrutech.recrutechplatform.jobposting.model.JobPosting;
import com.recrutech.recrutechplatform.jobposting.model.JobPostingStatus;
import com.recrutech.recrutechplatform.jobposting.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {

    @Mock
    private JobPostingRepository repository;

    @InjectMocks
    private JobPostingService service;

    private String companyId;
    private String userId;

    @BeforeEach
    void setUp() {
        companyId = "11111111-1111-1111-1111-111111111111";
        userId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    }

    @Test
    void create_success() {
        JobPostingCreateRequest request = new JobPostingCreateRequest(
                "Senior Backend Engineer",
                "Beschreibung",
                "Berlin",
                "FULL_TIME",
                new BigDecimal("60000"),
                new BigDecimal("80000"),
                "EUR",
                LocalDateTime.now().plusDays(30)
        );

        when(repository.save(any(JobPosting.class))).thenAnswer(invocation -> {
            JobPosting e = invocation.getArgument(0);
            e.setId("22222222-2222-2222-2222-222222222222");
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });

        JobPostingResponse resp = service.create(companyId, userId, request);

        assertThat(resp.id()).isNotBlank();
        assertThat(resp.companyId()).isEqualTo(companyId);
        assertThat(resp.createdByUserId()).isEqualTo(userId);
        assertThat(resp.status()).isEqualTo(JobPostingStatus.DRAFT);

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(repository).save(captor.capture());
        JobPosting saved = captor.getValue();
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
        assertThat(saved.getCreatedByUserId()).isEqualTo(userId);
        assertThat(saved.getTitle()).isEqualTo(request.title());
    }

    @Test
    void create_throws_whenSalaryMinGreaterThanMax() {
        JobPostingCreateRequest request = new JobPostingCreateRequest(
                "t","d",null,null,
                new BigDecimal("900"), new BigDecimal("100"), "EUR", LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(companyId, userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("salaryMin cannot be greater than salaryMax");
    }

    @Test
    void create_throws_whenCurrencyMissingWithSalary() {
        JobPostingCreateRequest request = new JobPostingCreateRequest(
                "t","d",null,null,
                new BigDecimal("100"), null, null, LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(companyId, userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("currency is required");
    }

    @Test
    void create_throws_whenExpiresAtPast() {
        JobPostingCreateRequest request = new JobPostingCreateRequest(
                "t","d",null,null,
                null, null, null, LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service.create(companyId, userId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expiresAt must be in the future");
    }

    @Test
    void getById_success() {
        JobPosting e = new JobPosting();
        e.setId("33333333-3333-3333-3333-333333333333");
        e.setCompanyId(companyId);
        e.setCreatedByUserId(userId);
        e.setTitle("t");
        e.setDescription("d");
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(e.getId(), companyId)).thenReturn(Optional.of(e));

        JobPostingResponse resp = service.getById(companyId, e.getId());
        assertThat(resp.id()).isEqualTo(e.getId());
    }

    @Test
    void getById_notFound() {
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(anyString(), anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(companyId, "44444444-4444-4444-4444-444444444444"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Job posting not found");
    }

    @Test
    void update_success() {
        String id = "55555555-5555-5555-5555-555555555555";
        JobPosting existing = new JobPosting();
        existing.setId(id);
        existing.setCompanyId(companyId);
        existing.setCreatedByUserId(userId);
        existing.setTitle("old");
        existing.setDescription("old");
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)).thenReturn(Optional.of(existing));
        when(repository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPostingUpdateRequest req = new JobPostingUpdateRequest("new","new","loc","FULL_TIME",
                new BigDecimal("1"), new BigDecimal("2"), "EUR", LocalDateTime.now().plusDays(10));

        JobPostingResponse resp = service.update(companyId, id, userId, req);
        assertThat(resp.title()).isEqualTo("new");
        assertThat(resp.updatedByUserId()).isEqualTo(userId);
    }

    @Test
    void publish_success_fromDraft() {
        String id = "66666666-6666-6666-6666-666666666666";
        JobPosting existing = new JobPosting();
        existing.setId(id);
        existing.setCompanyId(companyId);
        existing.setCreatedByUserId(userId);
        existing.setTitle("t");
        existing.setDescription("d");
        existing.setStatus(JobPostingStatus.DRAFT);
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)).thenReturn(Optional.of(existing));
        when(repository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPostingResponse resp = service.publish(companyId, id, userId);
        assertThat(resp.status()).isEqualTo(JobPostingStatus.PUBLISHED);
        assertThat(resp.publishedAt()).isNotNull();
    }

    @Test
    void publish_idempotent_whenAlreadyPublished() {
        String id = "77777777-7777-7777-7777-777777777777";
        JobPosting existing = new JobPosting();
        existing.setId(id);
        existing.setCompanyId(companyId);
        existing.setCreatedByUserId(userId);
        existing.setTitle("t");
        existing.setDescription("d");
        existing.setStatus(JobPostingStatus.PUBLISHED);
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)).thenReturn(Optional.of(existing));

        JobPostingResponse resp = service.publish(companyId, id, userId);
        assertThat(resp.status()).isEqualTo(JobPostingStatus.PUBLISHED);
        verify(repository, never()).save(any());
    }

    @Test
    void publish_fails_whenArchived() {
        String id = "88888888-8888-8888-8888-888888888888";
        JobPosting existing = new JobPosting();
        existing.setId(id);
        existing.setCompanyId(companyId);
        existing.setCreatedByUserId(userId);
        existing.setTitle("t");
        existing.setDescription("d");
        existing.setStatus(JobPostingStatus.ARCHIVED);
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.publish(companyId, id, userId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot publish an archived job posting");
    }

    @Test
    void close_success() {
        String id = "99999999-9999-9999-9999-999999999999";
        JobPosting existing = new JobPosting();
        existing.setId(id);
        existing.setCompanyId(companyId);
        existing.setCreatedByUserId(userId);
        existing.setTitle("t");
        existing.setDescription("d");
        existing.setStatus(JobPostingStatus.PUBLISHED);
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)).thenReturn(Optional.of(existing));
        when(repository.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));

        JobPostingResponse resp = service.close(companyId, id, userId);
        assertThat(resp.status()).isEqualTo(JobPostingStatus.ARCHIVED);
    }

    @Test
    void close_idempotent_whenAlreadyArchived() {
        String id = "aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb";
        JobPosting existing = new JobPosting();
        existing.setId(id);
        existing.setCompanyId(companyId);
        existing.setCreatedByUserId(userId);
        existing.setTitle("t");
        existing.setDescription("d");
        existing.setStatus(JobPostingStatus.ARCHIVED);
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)).thenReturn(Optional.of(existing));

        JobPostingResponse resp = service.close(companyId, id, userId);
        assertThat(resp.status()).isEqualTo(JobPostingStatus.ARCHIVED);
        verify(repository, never()).save(any());
    }

    @Test
    void softDelete_setsFlagsAndTimestamps() {
        String id = "bbbbbbbb-1111-2222-3333-cccccccccccc";
        JobPosting existing = new JobPosting();
        existing.setId(id);
        existing.setCompanyId(companyId);
        existing.setCreatedByUserId(userId);
        existing.setTitle("t");
        existing.setDescription("d");
        existing.setStatus(JobPostingStatus.DRAFT);
        when(repository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)).thenReturn(Optional.of(existing));

        service.softDelete(companyId, id, userId);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedByUserId()).isEqualTo(userId);
        verify(repository).save(existing);
    }
}
