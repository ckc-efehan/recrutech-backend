package com.recrutech.recrutechauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recrutech.common.event.BaseEvent;
import com.recrutech.recrutechauth.model.EventOutbox;
import com.recrutech.recrutechauth.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing the Event Outbox.
 * Provides transactional storage of events and statistics.
 * 
 * <p>This service ensures that events are persisted in the database
 * within the same transaction as the business operation, guaranteeing
 * that events are not lost even if Kafka is unavailable.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final EventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Stores an event in the outbox for later publishing.
     * This method should be called within the same transaction as the business operation.
     * 
     * @param event The domain event to store
     * @param topic The Kafka topic to publish to
     * @param partitionKey The partition key for Kafka
     * @return The stored EventOutbox entity
     */
    @Transactional
    public EventOutbox storeEvent(BaseEvent event, String topic, String partitionKey) {
        log.info("[OUTBOX] Storing event in outbox: eventId={}, eventType={}, topic={}", 
                event.getEventId(), event.getEventType(), topic);
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            EventOutbox outboxEvent = EventOutbox.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .topic(topic)
                    .partitionKey(partitionKey)
                    .payload(payload)
                    .status(EventOutbox.OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .attempts(0)
                    .build();
            
            EventOutbox saved = outboxRepository.save(outboxEvent);
            
            log.info("[OUTBOX] Event stored successfully in outbox: id={}, eventId={}", 
                    saved.getId(), saved.getEventId());
            
            return saved;
            
        } catch (JsonProcessingException e) {
            log.error("[OUTBOX] Failed to serialize event: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event for outbox", e);
        }
    }

    /**
     * Retrieves pending events ready for publishing.
     * 
     * @param limit Maximum number of events to retrieve
     * @return List of pending events
     */
    @Transactional(readOnly = true)
    public List<EventOutbox> getPendingEvents(int limit) {
        return outboxRepository.findTopPendingEvents(limit);
    }

    /**
     * Marks an event as successfully published.
     * 
     * @param outboxEvent The outbox event to mark
     */
    @Transactional
    public void markAsPublished(EventOutbox outboxEvent) {
        outboxEvent.markAsPublished();
        outboxRepository.save(outboxEvent);
        log.info("[OUTBOX] Event marked as published: id={}, eventId={}", 
                outboxEvent.getId(), outboxEvent.getEventId());
    }

    /**
     * Marks an event as processing.
     * 
     * @param outboxEvent The outbox event to mark
     */
    @Transactional
    public void markAsProcessing(EventOutbox outboxEvent) {
        outboxEvent.markAsProcessing();
        outboxRepository.save(outboxEvent);
    }

    /**
     * Marks an event as failed with an error message.
     * 
     * @param outboxEvent The outbox event to mark
     * @param errorMessage The error message
     */
    @Transactional
    public void markAsFailed(EventOutbox outboxEvent, String errorMessage) {
        outboxEvent.markAsFailed(errorMessage);
        outboxRepository.save(outboxEvent);
        log.error("[OUTBOX] Event marked as failed: id={}, eventId={}, error={}", 
                outboxEvent.getId(), outboxEvent.getEventId(), errorMessage);
    }

    /**
     * Resets stuck processing events back to pending status.
     * Should be called periodically to recover from crashes.
     * 
     * @param thresholdMinutes Minutes after which a PROCESSING event is considered stuck
     * @return Number of events reset
     */
    @Transactional
    public int resetStuckProcessingEvents(int thresholdMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(thresholdMinutes);
        List<EventOutbox> stuckEvents = outboxRepository.findStuckProcessingEvents(threshold);
        
        for (EventOutbox event : stuckEvents) {
            event.resetForRetry();
            outboxRepository.save(event);
            log.warn("[OUTBOX] Reset stuck processing event: id={}, eventId={}, lastAttempt={}", 
                    event.getId(), event.getEventId(), event.getLastAttemptAt());
        }
        
        return stuckEvents.size();
    }

    /**
     * Cleans up old published events to prevent unbounded table growth.
     * 
     * @param retentionDays Number of days to retain published events
     * @return Number of deleted events
     */
    @Transactional
    public int cleanupOldPublishedEvents(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int deleted = outboxRepository.deletePublishedEventsBefore(threshold);
        
        if (deleted > 0) {
            log.info("[OUTBOX] Cleaned up {} old published events (older than {} days)", 
                    deleted, retentionDays);
        }
        
        return deleted;
    }

    /**
     * Gets statistics about the outbox for monitoring.
     * 
     * @return Map with outbox statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long pendingCount = outboxRepository.countByStatus(EventOutbox.OutboxStatus.PENDING);
        long processingCount = outboxRepository.countByStatus(EventOutbox.OutboxStatus.PROCESSING);
        long publishedCount = outboxRepository.countByStatus(EventOutbox.OutboxStatus.PUBLISHED);
        long failedCount = outboxRepository.countByStatus(EventOutbox.OutboxStatus.FAILED);
        
        stats.put("pending", pendingCount);
        stats.put("processing", processingCount);
        stats.put("published", publishedCount);
        stats.put("failed", failedCount);
        stats.put("total", pendingCount + processingCount + publishedCount + failedCount);
        
        return stats;
    }

    /**
     * Checks if an event with the given ID already exists in the outbox.
     * 
     * @param eventId The event ID to check
     * @return true if the event exists
     */
    @Transactional(readOnly = true)
    public boolean eventExists(String eventId) {
        return outboxRepository.findByEventId(eventId).isPresent();
    }
}
