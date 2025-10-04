package com.recrutech.common.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Provides common fields like event ID, timestamp, and event type.
 */
@Getter
public abstract class BaseEvent {

    private final String eventId;
    private final LocalDateTime occurredAt;
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
