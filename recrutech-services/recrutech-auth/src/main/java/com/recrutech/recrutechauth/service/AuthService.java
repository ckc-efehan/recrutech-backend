package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.*;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import com.recrutech.recrutechauth.dto.auth.*;
import com.recrutech.recrutechauth.dto.registration.*;
import com.recrutech.recrutechauth.exception.BadCredentialsException;
import com.recrutech.recrutechauth.exception.AccountDisabledException;
import com.recrutech.recrutechauth.exception.AccountLockedException;
import com.recrutech.recrutechauth.exception.EmailNotVerifiedException;
import com.recrutech.recrutechauth.exception.PasswordExpiredException;
import com.recrutech.recrutechauth.exception.SuspiciousActivityException;
import com.recrutech.recrutechauth.exception.ConflictException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.common.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Authentication service using only public DTOs and exceptions.
 * Handles login, registration, and user management for all user types.
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final HREmployeeRepository hrEmployeeRepository;
    private final ApplicantRepository applicantRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TokenProvider tokenProvider;
    private final SecurityService securityService;

    public AuthService(UserRepository userRepository,
                      CompanyRepository companyRepository,
                      HREmployeeRepository hrEmployeeRepository,
                      ApplicantRepository applicantRepository,
                      PasswordEncoder passwordEncoder,
                      PasswordValidator passwordValidator,
                      TokenProvider tokenProvider,
                      SecurityService securityService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.hrEmployeeRepository = hrEmployeeRepository;
        this.applicantRepository = applicantRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.tokenProvider = tokenProvider;
        this.securityService = securityService;
    }

    /**
     * Unified login for all user types with comprehensive security checks.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp, String userAgent, String acceptLanguage) {
        
        // Rate limiting check
        if (securityService.isRateLimited(request.email())) {
            throw new SecurityException("Too many login attempts. Please try again later.");
        }

        // Load user
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Account status checks
        if (!user.isEnabled()) {
            throw new AccountDisabledException("Account is disabled");
        }

        if (user.isAccountLocked()) {
            throw new AccountLockedException("Account is temporarily locked");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email address not verified");
        }

        // Password verification
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            securityService.recordFailedAttempt(user);
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Password expiration check
        if (user.isPasswordExpired()) {
            throw new PasswordExpiredException("Password has expired");
        }

        // Two-factor authentication check
        if (user.isTwoFactorEnabled()) {
            // Generate temporary token for 2FA verification
            String tempToken = tokenProvider.generateSecureToken();
            securityService.logSecurityEvent(user.getId(), "2FA_REQUIRED", "Two-factor authentication required", clientIp);
            return AuthResponse.createTwoFactorRequiredResponse(tempToken);
        }

        // Suspicious activity check
        if (securityService.isSuspiciousActivity(user.getId(), clientIp)) {
            securityService.logSecurityEvent(user.getId(), "SUSPICIOUS_LOGIN", "Multiple IPs detected", clientIp);
            throw new SuspiciousActivityException("Suspicious activity detected");
        }

        // Device fingerprinting
        String deviceFingerprint = securityService.generateDeviceFingerprint(userAgent, acceptLanguage, clientIp);
        boolean isKnownDevice = securityService.isKnownDevice(user.getId(), deviceFingerprint);
        
        if (!isKnownDevice) {
            securityService.logSecurityEvent(user.getId(), "NEW_DEVICE_LOGIN", "Unknown device", clientIp);
            securityService.registerDevice(user.getId(), deviceFingerprint);
        }

        // Successful login
        securityService.clearFailedAttempts(user);
        securityService.clearRateLimit(request.email());
        
        // Validate existing session if present (utilizing unused isSessionValid method)
        if (user.getCurrentSessionId() != null) {
            boolean isCurrentSessionValid = tokenProvider.isSessionValid(user.getCurrentSessionId());
            if (!isCurrentSessionValid) {
                System.out.println("[DEBUG_LOG] Invalid existing session detected for user: " + user.getEmail());
                user.setCurrentSessionId(null); // Clear invalid session
            } else {
                System.out.println("[DEBUG_LOG] Valid existing session found for user: " + user.getEmail());
            }
        }

        // Create session
        String sessionId = UUID.randomUUID().toString();
        user.setCurrentSessionId(sessionId);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userRepository.save(user);

        // Create or get token pair (automatic generation)
        TokenPair tokenPair = tokenProvider.createOrGetTokenPair(user, sessionId, clientIp);
        
        // Load additional context data
        Object userContext = loadUserContext(user);

        // Use factory method to create success response
        return AuthResponse.createSuccessResponse(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            tokenPair.expiresIn(),
            user.getRole(),
            user.getId(),
            userContext
        );
    }

    /**
     * Company registration with automatic admin and HR user creation.
     * Creates both admin and HR users from single input with auto-generated emails.
     */
    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        
        // Business rule validation
        request.validateBusinessRules();
        
        // Validate unique business email and telephone
        if (companyRepository.existsByBusinessEmail(request.businessEmail())) {
            throw new ConflictException("Business email already exists");
        }
        if (companyRepository.existsByTelephone(request.telephone())) {
            throw new ConflictException("Telephone number already exists");
        }
        
        // Generate admin and HR emails automatically
        String adminEmail = request.generateAdminEmail();
        String hrEmail = request.generateHREmail();
        
        // Validate that generated emails don't already exist
        if (userRepository.existsByEmail(adminEmail)) {
            throw new ConflictException("Admin email already exists: " + adminEmail);
        }
        if (userRepository.existsByEmail(hrEmail)) {
            throw new ConflictException("HR email already exists: " + hrEmail);
        }
        
        // Log registration details for audit purposes
        System.out.println("[DEBUG_LOG] Company registration - Name: " + request.name() + 
                          ", Admin email: " + adminEmail + ", HR email: " + hrEmail);
        
        // Create admin user with generated email
        User adminUser = createUser(
            adminEmail,
            request.password(),
            request.firstName(),
            request.lastName(),
            UserRole.COMPANY_ADMIN
        );

        // Create company
        Company company = new Company();
        company.setName(request.name());
        company.setLocation(request.location());
        company.setBusinessEmail(request.businessEmail());
        company.setContactFirstName(request.firstName());
        company.setContactLastName(request.lastName());
        company.setTelephone(request.telephone());
        company.setAdminUserId(adminUser.getId());
        company.setVerificationToken(tokenProvider.generateSecureToken());
        company.setVerificationExpiry(LocalDateTime.now().plusHours(24));
        
        company = companyRepository.save(company);

        // Automatically create HR user with generated email
        User hrUser = createUser(
            hrEmail,
            request.password(),
            request.firstName(),
            request.lastName(),
            UserRole.HR
        );

        // Create HR employee record
        HREmployee hrEmployee = new HREmployee();
        hrEmployee.setUserId(hrUser.getId());
        hrEmployee.setCompanyId(company.getId());
        hrEmployee.setDepartment("Human Resources");
        hrEmployee.setPosition("HR Manager");
        hrEmployee.setHireDate(LocalDate.now());
        hrEmployeeRepository.save(hrEmployee);

        return CompanyRegistrationResponse.createSuccessResponse(
            company.getId(),
            adminUser.getId(),
            hrUser.getId(),
            null  // No temporary password needed since both users use the provided password
        );
    }

    /**
     * HR user registration.
     */
    @Transactional
    public UserRegistrationResponse registerHR(HRRegistrationRequest request) {
        
        // Business rule validation
        request.validateBusinessRules();
        
        // Log HR registration details
        String fullName = request.getFullName();
        boolean isSelfRegistration = request.isSelfRegistration();
        System.out.println("[DEBUG_LOG] HR registration - Full name: " + fullName + ", Self-registration: " + isSelfRegistration);
        
        // Validate company access
        validateCompanyAccess(request.companyId(), request.invitationToken());
        
        // Create HR user
        User hrUser = createUser(
            request.email(),
            request.password(),
            request.firstName(),
            request.lastName(),
            UserRole.HR
        );

        // Create HR employee record
        HREmployee hrEmployee = new HREmployee();
        hrEmployee.setUserId(hrUser.getId());
        hrEmployee.setCompanyId(request.companyId());
        hrEmployee.setDepartment(request.department());
        hrEmployee.setPosition(request.position());
        hrEmployee.setEmployeeId(request.employeeId());
        hrEmployeeRepository.save(hrEmployee);

        return UserRegistrationResponse.createSuccessResponse(hrUser.getId(), "hr");
    }

    /**
     * Applicant registration.
     */
    @Transactional
    public UserRegistrationResponse registerApplicant(ApplicantRegistrationRequest request) {
        
        // Business rule validation
        request.validateBusinessRules();
        
        // Log applicant registration details
        String fullName = request.getFullName();
        boolean hasExperience = request.hasExperience();
        String email = request.getEmail();
        System.out.println("[DEBUG_LOG] Applicant registration - Full name: " + fullName + 
                          ", Has experience: " + hasExperience + ", Email: " + email);
        
        // Create applicant user
        User applicantUser = createUser(
            request.personalInfo().email(),
            request.personalInfo().password(),
            request.personalInfo().firstName(),
            request.personalInfo().lastName(),
            UserRole.APPLICANT
        );

        // Create applicant profile
        Applicant applicant = new Applicant();
        applicant.setUserId(applicantUser.getId());
        applicant.setPhoneNumber(request.personalInfo().phoneNumber());
        applicant.setCurrentLocation(request.personalInfo().currentLocation());
        applicantRepository.save(applicant);

        return UserRegistrationResponse.createSuccessResponse(applicantUser.getId(), "applicant");
    }

    /**
     * Refresh token endpoint.
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken, String clientIp) {
        TokenPair newTokenPair = tokenProvider.refreshToken(refreshToken, clientIp);
        
        String userId = tokenProvider.getUserIdFromToken(newTokenPair.accessToken());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate current session (utilizing unused isSessionValid method)
        if (user.getCurrentSessionId() != null) {
            boolean isCurrentSessionValid = tokenProvider.isSessionValid(user.getCurrentSessionId());
            if (!isCurrentSessionValid) {
                System.out.println("[DEBUG_LOG] Invalid session during token refresh for user: " + user.getEmail() + " - continuing with token refresh");
                // Clear invalid session but continue with token refresh
                user.setCurrentSessionId(null);
                userRepository.save(user);
            } else {
                System.out.println("[DEBUG_LOG] Valid session confirmed during token refresh for user: " + user.getEmail());
            }
        }
        
        Object userContext = loadUserContext(user);
        
        return AuthResponse.builder()
            .accessToken(newTokenPair.accessToken())
            .refreshToken(newTokenPair.refreshToken())
            .expiresIn(newTokenPair.expiresIn())
            .userRole(user.getRole())
            .userId(user.getId())
            .userContext(userContext)
            .requiresTwoFactor(false)
            .build();
    }

    /**
     * Logout user and invalidate tokens.
     */
    @Transactional
    public void logout(String accessToken, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Blacklist current tokens
        tokenProvider.blacklistToken(accessToken);
        if (user.getCurrentRefreshToken() != null) {
            tokenProvider.blacklistToken(user.getCurrentRefreshToken());
        }
        
        // Invalidate session
        if (user.getCurrentSessionId() != null) {
            tokenProvider.invalidateSession(user.getCurrentSessionId());
            securityService.removeActiveSession(userId, user.getCurrentSessionId());
        }
        
        // Clear user tokens
        user.clearTokens();
        userRepository.save(user);
    }

    // Private helper methods
    
    private User createUser(String email, String password, String firstName, String lastName, UserRole role) {
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email address already exists");
        }
        
        // Validate password is provided
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password is required and cannot be empty");
        }
        
        // Validate password
        List<String> passwordErrors = passwordValidator.validate(password);
        if (!passwordErrors.isEmpty()) {
            throw new ValidationException("Password validation failed: " + String.join(", ", passwordErrors));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setEmailVerificationToken(tokenProvider.generateSecureToken());
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setEmailVerified(true); // Auto-verify since no email service

        return userRepository.save(user);
    }



    private void validateCompanyAccess(String companyId, String invitationToken) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Company not found"));
        
        // For now, just check if company exists
        // In a real implementation, you would validate the invitation token
    }

    private Object loadUserContext(User user) {
        return switch (user.getRole()) {
            case COMPANY_ADMIN -> companyRepository.findByAdminUserId(user.getId()).orElse(null);
            case HR -> hrEmployeeRepository.findByUserId(user.getId()).orElse(null);
            case APPLICANT -> applicantRepository.findByUserId(user.getId()).orElse(null);
        };
    }
}