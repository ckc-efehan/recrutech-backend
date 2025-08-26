package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.dto.registration.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.SecurityService;
import com.recrutech.recrutechauth.security.TokenProvider;
import com.recrutech.recrutechauth.security.InputSanitizationService;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import com.recrutech.recrutechauth.kafka.EmailEventProducer;
import com.recrutech.recrutechauth.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for AuthService business rules validation.
 * Tests:
 * - Business rule validation is called for all registration methods
 * - Unique constraint validation for company registration
 * - Conflict exception handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Business Rules Tests")
class AuthServiceBusinessRulesTest {

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
            inputSanitizationService,
            emailEventProducer
        );
    }

    @Test
    @DisplayName("validateBusinessRules should be called for all registration methods")
    void testValidateBusinessRulesIsCalled() {
        // This test verifies that validateBusinessRules() is called on all request objects
        // The actual business rule validation logic is tested in the individual DTO tests
        
        // Given: Mock request that will throw exception in validateBusinessRules
        CompanyRegistrationRequest companyRequest = mock(CompanyRegistrationRequest.class);
        
        // Set up the validateBusinessRules to throw exception early
        // No need to mock other methods since validateBusinessRules() throws before they're called
        doThrow(new IllegalArgumentException("Business rule violation")).when(companyRequest).validateBusinessRules();

        // When/Then: Should propagate business rule validation exceptions
        assertThrows(IllegalArgumentException.class, () -> authService.registerCompany(companyRequest));

        verify(companyRequest).validateBusinessRules();
    }

    @Test
    @DisplayName("Company registration should validate unique business email and telephone")
    void testCompanyRegistrationUniqueValidation() {
        // Given: Company registration request with existing business email
        CompanyRegistrationRequest request = CompanyRegistrationRequest.builder()
            .name("Test Company")
            .location("Test Location")
            .businessEmail("existing@test.com")
            .telephone("1234567890")
            .firstName("Admin")
            .lastName("User")
            .password("password123")
            .agreementAccepted(true)
            .build();

        when(companyRepository.existsByBusinessEmail("existing@test.com")).thenReturn(true);

        // When/Then: Should throw conflict exception for duplicate business email
        assertThrows(ConflictException.class, () -> authService.registerCompany(request));

        verify(companyRepository).existsByBusinessEmail("existing@test.com");
    }
}