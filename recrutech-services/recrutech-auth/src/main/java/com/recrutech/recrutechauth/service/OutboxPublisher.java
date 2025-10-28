package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.model.EventOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scheduled publisher for the Event Outbox Pattern.
 * Periodically processes pending events from the outbox and publishes them to Kafka.
 * 
 * <p>This ensures reliable event delivery with transactional guarantees.
 * Events are first stored in the database, then asynchronously published to Kafka.</p>
 * 
 * <p>The publisher handles:
 * - Batch processing of pending events
 * - Retry logic for failed events
 * - Recovery from stuck processing states
 * - Periodic cleanup of old published events
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${recrutech.outbox.batch-size:50}")
    private int batchSize;

    @Value("${recrutech.outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${recrutech.outbox.stuck-threshold-minutes:10}")
    private int stuckThresholdMinutes;

    @Value("${recrutech.outbox.retention-days:7}")
    private int retentionDays;

    /**
     * Processes pending events from the outbox and publishes them to Kafka.
     * Runs every 5 seconds to ensure timely event delivery.
     */
    @Scheduled(fixedDelayString = "${recrutech.outbox.publish-interval-ms:5000}")
    public void publishPendingEvents() {
        try {
            List<EventOutbox> pendingEvents = outboxService.getPendingEvents(batchSize);
            
            if (pendingEvents.isEmpty()) {
                return;
            }
            
            log.info("[OUTBOX_PUBLISHER] Processing {} pending events", pendingEvents.size());
            
            for (EventOutbox event : pendingEvents) {
                processEvent(event);
            }
            
        } catch (Exception e) {
            log.error("[OUTBOX_PUBLISHER] Error processing pending events: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single event from the outbox.
     * 
     * @param event The event to process
     */
    private void processEvent(EventOutbox event) {
        try {
            // Mark as processing to prevent concurrent processing
            outboxService.markAsProcessing(event);
            
            log.debug("[OUTBOX_PUBLISHER] Publishing event: id={}, eventId={}, eventType={}, topic={}", 
                    event.getId(), event.getEventId(), event.getEventType(), event.getTopic());
            
            // Publish to Kafka
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    event.getTopic(),
                    event.getPartitionKey(),
                    event.getPayload()
            );
            
            // Handle result asynchronously
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        // Success
                        outboxService.markAsPublished(event);
                        log.info("[OUTBOX_PUBLISHER] Event published successfully: id={}, eventId={}, eventType={}", 
                                event.getId(), event.getEventId(), event.getEventType());
                    } else {
                        // Failure
                        handlePublishFailure(event, ex);
                    }
                });
            } else {
                // KafkaTemplate returned null future (shouldn't happen, but handle gracefully)
                outboxService.markAsFailed(event, "KafkaTemplate returned null future");
                log.error("[OUTBOX_PUBLISHER] Failed to publish event (null future): id={}, eventId={}", 
                        event.getId(), event.getEventId());
            }
            
        } catch (Exception e) {
            handlePublishFailure(event, e);
        }
    }

    /**
     * Handles publishing failures with retry logic.
     * 
     * @param event The event that failed to publish
     * @param throwable The exception that occurred
     */
    private void handlePublishFailure(EventOutbox event, Throwable throwable) {
        String errorMessage = throwable.getMessage();
        
        if (event.getAttempts() >= maxAttempts) {
            // Max attempts reached, mark as failed
            outboxService.markAsFailed(event, errorMessage);
            log.error("[OUTBOX_PUBLISHER] Event failed after {} attempts: id={}, eventId={}, error={}", 
                    maxAttempts, event.getId(), event.getEventId(), errorMessage);
        } else {
            // Will retry on next scheduled run
            outboxService.markAsFailed(event, errorMessage);
            // Note: We mark as FAILED but the resetStuckProcessingEvents will handle retry
            // Alternatively, we could immediately reset to PENDING for faster retry
            event.resetForRetry();
            outboxService.markAsProcessing(event); // Save the reset state
            log.warn("[OUTBOX_PUBLISHER] Event publish failed (attempt {}/{}): id={}, eventId={}, error={}", 
                    event.getAttempts(), maxAttempts, event.getId(), event.getEventId(), errorMessage);
        }
    }

    /**
     * Resets events stuck in PROCESSING state.
     * Runs every minute to recover from crashes or unexpected errors.
     */
    @Scheduled(fixedDelayString = "${recrutech.outbox.stuck-check-interval-ms:60000}")
    public void resetStuckProcessingEvents() {
        try {
            int resetCount = outboxService.resetStuckProcessingEvents(stuckThresholdMinutes);
            
            if (resetCount > 0) {
                log.warn("[OUTBOX_PUBLISHER] Reset {} stuck processing events", resetCount);
            }
            
        } catch (Exception e) {
            log.error("[OUTBOX_PUBLISHER] Error resetting stuck processing events: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleans up old published events to prevent unbounded table growth.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "${recrutech.outbox.cleanup-cron:0 0 3 * * ?}")
    public void cleanupOldPublishedEvents() {
        try {
            int deletedCount = outboxService.cleanupOldPublishedEvents(retentionDays);
            
            if (deletedCount > 0) {
                log.info("[OUTBOX_PUBLISHER] Cleaned up {} old published events", deletedCount);
            }
            
        } catch (Exception e) {
            log.error("[OUTBOX_PUBLISHER] Error cleaning up old published events: {}", e.getMessage(), e);
        }
    }

    /**
     * Logs outbox statistics for monitoring.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${recrutech.outbox.stats-interval-ms:300000}")
    public void logStatistics() {
        try {
            var stats = outboxService.getStatistics();
            log.info("[OUTBOX_STATS] Pending: {}, Processing: {}, Published: {}, Failed: {}, Total: {}", 
                    stats.get("pending"), stats.get("processing"), stats.get("published"), 
                    stats.get("failed"), stats.get("total"));
        } catch (Exception e) {
            log.error("[OUTBOX_PUBLISHER] Error logging statistics: {}", e.getMessage(), e);
        }
    }
}
