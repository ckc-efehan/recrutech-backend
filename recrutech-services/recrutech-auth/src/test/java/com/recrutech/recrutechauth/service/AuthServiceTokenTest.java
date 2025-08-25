package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.auth.*;
import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.SecurityService;
import com.recrutech.recrutechauth.security.TokenProvider;
import com.recrutech.recrutechauth.security.TokenPair;
import com.recrutech.recrutechauth.security.InputSanitizationService;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for AuthService token management methods.
 * Tests:
 * - refreshToken() method with success and error scenarios
 * - logout() method with token blacklisting and session invalidation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Token Management Tests")
class AuthServiceTokenTest {

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

        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.APPLICANT);
        testUser.setCurrentRefreshToken("old-refresh-token");
        testUser.setCurrentSessionId("session-123");
    }

    @Test
    @DisplayName("refreshToken should return new AuthResponse with updated tokens")
    void testRefreshTokenSuccess() {
        // Given
        String refreshToken = "valid-refresh-token";
        String clientIp = "192.168.1.1";
        
        TokenPair newTokenPair = new TokenPair("new-access-token", "new-refresh-token", 3600L);
        when(tokenProvider.refreshToken(refreshToken, clientIp)).thenReturn(newTokenPair);
        when(tokenProvider.getUserIdFromToken("new-access-token")).thenReturn("user-123");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        when(applicantRepository.findByUserId("user-123")).thenReturn(Optional.of(new Applicant()));

        // When
        AuthResponse response = authService.refreshToken(refreshToken, clientIp);

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals(3600L, response.expiresIn());
        assertEquals(UserRole.APPLICANT, response.userRole());
        assertEquals("user-123", response.userId());
        assertFalse(response.requiresTwoFactor());
        assertNotNull(response.userContext());

        verify(tokenProvider).refreshToken(refreshToken, clientIp);
        verify(tokenProvider).getUserIdFromToken("new-access-token");
        verify(userRepository).findById("user-123");
    }

    @Test
    @DisplayName("refreshToken should throw IllegalArgumentException when user not found")
    void testRefreshTokenUserNotFound() {
        // Given
        String refreshToken = "valid-refresh-token";
        String clientIp = "192.168.1.1";
        
        TokenPair newTokenPair = new TokenPair("new-access-token", "new-refresh-token", 3600L);
        when(tokenProvider.refreshToken(refreshToken, clientIp)).thenReturn(newTokenPair);
        when(tokenProvider.getUserIdFromToken("new-access-token")).thenReturn("nonexistent-user");
        when(userRepository.findById("nonexistent-user")).thenReturn(Optional.empty());

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authService.refreshToken(refreshToken, clientIp));
        
        assertEquals("User not found", exception.getMessage());
        verify(tokenProvider).refreshToken(refreshToken, clientIp);
        verify(userRepository).findById("nonexistent-user");
    }

    @Test
    @DisplayName("refreshToken should load correct user context for different roles")
    void testRefreshTokenUserContextLoading() {
        // Test HR role context loading
        testUser.setRole(UserRole.HR);
        String refreshToken = "valid-refresh-token";
        String clientIp = "192.168.1.1";
        
        TokenPair newTokenPair = new TokenPair("new-access-token", "new-refresh-token", 3600L);
        when(tokenProvider.refreshToken(refreshToken, clientIp)).thenReturn(newTokenPair);
        when(tokenProvider.getUserIdFromToken("new-access-token")).thenReturn("user-123");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
        
        HREmployee hrEmployee = new HREmployee();
        when(hrEmployeeRepository.findByUserId("user-123")).thenReturn(Optional.of(hrEmployee));

        // When
        AuthResponse response = authService.refreshToken(refreshToken, clientIp);

        // Then
        assertNotNull(response);
        assertEquals(UserRole.HR, response.userRole());
        assertNotNull(response.userContext());
        verify(hrEmployeeRepository).findByUserId("user-123");
    }

    @Test
    @DisplayName("logout should blacklist tokens and invalidate session")
    void testLogoutSuccess() {
        // Given
        String accessToken = "current-access-token";
        String userId = "user-123";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.logout(accessToken, userId);

        // Then
        verify(userRepository).findById(userId);
        verify(tokenProvider).blacklistToken(accessToken);
        verify(tokenProvider).blacklistToken("old-refresh-token");
        verify(tokenProvider).invalidateSession("session-123");
        verify(securityService).removeActiveSession(userId, "session-123");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("logout should handle user with no refresh token")
    void testLogoutNoRefreshToken() {
        // Given
        testUser.setCurrentRefreshToken(null);
        String accessToken = "current-access-token";
        String userId = "user-123";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.logout(accessToken, userId);

        // Then
        verify(userRepository).findById(userId);
        verify(tokenProvider).blacklistToken(accessToken);
        verify(tokenProvider, never()).blacklistToken("old-refresh-token"); // Refresh token should not be blacklisted since it's null
        verify(tokenProvider).invalidateSession("session-123");
        verify(securityService).removeActiveSession(userId, "session-123");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("logout should handle user with no session")
    void testLogoutNoSession() {
        // Given
        testUser.setCurrentSessionId(null);
        String accessToken = "current-access-token";
        String userId = "user-123";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.logout(accessToken, userId);

        // Then
        verify(userRepository).findById(userId);
        verify(tokenProvider).blacklistToken(accessToken);
        verify(tokenProvider).blacklistToken("old-refresh-token");
        verify(tokenProvider, never()).invalidateSession(anyString());
        verify(securityService, never()).removeActiveSession(anyString(), anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("logout should throw IllegalArgumentException when user not found")
    void testLogoutUserNotFound() {
        // Given
        String accessToken = "current-access-token";
        String userId = "nonexistent-user";
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authService.logout(accessToken, userId));
        
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(tokenProvider, never()).blacklistToken(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("logout should clear all user tokens")
    void testLogoutClearsUserTokens() {
        // Given
        String accessToken = "current-access-token";
        String userId = "user-123";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            // Verify that clearTokens() was called
            assertNull(savedUser.getCurrentAccessToken());
            assertNull(savedUser.getCurrentRefreshToken());
            assertNull(savedUser.getTokenExpiresAt());
            assertNull(savedUser.getCurrentSessionId());
            return savedUser;
        });

        // When
        authService.logout(accessToken, userId);

        // Then
        verify(userRepository).save(testUser);
    }
}