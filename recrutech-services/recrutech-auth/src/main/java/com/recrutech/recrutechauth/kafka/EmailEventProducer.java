package com.recrutech.recrutechauth.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * Kafka producer service for publishing email events.
 * Handles both email verification and welcome email events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${recrutech.kafka.topics.email-verification:email-verification}")
    private String emailVerificationTopic;

    @Value("${recrutech.kafka.topics.welcome-email:welcome-email}")
    private String welcomeEmailTopic;

    @Value("${recrutech.kafka.topics.password-reset:password-reset}")
    private String passwordResetTopic;

    /**
     * Publishes an email verification event to Kafka.
     * @param user The user who needs email verification
     */
    public void publishEmailVerificationEvent(User user) {
        log.info("[DEBUG_LOG] Publishing email verification event for user: {}", user.getEmail());
        
        try {
            // Create email verification event with safe property access
            LocalDateTime registrationDate = null;
            try {
                registrationDate = user.getCreatedAt();
            } catch (RuntimeException e) {
                log.warn("[DEBUG_LOG] Failed to get createdAt for user: {}. Using current time. Error: {}", 
                        user.getEmail(), e.getMessage());
                registrationDate = LocalDateTime.now();
            }
            
            EmailVerificationEventDto event = EmailVerificationEventDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .verificationToken(user.getEmailVerificationToken())
                    .userRole(user.getRole().name())
                    .registrationDate(registrationDate)
                    .tokenExpiryDate(user.getEmailVerificationExpiry())
                    .build();

            // Convert to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Send to Kafka
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                emailVerificationTopic, 
                user.getEmail(), // Use email as key for partitioning
                eventJson
            );
            
            // Handle result asynchronously (handle null future gracefully)
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[DEBUG_LOG] Email verification event sent successfully for user: {} to partition: {}", 
                                user.getEmail(), result.getRecordMetadata().partition());
                    } else {
                        log.error("[DEBUG_LOG] Failed to send email verification event for user: {}. Error: {}", 
                                 user.getEmail(), ex.getMessage(), ex);
                    }
                });
            } else {
                log.warn("[DEBUG_LOG] Kafka send returned null future for user: {}", user.getEmail());
            }

        } catch (JsonProcessingException e) {
            log.error("[DEBUG_LOG] Failed to serialize email verification event for user: {}. Error: {}", 
                     user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a welcome email event to Kafka.
     * @param user The user who successfully verified their email
     */
    public void publishWelcomeEmailEvent(User user) {
        log.info("[DEBUG_LOG] Publishing welcome email event for user: {}", user.getEmail());
        
        try {
            // Create welcome email event with safe property access
            LocalDateTime registrationDate = null;
            try {
                registrationDate = user.getCreatedAt();
            } catch (RuntimeException e) {
                log.warn("[DEBUG_LOG] Failed to get createdAt for user: {}. Using current time. Error: {}", 
                        user.getEmail(), e.getMessage());
                registrationDate = LocalDateTime.now();
            }
            
            WelcomeEmailEventDto event = WelcomeEmailEventDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .userRole(user.getRole().name())
                    .registrationDate(registrationDate)
                    .verificationDate(LocalDateTime.now())
                    .build();

            // Convert to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Send to Kafka
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                welcomeEmailTopic, 
                user.getEmail(), // Use email as key for partitioning
                eventJson
            );
            
            // Handle result asynchronously (handle null future gracefully)
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[DEBUG_LOG] Welcome email event sent successfully for user: {} to partition: {}", 
                                user.getEmail(), result.getRecordMetadata().partition());
                    } else {
                        log.error("[DEBUG_LOG] Failed to send welcome email event for user: {}. Error: {}", 
                                 user.getEmail(), ex.getMessage(), ex);
                    }
                });
            } else {
                log.warn("[DEBUG_LOG] Kafka send returned null future for user: {}", user.getEmail());
            }

        } catch (JsonProcessingException e) {
            log.error("[DEBUG_LOG] Failed to serialize welcome email event for user: {}. Error: {}", 
                     user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a password reset email event to Kafka.
     * @param user The user who requested password reset
     * @param resetUrl The password reset URL with token
     */
    public void publishPasswordResetEvent(User user, String resetUrl) {
        log.info("[DEBUG_LOG] Publishing password reset email event for user: {}", user.getEmail());
        
        try {
            PasswordResetEventDto event = PasswordResetEventDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .resetUrl(resetUrl)
                    .resetToken(user.getPasswordResetToken())
                    .requestDate(LocalDateTime.now())
                    .expiryDate(user.getPasswordResetExpiry())
                    .build();

            // Convert to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Send to Kafka
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                passwordResetTopic, 
                user.getEmail(), // Use email as key for partitioning
                eventJson
            );
            
            // Handle result asynchronously (handle null future gracefully)
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[DEBUG_LOG] Password reset email event sent successfully for user: {} to partition: {}", 
                                user.getEmail(), result.getRecordMetadata().partition());
                    } else {
                        log.error("[DEBUG_LOG] Failed to send password reset email event for user: {}. Error: {}", 
                                 user.getEmail(), ex.getMessage(), ex);
                    }
                });
            } else {
                log.warn("[DEBUG_LOG] Kafka send returned null future for user: {}", user.getEmail());
            }

        } catch (JsonProcessingException e) {
            log.error("[DEBUG_LOG] Failed to serialize password reset email event for user: {}. Error: {}", 
                     user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * DTO for email verification events.
     * This mirrors the DTO in the notification service but is defined here 
     * to avoid cross-module dependencies.
     */
    public static class EmailVerificationEventDto {
        public String userId;
        public String email;
        public String firstName;
        public String lastName;
        public String verificationToken;
        public String userRole;
        public LocalDateTime registrationDate;
        @JsonProperty("tokenExpiryDate")
        public LocalDateTime tokenExpiryDate;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final EmailVerificationEventDto event = new EmailVerificationEventDto();

            public Builder userId(String userId) {
                event.userId = userId;
                return this;
            }

            public Builder email(String email) {
                event.email = email;
                return this;
            }

            public Builder firstName(String firstName) {
                event.firstName = firstName;
                return this;
            }

            public Builder lastName(String lastName) {
                event.lastName = lastName;
                return this;
            }

            public Builder verificationToken(String verificationToken) {
                event.verificationToken = verificationToken;
                return this;
            }

            public Builder userRole(String userRole) {
                event.userRole = userRole;
                return this;
            }

            public Builder registrationDate(LocalDateTime registrationDate) {
                event.registrationDate = registrationDate;
                return this;
            }

            public Builder tokenExpiryDate(LocalDateTime tokenExpiryDate) {
                event.tokenExpiryDate = tokenExpiryDate;
                return this;
            }

            public EmailVerificationEventDto build() {
                return event;
            }
        }
    }

    /**
     * DTO for welcome email events.
     * This mirrors the DTO in the notification service but is defined here 
     * to avoid cross-module dependencies.
     */
    public static class WelcomeEmailEventDto {
        public String userId;
        public String email;
        public String firstName;
        public String lastName;
        public String userRole;
        public LocalDateTime registrationDate;
        public LocalDateTime verificationDate;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final WelcomeEmailEventDto event = new WelcomeEmailEventDto();

            public Builder userId(String userId) {
                event.userId = userId;
                return this;
            }

            public Builder email(String email) {
                event.email = email;
                return this;
            }

            public Builder firstName(String firstName) {
                event.firstName = firstName;
                return this;
            }

            public Builder lastName(String lastName) {
                event.lastName = lastName;
                return this;
            }

            public Builder userRole(String userRole) {
                event.userRole = userRole;
                return this;
            }

            public Builder registrationDate(LocalDateTime registrationDate) {
                event.registrationDate = registrationDate;
                return this;
            }

            public Builder verificationDate(LocalDateTime verificationDate) {
                event.verificationDate = verificationDate;
                return this;
            }

            public WelcomeEmailEventDto build() {
                return event;
            }
        }
    }

    /**
     * DTO for password reset email events.
     * This mirrors the DTO in the notification service but is defined here 
     * to avoid cross-module dependencies.
     */
    public static class PasswordResetEventDto {
        public String userId;
        public String email;
        public String firstName;
        public String lastName;
        public String resetUrl;
        public String resetToken;
        public LocalDateTime requestDate;
        public LocalDateTime expiryDate;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final PasswordResetEventDto event = new PasswordResetEventDto();

            public Builder userId(String userId) {
                event.userId = userId;
                return this;
            }

            public Builder email(String email) {
                event.email = email;
                return this;
            }

            public Builder firstName(String firstName) {
                event.firstName = firstName;
                return this;
            }

            public Builder lastName(String lastName) {
                event.lastName = lastName;
                return this;
            }

            public Builder resetUrl(String resetUrl) {
                event.resetUrl = resetUrl;
                return this;
            }

            public Builder resetToken(String resetToken) {
                event.resetToken = resetToken;
                return this;
            }

            public Builder requestDate(LocalDateTime requestDate) {
                event.requestDate = requestDate;
                return this;
            }

            public Builder expiryDate(LocalDateTime expiryDate) {
                event.expiryDate = expiryDate;
                return this;
            }

            public PasswordResetEventDto build() {
                return event;
            }
        }
    }
}