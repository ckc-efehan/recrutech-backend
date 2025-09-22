package com.recrutech.recrutechplatform.company.repository;

import com.recrutech.recrutechplatform.company.model.JobPosting;
import com.recrutech.recrutechplatform.company.model.JobPostingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
class JobPostingRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("recrutech_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.jpa.show-sql", () -> "false");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.properties.hibernate.type.preferred_boolean_jdbc_type", () -> "TINYINT");
        // Use test-specific changelog that creates stub users/companies before job_postings
        r.add("spring.liquibase.change-log", () -> "classpath:db.changelog/test/test-changelog.xml");
    }

    @Autowired
    JobPostingRepository repo;

    @Autowired
    JdbcTemplate jdbc;

    private final String companyId = "11111111-1111-1111-1111-111111111111";
    private final String userId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @BeforeEach
    void setup() {
        // Clean tables in order (job_postings has FKs to users/companies)
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        jdbc.update("TRUNCATE TABLE job_postings");
        jdbc.update("TRUNCATE TABLE companies");
        jdbc.update("TRUNCATE TABLE users");
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");

        // Seed minimal users and companies to satisfy FK constraints
        jdbc.update("INSERT INTO users (id, created_at) VALUES (?, ?)", userId, LocalDateTime.now());
        jdbc.update("INSERT INTO companies (id, created_at, name) VALUES (?, ?, ?)", companyId, LocalDateTime.now(), "ACME");

        // Seed postings: 2 DRAFT, 1 PUBLISHED, 1 deleted
        repo.save(entity("Title-A", JobPostingStatus.DRAFT, false));
        repo.save(entity("Title-B", JobPostingStatus.DRAFT, false));
        repo.save(entity("Title-C", JobPostingStatus.PUBLISHED, false));
        JobPosting deleted = entity("Title-D", JobPostingStatus.PUBLISHED, true);
        deleted.setDeletedAt(LocalDateTime.now());
        repo.save(deleted);
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
