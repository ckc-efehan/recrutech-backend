package com.recrutech.common.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Provides common fields like event ID, timestamp, and event type.
 * 
 * <p>The eventId serves as the idempotency key for event processing.
 * Consumers should track processed eventIds to ensure idempotent handling
 * and prevent duplicate processing during replays or retries.</p>
 * 
 * <p>This class is designed to work with the Outbox Pattern for reliable
 * event publishing with transactional guarantees.</p>
 */
@Getter
public abstract class BaseEvent {

    /**
     * Unique identifier for this event instance.
     * Used as the idempotency key to ensure events are processed exactly once.
     */
    private final String eventId;
    
    /**
     * Timestamp when the event occurred.
     */
    private final LocalDateTime occurredAt;
    
    /**
     * Type of the event (e.g., USER_REGISTERED, EMAIL_VERIFIED).
     */
    private final String eventType;

    protected BaseEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "BaseEvent{" +
                "eventId='" + eventId + '\'' +
                ", occurredAt=" + occurredAt +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
