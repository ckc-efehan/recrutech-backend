package com.recrutech.recrutechauth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbox Pattern entity for reliable event publishing.
 * Events are first stored in this table within the same transaction as the business operation,
 * then asynchronously published to Kafka by a background scheduler.
 * 
 * <p>This ensures that events are not lost even if Kafka is temporarily unavailable,
 * and provides transactional guarantees between database changes and event publishing.</p>
 * 
 * <p>The eventId serves as the idempotency key for consumers to detect and ignore
 * duplicate events during replays or retries.</p>
 */
@Entity
@Table(name = "event_outbox", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status,createdAt"),
    @Index(name = "idx_outbox_event_id", columnList = "eventId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique event identifier (idempotency key).
     * Same as the eventId in the domain event.
     */
    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    /**
     * Type of the event (e.g., USER_REGISTERED, EMAIL_VERIFIED).
     */
    @Column(nullable = false, length = 100)
    private String eventType;

    /**
     * Kafka topic to publish the event to.
     */
    @Column(nullable = false, length = 200)
    private String topic;

    /**
     * Partition key for Kafka (typically accountId or entityId).
     */
    @Column(length = 100)
    private String partitionKey;

    /**
     * Serialized event payload (JSON).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Current status of the event.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    /**
     * When the event was created in the outbox.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * When the event was successfully published.
     */
    private LocalDateTime publishedAt;

    /**
     * Number of publish attempts.
     */
    @Builder.Default
    private Integer attempts = 0;

    /**
     * Last error message if publishing failed.
     */
    @Column(columnDefinition = "TEXT")
    private String lastError;

    /**
     * When the last publish attempt was made.
     */
    private LocalDateTime lastAttemptAt;

    /**
     * Marks the event as being processed to prevent concurrent publishing.
     */
    public void markAsProcessing() {
        this.status = OutboxStatus.PROCESSING;
        this.attempts++;
        this.lastAttemptAt = LocalDateTime.now();
    }

    /**
     * Marks the event as successfully published.
     */
    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * Marks the event as failed with an error message.
     */
    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.lastError = errorMessage;
    }

    /**
     * Resets status to PENDING for retry.
     */
    public void resetForRetry() {
        this.status = OutboxStatus.PENDING;
    }

    /**
     * Status of event in the outbox.
     */
    public enum OutboxStatus {
        /**
         * Event is waiting to be published.
         */
        PENDING,
        
        /**
         * Event is currently being processed/published.
         */
        PROCESSING,
        
        /**
         * Event was successfully published to Kafka.
         */
        PUBLISHED,
        
        /**
         * Event publishing failed after max retries.
         */
        FAILED
    }
}
