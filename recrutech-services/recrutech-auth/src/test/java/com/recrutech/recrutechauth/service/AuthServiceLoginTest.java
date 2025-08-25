package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.auth.*;
import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.SecurityService;
import com.recrutech.recrutechauth.security.TokenProvider;
import com.recrutech.recrutechauth.security.TokenPair;
import com.recrutech.recrutechauth.security.InputSanitizationService;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import com.recrutech.recrutechauth.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for AuthService login method.
 * Tests all security scenarios and branches in the login flow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Login Tests")
class AuthServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private HREmployeeRepository hrEmployeeRepository;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordValidator passwordValidator;
    @Mock private TokenProvider tokenProvider;
    @Mock private SecurityService securityService;
    @Mock private InputSanitizationService inputSanitizationService;

    private AuthService authService;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            companyRepository,
            hrEmployeeRepository,
            applicantRepository,
            passwordEncoder,
            passwordValidator,
            tokenProvider,
            securityService,
            inputSanitizationService
        );

        loginRequest = LoginRequest.builder()
            .email("test@example.com")
            .password("password123")
            .rememberMe(false)
            .deviceInfo(null)
            .build();
        
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.APPLICANT);
        testUser.setEnabled(true);
        testUser.setAccountLockedUntil(null); // Not locked
        testUser.setEmailVerified(true);
        testUser.setLastPasswordChange(LocalDateTime.now()); // Not expired
        testUser.setTwoFactorEnabled(false);
    }

    @Test
    @DisplayName("Successful login should return AuthResponse with tokens")
    void testSuccessfulLogin() {
        // Given
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(securityService.isSuspiciousActivity(anyString(), anyString())).thenReturn(false);
        when(securityService.generateDeviceFingerprint(anyString(), anyString(), anyString())).thenReturn("device-123");
        when(securityService.isKnownDevice(anyString(), anyString())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token", 3600L);
        when(tokenProvider.createOrGetTokenPair(any(User.class), anyString(), anyString())).thenReturn(tokenPair);
        when(applicantRepository.findByUserId(anyString())).thenReturn(Optional.of(new Applicant()));

        // When
        AuthResponse response = authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(3600L, response.expiresIn());
        assertEquals(UserRole.APPLICANT, response.userRole());
        assertEquals("user-123", response.userId());
        assertFalse(response.requiresTwoFactor());

        verify(securityService).clearFailedAttempts(testUser);
        verify(securityService).clearRateLimit("test@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Login should throw SecurityException when rate limited")
    void testLoginRateLimited() {
        // Given
        when(securityService.isRateLimited("test@example.com")).thenReturn(true);

        // When/Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Too many login attempts. Please try again later.", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Login should throw BadCredentialsException when user not found")
    void testLoginUserNotFound() {
        // Given
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // When/Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    @DisplayName("Login should throw AccountDisabledException when account disabled")
    void testLoginAccountDisabled() {
        // Given
        testUser.setEnabled(false);
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When/Then
        AccountDisabledException exception = assertThrows(AccountDisabledException.class,
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Account is disabled", exception.getMessage());
    }

    @Test
    @DisplayName("Login should throw AccountLockedException when account locked")
    void testLoginAccountLocked() {
        // Given
        testUser.setAccountLockedUntil(LocalDateTime.now().plusHours(1)); // Lock for 1 hour
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When/Then
        AccountLockedException exception = assertThrows(AccountLockedException.class,
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Account is temporarily locked", exception.getMessage());
    }

    @Test
    @DisplayName("Login should throw EmailNotVerifiedException when email not verified")
    void testLoginEmailNotVerified() {
        // Given
        testUser.setEmailVerified(false);
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When/Then
        EmailNotVerifiedException exception = assertThrows(EmailNotVerifiedException.class,
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Email address not verified", exception.getMessage());
    }

    @Test
    @DisplayName("Login should record failed attempt and throw BadCredentialsException for wrong password")
    void testLoginWrongPassword() {
        // Given
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When/Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Invalid credentials", exception.getMessage());
        verify(securityService).recordFailedAttempt(testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Login should throw PasswordExpiredException when password expired")
    void testLoginPasswordExpired() {
        // Given
        testUser.setLastPasswordChange(LocalDateTime.now().minusDays(91)); // Expired (90+ days old)
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When/Then
        PasswordExpiredException exception = assertThrows(PasswordExpiredException.class,
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Password has expired", exception.getMessage());
    }

    @Test
    @DisplayName("Login should return 2FA required response when 2FA enabled")
    void testLoginTwoFactorRequired() {
        // Given
        testUser.setTwoFactorEnabled(true);
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenProvider.generateSecureToken()).thenReturn("temp-token-123");

        // When
        AuthResponse response = authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");

        // Then
        assertNotNull(response);
        assertTrue(response.requiresTwoFactor());
        verify(securityService).logSecurityEvent("user-123", "2FA_REQUIRED", "Two-factor authentication required", "192.168.1.1");
    }

    @Test
    @DisplayName("Login should throw SuspiciousActivityException when suspicious activity detected")
    void testLoginSuspiciousActivity() {
        // Given
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(securityService.isSuspiciousActivity("user-123", "192.168.1.1")).thenReturn(true);

        // When/Then
        SuspiciousActivityException exception = assertThrows(SuspiciousActivityException.class,
            () -> authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US"));
        
        assertEquals("Suspicious activity detected", exception.getMessage());
        verify(securityService).logSecurityEvent("user-123", "SUSPICIOUS_LOGIN", "Multiple IPs detected", "192.168.1.1");
    }

    @Test
    @DisplayName("Login should register new device when unknown device detected")
    void testLoginNewDevice() {
        // Given
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(securityService.isSuspiciousActivity(anyString(), anyString())).thenReturn(false);
        when(securityService.generateDeviceFingerprint("Mozilla/5.0", "en-US", "192.168.1.1")).thenReturn("new-device-123");
        when(securityService.isKnownDevice("user-123", "new-device-123")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token", 3600L);
        when(tokenProvider.createOrGetTokenPair(any(User.class), anyString(), anyString())).thenReturn(tokenPair);
        when(applicantRepository.findByUserId(anyString())).thenReturn(Optional.of(new Applicant()));

        // When
        AuthResponse response = authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");

        // Then
        assertNotNull(response);
        verify(securityService).logSecurityEvent("user-123", "NEW_DEVICE_LOGIN", "Unknown device", "192.168.1.1");
        verify(securityService).registerDevice("user-123", "new-device-123");
    }

    @Test
    @DisplayName("Login should load correct user context for different roles")
    void testLoginUserContextLoading() {
        // Test COMPANY_ADMIN context
        testUser.setRole(UserRole.COMPANY_ADMIN);
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(securityService.isSuspiciousActivity(anyString(), anyString())).thenReturn(false);
        when(securityService.generateDeviceFingerprint(anyString(), anyString(), anyString())).thenReturn("device-123");
        when(securityService.isKnownDevice(anyString(), anyString())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token", 3600L);
        when(tokenProvider.createOrGetTokenPair(any(User.class), anyString(), anyString())).thenReturn(tokenPair);
        
        Company company = new Company();
        when(companyRepository.findByAdminUserId("user-123")).thenReturn(Optional.of(company));

        // When
        AuthResponse response = authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");

        // Then
        assertNotNull(response);
        assertEquals(UserRole.COMPANY_ADMIN, response.userRole());
        assertNotNull(response.userContext());
        verify(companyRepository).findByAdminUserId("user-123");
    }
}