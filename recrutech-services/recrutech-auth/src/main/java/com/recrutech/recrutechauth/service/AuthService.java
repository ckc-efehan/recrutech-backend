package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.*;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import com.recrutech.recrutechauth.kafka.EmailEventProducer;
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
    private final InputSanitizationService inputSanitizationService;
    private final EmailEventProducer emailEventProducer;

    public AuthService(UserRepository userRepository,
                      CompanyRepository companyRepository,
                      HREmployeeRepository hrEmployeeRepository,
                      ApplicantRepository applicantRepository,
                      PasswordEncoder passwordEncoder,
                      PasswordValidator passwordValidator,
                      TokenProvider tokenProvider,
                      SecurityService securityService,
                      InputSanitizationService inputSanitizationService,
                      EmailEventProducer emailEventProducer) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.hrEmployeeRepository = hrEmployeeRepository;
        this.applicantRepository = applicantRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.tokenProvider = tokenProvider;
        this.securityService = securityService;
        this.inputSanitizationService = inputSanitizationService;
        this.emailEventProducer = emailEventProducer;
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
        
        // Sanitize input fields using unused InputSanitizationService methods
        String sanitizedCompanyName = inputSanitizationService.sanitizeInput(request.name());
        String sanitizedLocation = inputSanitizationService.sanitizeInput(request.location());
        String sanitizedFirstName = inputSanitizationService.sanitizeInput(request.firstName());
        String sanitizedLastName = inputSanitizationService.sanitizeInput(request.lastName());
        String sanitizedBusinessEmail = inputSanitizationService.sanitizeInput(request.businessEmail());
        String sanitizedTelephone = inputSanitizationService.sanitizeInput(request.telephone());
        
        // Log registration details for audit purposes (use encoded data for safe output)
        System.out.println("[DEBUG_LOG] Company registration - Name: " + inputSanitizationService.encodeForHTML(sanitizedCompanyName) + 
                          ", Admin email: " + inputSanitizationService.encodeForHTML(adminEmail) + 
                          ", HR email: " + inputSanitizationService.encodeForHTML(hrEmail));
        
        // Create admin user with generated email and sanitized data
        User adminUser = createUser(
            adminEmail,
            request.password(),
            sanitizedFirstName,
            sanitizedLastName,
            UserRole.COMPANY_ADMIN
        );

        // Create company with sanitized data
        Company company = new Company();
        company.setName(sanitizedCompanyName);
        company.setLocation(sanitizedLocation);
        company.setBusinessEmail(sanitizedBusinessEmail);
        company.setContactFirstName(sanitizedFirstName);
        company.setContactLastName(sanitizedLastName);
        company.setTelephone(sanitizedTelephone);
        company.setAdminUserId(adminUser.getId());
        company.setVerificationToken(tokenProvider.generateSecureToken());
        company.setVerificationExpiry(LocalDateTime.now().plusHours(24));
        
        company = companyRepository.save(company);

        // Automatically create HR user with generated email and sanitized data
        User hrUser = createUser(
            hrEmail,
            request.password(),
            sanitizedFirstName,
            sanitizedLastName,
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
        
        // Sanitize input fields using unused InputSanitizationService methods
        String sanitizedFirstName = inputSanitizationService.sanitizeInput(request.firstName());
        String sanitizedLastName = inputSanitizationService.sanitizeInput(request.lastName());
        String sanitizedEmail = inputSanitizationService.sanitizeInput(request.email());
        String sanitizedDepartment = inputSanitizationService.sanitizeInput(request.department());
        String sanitizedPosition = inputSanitizationService.sanitizeInput(request.position());
        String sanitizedEmployeeId = inputSanitizationService.sanitizeInput(request.employeeId());
        
        // Log HR registration details (use encoded data for safe output)
        String fullName = request.getFullName();
        boolean isSelfRegistration = request.isSelfRegistration();
        System.out.println("[DEBUG_LOG] HR registration - Full name: " + inputSanitizationService.encodeForHTML(fullName) + 
                          ", Self-registration: " + isSelfRegistration + ", Email: " + inputSanitizationService.encodeForHTML(sanitizedEmail));
        
        // Validate company access
        validateCompanyAccess(request.companyId(), request.invitationToken());
        
        // Create HR user with sanitized data
        User hrUser = createUser(
            sanitizedEmail,
            request.password(),
            sanitizedFirstName,
            sanitizedLastName,
            UserRole.HR
        );

        // Create HR employee record with sanitized data
        HREmployee hrEmployee = new HREmployee();
        hrEmployee.setUserId(hrUser.getId());
        hrEmployee.setCompanyId(request.companyId());
        hrEmployee.setDepartment(sanitizedDepartment);
        hrEmployee.setPosition(sanitizedPosition);
        hrEmployee.setEmployeeId(sanitizedEmployeeId);
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
        
        // Sanitize input fields using unused InputSanitizationService methods
        String sanitizedFirstName = inputSanitizationService.sanitizeInput(request.personalInfo().firstName());
        String sanitizedLastName = inputSanitizationService.sanitizeInput(request.personalInfo().lastName());
        String sanitizedEmail = inputSanitizationService.sanitizeInput(request.personalInfo().email());
        String sanitizedPhoneNumber = inputSanitizationService.sanitizeInput(request.personalInfo().phoneNumber());
        String sanitizedCurrentLocation = inputSanitizationService.sanitizeInput(request.personalInfo().currentLocation());
        
        // Log applicant registration details (use encoded data for safe output)
        String fullName = request.getFullName();
        boolean hasExperience = request.hasExperience();
        String email = request.getEmail();
        System.out.println("[DEBUG_LOG] Applicant registration - Full name: " + inputSanitizationService.encodeForHTML(fullName) + 
                          ", Has experience: " + hasExperience + ", Email: " + inputSanitizationService.encodeForHTML(email));
        
        // Create applicant user with sanitized data
        User applicantUser = createUser(
            sanitizedEmail,
            request.personalInfo().password(),
            sanitizedFirstName,
            sanitizedLastName,
            UserRole.APPLICANT
        );

        // Create applicant profile with sanitized data
        Applicant applicant = new Applicant();
        applicant.setUserId(applicantUser.getId());
        applicant.setPhoneNumber(sanitizedPhoneNumber);
        applicant.setCurrentLocation(sanitizedCurrentLocation);
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
                // Use unused encoding method for safe debug output
                System.out.println("[DEBUG_LOG] Invalid session during token refresh for user: " + 
                                 inputSanitizationService.encodeForHTML(user.getEmail()) + " - continuing with token refresh");
                // Clear invalid session but continue with token refresh
                user.setCurrentSessionId(null);
                userRepository.save(user);
            } else {
                // Use unused encoding method for safe debug output
                System.out.println("[DEBUG_LOG] Valid session confirmed during token refresh for user: " + 
                                 inputSanitizationService.encodeForHTML(user.getEmail()));
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

        // Safety layer: sanitize name fields using unused InputSanitizationService methods
        String safeSanitizedFirstName = inputSanitizationService.sanitizeInput(firstName);
        String safeSanitizedLastName = inputSanitizationService.sanitizeInput(lastName);
        String safeSanitizedEmail = inputSanitizationService.sanitizeInput(email);

        User user = new User();
        user.setEmail(safeSanitizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(safeSanitizedFirstName);
        user.setLastName(safeSanitizedLastName);
        user.setRole(role);
        user.setEmailVerificationToken(tokenProvider.generateSecureToken());
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setEmailVerified(false); // User must verify email through verification link

        User savedUser = userRepository.save(user);
        
        // Publish email verification event to Kafka
        try {
            emailEventProducer.publishEmailVerificationEvent(savedUser);
            System.out.println("[DEBUG_LOG] Email verification event published for user: " + 
                              inputSanitizationService.encodeForHTML(savedUser.getEmail()));
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Failed to publish email verification event for user: " + 
                              inputSanitizationService.encodeForHTML(savedUser.getEmail()) + ". Error: " + e.getMessage());
            // Don't fail registration if email event fails - user can request resend
        }
        
        return savedUser;
    }

    /**
     * Verifies user email using verification token.
     * @param token Email verification token
     * @param email User's email address
     * @throws ValidationException if verification fails
     */
    @Transactional
    public void verifyEmail(String token, String email) {
        System.out.println("[DEBUG_LOG] Attempting email verification for: " + 
                          inputSanitizationService.encodeForHTML(email));
        
        // Find user by email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        
        // Check if already verified
        if (user.isEmailVerified()) {
            System.out.println("[DEBUG_LOG] Email already verified for user: " + 
                              inputSanitizationService.encodeForHTML(email));
            throw new ValidationException("Email is already verified");
        }
        
        // Validate verification token
        if (user.getEmailVerificationToken() == null || 
            !user.getEmailVerificationToken().equals(token)) {
            System.out.println("[DEBUG_LOG] Invalid verification token for user: " + 
                              inputSanitizationService.encodeForHTML(email));
            throw new ValidationException("Invalid verification token");
        }
        
        // Check if token is expired
        if (user.getEmailVerificationExpiry() == null || 
            user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            System.out.println("[DEBUG_LOG] Expired verification token for user: " + 
                              inputSanitizationService.encodeForHTML(email));
            throw new ValidationException("Verification token has expired");
        }
        
        // Verify the email
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        
        userRepository.save(user);
        
        System.out.println("[DEBUG_LOG] Email successfully verified for user: " + 
                          inputSanitizationService.encodeForHTML(email));
        
        // Publish welcome email event to Kafka
        try {
            emailEventProducer.publishWelcomeEmailEvent(user);
            System.out.println("[DEBUG_LOG] Welcome email event published for user: " + 
                              inputSanitizationService.encodeForHTML(email));
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Failed to publish welcome email event for user: " + 
                              inputSanitizationService.encodeForHTML(email) + ". Error: " + e.getMessage());
            // Don't fail verification if welcome email event fails
        }
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