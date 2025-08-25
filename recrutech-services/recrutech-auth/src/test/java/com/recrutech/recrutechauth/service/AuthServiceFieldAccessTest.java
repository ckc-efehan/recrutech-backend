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
 * Test suite for AuthService field access patterns.
 * Tests:
 * - Service uses correct record field access patterns
 * - Record syntax (field() instead of getField()) with simplified structure
 * - Field access validation for registration methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Field Access Tests")
class AuthServiceFieldAccessTest {

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
    @DisplayName("Service should use correct record field access patterns")
    void testRecordFieldAccessPatterns() {
        // This test verifies that the service correctly accesses record fields
        // using the record syntax (field() instead of getField()) with simplified structure
        
        // Given: Real request object with simplified flat structure
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

        // Mock dependencies for successful execution
        when(companyRepository.existsByBusinessEmail(anyString())).thenReturn(false);
        when(companyRepository.existsByTelephone(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordValidator.validate(anyString())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(tokenProvider.generateSecureToken()).thenReturn("secure_token");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Calling the service method
        CompanyRegistrationResponse response = authService.registerCompany(request);

        // Then: Should successfully access all record fields
        assertNotNull(response);
        
        // Verify that the record field access worked correctly by checking
        // that the correct values were used in repository calls
        verify(companyRepository).existsByBusinessEmail("business@test.com");
        verify(companyRepository).existsByTelephone("1234567890");
        // Verify that auto-generated emails were used for user creation
        verify(userRepository, times(4)).existsByEmail(anyString()); // 2 for existence check + 2 for user creation
    }
}