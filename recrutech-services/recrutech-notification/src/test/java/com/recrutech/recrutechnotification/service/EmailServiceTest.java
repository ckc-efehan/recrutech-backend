package com.recrutech.recrutechnotification.service;

import com.recrutech.recrutechnotification.dto.EmailVerificationEvent;
import com.recrutech.recrutechnotification.dto.WelcomeEmailEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 * Tests email validation, creation, and template processing functionality.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private TemplateEngine templateEngine;
    
    @InjectMocks
    private EmailService emailService;
    
    private EmailVerificationEvent sampleVerificationEvent;
    private WelcomeEmailEvent sampleWelcomeEvent;
    private EmailVerificationEvent expiredTokenEvent;
    private MimeMessage mockMimeMessage;
    
    @BeforeEach
    void setUp() {
        // Set up test configuration values using ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@recrutech.com");
        ReflectionTestUtils.setField(emailService, "fromName", "RecruTech Team");
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", "https://recrutech.com/verify");
        
        // Create sample verification event
        sampleVerificationEvent = EmailVerificationEvent.builder()
                .userId("user-123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .verificationToken("sample-token-123")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now().minusHours(1))
                .tokenExpiryDate(LocalDateTime.now().plusHours(24))
                .build();
                
        // Create expired token event
        expiredTokenEvent = EmailVerificationEvent.builder()
                .userId("user-expired")
                .email("expired@example.com")
                .firstName("Expired")
                .lastName("User")
                .verificationToken("expired-token")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now().minusHours(48))
                .tokenExpiryDate(LocalDateTime.now().minusHours(1)) // Expired 1 hour ago
                .build();
                
        // Create sample welcome event
        sampleWelcomeEvent = WelcomeEmailEvent.builder()
                .userId("user-456")
                .email("welcome@example.com")
                .firstName("John")
                .lastName("Doe")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now().minusHours(2))
                .verificationDate(LocalDateTime.now().minusMinutes(5))
                .build();
                
        // Create mock MimeMessage
        Session session = Session.getInstance(new Properties());
        mockMimeMessage = new MimeMessage(session);
    }
    
    @Test
    void sendVerificationEmail_ValidEvent_SendsEmailSuccessfully() throws MessagingException {
        // Given
        when(templateEngine.process(eq("email-verification"), any(Context.class)))
                .thenReturn("<html><body>Verification email content</body></html>");
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        
        // When
        assertDoesNotThrow(() -> emailService.sendVerificationEmail(sampleVerificationEvent));
        
        // Then
        verify(templateEngine).process(eq("email-verification"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }
    
    @Test
    void sendVerificationEmail_ExpiredToken_ThrowsIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.sendVerificationEmail(expiredTokenEvent));
        
        assertEquals("Verification token is expired", exception.getMessage());
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
    
    @Test
    void sendWelcomeEmail_ValidEvent_SendsEmailSuccessfully() throws MessagingException {
        // Given
        when(templateEngine.process(eq("welcome-email"), any(Context.class)))
                .thenReturn("<html><body>Welcome email content</body></html>");
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        
        // When
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(sampleWelcomeEvent));
        
        // Then
        verify(templateEngine).process(eq("welcome-email"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }
    
    @Test
    void sendVerificationEmail_TemplateProcessingFails_ThrowsMessagingException() throws MessagingException {
        // Given
        when(templateEngine.process(eq("email-verification"), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));
        
        // When & Then
        MessagingException exception = assertThrows(MessagingException.class, 
                () -> emailService.sendVerificationEmail(sampleVerificationEvent));
        
        assertEquals("Failed to send verification email", exception.getMessage());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
    
    @Test
    void sendWelcomeEmail_MailSendingFails_ThrowsMessagingException() throws MessagingException {
        // Given
        when(templateEngine.process(eq("welcome-email"), any(Context.class)))
                .thenReturn("<html><body>Welcome email content</body></html>");
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        doThrow(new RuntimeException("Mail sending failed")).when(mailSender).send(any(MimeMessage.class));
        
        // When & Then
        MessagingException exception = assertThrows(MessagingException.class, 
                () -> emailService.sendWelcomeEmail(sampleWelcomeEvent));
        
        assertEquals("Failed to send welcome email", exception.getMessage());
        verify(templateEngine).process(eq("welcome-email"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }
    
    @Test
    void validateEmailEvent_ValidVerificationEvent_DoesNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> emailService.validateEmailEvent(sampleVerificationEvent));
    }
    
    @Test
    void validateEmailEvent_ValidWelcomeEvent_DoesNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> emailService.validateEmailEvent(sampleWelcomeEvent));
    }
    
    @Test
    void validateEmailEvent_NullEvent_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.validateEmailEvent(null));
        assertEquals("Email event cannot be null", exception.getMessage());
    }
    
    @Test
    void validateEmailEvent_VerificationEventWithNullEmail_ThrowsException() {
        // Given
        EmailVerificationEvent invalidEvent = EmailVerificationEvent.builder()
                .userId("user-123")
                .email(null)
                .firstName("Test")
                .lastName("User")
                .verificationToken("token")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now())
                .tokenExpiryDate(LocalDateTime.now().plusHours(1))
                .build();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.validateEmailEvent(invalidEvent));
        assertEquals("Email address is required", exception.getMessage());
    }
    
    @Test
    void validateEmailEvent_VerificationEventWithBlankEmail_ThrowsException() {
        // Given
        EmailVerificationEvent invalidEvent = EmailVerificationEvent.builder()
                .userId("user-123")
                .email("")
                .firstName("Test")
                .lastName("User")
                .verificationToken("token")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now())
                .tokenExpiryDate(LocalDateTime.now().plusHours(1))
                .build();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.validateEmailEvent(invalidEvent));
        assertEquals("Email address is required", exception.getMessage());
    }
    
    @Test
    void validateEmailEvent_VerificationEventWithNullToken_ThrowsException() {
        // Given
        EmailVerificationEvent invalidEvent = EmailVerificationEvent.builder()
                .userId("user-123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .verificationToken(null)
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now())
                .tokenExpiryDate(LocalDateTime.now().plusHours(1))
                .build();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.validateEmailEvent(invalidEvent));
        assertEquals("Verification token is required", exception.getMessage());
    }
    
    @Test
    void validateEmailEvent_VerificationEventWithBlankToken_ThrowsException() {
        // Given
        EmailVerificationEvent invalidEvent = EmailVerificationEvent.builder()
                .userId("user-123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .verificationToken("")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now())
                .tokenExpiryDate(LocalDateTime.now().plusHours(1))
                .build();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.validateEmailEvent(invalidEvent));
        assertEquals("Verification token is required", exception.getMessage());
    }
    
    @Test
    void validateEmailEvent_WelcomeEventWithNullEmail_ThrowsException() {
        // Given
        WelcomeEmailEvent invalidEvent = WelcomeEmailEvent.builder()
                .userId("user-456")
                .email(null)
                .firstName("John")
                .lastName("Doe")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now())
                .verificationDate(LocalDateTime.now())
                .build();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.validateEmailEvent(invalidEvent));
        assertEquals("Email address is required", exception.getMessage());
    }
    
    @Test
    void validateEmailEvent_WelcomeEventWithBlankEmail_ThrowsException() {
        // Given
        WelcomeEmailEvent invalidEvent = WelcomeEmailEvent.builder()
                .userId("user-456")
                .email("")
                .firstName("John")
                .lastName("Doe")
                .userRole("APPLICANT")
                .registrationDate(LocalDateTime.now())
                .verificationDate(LocalDateTime.now())
                .build();
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> emailService.validateEmailEvent(invalidEvent));
        assertEquals("Email address is required", exception.getMessage());
    }
    
    @Test
    void sendVerificationEmail_ContextCreation_WorksCorrectly() throws MessagingException {
        // Given
        when(templateEngine.process(eq("email-verification"), any(Context.class)))
                .thenReturn("<html><body>Test</body></html>");
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        
        // When
        assertDoesNotThrow(() -> emailService.sendVerificationEmail(sampleVerificationEvent));
        
        // Then - Verify template processing was called with correct template name
        verify(templateEngine).process(eq("email-verification"), any(Context.class));
        System.out.println("[DEBUG_LOG] Testing verification email context creation for: " + sampleVerificationEvent.getEmail());
    }
    
    @Test
    void sendWelcomeEmail_ContextCreation_WorksCorrectly() throws MessagingException {
        // Given
        when(templateEngine.process(eq("welcome-email"), any(Context.class)))
                .thenReturn("<html><body>Test</body></html>");
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        
        // When
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(sampleWelcomeEvent));
        
        // Then - Verify template processing was called with correct template name
        verify(templateEngine).process(eq("welcome-email"), any(Context.class));
        System.out.println("[DEBUG_LOG] Testing welcome email context creation for: " + sampleWelcomeEvent.getEmail());
    }
}