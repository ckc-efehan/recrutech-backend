package com.recrutech.common.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when a new user is registered in the auth service.
 * Platform service consumes this event to create corresponding domain entities
 * (Applicant, Company, HREmployee) based on the user role.
 * 
 * This event supports the separation of identity (auth) from domain (platform).
 */
@Getter
public class UserRegisteredEvent extends BaseEvent {

    private final String accountId;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String role; // APPLICANT, COMPANY_ADMIN, HR
    private final LocalDateTime registeredAt;
    private final String registrationContext; // JSON with additional context (e.g., companyId for HR)

    public UserRegisteredEvent(
            String accountId,
            String email,
            String firstName,
            String lastName,
            String role,
            LocalDateTime registeredAt,
            String registrationContext) {
        super("USER_REGISTERED");
        this.accountId = accountId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.registeredAt = registeredAt;
        this.registrationContext = registrationContext;
    }

    @Override
    public String toString() {
        return "UserRegisteredEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role='" + role + '\'' +
                ", registeredAt=" + registeredAt +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
