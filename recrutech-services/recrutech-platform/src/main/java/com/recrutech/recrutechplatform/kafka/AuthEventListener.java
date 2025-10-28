package com.recrutech.recrutechplatform.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recrutech.common.event.*;
import com.recrutech.recrutechplatform.domain.applicant.Applicant;
import com.recrutech.recrutechplatform.domain.applicant.ApplicantRepository;
import com.recrutech.recrutechplatform.domain.company.Company;
import com.recrutech.recrutechplatform.domain.company.CompanyRepository;
import com.recrutech.recrutechplatform.domain.event.ProcessedEvent;
import com.recrutech.recrutechplatform.domain.event.ProcessedEventRepository;
import com.recrutech.recrutechplatform.domain.hremployee.HREmployee;
import com.recrutech.recrutechplatform.domain.hremployee.HREmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Kafka listener for consuming auth domain events.
 * Handles events from the auth service with idempotent processing.
 * 
 * <p>This service ensures that events are processed exactly once by tracking
 * processed event IDs. It creates and updates domain entities (Applicant, Company, HREmployee)
 * based on auth events, maintaining separation between identity (auth) and domain (platform).</p>
 * 
 * <p>Each handler follows the pattern:
 * 1. Check if event was already processed (idempotency)
 * 2. Process the event (create/update domain entities)
 * 3. Record the event as processed
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventListener {

    private final ProcessedEventRepository processedEventRepository;
    private final ApplicantRepository applicantRepository;
    private final CompanyRepository companyRepository;
    private final HREmployeeRepository hrEmployeeRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Handles UserRegisteredEvent from auth service.
     * Creates corresponding domain entity based on user role.
     * 
     * @param eventJson The event as JSON string
     */
    @KafkaListener(topics = "${recrutech.kafka.topics.user-registered:auth.user.registered}", 
                   groupId = "${recrutech.kafka.group-id:platform-service}")
    @Transactional
    public void handleUserRegistered(String eventJson) {
        try {
            UserRegisteredEvent event = objectMapper.readValue(eventJson, UserRegisteredEvent.class);
            
            log.info("[EVENT_CONSUMER] Received UserRegisteredEvent: eventId={}, accountId={}, role={}", 
                    event.getEventId(), event.getAccountId(), event.getRole());
            
            // Check idempotency
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.warn("[EVENT_CONSUMER] Event already processed (idempotency): eventId={}", 
                        event.getEventId());
                return;
            }
            
            // Process event based on role
            switch (event.getRole()) {
                case "APPLICANT":
                    handleApplicantRegistration(event);
                    break;
                case "COMPANY_ADMIN":
                    handleCompanyAdminRegistration(event);
                    break;
                case "HR":
                    handleHRRegistration(event);
                    break;
                default:
                    log.warn("[EVENT_CONSUMER] Unknown role in UserRegisteredEvent: {}", event.getRole());
            }
            
            // Record as processed
            recordProcessedEvent(event.getEventId(), event.getEventType(), event.getAccountId());
            
            log.info("[EVENT_CONSUMER] UserRegisteredEvent processed successfully: eventId={}", 
                    event.getEventId());
            
        } catch (Exception e) {
            log.error("[EVENT_CONSUMER] Error processing UserRegisteredEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process UserRegisteredEvent", e);
        }
    }

    /**
     * Creates an Applicant entity from user registration.
     */
    private void handleApplicantRegistration(UserRegisteredEvent event) {
        // Check if applicant already exists
        Optional<Applicant> existingApplicant = applicantRepository.findByAccountId(event.getAccountId());
        if (existingApplicant.isPresent()) {
            log.warn("[EVENT_CONSUMER] Applicant already exists for accountId: {}", event.getAccountId());
            return;
        }
        
        Applicant applicant = new Applicant();
        applicant.setAccountId(event.getAccountId());
        applicant.setEmail(event.getEmail());
        applicant.setFirstName(event.getFirstName());
        applicant.setLastName(event.getLastName());
        applicant.setCreatedAt(event.getRegisteredAt());
        applicant.setUpdatedAt(LocalDateTime.now());
        
        applicantRepository.save(applicant);
        
        log.info("[EVENT_CONSUMER] Created Applicant: accountId={}, email={}", 
                event.getAccountId(), event.getEmail());
    }

    /**
     * Creates a Company entity from company admin registration.
     */
    private void handleCompanyAdminRegistration(UserRegisteredEvent event) {
        // Check if company already exists
        Optional<Company> existingCompany = companyRepository.findByAccountId(event.getAccountId());
        if (existingCompany.isPresent()) {
            log.warn("[EVENT_CONSUMER] Company already exists for accountId: {}", event.getAccountId());
            return;
        }
        
        Company company = new Company();
        company.setAccountId(event.getAccountId());
        company.setContactEmail(event.getEmail());
        company.setCreatedAt(event.getRegisteredAt());
        company.setUpdatedAt(LocalDateTime.now());
        
        // Parse registration context for additional company data if provided
        // registrationContext might contain companyName, industry, etc.
        
        companyRepository.save(company);
        
        log.info("[EVENT_CONSUMER] Created Company: accountId={}, email={}", 
                event.getAccountId(), event.getEmail());
    }

    /**
     * Creates an HREmployee entity from HR registration.
     */
    private void handleHRRegistration(UserRegisteredEvent event) {
        // Check if HR employee already exists
        Optional<HREmployee> existingHR = hrEmployeeRepository.findByAccountId(event.getAccountId());
        if (existingHR.isPresent()) {
            log.warn("[EVENT_CONSUMER] HREmployee already exists for accountId: {}", event.getAccountId());
            return;
        }
        
        HREmployee hrEmployee = new HREmployee();
        hrEmployee.setAccountId(event.getAccountId());
        hrEmployee.setEmail(event.getEmail());
        hrEmployee.setFirstName(event.getFirstName());
        hrEmployee.setLastName(event.getLastName());
        hrEmployee.setCreatedAt(event.getRegisteredAt());
        hrEmployee.setUpdatedAt(LocalDateTime.now());
        
        // Parse registration context for companyId if provided
        // registrationContext might contain the companyId this HR belongs to
        
        hrEmployeeRepository.save(hrEmployee);
        
        log.info("[EVENT_CONSUMER] Created HREmployee: accountId={}, email={}", 
                event.getAccountId(), event.getEmail());
    }

    /**
     * Handles EmailVerifiedEvent from auth service.
     * Updates domain entities to reflect verified email status.
     * 
     * @param eventJson The event as JSON string
     */
    @KafkaListener(topics = "${recrutech.kafka.topics.email-verified:auth.email.verified}", 
                   groupId = "${recrutech.kafka.group-id:platform-service}")
    @Transactional
    public void handleEmailVerified(String eventJson) {
        try {
            EmailVerifiedEvent event = objectMapper.readValue(eventJson, EmailVerifiedEvent.class);
            
            log.info("[EVENT_CONSUMER] Received EmailVerifiedEvent: eventId={}, accountId={}", 
                    event.getEventId(), event.getAccountId());
            
            // Check idempotency
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.warn("[EVENT_CONSUMER] Event already processed (idempotency): eventId={}", 
                        event.getEventId());
                return;
            }
            
            // Update domain entities - mark email as verified
            updateEmailVerificationStatus(event.getAccountId());
            
            // Record as processed
            recordProcessedEvent(event.getEventId(), event.getEventType(), event.getAccountId());
            
            log.info("[EVENT_CONSUMER] EmailVerifiedEvent processed successfully: eventId={}", 
                    event.getEventId());
            
        } catch (Exception e) {
            log.error("[EVENT_CONSUMER] Error processing EmailVerifiedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process EmailVerifiedEvent", e);
        }
    }

    /**
     * Updates email verification status for domain entities.
     */
    private void updateEmailVerificationStatus(String accountId) {
        // Try to find and update in each entity type
        applicantRepository.findByAccountId(accountId).ifPresent(applicant -> {
            applicant.setEmailVerified(true);
            applicant.setUpdatedAt(LocalDateTime.now());
            applicantRepository.save(applicant);
            log.info("[EVENT_CONSUMER] Updated Applicant email verification: accountId={}", accountId);
        });
        
        companyRepository.findByAccountId(accountId).ifPresent(company -> {
            company.setEmailVerified(true);
            company.setUpdatedAt(LocalDateTime.now());
            companyRepository.save(company);
            log.info("[EVENT_CONSUMER] Updated Company email verification: accountId={}", accountId);
        });
        
        hrEmployeeRepository.findByAccountId(accountId).ifPresent(hrEmployee -> {
            hrEmployee.setEmailVerified(true);
            hrEmployee.setUpdatedAt(LocalDateTime.now());
            hrEmployeeRepository.save(hrEmployee);
            log.info("[EVENT_CONSUMER] Updated HREmployee email verification: accountId={}", accountId);
        });
    }

    /**
     * Handles RoleChangedEvent from auth service.
     * Updates domain entity access and permissions.
     * 
     * @param eventJson The event as JSON string
     */
    @KafkaListener(topics = "${recrutech.kafka.topics.role-changed:auth.role.changed}", 
                   groupId = "${recrutech.kafka.group-id:platform-service}")
    @Transactional
    public void handleRoleChanged(String eventJson) {
        try {
            RoleChangedEvent event = objectMapper.readValue(eventJson, RoleChangedEvent.class);
            
            log.info("[EVENT_CONSUMER] Received RoleChangedEvent: eventId={}, accountId={}, oldRole={}, newRole={}", 
                    event.getEventId(), event.getAccountId(), event.getOldRole(), event.getNewRole());
            
            // Check idempotency
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.warn("[EVENT_CONSUMER] Event already processed (idempotency): eventId={}", 
                        event.getEventId());
                return;
            }
            
            // Handle role change - may need to migrate entities between types
            log.info("[EVENT_CONSUMER] Role changed for accountId {}: {} -> {}", 
                    event.getAccountId(), event.getOldRole(), event.getNewRole());
            
            // Record as processed
            recordProcessedEvent(event.getEventId(), event.getEventType(), event.getAccountId());
            
            log.info("[EVENT_CONSUMER] RoleChangedEvent processed successfully: eventId={}", 
                    event.getEventId());
            
        } catch (Exception e) {
            log.error("[EVENT_CONSUMER] Error processing RoleChangedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process RoleChangedEvent", e);
        }
    }

    /**
     * Handles AccountDisabledEvent from auth service.
     * Deactivates domain entities and performs cleanup.
     * 
     * @param eventJson The event as JSON string
     */
    @KafkaListener(topics = "${recrutech.kafka.topics.account-disabled:auth.account.disabled}", 
                   groupId = "${recrutech.kafka.group-id:platform-service}")
    @Transactional
    public void handleAccountDisabled(String eventJson) {
        try {
            AccountDisabledEvent event = objectMapper.readValue(eventJson, AccountDisabledEvent.class);
            
            log.info("[EVENT_CONSUMER] Received AccountDisabledEvent: eventId={}, accountId={}, reason={}", 
                    event.getEventId(), event.getAccountId(), event.getReason());
            
            // Check idempotency
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.warn("[EVENT_CONSUMER] Event already processed (idempotency): eventId={}", 
                        event.getEventId());
                return;
            }
            
            // Deactivate/disable domain entities
            deactivateDomainEntities(event.getAccountId(), event.getReason());
            
            // Record as processed
            recordProcessedEvent(event.getEventId(), event.getEventType(), event.getAccountId());
            
            log.info("[EVENT_CONSUMER] AccountDisabledEvent processed successfully: eventId={}", 
                    event.getEventId());
            
        } catch (Exception e) {
            log.error("[EVENT_CONSUMER] Error processing AccountDisabledEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process AccountDisabledEvent", e);
        }
    }

    /**
     * Deactivates domain entities when account is disabled.
     * Cleanup action as per Phase 5 requirements.
     */
    private void deactivateDomainEntities(String accountId, String reason) {
        // Deactivate Applicant
        applicantRepository.findByAccountId(accountId).ifPresent(applicant -> {
            applicant.setActive(false);
            applicant.setUpdatedAt(LocalDateTime.now());
            applicantRepository.save(applicant);
            log.info("[EVENT_CONSUMER] Deactivated Applicant: accountId={}, reason={}", accountId, reason);
        });
        
        // Deactivate Company
        companyRepository.findByAccountId(accountId).ifPresent(company -> {
            company.setActive(false);
            company.setUpdatedAt(LocalDateTime.now());
            companyRepository.save(company);
            log.info("[EVENT_CONSUMER] Deactivated Company: accountId={}, reason={}", accountId, reason);
        });
        
        // Deactivate HREmployee
        hrEmployeeRepository.findByAccountId(accountId).ifPresent(hrEmployee -> {
            hrEmployee.setActive(false);
            hrEmployee.setUpdatedAt(LocalDateTime.now());
            hrEmployeeRepository.save(hrEmployee);
            log.info("[EVENT_CONSUMER] Deactivated HREmployee: accountId={}, reason={}", accountId, reason);
        });
    }

    /**
     * Records an event as processed for idempotency.
     */
    private void recordProcessedEvent(String eventId, String eventType, String relatedEntityId) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .relatedEntityId(relatedEntityId)
                .processedAt(LocalDateTime.now())
                .status(ProcessedEvent.ProcessingStatus.PROCESSED)
                .attempts(1)
                .build();
        
        processedEventRepository.save(processedEvent);
        
        log.debug("[EVENT_CONSUMER] Recorded processed event: eventId={}, eventType={}", 
                eventId, eventType);
    }
}
