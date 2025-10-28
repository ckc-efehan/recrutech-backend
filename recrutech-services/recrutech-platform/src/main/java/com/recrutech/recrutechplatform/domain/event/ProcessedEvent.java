package com.recrutech.recrutechplatform.domain.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for tracking processed events to ensure idempotent event handling.
 * Prevents duplicate processing of events during replays or retries.
 * 
 * <p>The eventId from the domain event serves as the idempotency key.
 * Before processing an event, consumers check if the eventId exists in this table.
 * If it exists, the event is skipped to prevent duplicate processing.</p>
 */
@Entity
@Table(name = "processed_event", indexes = {
    @Index(name = "idx_processed_event_id", columnList = "eventId", unique = true),
    @Index(name = "idx_processed_event_type_time", columnList = "eventType,processedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique event identifier (idempotency key).
     * Same as the eventId from the domain event.
     */
    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    /**
     * Type of the event (e.g., USER_REGISTERED, EMAIL_VERIFIED).
     */
    @Column(nullable = false, length = 100)
    private String eventType;

    /**
     * When the event was processed.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();

    /**
     * The account ID or entity ID related to the event.
     * Useful for debugging and cleanup operations.
     */
    @Column(length = 100)
    private String relatedEntityId;

    /**
     * Status of event processing.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProcessingStatus status = ProcessingStatus.PROCESSED;

    /**
     * Error message if processing failed.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of processing attempts (for failed events).
     */
    @Builder.Default
    private Integer attempts = 1;

    /**
     * Processing status.
     */
    public enum ProcessingStatus {
        /**
         * Event was successfully processed.
         */
        PROCESSED,
        
        /**
         * Event processing failed.
         */
        FAILED,
        
        /**
         * Event was skipped (duplicate).
         */
        SKIPPED
    }
}
