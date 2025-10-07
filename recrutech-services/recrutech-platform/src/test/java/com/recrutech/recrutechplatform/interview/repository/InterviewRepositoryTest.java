package com.recrutech.recrutechplatform.interview.repository;

import com.recrutech.recrutechplatform.interview.model.Interview;
import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import com.recrutech.recrutechplatform.interview.model.InterviewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InterviewRepositoryTest {

    @Autowired
    private InterviewRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private String applicationId1;
    private String applicationId2;
    private String interviewerId1;
    private String interviewerId2;
    private String userId;
    private Interview scheduledInterview1;
    private Interview scheduledInterview2;
    private Interview completedInterview;
    private Interview deletedInterview;

    @BeforeEach
    void setUp() {
        applicationId1 = "11111111-1111-1111-1111-111111111111";
        applicationId2 = "22222222-2222-2222-2222-222222222222";
        interviewerId1 = "33333333-3333-3333-3333-333333333333";
        interviewerId2 = "44444444-4444-4444-4444-444444444444";
        userId = "55555555-5555-5555-5555-555555555555";

        // Create test interviews
        scheduledInterview1 = createInterview(
                applicationId1,
                interviewerId1,
                LocalDateTime.now().plusDays(1),
                InterviewStatus.SCHEDULED,
                InterviewType.VIDEO,
                false
        );

        scheduledInterview2 = createInterview(
                applicationId1,
                interviewerId2,
                LocalDateTime.now().plusDays(2),
                InterviewStatus.SCHEDULED,
                InterviewType.ONSITE,
                false
        );

        completedInterview = createInterview(
                applicationId2,
                interviewerId1,
                LocalDateTime.now().minusDays(1),
                InterviewStatus.COMPLETED,
                InterviewType.PHONE,
                false
        );

        deletedInterview = createInterview(
                applicationId1,
                interviewerId1,
                LocalDateTime.now().plusDays(3),
                InterviewStatus.CANCELLED,
                InterviewType.VIDEO,
                true
        );
        deletedInterview.setDeletedAt(LocalDateTime.now());

        repository.save(scheduledInterview1);
        repository.save(scheduledInterview2);
        repository.save(completedInterview);
        repository.save(deletedInterview);

        entityManager.flush();
        entityManager.clear();
    }

    private Interview createInterview(String applicationId, String interviewerId,
                                      LocalDateTime scheduledAt, InterviewStatus status,
                                      InterviewType type, boolean deleted) {
        Interview interview = new Interview();
        interview.setApplicationId(applicationId);
        interview.setInterviewerUserId(interviewerId);
        interview.setScheduledAt(scheduledAt);
        interview.setStatus(status);
        interview.setInterviewType(type);
        interview.setDurationMinutes(60);
        interview.setLocation("Office");
        interview.setMeetingLink("https://zoom.us/meeting");
        interview.setDescription("Test interview");
        interview.setCreatedByUserId(userId);
        interview.setDeleted(deleted);
        return interview;
    }

    // ========== Find By ID Tests ==========

    @Test
    void findByIdAndIsDeletedFalse_returnsInterview_whenExists() {
        var result = repository.findByIdAndIsDeletedFalse(scheduledInterview1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(scheduledInterview1.getId());
        assertThat(result.get().isDeleted()).isFalse();
    }

    @Test
    void findByIdAndIsDeletedFalse_returnsEmpty_whenDeleted() {
        var result = repository.findByIdAndIsDeletedFalse(deletedInterview.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    void findByIdAndIsDeletedFalse_returnsEmpty_whenNotExists() {
        var result = repository.findByIdAndIsDeletedFalse("99999999-9999-9999-9999-999999999999");

        assertThat(result).isNotPresent();
    }

    // ========== Find By Application Tests ==========

    @Test
    void findAllByApplicationIdAndIsDeletedFalse_returnsInterviewsForApplication() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Interview> result = repository.findAllByApplicationIdAndIsDeletedFalse(applicationId1, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(i -> i.getApplicationId().equals(applicationId1));
        assertThat(result.getContent()).allMatch(i -> !i.isDeleted());
    }

    @Test
    void findByApplicationIdAndIsDeletedFalse_returnsListOfInterviews() {
        List<Interview> result = repository.findByApplicationIdAndIsDeletedFalse(applicationId1);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(i -> i.getApplicationId().equals(applicationId1));
        assertThat(result).allMatch(i -> !i.isDeleted());
    }

    @Test
    void findAllByApplicationIdAndIsDeletedFalse_excludesDeletedInterviews() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Interview> result = repository.findAllByApplicationIdAndIsDeletedFalse(applicationId1, pageable);

        assertThat(result.getContent()).noneMatch(Interview::isDeleted);
    }

    // ========== Find By Interviewer Tests ==========

    @Test
    void findAllByInterviewerUserIdAndIsDeletedFalse_returnsInterviewsForInterviewer() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Interview> result = repository.findAllByInterviewerUserIdAndIsDeletedFalse(interviewerId1, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(i -> i.getInterviewerUserId().equals(interviewerId1));
        assertThat(result.getContent()).allMatch(i -> !i.isDeleted());
    }

    // ========== Find By Status Tests ==========

    @Test
    void findAllByStatusAndIsDeletedFalse_returnsInterviewsWithStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Interview> result = repository.findAllByStatusAndIsDeletedFalse(InterviewStatus.SCHEDULED, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(i -> i.getStatus() == InterviewStatus.SCHEDULED);
        assertThat(result.getContent()).allMatch(i -> !i.isDeleted());
    }

    @Test
    void findAllByStatusAndIsDeletedFalse_excludesDeletedInterviews() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Interview> result = repository.findAllByStatusAndIsDeletedFalse(InterviewStatus.CANCELLED, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ========== Find By Date Range Tests ==========

    @Test
    void findByStatusAndScheduledAtBetweenAndIsDeletedFalse_returnsInterviewsInRange() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now().plusDays(5);

        List<Interview> result = repository.findByStatusAndScheduledAtBetweenAndIsDeletedFalse(
                InterviewStatus.SCHEDULED, from, to
        );

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(i -> i.getStatus() == InterviewStatus.SCHEDULED);
        assertThat(result).allMatch(i -> !i.getScheduledAt().isBefore(from));
        assertThat(result).allMatch(i -> !i.getScheduledAt().isAfter(to));
    }

    @Test
    void findByScheduledAtBetweenAndIsDeletedFalse_returnsAllInterviewsInRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(2);
        LocalDateTime to = LocalDateTime.now().plusDays(5);

        List<Interview> result = repository.findByScheduledAtBetweenAndIsDeletedFalse(from, to);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(i -> !i.isDeleted());
    }

    // ========== Custom Query Tests ==========

    @Test
    void findUpcomingInterviewsByInterviewer_returnsOnlyFutureScheduledInterviews() {
        LocalDateTime now = LocalDateTime.now();

        List<Interview> result = repository.findUpcomingInterviewsByInterviewer(interviewerId1, now);

        assertThat(result).hasSize(1);
        assertThat(result).allMatch(i -> i.getInterviewerUserId().equals(interviewerId1));
        assertThat(result).allMatch(i -> i.getStatus() == InterviewStatus.SCHEDULED);
        assertThat(result).allMatch(i -> i.getScheduledAt().isAfter(now));
    }

    @Test
    void findTodaysInterviewsByInterviewer_returnsOnlyTodaysInterviews() {
        // Create an interview for today
        Interview todayInterview = createInterview(
                applicationId1,
                interviewerId1,
                LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)),
                InterviewStatus.SCHEDULED,
                InterviewType.VIDEO,
                false
        );
        repository.save(todayInterview);
        entityManager.flush();

        LocalDateTime today = LocalDateTime.now();
        List<Interview> result = repository.findTodaysInterviewsByInterviewer(interviewerId1, today);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduledAt().toLocalDate()).isEqualTo(LocalDate.now());
    }

    // ========== Count Tests ==========

    @Test
    void countByApplicationIdAndStatusAndIsDeletedFalse_returnsCorrectCount() {
        long count = repository.countByApplicationIdAndStatusAndIsDeletedFalse(
                applicationId1,
                InterviewStatus.SCHEDULED
        );

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByApplicationIdAndStatusAndIsDeletedFalse_excludesDeletedInterviews() {
        long count = repository.countByApplicationIdAndStatusAndIsDeletedFalse(
                applicationId1,
                InterviewStatus.CANCELLED
        );

        assertThat(count).isEqualTo(0);
    }

    // ========== Existence Check Tests ==========

    @Test
    @Disabled("Requires applications table from cross-module dependency. Use integration tests instead.")
    void applicationExists_returnsTrue_whenApplicationExists() {
        // Note: This test requires the applications table which is not available in isolated @DataJpaTest
        // The query works correctly in production and integration tests with full database schema
        boolean exists = repository.applicationExists(applicationId1);

        // In a real scenario with proper test data, this would return true
        assertThat(exists).isTrue();
    }

    @Test
    @Disabled("Requires users table from cross-module dependency. Use integration tests instead.")
    void userExists_returnsTrue_whenUserExists() {
        // Note: This test requires the users table which is not available in isolated @DataJpaTest
        // The query works correctly in production and integration tests with full database schema
        boolean exists = repository.userExists(userId);

        // In a real scenario with proper test data, this would return true
        assertThat(exists).isTrue();
    }

    // ========== Pagination Tests ==========

    @Test
    void findAllByApplicationIdAndIsDeletedFalse_supportsPagination() {
        Pageable firstPage = PageRequest.of(0, 1);
        Page<Interview> result = repository.findAllByApplicationIdAndIsDeletedFalse(applicationId1, firstPage);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void findAllByInterviewerUserIdAndIsDeletedFalse_supportsPagination() {
        Pageable firstPage = PageRequest.of(0, 1);
        Page<Interview> result = repository.findAllByInterviewerUserIdAndIsDeletedFalse(interviewerId1, firstPage);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
    }

    // ========== Edge Case Tests ==========

    @Test
    void findByApplicationIdAndIsDeletedFalse_returnsEmptyList_whenNoInterviews() {
        String nonExistentApplicationId = "99999999-9999-9999-9999-999999999999";

        List<Interview> result = repository.findByApplicationIdAndIsDeletedFalse(nonExistentApplicationId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusAndScheduledAtBetweenAndIsDeletedFalse_returnsEmptyList_whenNoMatchingInterviews() {
        LocalDateTime from = LocalDateTime.now().plusYears(1);
        LocalDateTime to = LocalDateTime.now().plusYears(2);

        List<Interview> result = repository.findByStatusAndScheduledAtBetweenAndIsDeletedFalse(
                InterviewStatus.SCHEDULED, from, to
        );

        assertThat(result).isEmpty();
    }
}
