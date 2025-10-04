package com.recrutech.recrutechnotification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recrutech.recrutechnotification.dto.EmailVerificationEvent;
import com.recrutech.recrutechnotification.dto.WelcomeEmailEvent;
import com.recrutech.recrutechnotification.dto.PasswordResetEvent;
import com.recrutech.recrutechnotification.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing email events.
 * Handles both email verification and welcome email events with proper error handling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Consumes email verification events from Kafka topic.
     * @param message JSON message containing email verification event
     * @param partition Kafka partition
     * @param offset Message offset
     * @param acknowledgment Manual acknowledgment
     */
    @KafkaListener(
        topics = "${recrutech.kafka.topics.email-verification:email-verification}",
        groupId = "${spring.kafka.consumer.group-id:recrutech-notification}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleEmailVerificationEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("[DEBUG_LOG] Received email verification event from partition: {}, offset: {}", partition, offset);
        
        // Handle null or empty messages gracefully
        if (message == null || message.trim().isEmpty()) {
            log.warn("[DEBUG_LOG] Received null or empty message from partition: {}, offset: {}. Skipping processing.", 
                    partition, offset);
            return;
        }
        
        try {
            // Parse the JSON message
            EmailVerificationEvent event = objectMapper.readValue(message, EmailVerificationEvent.class);
            
            log.info("[DEBUG_LOG] Processing email verification event for user: {}", event.getEmail());
            
            // Validate the event
            emailService.validateEmailEvent(event);
            
            // Check if token is not expired
            if (event.isTokenExpired()) {
                log.warn("[DEBUG_LOG] Email verification token is expired for user: {}. Skipping email send.", 
                        event.getEmail());
                acknowledgment.acknowledge();
                return;
            }
            
            // Send the verification email
            emailService.sendVerificationEmail(event);
            
            log.info("[DEBUG_LOG] Email verification email sent successfully for user: {}", event.getEmail());
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
        } catch (JsonProcessingException e) {
            log.error("[DEBUG_LOG] Failed to parse email verification event from partition: {}, offset: {}. Error: {}", 
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
            
        } catch (MessagingException e) {
            log.error("[DEBUG_LOG] Failed to send verification email for event from partition: {}, offset: {}. Error: {}", 
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
            
        } catch (Exception e) {
            log.error("[DEBUG_LOG] Unexpected error processing email verification event from partition: {}, offset: {}. Error: {}", 
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
        }
    }

    /**
     * Consumes welcome email events from Kafka topic.
     * @param message JSON message containing welcome email event
     * @param partition Kafka partition
     * @param offset Message offset
     * @param acknowledgment Manual acknowledgment
     */
    @KafkaListener(
        topics = "${recrutech.kafka.topics.welcome-email:welcome-email}",
        groupId = "${spring.kafka.consumer.group-id:recrutech-notification}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWelcomeEmailEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("[DEBUG_LOG] Received welcome email event from partition: {}, offset: {}", partition, offset);
        
        // Handle null or empty messages gracefully
        if (message == null || message.trim().isEmpty()) {
            log.warn("[DEBUG_LOG] Received null or empty message from partition: {}, offset: {}. Skipping processing.", 
                    partition, offset);
            return;
        }
        
        try {
            // Parse the JSON message
            WelcomeEmailEvent event = objectMapper.readValue(message, WelcomeEmailEvent.class);
            
            log.info("[DEBUG_LOG] Processing welcome email event for user: {} with role: {}", 
                    event.getEmail(), event.getUserRole());
            
            // Validate the event
            emailService.validateEmailEvent(event);
            
            // Send the welcome email
            emailService.sendWelcomeEmail(event);
            
            log.info("[DEBUG_LOG] Welcome email sent successfully for user: {} with role: {}", 
                    event.getEmail(), event.getDisplayRoleName());
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
        } catch (JsonProcessingException e) {
            log.error("[DEBUG_LOG] Failed to parse welcome email event from partition: {}, offset: {}. Error: {}", 
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
            
        } catch (MessagingException e) {
            log.error("[DEBUG_LOG] Failed to send welcome email for event from partition: {}, offset: {}. Error: {}", 
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
            
        } catch (Exception e) {
            log.error("[DEBUG_LOG] Unexpected error processing welcome email event from partition: {}, offset: {}. Error: {}", 
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
        }
    }

    /**
     * Consumes password reset email events from Kafka topic.
     */
    @KafkaListener(
        topics = "${recrutech.kafka.topics.password-reset:password-reset}",
        groupId = "${spring.kafka.consumer.group-id:recrutech-notification}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePasswordResetEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("[DEBUG_LOG] Received password reset event from partition: {}, offset: {}", partition, offset);

        if (message == null || message.trim().isEmpty()) {
            log.warn("[DEBUG_LOG] Received null or empty password reset message from partition: {}, offset: {}. Skipping.",
                    partition, offset);
            return;
        }

        try {
            PasswordResetEvent event = objectMapper.readValue(message, PasswordResetEvent.class);
            log.info("[DEBUG_LOG] Processing password reset event for user: {}", event.getEmail());

            emailService.validateEmailEvent(event);

            if (event.isTokenExpired()) {
                log.warn("[DEBUG_LOG] Password reset token expired for user: {}. Skipping email send.", event.getEmail());
                acknowledgment.acknowledge();
                return;
            }

            emailService.sendPasswordResetEmail(event);
            log.info("[DEBUG_LOG] Password reset email sent successfully for user: {}", event.getEmail());

            acknowledgment.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("[DEBUG_LOG] Failed to parse password reset event from partition: {}, offset: {}. Error: {}",
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
        } catch (MessagingException e) {
            log.error("[DEBUG_LOG] Failed to send password reset email for event from partition: {}, offset: {}. Error: {}",
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
        } catch (Exception e) {
            log.error("[DEBUG_LOG] Unexpected error processing password reset event from partition: {}, offset: {}. Error: {}",
                     partition, offset, e.getMessage(), e);
            // Don't acknowledge - will be retried
        }
    }
}