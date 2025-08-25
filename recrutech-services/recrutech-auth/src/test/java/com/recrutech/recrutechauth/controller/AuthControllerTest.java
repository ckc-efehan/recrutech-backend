package com.recrutech.recrutechauth.controller;

import com.recrutech.recrutechauth.service.AuthService;
import com.recrutech.recrutechauth.service.HttpRequestService;
import com.recrutech.recrutechauth.dto.auth.*;
import com.recrutech.recrutechauth.dto.registration.*;
import com.recrutech.recrutechauth.dto.common.*;
import com.recrutech.recrutechauth.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for AuthController.
 * Tests all authentication endpoints, validation groups, and business rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;
    
    @Mock
    private HttpRequestService httpRequestService;
    
    @Mock
    private HttpServletRequest httpServletRequest;
    
    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, httpRequestService);
        
        // Setup common HTTP request mocks (lenient to avoid UnnecessaryStubbingException)
        lenient().when(httpRequestService.getClientIpAddress(any(HttpServletRequest.class))).thenReturn("192.168.1.1");
        lenient().when(httpRequestService.getUserAgent(any(HttpServletRequest.class))).thenReturn("Mozilla/5.0");
        lenient().when(httpRequestService.getAcceptLanguage(any(HttpServletRequest.class))).thenReturn("en-US");
        lenient().when(httpRequestService.extractTokenFromHeader(anyString())).thenReturn("valid-token");
        
        System.out.println("[DEBUG_LOG] AuthController test setup completed");
    }

    // ========== LOGIN ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful login")
    void testSuccessfulLogin() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123", false, null);
        AuthResponse expectedResponse = AuthResponse.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .userId(UUID.randomUUID().toString())
            .userRole(UserRole.APPLICANT)
            .expiresIn(3600L)
            .build();

        when(authService.login(eq(loginRequest), eq("192.168.1.1"), eq("Mozilla/5.0"), eq("en-US")))
            .thenReturn(expectedResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(loginRequest, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");
        
        System.out.println("[DEBUG_LOG] Successful login test passed");
    }

    @Test
    @DisplayName("Test login with business rule validation failure")
    void testLoginBusinessRuleValidationFailure() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password", false, null); // Request that will trigger business rule validation
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authController.login(loginRequest, httpServletRequest));
        
        // Verify service was not called due to validation failure
        verify(authService, never()).login(any(), any(), any(), any());
        
        System.out.println("[DEBUG_LOG] Login business rule validation test passed");
    }

    // ========== COMPANY REGISTRATION ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful company registration")
    void testSuccessfulCompanyRegistration() {
        // Arrange
        CompanyRegistrationRequest request = CompanyRegistrationRequest.builder()
            .name("Test Company")
            .location("New York")
            .businessEmail("company@test.com")
            .telephone("+1234567890")
            .firstName("John")
            .lastName("Doe")
            .password("SecurePass123!")
            .agreementAccepted(true)
            .build();

        CompanyRegistrationResponse expectedResponse = CompanyRegistrationResponse.builder()
            .companyId(UUID.randomUUID().toString())
            .adminUserId(UUID.randomUUID().toString())
            .hrUserId(UUID.randomUUID().toString())
            .message("Company registration completed successfully")
            .registrationTimestamp(LocalDateTime.now())
            .build();

        when(authService.registerCompany(request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<CompanyRegistrationResponse> response = authController.registerCompany(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).registerCompany(request);
        
        System.out.println("[DEBUG_LOG] Successful company registration test passed");
    }

    // ========== HR REGISTRATION ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful HR registration")
    void testSuccessfulHRRegistration() {
        // Arrange
        HRRegistrationRequest request = HRRegistrationRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane@company.com")
            .password("SecurePass123!")
            .companyId(UUID.randomUUID().toString())
            .department("Human Resources")
            .build();

        UserRegistrationResponse expectedResponse = UserRegistrationResponse.builder()
            .userId(UUID.randomUUID().toString())
            .message("HR registration completed successfully")
            .registrationTimestamp(LocalDateTime.now())
            .build();

        when(authService.registerHR(request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<UserRegistrationResponse> response = authController.registerHR(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).registerHR(request);
        
        System.out.println("[DEBUG_LOG] Successful HR registration test passed");
    }

    // ========== APPLICANT REGISTRATION ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful applicant registration")
    void testSuccessfulApplicantRegistration() {
        // Arrange
        ApplicantRegistrationRequest request = ApplicantRegistrationRequest.builder()
            .personalInfo(ApplicantRegistrationRequest.PersonalInfo.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob@example.com")
                .password("SecurePass123!")
                .phoneNumber("+1234567890")
                .build())
            .privacyConsent(true)
            .build();

        UserRegistrationResponse expectedResponse = UserRegistrationResponse.builder()
            .userId(UUID.randomUUID().toString())
            .message("Applicant registration completed successfully")
            .registrationTimestamp(LocalDateTime.now())
            .build();

        when(authService.registerApplicant(request)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<UserRegistrationResponse> response = authController.registerApplicant(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).registerApplicant(request);
        
        System.out.println("[DEBUG_LOG] Successful applicant registration test passed");
    }

    // ========== TOKEN REFRESH ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful token refresh")
    void testSuccessfulTokenRefresh() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token", null);
        AuthResponse expectedResponse = AuthResponse.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .userId(UUID.randomUUID().toString())
            .userRole(UserRole.APPLICANT)
            .expiresIn(3600L)
            .build();

        when(authService.refreshToken("valid-refresh-token", "192.168.1.1"))
            .thenReturn(expectedResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.refreshToken(request, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).refreshToken("valid-refresh-token", "192.168.1.1");
        
        System.out.println("[DEBUG_LOG] Successful token refresh test passed");
    }

    @Test
    @DisplayName("Test token refresh with invalid token format")
    void testTokenRefreshInvalidFormat() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("", null); // Invalid token format
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authController.refreshToken(request, httpServletRequest));
        
        // Verify service was not called due to validation failure
        verify(authService, never()).refreshToken(any(), any());
        
        System.out.println("[DEBUG_LOG] Token refresh validation test passed");
    }

    // ========== LOGOUT ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test successful logout")
    void testSuccessfulLogout() {
        // Arrange
        String authHeader = "Bearer valid-token";
        LogoutRequest request = new LogoutRequest(UUID.randomUUID().toString(), false, null);

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Successfully logged out", response.getBody().message());
        
        verify(authService).logout("valid-token", request.userId());
        verify(httpRequestService).extractTokenFromHeader(authHeader);
        
        System.out.println("[DEBUG_LOG] Successful logout test passed");
    }

    @Test
    @DisplayName("Test logout from all devices")
    void testLogoutFromAllDevices() {
        // Arrange
        String authHeader = "Bearer valid-token";
        LogoutRequest request = new LogoutRequest(UUID.randomUUID().toString(), true, null);

        // Act
        ResponseEntity<LogoutResponse> response = authController.logout(authHeader, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Successfully logged out from all devices", response.getBody().message());
        
        verify(authService).logout("valid-token", request.userId());
        
        System.out.println("[DEBUG_LOG] Logout from all devices test passed");
    }

    // ========== HEALTH ENDPOINT TESTS ==========

    @Test
    @DisplayName("Test health check endpoint")
    void testHealthCheck() {
        // Act
        ResponseEntity<HealthResponse> response = authController.health();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HealthResponse.HealthStatus.UP, response.getBody().status());
        assertEquals("Authentication service is running normally", response.getBody().message());
        
        System.out.println("[DEBUG_LOG] Health check test passed");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("Test login with service exception")
    void testLoginServiceException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123", false, null);
        when(authService.login(any(), any(), any(), any()))
            .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authController.login(loginRequest, httpServletRequest));
        
        System.out.println("[DEBUG_LOG] Login service exception test passed");
    }

    @Test
    @DisplayName("Test company registration with service exception")
    void testCompanyRegistrationServiceException() {
        // Arrange
        CompanyRegistrationRequest request = CompanyRegistrationRequest.builder()
            .name("Test Company")
            .location("New York")
            .businessEmail("company@test.com")
            .telephone("+1234567890")
            .firstName("John")
            .lastName("Doe")
            .password("SecurePass123!")
            .agreementAccepted(true)
            .build();

        when(authService.registerCompany(request))
            .thenThrow(new RuntimeException("Registration failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authController.registerCompany(request));
        
        System.out.println("[DEBUG_LOG] Company registration service exception test passed");
    }

    // ========== HTTP REQUEST SERVICE INTEGRATION TESTS ==========

    @Test
    @DisplayName("Test HTTP request data extraction")
    void testHttpRequestDataExtraction() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123", false, null);
        AuthResponse expectedResponse = AuthResponse.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .userId(UUID.randomUUID().toString())
            .userRole(UserRole.APPLICANT)
            .expiresIn(3600L)
            .build();

        when(httpRequestService.getClientIpAddress(httpServletRequest)).thenReturn("10.0.0.1");
        when(httpRequestService.getUserAgent(httpServletRequest)).thenReturn("Custom-Agent/1.0");
        when(httpRequestService.getAcceptLanguage(httpServletRequest)).thenReturn("de-DE");
        when(authService.login(eq(loginRequest), eq("10.0.0.1"), eq("Custom-Agent/1.0"), eq("de-DE")))
            .thenReturn(expectedResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(loginRequest, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(httpRequestService).getClientIpAddress(httpServletRequest);
        verify(httpRequestService).getUserAgent(httpServletRequest);
        verify(httpRequestService).getAcceptLanguage(httpServletRequest);
        verify(authService).login(loginRequest, "10.0.0.1", "Custom-Agent/1.0", "de-DE");
        
        System.out.println("[DEBUG_LOG] HTTP request data extraction test passed");
    }
}