package com.recrutech.recrutechplatform.jobposting.repository;

import com.recrutech.recrutechplatform.jobposting.model.JobPosting;
import com.recrutech.recrutechplatform.jobposting.model.JobPostingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JobPostingRepositoryTest {

    @Autowired
    JobPostingRepository repo;

    @Autowired
    TestEntityManager entityManager;

    private final String companyId = "11111111-1111-1111-1111-111111111111";
    private final String userId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @BeforeEach
    void setup() {
        // Database is fresh with create-drop, so just seed test data
        // Seed postings: 2 DRAFT, 1 PUBLISHED, 1 deleted
        repo.save(entity("Title-A", JobPostingStatus.DRAFT, false));
        repo.save(entity("Title-B", JobPostingStatus.DRAFT, false));
        repo.save(entity("Title-C", JobPostingStatus.PUBLISHED, false));
        JobPosting deleted = entity("Title-D", JobPostingStatus.PUBLISHED, true);
        deleted.setDeletedAt(LocalDateTime.now());
        repo.save(deleted);
        
        entityManager.flush();
        entityManager.clear();
    }

    private JobPosting entity(String title, JobPostingStatus status, boolean deleted) {
        JobPosting e = new JobPosting();
        e.setCompanyId(companyId);
        e.setCreatedByUserId(userId);
        e.setTitle(title);
        e.setDescription("Beschreibung");
        e.setStatus(status);
        e.setDeleted(deleted);
        return e;
    }

    @Test
    void findAllByCompany_filtersDeleted() {
        Pageable pageable = PageRequest.of(0, 10);
        var page = repo.findAllByCompanyIdAndIsDeletedFalse(companyId, pageable);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allMatch(e -> !e.isDeleted());
    }

    @Test
    void findByIdAndCompany_onlyReturnsMatchingCompany_andNotDeleted() {
        var any = repo.findAllByCompanyIdAndIsDeletedFalse(companyId, PageRequest.of(0, 1))
                .getContent().getFirst();
        assertThat(repo.findByIdAndCompanyIdAndIsDeletedFalse(any.getId(), companyId)).isPresent();
        assertThat(repo.findByIdAndCompanyIdAndIsDeletedFalse(any.getId(), "22222222-2222-2222-2222-222222222222")).isNotPresent();
    }

    @Test
    void findAllByCompanyAndStatus_filtersByStatusAndNotDeleted() {
        var page = repo.findAllByCompanyIdAndStatusAndIsDeletedFalse(companyId, JobPostingStatus.DRAFT, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(e -> e.getStatus() == JobPostingStatus.DRAFT && !e.isDeleted());
    }
}
