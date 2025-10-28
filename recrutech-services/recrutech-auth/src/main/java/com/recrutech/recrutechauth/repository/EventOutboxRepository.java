package com.recrutech.recrutechauth.repository;

import com.recrutech.recrutechauth.model.EventOutbox;
import com.recrutech.recrutechauth.model.EventOutbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for EventOutbox entities.
 * Provides queries for the Outbox Pattern implementation.
 */
@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {

    /**
     * Finds all pending events ordered by creation time.
     * 
     * @param limit Maximum number of events to retrieve
     * @return List of pending events
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<EventOutbox> findPendingEvents(@Param("limit") int limit);

    /**
     * Finds pending events with a limit using native query for better performance.
     * 
     * @param limit Maximum number of events to retrieve
     * @return List of pending events
     */
    @Query(value = "SELECT * FROM event_outbox WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit", 
           nativeQuery = true)
    List<EventOutbox> findTopPendingEvents(@Param("limit") int limit);

    /**
     * Finds events that are stuck in PROCESSING state (likely due to crash).
     * These should be reset to PENDING for retry.
     * 
     * @param threshold Time threshold for stuck events
     * @return List of stuck processing events
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'PROCESSING' AND e.lastAttemptAt < :threshold")
    List<EventOutbox> findStuckProcessingEvents(@Param("threshold") LocalDateTime threshold);

    /**
     * Finds failed events that can be retried.
     * 
     * @param maxAttempts Maximum number of retry attempts
     * @return List of retryable failed events
     */
    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'FAILED' AND e.attempts < :maxAttempts ORDER BY e.createdAt ASC")
    List<EventOutbox> findRetryableFailedEvents(@Param("maxAttempts") int maxAttempts);

    /**
     * Finds an event by its event ID (idempotency key).
     * 
     * @param eventId The event ID
     * @return Optional containing the event if found
     */
    Optional<EventOutbox> findByEventId(String eventId);

    /**
     * Counts events by status.
     * 
     * @param status The status to count
     * @return Number of events with the given status
     */
    long countByStatus(OutboxStatus status);

    /**
     * Deletes successfully published events older than the specified date.
     * Cleanup method to prevent unbounded table growth.
     * 
     * @param threshold Delete events published before this date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM EventOutbox e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :threshold")
    int deletePublishedEventsBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * Gets statistics about the outbox.
     * 
     * @return List of status counts
     */
    @Query("SELECT e.status, COUNT(e) FROM EventOutbox e GROUP BY e.status")
    List<Object[]> getOutboxStatistics();
}
