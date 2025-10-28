package com.recrutech.common.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a user's role is changed in the auth service.
 * Platform service consumes this event to update domain entity permissions
 * and access controls.
 * 
 * This event supports the separation of identity (auth) from domain (platform).
 */
@Getter
public class RoleChangedEvent extends BaseEvent {

    private final String accountId;
    private final String oldRole;
    private final String newRole;
    private final LocalDateTime changedAt;
    private final String changedBy; // accountId of admin who made the change

    public RoleChangedEvent(
            String accountId,
            String oldRole,
            String newRole,
            LocalDateTime changedAt,
            String changedBy) {
        super("ROLE_CHANGED");
        this.accountId = accountId;
        this.oldRole = oldRole;
        this.newRole = newRole;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    @Override
    public String toString() {
        return "RoleChangedEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", oldRole='" + oldRole + '\'' +
                ", newRole='" + newRole + '\'' +
                ", changedAt=" + changedAt +
                ", changedBy='" + changedBy + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
