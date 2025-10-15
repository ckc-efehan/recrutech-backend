package com.recrutech.recrutechplatform.jobposting.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

class JobPostingEntityTest {

    @Test
    void prePersist_setsIdAndCreatedAt_andDefaults() {
        JobPosting e = new JobPosting();
        e.setCompanyId("11111111-1111-1111-1111-111111111111");
        e.setCreatedByUserId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        e.setTitle("Title");
        e.setDescription("Desc");

        assertThat(e.getId()).isNull();
        assertThat(e.getCreatedAt()).isNull();
        assertThat(e.getStatus()).isEqualTo(JobPostingStatus.DRAFT);
        assertThat(e.isDeleted()).isFalse();

        e.prePersist();

        assertThat(e.getId()).isNotBlank();
        assertThat(e.getCreatedAt()).isNotNull();
        assertThat(e.getStatus()).isEqualTo(JobPostingStatus.DRAFT);
        assertThat(e.isDeleted()).isFalse();
    }

    @Test
    void canUpdateAuditFields_andKeepTimestampsUntouchedInPreUpdateHook() {
        JobPosting e = new JobPosting();
        e.setCompanyId("11111111-1111-1111-1111-111111111111");
        e.setCreatedByUserId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        e.setTitle("Title");
        e.setDescription("Desc");
        e.prePersist();
        LocalDateTime createdAt = e.getCreatedAt();

        e.setUpdatedByUserId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        e.preUpdate(); // no-op for now

        assertThat(e.getUpdatedByUserId()).isEqualTo("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        assertThat(e.getCreatedAt()).isEqualTo(createdAt);
    }
}
