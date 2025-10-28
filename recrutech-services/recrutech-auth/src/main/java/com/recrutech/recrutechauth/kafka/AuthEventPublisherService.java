package com.recrutech.recrutechauth.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recrutech.common.event.*;
import com.recrutech.recrutechauth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer service for publishing auth domain events.
 * Publishes events that platform service consumes to maintain domain entities.
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

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
     * @param user The newly registered user
     * @param registrationContext Additional context (e.g., companyId for HR, company data for admin)
     */
    public void publishUserRegisteredEvent(User user, String registrationContext) {
        log.info("[AUTH_EVENT] Publishing UserRegisteredEvent for accountId: {}, role: {}", 
                user.getId(), user.getRole());
        
        try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now(),
                registrationContext
            );

            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                userRegisteredTopic,
                user.getId(), // Use accountId as partition key
                eventJson
            );
            
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[AUTH_EVENT] UserRegisteredEvent published successfully for accountId: {}", 
                                user.getId());
                    } else {
                        log.error("[AUTH_EVENT] Failed to publish UserRegisteredEvent for accountId: {}. Error: {}", 
                                 user.getId(), ex.getMessage(), ex);
                    }
                });
            }

        } catch (JsonProcessingException e) {
            log.error("[AUTH_EVENT] Failed to serialize UserRegisteredEvent for accountId: {}. Error: {}", 
                     user.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes an EmailVerifiedEvent when a user verifies their email.
     * 
     * @param user The user who verified their email
     */
    public void publishEmailVerifiedEvent(User user) {
        log.info("[AUTH_EVENT] Publishing EmailVerifiedEvent for accountId: {}", user.getId());
        
        try {
            EmailVerifiedEvent event = new EmailVerifiedEvent(
                user.getId(),
                user.getEmail(),
                LocalDateTime.now()
            );

            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                emailVerifiedTopic,
                user.getId(),
                eventJson
            );
            
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[AUTH_EVENT] EmailVerifiedEvent published successfully for accountId: {}", 
                                user.getId());
                    } else {
                        log.error("[AUTH_EVENT] Failed to publish EmailVerifiedEvent for accountId: {}. Error: {}", 
                                 user.getId(), ex.getMessage(), ex);
                    }
                });
            }

        } catch (JsonProcessingException e) {
            log.error("[AUTH_EVENT] Failed to serialize EmailVerifiedEvent for accountId: {}. Error: {}", 
                     user.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a RoleChangedEvent when a user's role is changed.
     * 
     * @param accountId The account ID
     * @param oldRole The previous role
     * @param newRole The new role
     * @param changedBy The accountId of who made the change (or SYSTEM)
     */
    public void publishRoleChangedEvent(String accountId, String oldRole, String newRole, String changedBy) {
        log.info("[AUTH_EVENT] Publishing RoleChangedEvent for accountId: {} from {} to {}", 
                accountId, oldRole, newRole);
        
        try {
            RoleChangedEvent event = new RoleChangedEvent(
                accountId,
                oldRole,
                newRole,
                LocalDateTime.now(),
                changedBy
            );

            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                roleChangedTopic,
                accountId,
                eventJson
            );
            
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[AUTH_EVENT] RoleChangedEvent published successfully for accountId: {}", 
                                accountId);
                    } else {
                        log.error("[AUTH_EVENT] Failed to publish RoleChangedEvent for accountId: {}. Error: {}", 
                                 accountId, ex.getMessage(), ex);
                    }
                });
            }

        } catch (JsonProcessingException e) {
            log.error("[AUTH_EVENT] Failed to serialize RoleChangedEvent for accountId: {}. Error: {}", 
                     accountId, e.getMessage(), e);
        }
    }

    /**
     * Publishes an AccountDisabledEvent when an account is disabled.
     * 
     * @param accountId The account ID
     * @param reason The reason for disabling
     * @param disabledBy The accountId of who disabled it (or SYSTEM)
     */
    public void publishAccountDisabledEvent(String accountId, String reason, String disabledBy) {
        log.info("[AUTH_EVENT] Publishing AccountDisabledEvent for accountId: {}, reason: {}", 
                accountId, reason);
        
        try {
            AccountDisabledEvent event = new AccountDisabledEvent(
                accountId,
                reason,
                LocalDateTime.now(),
                disabledBy
            );

            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                accountDisabledTopic,
                accountId,
                eventJson
            );
            
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[AUTH_EVENT] AccountDisabledEvent published successfully for accountId: {}", 
                                accountId);
                    } else {
                        log.error("[AUTH_EVENT] Failed to publish AccountDisabledEvent for accountId: {}. Error: {}", 
                                 accountId, ex.getMessage(), ex);
                    }
                });
            }

        } catch (JsonProcessingException e) {
            log.error("[AUTH_EVENT] Failed to serialize AccountDisabledEvent for accountId: {}. Error: {}", 
                     accountId, e.getMessage(), e);
        }
    }
}
