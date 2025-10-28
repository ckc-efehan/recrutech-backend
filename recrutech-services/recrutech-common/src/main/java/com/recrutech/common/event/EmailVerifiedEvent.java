package com.recrutech.common.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a user's email is verified in the auth service.
 * Platform service can consume this event to update user profile status
 * or trigger welcome workflows.
 * 
 * This event supports the separation of identity (auth) from domain (platform).
 */
@Getter
public class EmailVerifiedEvent extends BaseEvent {

    private final String accountId;
    private final String email;
    private final LocalDateTime verifiedAt;

    public EmailVerifiedEvent(
            String accountId,
            String email,
            LocalDateTime verifiedAt) {
        super("EMAIL_VERIFIED");
        this.accountId = accountId;
        this.email = email;
        this.verifiedAt = verifiedAt;
    }

    @Override
    public String toString() {
        return "EmailVerifiedEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", email='" + email + '\'' +
                ", verifiedAt=" + verifiedAt +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
