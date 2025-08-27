package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.model.UserRole;
import com.recrutech.recrutechauth.repository.UserRepository;
import com.recrutech.recrutechauth.repository.CompanyRepository;
import com.recrutech.recrutechauth.repository.HREmployeeRepository;
import com.recrutech.recrutechauth.repository.ApplicantRepository;
import com.recrutech.recrutechauth.security.TokenProvider;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import com.recrutech.recrutechauth.kafka.EmailEventProducer;
import com.recrutech.recrutechauth.security.SecurityService;
import com.recrutech.recrutechauth.security.InputSanitizationService;
import com.recrutech.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for password reset functionality in AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServicePasswordResetTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private HREmployeeRepository hrEmployeeRepository;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordValidator passwordValidator;
    @Mock private TokenProvider tokenProvider;
    @Mock private SecurityService securityService;
    @Mock private InputSanitizationService inputSanitizationService;
    @Mock private EmailEventProducer emailEventProducer;

    private AuthService authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository, companyRepository, hrEmployeeRepository, applicantRepository,
            passwordEncoder, passwordValidator, tokenProvider, 
            securityService, inputSanitizationService, emailEventProducer
        );

        // Create test user
        testUser = new User();
        testUser.setId("user123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.APPLICANT);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setPassword("encodedPassword");

        // Setup mocks
        lenient().when(inputSanitizationService.encodeForHTML(anyString())).thenReturn("sanitized");
        lenient().when(passwordValidator.validate(anyString())).thenReturn(new ArrayList<>());
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
    }

    @Test
    void testForgotPasswordSuccess() {
        System.out.println("[DEBUG_LOG] Testing forgot password success scenario");
        
        // Setup
        String resetToken = "reset123token";
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateSecureToken()).thenReturn(resetToken);
        
        // Execute
        assertDoesNotThrow(() -> authService.forgotPassword("test@example.com"));
        
        // Verify
        verify(userRepository).save(testUser);
        verify(emailEventProducer).publishPasswordResetEvent(eq(testUser), anyString());
        assertNotNull(testUser.getPasswordResetToken());
        assertNotNull(testUser.getPasswordResetExpiry());
        assertTrue(testUser.getPasswordResetExpiry().isAfter(LocalDateTime.now()));
        
        System.out.println("[DEBUG_LOG] Forgot password test passed");
    }

    @Test
    void testForgotPasswordNonExistentEmail() {
        System.out.println("[DEBUG_LOG] Testing forgot password with non-existent email");
        
        // Setup
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // Execute - should not throw exception (security)
        assertDoesNotThrow(() -> authService.forgotPassword("nonexistent@example.com"));
        
        // Verify no email is sent
        verify(emailEventProducer, never()).publishPasswordResetEvent(any(), any());
        
        System.out.println("[DEBUG_LOG] Non-existent email test passed");
    }

    @Test
    void testResetPasswordSuccess() {
        System.out.println("[DEBUG_LOG] Testing password reset success scenario");
        
        // Setup
        String resetToken = "reset123token";
        String newPassword = "NewPassword123!";
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(10));
        
        when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));
        
        // Execute
        assertDoesNotThrow(() -> authService.resetPassword(resetToken, newPassword));
        
        // Verify
        verify(userRepository).save(testUser);
        verify(passwordEncoder).encode(newPassword);
        assertNull(testUser.getPasswordResetToken());
        assertNull(testUser.getPasswordResetExpiry());
        assertNotNull(testUser.getLastPasswordChange());
        
        System.out.println("[DEBUG_LOG] Password reset test passed");
    }

    @Test
    void testResetPasswordExpiredToken() {
        System.out.println("[DEBUG_LOG] Testing password reset with expired token");
        
        // Setup
        String resetToken = "expiredToken";
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetExpiry(LocalDateTime.now().minusMinutes(5)); // Expired
        
        when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));
        
        // Execute & Verify
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> authService.resetPassword(resetToken, "NewPassword123!"));
        
        assertTrue(exception.getMessage().contains("expired"));
        
        System.out.println("[DEBUG_LOG] Expired token test passed");
    }

    @Test
    void testResetPasswordInvalidToken() {
        System.out.println("[DEBUG_LOG] Testing password reset with invalid token");
        
        // Setup
        when(userRepository.findByPasswordResetToken("invalidToken")).thenReturn(Optional.empty());
        
        // Execute & Verify
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> authService.resetPassword("invalidToken", "NewPassword123!"));
        
        assertTrue(exception.getMessage().contains("Invalid"));
        
        System.out.println("[DEBUG_LOG] Invalid token test passed");
    }

    @Test
    void testCompletePasswordResetFlow() {
        System.out.println("[DEBUG_LOG] Testing complete password reset flow");
        
        // Step 1: Request password reset
        String resetToken = "flowTestToken";
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateSecureToken()).thenReturn(resetToken);
        
        authService.forgotPassword("test@example.com");
        
        // Verify token was set
        assertNotNull(testUser.getPasswordResetToken());
        assertEquals(resetToken, testUser.getPasswordResetToken());
        
        // Step 2: Reset password using token
        String newPassword = "CompleteFlow123!";
        when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));
        
        authService.resetPassword(resetToken, newPassword);
        
        // Verify password was reset and token cleared
        assertNull(testUser.getPasswordResetToken());
        assertNull(testUser.getPasswordResetExpiry());
        verify(passwordEncoder).encode(newPassword);
        
        System.out.println("[DEBUG_LOG] Complete flow test passed");
    }
}