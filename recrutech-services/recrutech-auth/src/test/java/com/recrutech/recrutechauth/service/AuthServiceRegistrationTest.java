package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.registration.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.SecurityService;
import com.recrutech.recrutechauth.security.TokenProvider;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test suite for AuthService registration methods.
 * Tests:
 * - Company registration with simplified CompanyRegistrationRequest record
 * - HR registration with separate HRRegistrationRequest record
 * - Applicant registration with separate ApplicantRegistrationRequest record
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Registration Tests")
class AuthServiceRegistrationTest {

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
    @DisplayName("registerCompany should work with simplified CompanyRegistrationRequest record")
    void testRegisterCompanyWithSeparateRecord() {
        // Given: Valid company registration request using simplified structure
        CompanyRegistrationRequest request = CompanyRegistrationRequest.builder()
            .name("Test Company")
            .location("Test Location")
            .businessEmail("business@test.com")
            .telephone("1234567890")
            .firstName("Admin")
            .lastName("User")
            .password("password123")
            .agreementAccepted(true)
            .build();

        // Mock dependencies
        when(companyRepository.existsByBusinessEmail(anyString())).thenReturn(false);
        when(companyRepository.existsByTelephone(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordValidator.validate(anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(tokenProvider.generateSecureToken()).thenReturn("secure_token");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Calling registerCompany
        CompanyRegistrationResponse response = authService.registerCompany(request);

        // Then: Should return successful response
        assertNotNull(response);
        verify(companyRepository).existsByBusinessEmail("business@test.com");
        verify(companyRepository).existsByTelephone("1234567890");
        verify(userRepository, times(2)).save(any()); // Admin and HR users
        verify(companyRepository).save(any());
        verify(hrEmployeeRepository).save(any());
    }

    @Test
    @DisplayName("registerHR should work with separate HRRegistrationRequest record")
    void testRegisterHRWithSeparateRecord() {
        // Given: Valid HR registration request using separate record
        HRRegistrationRequest request = HRRegistrationRequest.builder()
            .email("hr@test.com")
            .password("password123")
            .firstName("HR")
            .lastName("User")
            .companyId("company-123")
            .invitationToken("validInvitationToken123456789")
            .department("Human Resources")
            .position("HR Manager")
            .employeeId("HR001")
            .agreementAccepted(true)
            .build();

        // Mock dependencies
        when(companyRepository.findById(anyString())).thenReturn(java.util.Optional.of(new com.recrutech.recrutechauth.model.Company()));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordValidator.validate(anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Calling registerHR
        UserRegistrationResponse response = authService.registerHR(request);

        // Then: Should return successful response
        assertNotNull(response);
        verify(companyRepository).findById("company-123");
        verify(userRepository).save(any());
        verify(hrEmployeeRepository).save(any());
    }

    @Test
    @DisplayName("registerApplicant should work with separate ApplicantRegistrationRequest record")
    void testRegisterApplicantWithSeparateRecord() {
        // Given: Valid applicant registration request using separate record
        ApplicantRegistrationRequest request = ApplicantRegistrationRequest.builder()
            .personalInfo(ApplicantRegistrationRequest.PersonalInfo.builder()
                .email("applicant@test.com")
                .password("password123")
                .firstName("John")
                .lastName("Applicant")
                .phoneNumber("1234567890")
                .currentLocation("Test City")
                .build())
            .privacyConsent(true)
            .build();

        // Mock dependencies
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordValidator.validate(anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Calling registerApplicant
        UserRegistrationResponse response = authService.registerApplicant(request);

        // Then: Should return successful response
        assertNotNull(response);
        verify(userRepository).save(any());
        verify(applicantRepository).save(any());
    }
}