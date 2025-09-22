package com.recrutech.recrutechplatform.company.mapper;

import com.recrutech.recrutechplatform.company.dto.JobPostingCreateRequest;
import com.recrutech.recrutechplatform.company.dto.JobPostingResponse;
import com.recrutech.recrutechplatform.company.dto.JobPostingUpdateRequest;
import com.recrutech.recrutechplatform.company.model.JobPosting;
import com.recrutech.recrutechplatform.company.model.JobPostingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JobPostingMapperTest {

    @Test
    void toEntity_and_updateEntity_mapAllFields() {
        var create = new JobPostingCreateRequest(
                "Title","Desc","Loc","FULL_TIME",
                new BigDecimal("1"), new BigDecimal("2"), "EUR",
                LocalDateTime.now().plusDays(1)
        );
        JobPosting e = JobPostingMapper.toEntity(create);
        assertThat(e.getTitle()).isEqualTo("Title");
        assertThat(e.getDescription()).isEqualTo("Desc");
        assertThat(e.getLocation()).isEqualTo("Loc");
        assertThat(e.getEmploymentType()).isEqualTo("FULL_TIME");
        assertThat(e.getSalaryMin()).isEqualTo(new BigDecimal("1"));
        assertThat(e.getSalaryMax()).isEqualTo(new BigDecimal("2"));
        assertThat(e.getCurrency()).isEqualTo("EUR");
        assertThat(e.getExpiresAt()).isNotNull();

        var update = new JobPostingUpdateRequest(
                "Title2","Desc2","Loc2","PART_TIME",
                new BigDecimal("3"), new BigDecimal("4"), "USD",
                LocalDateTime.now().plusDays(2)
        );
        JobPostingMapper.updateEntity(update, e);
        assertThat(e.getTitle()).isEqualTo("Title2");
        assertThat(e.getEmploymentType()).isEqualTo("PART_TIME");
        assertThat(e.getSalaryMin()).isEqualByComparingTo("3");
        assertThat(e.getSalaryMax()).isEqualByComparingTo("4");
        assertThat(e.getCurrency()).isEqualTo("USD");
    }

    @Test
    void toResponse_containsAllFields() {
        JobPosting e = new JobPosting();
        e.setId("id-1");
        e.setCompanyId("c-1");
        e.setTitle("Title");
        e.setDescription("Desc");
        e.setLocation("Loc");
        e.setEmploymentType("FULL_TIME");
        e.setSalaryMin(new BigDecimal("1"));
        e.setSalaryMax(new BigDecimal("2"));
        e.setCurrency("EUR");
        e.setStatus(JobPostingStatus.DRAFT);
        e.setPublishedAt(LocalDateTime.now());
        e.setExpiresAt(LocalDateTime.now().plusDays(1));
        e.setDeleted(false);
        e.setCreatedAt(LocalDateTime.now());
        e.setCreatedByUserId("u-1");
        e.setUpdatedByUserId("u-2");
        e.setDeletedByUserId("u-3");
        e.setDeletedAt(null);

        JobPostingResponse r = JobPostingMapper.toResponse(e);
        assertThat(r.id()).isEqualTo("id-1");
        assertThat(r.companyId()).isEqualTo("c-1");
        assertThat(r.title()).isEqualTo("Title");
        assertThat(r.description()).isEqualTo("Desc");
        assertThat(r.location()).isEqualTo("Loc");
        assertThat(r.employmentType()).isEqualTo("FULL_TIME");
        assertThat(r.salaryMin()).isEqualByComparingTo("1");
        assertThat(r.salaryMax()).isEqualByComparingTo("2");
        assertThat(r.currency()).isEqualTo("EUR");
        assertThat(r.status()).isEqualTo(JobPostingStatus.DRAFT);
        assertThat(r.isDeleted()).isFalse();
        assertThat(r.createdByUserId()).isEqualTo("u-1");
        assertThat(r.updatedByUserId()).isEqualTo("u-2");
    }
}
