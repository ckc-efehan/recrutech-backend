package com.recrutech.recrutechplatform.interview.repository;

import com.recrutech.recrutechplatform.interview.model.Interview;
import com.recrutech.recrutechplatform.interview.model.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Interview entity.
 * Provides data access methods for interview scheduling and management with pagination and filtering.
 */
@Repository
public interface InterviewRepository extends JpaRepository<Interview, String> {

    /**
     * Find an interview by ID, ensuring it's not soft-deleted.
     */
    Optional<Interview> findByIdAndIsDeletedFalse(String id);

    /**
     * Find all interviews for a specific application.
     */
    Page<Interview> findAllByApplicationIdAndIsDeletedFalse(String applicationId, Pageable pageable);

    /**
     * Find all interviews for a specific application as a list.
     */
    List<Interview> findByApplicationIdAndIsDeletedFalse(String applicationId);

    /**
     * Find all interviews for a specific interviewer.
     */
    Page<Interview> findAllByInterviewerUserIdAndIsDeletedFalse(String interviewerUserId, Pageable pageable);

    /**
     * Find all interviews by status.
     */
    Page<Interview> findAllByStatusAndIsDeletedFalse(InterviewStatus status, Pageable pageable);

    /**
     * Find interviews scheduled within a date range with specific status.
     */
    List<Interview> findByStatusAndScheduledAtBetweenAndIsDeletedFalse(
            InterviewStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Find all interviews scheduled within a date range.
     */
    List<Interview> findByScheduledAtBetweenAndIsDeletedFalse(
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Find upcoming interviews for a specific interviewer.
     */
    @Query("SELECT i FROM Interview i WHERE i.interviewerUserId = :interviewerUserId " +
           "AND i.scheduledAt >= :from AND i.status = 'SCHEDULED' " +
           "AND i.isDeleted = false ORDER BY i.scheduledAt ASC")
    List<Interview> findUpcomingInterviewsByInterviewer(
            @Param("interviewerUserId") String interviewerUserId,
            @Param("from") LocalDateTime from
    );

    /**
     * Find today's interviews for a specific interviewer.
     */
    @Query("SELECT i FROM Interview i WHERE i.interviewerUserId = :interviewerUserId " +
           "AND DATE(i.scheduledAt) = DATE(:today) AND i.isDeleted = false " +
           "ORDER BY i.scheduledAt ASC")
    List<Interview> findTodaysInterviewsByInterviewer(
            @Param("interviewerUserId") String interviewerUserId,
            @Param("today") LocalDateTime today
    );

    /**
     * Count interviews for an application by status.
     */
    long countByApplicationIdAndStatusAndIsDeletedFalse(String applicationId, InterviewStatus status);

    /**
     * Check if an application exists in the database.
     * Uses native SQL for efficient existence checking.
     * This is used for proactive validation before creating an interview.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM applications WHERE id = :applicationId AND is_deleted = false", nativeQuery = true)
    boolean applicationExists(@Param("applicationId") String applicationId);

    /**
     * Check if a user exists in the database.
     * Uses native SQL for efficient existence checking.
     * This is used for proactive validation before assigning an interviewer.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE id = :userId", nativeQuery = true)
    boolean userExists(@Param("userId") String userId);
}
