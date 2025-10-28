package com.recrutech.common.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a user account is disabled in the auth service.
 * Platform service consumes this event to deactivate related domain entities
 * and revoke access to resources.
 * 
 * This event supports the separation of identity (auth) from domain (platform).
 */
@Getter
public class AccountDisabledEvent extends BaseEvent {

    private final String accountId;
    private final String reason;
    private final LocalDateTime disabledAt;
    private final String disabledBy; // accountId of admin who disabled the account, or SYSTEM

    public AccountDisabledEvent(
            String accountId,
            String reason,
            LocalDateTime disabledAt,
            String disabledBy) {
        super("ACCOUNT_DISABLED");
        this.accountId = accountId;
        this.reason = reason;
        this.disabledAt = disabledAt;
        this.disabledBy = disabledBy;
    }

    @Override
    public String toString() {
        return "AccountDisabledEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", reason='" + reason + '\'' +
                ", disabledAt=" + disabledAt +
                ", disabledBy='" + disabledBy + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
