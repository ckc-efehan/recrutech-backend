package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.registration.*;
import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.SecurityService;
import com.recrutech.recrutechauth.security.TokenProvider;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import com.recrutech.recrutechauth.exception.ConflictException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for AuthService private helper methods.
 * Tests private methods through public method calls that use them:
 * - createUser() method through registration methods
 * - validateCompanyAccess() method through HR registration
 * - loadUserContext() method through login and refresh token methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Helper Methods Tests")
class AuthServiceHelperMethodsTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private HREmployeeRepository hrEmployeeRepository;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordValidator passwordValidator;
    @Mock private TokenProvider tokenProvider;
    @Mock private SecurityService securityService;

    private AuthService authService;

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
            securityService
        );
    }

    @Test
    @DisplayName("createUser should throw ConflictException when email already exists")
    void testCreateUserEmailExists() {
        // Given: Applicant registration that will trigger createUser with existing email
        ApplicantRegistrationRequest request = ApplicantRegistrationRequest.builder()
            .personalInfo(ApplicantRegistrationRequest.PersonalInfo.builder()
                .email("existing@test.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .currentLocation("Test City")
                .build())
            .privacyConsent(true)
            .build();

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        // When/Then: Should throw ConflictException from createUser method
        ConflictException exception = assertThrows(ConflictException.class,
            () -> authService.registerApplicant(request));
        
        assertEquals("Email address already exists", exception.getMessage());
        verify(userRepository).existsByEmail("existing@test.com");
    }

    @Test
    @DisplayName("createUser should throw ValidationException when password validation fails")
    void testCreateUserPasswordValidationFails() {
        // Given: Applicant registration with invalid password
        ApplicantRegistrationRequest request = ApplicantRegistrationRequest.builder()
            .personalInfo(ApplicantRegistrationRequest.PersonalInfo.builder()
                .email("test@example.com")
                .password("weak")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .currentLocation("Test City")
                .build())
            .privacyConsent(true)
            .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordValidator.validate("weak")).thenReturn(List.of("Password too short", "Missing special characters"));

        // When/Then: Should throw ValidationException from createUser method
        ValidationException exception = assertThrows(ValidationException.class,
            () -> authService.registerApplicant(request));
        
        assertTrue(exception.getMessage().contains("Password validation failed"));
        assertTrue(exception.getMessage().contains("Password too short"));
        assertTrue(exception.getMessage().contains("Missing special characters"));
        verify(passwordValidator).validate("weak");
    }

    @Test
    @DisplayName("createUser should successfully create user with valid data")
    void testCreateUserSuccess() {
        // Given: Valid applicant registration
        ApplicantRegistrationRequest request = ApplicantRegistrationRequest.builder()
            .personalInfo(ApplicantRegistrationRequest.PersonalInfo.builder()
                .email("test@example.com")
                .password("validPassword123!")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .currentLocation("Test City")
                .build())
            .privacyConsent(true)
            .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordValidator.validate("validPassword123!")).thenReturn(List.of()); // No validation errors
        when(passwordEncoder.encode("validPassword123!")).thenReturn("encoded_password");
        when(tokenProvider.generateSecureToken()).thenReturn("verification_token");
        
        User savedUser = new User();
        savedUser.setId("user-123");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When: Register applicant (which calls createUser internally)
        UserRegistrationResponse response = authService.registerApplicant(request);

        // Then: Should successfully create user
        assertNotNull(response);
        assertEquals("user-123", response.userId());
        
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordValidator).validate("validPassword123!");
        verify(passwordEncoder).encode("validPassword123!");
        verify(tokenProvider).generateSecureToken();
        verify(userRepository).save(any(User.class));
        verify(applicantRepository).save(any(Applicant.class));
    }

    @Test
    @DisplayName("validateCompanyAccess should throw NotFoundException when company not found")
    void testValidateCompanyAccessCompanyNotFound() {
        // Given: HR registration with non-existent company
        HRRegistrationRequest request = HRRegistrationRequest.builder()
            .email("hr@test.com")
            .password("password123")
            .firstName("HR")
            .lastName("User")
            .companyId("nonexistent-company")
            .invitationToken("validInvitationToken123456789") // Valid format (25+ chars)
            .department("HR")
            .position("Manager")
            .employeeId("HR001")
            .agreementAccepted(true)
            .build();

        when(companyRepository.findById("nonexistent-company")).thenReturn(Optional.empty());

        // When/Then: Should throw NotFoundException from validateCompanyAccess method
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> authService.registerHR(request));
        
        assertEquals("Company not found", exception.getMessage());
        verify(companyRepository).findById("nonexistent-company");
    }

    @Test
    @DisplayName("validateCompanyAccess should succeed when company exists")
    void testValidateCompanyAccessSuccess() {
        // Given: Valid HR registration with existing company
        HRRegistrationRequest request = HRRegistrationRequest.builder()
            .email("hr@test.com")
            .password("password123")
            .firstName("HR")
            .lastName("User")
            .companyId("existing-company")
            .invitationToken("validInvitationToken123456789") // Valid format (25+ chars)
            .department("HR")
            .position("Manager")
            .employeeId("HR001")
            .agreementAccepted(true)
            .build();

        Company company = new Company();
        company.setId("existing-company");
        when(companyRepository.findById("existing-company")).thenReturn(Optional.of(company));
        when(userRepository.existsByEmail("hr@test.com")).thenReturn(false);
        when(passwordValidator.validate("password123")).thenReturn(List.of());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        
        User savedUser = new User();
        savedUser.setId("user-123");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When: Register HR (which calls validateCompanyAccess internally)
        UserRegistrationResponse response = authService.registerHR(request);

        // Then: Should successfully validate company access
        assertNotNull(response);
        assertEquals("user-123", response.userId());
        verify(companyRepository).findById("existing-company");
    }

    @Test
    @DisplayName("loadUserContext should return Company for COMPANY_ADMIN role")
    void testLoadUserContextCompanyAdmin() {
        // Given: Company admin user login
        User adminUser = new User();
        adminUser.setId("admin-123");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("encoded_password123");
        adminUser.setRole(UserRole.COMPANY_ADMIN);
        adminUser.setEnabled(true);
        adminUser.setEmailVerified(true);
        adminUser.setTwoFactorEnabled(false);

        Company company = new Company();
        company.setId("company-123");
        
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("password123", "encoded_password123")).thenReturn(true);
        when(securityService.isSuspiciousActivity(anyString(), anyString())).thenReturn(false);
        when(securityService.generateDeviceFingerprint(anyString(), anyString(), anyString())).thenReturn("device-123");
        when(securityService.isKnownDevice(anyString(), anyString())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(tokenProvider.createOrGetTokenPair(any(User.class), anyString(), anyString()))
            .thenReturn(new com.recrutech.recrutechauth.security.TokenPair("access", "refresh", 3600L));
        when(companyRepository.findByAdminUserId("admin-123")).thenReturn(Optional.of(company));

        // When: Login (which calls loadUserContext internally)
        com.recrutech.recrutechauth.dto.auth.LoginRequest loginRequest = 
            com.recrutech.recrutechauth.dto.auth.LoginRequest.builder()
                .email("admin@test.com")
                .password("password123")
                .rememberMe(false)
                .build();
        
        com.recrutech.recrutechauth.dto.auth.AuthResponse response = 
            authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");

        // Then: Should load company context
        assertNotNull(response);
        assertEquals(UserRole.COMPANY_ADMIN, response.userRole());
        assertNotNull(response.userContext());
        verify(companyRepository).findByAdminUserId("admin-123");
    }

    @Test
    @DisplayName("loadUserContext should return HREmployee for HR role")
    void testLoadUserContextHR() {
        // Given: HR user login
        User hrUser = new User();
        hrUser.setId("hr-123");
        hrUser.setEmail("hr@test.com");
        hrUser.setPassword("encoded_password123");
        hrUser.setRole(UserRole.HR);
        hrUser.setEnabled(true);
        hrUser.setEmailVerified(true);
        hrUser.setTwoFactorEnabled(false);

        HREmployee hrEmployee = new HREmployee();
        hrEmployee.setUserId("hr-123");
        
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail("hr@test.com")).thenReturn(Optional.of(hrUser));
        when(passwordEncoder.matches("password123", "encoded_password123")).thenReturn(true);
        when(securityService.isSuspiciousActivity(anyString(), anyString())).thenReturn(false);
        when(securityService.generateDeviceFingerprint(anyString(), anyString(), anyString())).thenReturn("device-123");
        when(securityService.isKnownDevice(anyString(), anyString())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(hrUser);
        when(tokenProvider.createOrGetTokenPair(any(User.class), anyString(), anyString()))
            .thenReturn(new com.recrutech.recrutechauth.security.TokenPair("access", "refresh", 3600L));
        when(hrEmployeeRepository.findByUserId("hr-123")).thenReturn(Optional.of(hrEmployee));

        // When: Login (which calls loadUserContext internally)
        com.recrutech.recrutechauth.dto.auth.LoginRequest loginRequest = 
            com.recrutech.recrutechauth.dto.auth.LoginRequest.builder()
                .email("hr@test.com")
                .password("password123")
                .rememberMe(false)
                .build();
        
        com.recrutech.recrutechauth.dto.auth.AuthResponse response = 
            authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");

        // Then: Should load HR employee context
        assertNotNull(response);
        assertEquals(UserRole.HR, response.userRole());
        assertNotNull(response.userContext());
        verify(hrEmployeeRepository).findByUserId("hr-123");
    }

    @Test
    @DisplayName("loadUserContext should return Applicant for APPLICANT role")
    void testLoadUserContextApplicant() {
        // Given: Applicant user login
        User applicantUser = new User();
        applicantUser.setId("applicant-123");
        applicantUser.setEmail("applicant@test.com");
        applicantUser.setPassword("encoded_password123");
        applicantUser.setRole(UserRole.APPLICANT);
        applicantUser.setEnabled(true);
        applicantUser.setEmailVerified(true);
        applicantUser.setTwoFactorEnabled(false);

        Applicant applicant = new Applicant();
        applicant.setUserId("applicant-123");
        
        when(securityService.isRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByEmail("applicant@test.com")).thenReturn(Optional.of(applicantUser));
        when(passwordEncoder.matches("password123", "encoded_password123")).thenReturn(true);
        when(securityService.isSuspiciousActivity(anyString(), anyString())).thenReturn(false);
        when(securityService.generateDeviceFingerprint(anyString(), anyString(), anyString())).thenReturn("device-123");
        when(securityService.isKnownDevice(anyString(), anyString())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(applicantUser);
        when(tokenProvider.createOrGetTokenPair(any(User.class), anyString(), anyString()))
            .thenReturn(new com.recrutech.recrutechauth.security.TokenPair("access", "refresh", 3600L));
        when(applicantRepository.findByUserId("applicant-123")).thenReturn(Optional.of(applicant));

        // When: Login (which calls loadUserContext internally)
        com.recrutech.recrutechauth.dto.auth.LoginRequest loginRequest = 
            com.recrutech.recrutechauth.dto.auth.LoginRequest.builder()
                .email("applicant@test.com")
                .password("password123")
                .rememberMe(false)
                .build();
        
        com.recrutech.recrutechauth.dto.auth.AuthResponse response = 
            authService.login(loginRequest, "192.168.1.1", "Mozilla/5.0", "en-US");

        // Then: Should load applicant context
        assertNotNull(response);
        assertEquals(UserRole.APPLICANT, response.userRole());
        assertNotNull(response.userContext());
        verify(applicantRepository).findByUserId("applicant-123");
    }
}