package com.recrutech.recrutechplatform.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for ProcessedEvent entities.
 * Used for idempotent event processing.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * Finds a processed event by its event ID (idempotency key).
     * 
     * @param eventId The event ID
     * @return Optional containing the processed event if found
     */
    Optional<ProcessedEvent> findByEventId(String eventId);

    /**
     * Checks if an event has already been processed.
     * 
     * @param eventId The event ID
     * @return true if the event was already processed
     */
    boolean existsByEventId(String eventId);

    /**
     * Counts processed events by type.
     * 
     * @param eventType The event type
     * @return Number of processed events of the given type
     */
    long countByEventType(String eventType);

    /**
     * Deletes processed events older than the specified date.
     * Cleanup method to prevent unbounded table growth.
     * 
     * @param threshold Delete events processed before this date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM ProcessedEvent e WHERE e.processedAt < :threshold")
    int deleteProcessedEventsBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * Finds failed events for retry.
     * 
     * @param maxAttempts Maximum number of retry attempts
     * @return List of failed events that can be retried
     */
    @Query("SELECT e FROM ProcessedEvent e WHERE e.status = 'FAILED' AND e.attempts < :maxAttempts ORDER BY e.processedAt ASC")
    java.util.List<ProcessedEvent> findRetryableFailedEvents(@Param("maxAttempts") int maxAttempts);
}
