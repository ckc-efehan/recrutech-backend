package com.recrutech.recrutechnotification.service;

import com.recrutech.recrutechnotification.dto.EmailVerificationEvent;
import com.recrutech.recrutechnotification.dto.WelcomeEmailEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Service for sending emails using Thymeleaf templates and Spring Mail.
 * Handles verification emails and welcome emails with proper error handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${recrutech.email.from}")
    private String fromEmail;

    @Value("${recrutech.email.from-name}")
    private String fromName;

    @Value("${recrutech.email.verification-url}")
    private String verificationBaseUrl;

    /**
     * Sends an email verification email to the user.
     * @param event Email verification event containing user details
     * @throws MessagingException if email sending fails
     */
    public void sendVerificationEmail(EmailVerificationEvent event) throws MessagingException {
        log.info("[DEBUG_LOG] Sending verification email to: {}", event.getEmail());
        
        if (event.isTokenExpired()) {
            log.warn("[DEBUG_LOG] Verification token is expired for user: {}", event.getEmail());
            throw new IllegalArgumentException("Verification token is expired");
        }

        try {
            // Create Thymeleaf context with template variables
            Context context = createVerificationEmailContext(event);
            
            // Process the template
            String htmlContent = templateEngine.process("email-verification", context);
            
            // Create and send the email
            MimeMessage message = createMimeMessage(
                event.getEmail(),
                event.getFullName(),
                "E-Mail Bestätigung - RecruTech",
                htmlContent
            );
            
            mailSender.send(message);
            
            log.info("[DEBUG_LOG] Verification email sent successfully to: {}", event.getEmail());
            
        } catch (Exception e) {
            log.error("[DEBUG_LOG] Failed to send verification email to: {}. Error: {}", 
                     event.getEmail(), e.getMessage());
            throw new MessagingException("Failed to send verification email", e);
        }
    }

    /**
     * Sends a welcome email to the user after successful verification.
     * @param event Welcome email event containing user details
     * @throws MessagingException if email sending fails
     */
    public void sendWelcomeEmail(WelcomeEmailEvent event) throws MessagingException {
        log.info("[DEBUG_LOG] Sending welcome email to: {}", event.getEmail());
        
        try {
            // Create Thymeleaf context with template variables
            Context context = createWelcomeEmailContext(event);
            
            // Process the template
            String htmlContent = templateEngine.process("welcome-email", context);
            
            // Create and send the email
            MimeMessage message = createMimeMessage(
                event.getEmail(),
                event.getFullName(),
                "Willkommen bei RecruTech! 🎉",
                htmlContent
            );
            
            mailSender.send(message);
            
            log.info("[DEBUG_LOG] Welcome email sent successfully to: {}", event.getEmail());
            
        } catch (Exception e) {
            log.error("[DEBUG_LOG] Failed to send welcome email to: {}. Error: {}", 
                     event.getEmail(), e.getMessage());
            throw new MessagingException("Failed to send welcome email", e);
        }
    }

    /**
     * Creates Thymeleaf context for verification email template.
     * @param event Email verification event
     * @return Configured Thymeleaf context
     */
    private Context createVerificationEmailContext(EmailVerificationEvent event) {
        Context context = new Context(Locale.GERMAN);
        
        // Add template variables
        context.setVariable("firstName", event.getFirstName());
        context.setVariable("lastName", event.getLastName());
        context.setVariable("fullName", event.getFullName());
        context.setVariable("email", event.getEmail());
        context.setVariable("userRole", event.getUserRole());
        context.setVariable("registrationDate", event.getRegistrationDate());
        context.setVariable("tokenExpiryDate", event.getTokenExpiryDate());
        context.setVariable("verificationUrl", event.createVerificationUrl(verificationBaseUrl));
        
        // Add utility variables
        context.setVariable("isTokenExpired", event.isTokenExpired());
        
        log.debug("[DEBUG_LOG] Created verification email context for user: {} with URL: {}", 
                 event.getEmail(), event.createVerificationUrl(verificationBaseUrl));
        
        return context;
    }

    /**
     * Creates Thymeleaf context for welcome email template.
     * @param event Welcome email event
     * @return Configured Thymeleaf context
     */
    private Context createWelcomeEmailContext(WelcomeEmailEvent event) {
        Context context = new Context(Locale.GERMAN);
        
        // Add template variables
        context.setVariable("firstName", event.getFirstName());
        context.setVariable("lastName", event.getLastName());
        context.setVariable("fullName", event.getFullName());
        context.setVariable("email", event.getEmail());
        context.setVariable("userRole", event.getUserRole());
        context.setVariable("registrationDate", event.getRegistrationDate());
        context.setVariable("verificationDate", event.getVerificationDate());
        
        // Add role-specific variables
        context.setVariable("displayRoleName", event.getDisplayRoleName());
        context.setVariable("roleSpecificMessage", event.getRoleSpecificWelcomeMessage());
        context.setVariable("isApplicant", event.isApplicant());
        context.setVariable("isHR", event.isHR());
        context.setVariable("isCompany", event.isCompany());
        
        log.debug("[DEBUG_LOG] Created welcome email context for user: {} with role: {}", 
                 event.getEmail(), event.getDisplayRoleName());
        
        return context;
    }

    /**
     * Creates a MIME message with HTML content.
     * @param toEmail Recipient email address
     * @param toName Recipient name
     * @param subject Email subject
     * @param htmlContent HTML content of the email
     * @return Configured MIME message
     * @throws MessagingException if message creation fails
     */
    private MimeMessage createMimeMessage(String toEmail, String toName, String subject, String htmlContent) 
            throws MessagingException {
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        // Set sender
        try {
            helper.setFrom(fromEmail, fromName);
        } catch (Exception e) {
            log.warn("[DEBUG_LOG] Failed to set from name, using email only: {}", fromEmail);
            helper.setFrom(fromEmail);
        }
        
        // Set recipient
        helper.setTo(toEmail);
        
        // Set subject
        helper.setSubject(subject);
        
        // Set HTML content
        helper.setText(htmlContent, true);
        
        // Add headers for better email client compatibility
        message.addHeader("Content-Type", "text/html; charset=UTF-8");
        message.addHeader("X-Mailer", "RecruTech Notification Service");
        
        log.debug("[DEBUG_LOG] Created MIME message for: {} with subject: {}", toEmail, subject);
        
        return message;
    }

    /**
     * Validates email event data before processing.
     * @param event Email event to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateEmailEvent(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("Email event cannot be null");
        }
        
        if (event instanceof EmailVerificationEvent verificationEvent) {
            if (verificationEvent.getEmail() == null || verificationEvent.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email address is required");
            }
            if (verificationEvent.getVerificationToken() == null || verificationEvent.getVerificationToken().trim().isEmpty()) {
                throw new IllegalArgumentException("Verification token is required");
            }
        } else if (event instanceof WelcomeEmailEvent welcomeEvent) {
            if (welcomeEvent.getEmail() == null || welcomeEvent.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email address is required");
            }
        }
    }
}