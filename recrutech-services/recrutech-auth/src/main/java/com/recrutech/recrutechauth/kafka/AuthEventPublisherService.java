package com.recrutech.recrutechauth.kafka;

import com.recrutech.common.event.*;
import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for publishing auth domain events using the Outbox Pattern.
 * Events are stored transactionally in the outbox table, then asynchronously
 * published to Kafka by the OutboxPublisher scheduler.
 * 
 * <p>This ensures transactional guarantees: events are never lost even if
 * Kafka is unavailable, and events are published if and only if the
 * business transaction commits successfully.</p>
 * 
 * Events published:
 * - UserRegisteredEvent: When a new user account is created
 * - EmailVerifiedEvent: When a user verifies their email
 * - RoleChangedEvent: When a user's role changes
 * - AccountDisabledEvent: When an account is disabled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventPublisherService {

    private final OutboxService outboxService;

    @Value("${recrutech.kafka.topics.user-registered:auth.user.registered}")
    private String userRegisteredTopic;

    @Value("${recrutech.kafka.topics.email-verified:auth.email.verified}")
    private String emailVerifiedTopic;

    @Value("${recrutech.kafka.topics.role-changed:auth.role.changed}")
    private String roleChangedTopic;

    @Value("${recrutech.kafka.topics.account-disabled:auth.account.disabled}")
    private String accountDisabledTopic;

    /**
     * Publishes a UserRegisteredEvent when a new user account is created.
     * Platform service consumes this to create corresponding domain entities.
     * 
     * <p>Uses the Outbox Pattern for reliable, transactional event publishing.</p>
     * 
     * @param user The newly registered user
     * @param registrationContext Additional context (e.g., companyId for HR, company data for admin)
     */
    @Transactional
    public void publishUserRegisteredEvent(User user, String registrationContext) {
        log.info("[AUTH_EVENT] Storing UserRegisteredEvent in outbox for accountId: {}, role: {}", 
                user.getId(), user.getRole());
        
        UserRegisteredEvent event = new UserRegisteredEvent(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().name(),
            user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now(),
            registrationContext
        );

        outboxService.storeEvent(event, userRegisteredTopic, user.getId());
        
        log.info("[AUTH_EVENT] UserRegisteredEvent stored in outbox for accountId: {}, eventId: {}", 
                user.getId(), event.getEventId());
    }

    /**
     * Publishes an EmailVerifiedEvent when a user verifies their email.
     * 
     * <p>Uses the Outbox Pattern for reliable, transactional event publishing.</p>
     * 
     * @param user The user who verified their email
     */
    @Transactional
    public void publishEmailVerifiedEvent(User user) {
        log.info("[AUTH_EVENT] Storing EmailVerifiedEvent in outbox for accountId: {}", user.getId());
        
        EmailVerifiedEvent event = new EmailVerifiedEvent(
            user.getId(),
            user.getEmail(),
            LocalDateTime.now()
        );

        outboxService.storeEvent(event, emailVerifiedTopic, user.getId());
        
        log.info("[AUTH_EVENT] EmailVerifiedEvent stored in outbox for accountId: {}, eventId: {}", 
                user.getId(), event.getEventId());
    }

    /**
     * Publishes a RoleChangedEvent when a user's role is changed.
     * 
     * <p>Uses the Outbox Pattern for reliable, transactional event publishing.</p>
     * 
     * @param accountId The account ID
     * @param oldRole The previous role
     * @param newRole The new role
     * @param changedBy The accountId of who made the change (or SYSTEM)
     */
    @Transactional
    public void publishRoleChangedEvent(String accountId, String oldRole, String newRole, String changedBy) {
        log.info("[AUTH_EVENT] Storing RoleChangedEvent in outbox for accountId: {} from {} to {}", 
                accountId, oldRole, newRole);
        
        RoleChangedEvent event = new RoleChangedEvent(
            accountId,
            oldRole,
            newRole,
            LocalDateTime.now(),
            changedBy
        );

        outboxService.storeEvent(event, roleChangedTopic, accountId);
        
        log.info("[AUTH_EVENT] RoleChangedEvent stored in outbox for accountId: {}, eventId: {}", 
                accountId, event.getEventId());
    }

    /**
     * Publishes an AccountDisabledEvent when an account is disabled.
     * 
     * <p>Uses the Outbox Pattern for reliable, transactional event publishing.</p>
     * 
     * @param accountId The account ID
     * @param reason The reason for disabling
     * @param disabledBy The accountId of who disabled it (or SYSTEM)
     */
    @Transactional
    public void publishAccountDisabledEvent(String accountId, String reason, String disabledBy) {
        log.info("[AUTH_EVENT] Storing AccountDisabledEvent in outbox for accountId: {}, reason: {}", 
                accountId, reason);
        
        AccountDisabledEvent event = new AccountDisabledEvent(
            accountId,
            reason,
            LocalDateTime.now(),
            disabledBy
        );

        outboxService.storeEvent(event, accountDisabledTopic, accountId);
        
        log.info("[AUTH_EVENT] AccountDisabledEvent stored in outbox for accountId: {}, eventId: {}", 
                accountId, event.getEventId());
    }
}
