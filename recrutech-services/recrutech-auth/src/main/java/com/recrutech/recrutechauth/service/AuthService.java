package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.model.*;
import com.recrutech.recrutechauth.repository.*;
import com.recrutech.recrutechauth.security.*;
import com.recrutech.recrutechauth.validator.PasswordValidator;
import com.recrutech.recrutechauth.kafka.EmailEventProducer;
import com.recrutech.recrutechauth.kafka.AuthEventPublisherService;
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
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TokenProvider tokenProvider;
    private final SecurityService securityService;
    private final InputSanitizationService inputSanitizationService;
    private final EmailEventProducer emailEventProducer;
    private final AuthEventPublisherService authEventPublisherService;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      PasswordValidator passwordValidator,
                      TokenProvider tokenProvider,
                      SecurityService securityService,
                      InputSanitizationService inputSanitizationService,
                      EmailEventProducer emailEventProducer,
                      AuthEventPublisherService authEventPublisherService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.tokenProvider = tokenProvider;
        this.securityService = securityService;
        this.inputSanitizationService = inputSanitizationService;
        this.emailEventProducer = emailEventProducer;
        this.authEventPublisherService = authEventPublisherService;
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
        
        // Phase 2: Domain data (userContext, companyId) is in platform service, not available here
        // Clients should query platform service for domain data using userId from token

        // Use factory method to create success response
        return AuthResponse.createSuccessResponse(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            tokenPair.expiresIn(),
            user.getRole(),
            user.getId(),
            null,  // companyId not available (domain data in platform)
            null   // userContext not available (domain data in platform)
        );
    }

    /**
     * Company registration - Phase 2 refactored.
     * Creates only admin User account and publishes UserRegisteredEvent.
     * Platform service consumes event to create Company domain entity.
     */
    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        
        // Business rule validation
        request.validateBusinessRules();
        
        // Generate admin email automatically
        String adminEmail = request.generateAdminEmail();
        
        // Validate that generated email doesn't already exist
        if (userRepository.existsByEmail(adminEmail)) {
            throw new ConflictException("Admin email already exists: " + adminEmail);
        }
        
        // Sanitize input fields
        String sanitizedCompanyName = inputSanitizationService.sanitizeInput(request.name());
        String sanitizedLocation = inputSanitizationService.sanitizeInput(request.location());
        String sanitizedFirstName = inputSanitizationService.sanitizeInput(request.firstName());
        String sanitizedLastName = inputSanitizationService.sanitizeInput(request.lastName());
        String sanitizedBusinessEmail = inputSanitizationService.sanitizeInput(request.businessEmail());
        String sanitizedTelephone = inputSanitizationService.sanitizeInput(request.telephone());
        
        // Log registration details for audit purposes
        System.out.println("[DEBUG_LOG] Company registration - Name: " + inputSanitizationService.encodeForHTML(sanitizedCompanyName) + 
                          ", Admin email: " + inputSanitizationService.encodeForHTML(adminEmail));
        
        // Create admin user with generated email and sanitized data
        User adminUser = createUser(
            adminEmail,
            request.password(),
            sanitizedFirstName,
            sanitizedLastName,
            UserRole.COMPANY_ADMIN
        );

        // Prepare company context data as JSON for platform service
        String companyContext = String.format(
            "{\"companyName\":\"%s\",\"location\":\"%s\",\"businessEmail\":\"%s\"," +
            "\"contactFirstName\":\"%s\",\"contactLastName\":\"%s\",\"telephone\":\"%s\"}",
            sanitizedCompanyName, sanitizedLocation, sanitizedBusinessEmail,
            sanitizedFirstName, sanitizedLastName, sanitizedTelephone
        );
        
        // Publish UserRegisteredEvent for platform service to create Company entity
        authEventPublisherService.publishUserRegisteredEvent(adminUser, companyContext);

        return CompanyRegistrationResponse.createSuccessResponse(
            null,  // companyId not available immediately (created async by platform)
            adminUser.getId(),
            null,  // hrUserId not created in this flow anymore
            null
        );
    }

    /**
     * HR user registration - Phase 2 refactored.
     * Creates only User account and publishes UserRegisteredEvent.
     * Platform service consumes event to create HREmployee domain entity.
     */
    @Transactional
    public UserRegistrationResponse registerHR(HRRegistrationRequest request) {
        
        // Business rule validation
        request.validateBusinessRules();
        
        // Sanitize input fields
        String sanitizedFirstName = inputSanitizationService.sanitizeInput(request.firstName());
        String sanitizedLastName = inputSanitizationService.sanitizeInput(request.lastName());
        String sanitizedEmail = inputSanitizationService.sanitizeInput(request.email());
        String sanitizedDepartment = inputSanitizationService.sanitizeInput(request.department());
        String sanitizedPosition = inputSanitizationService.sanitizeInput(request.position());
        String sanitizedEmployeeId = inputSanitizationService.sanitizeInput(request.employeeId());
        
        // Log HR registration details
        String fullName = request.getFullName();
        boolean isSelfRegistration = request.isSelfRegistration();
        System.out.println("[DEBUG_LOG] HR registration - Full name: " + inputSanitizationService.encodeForHTML(fullName) + 
                          ", Self-registration: " + isSelfRegistration + ", Email: " + inputSanitizationService.encodeForHTML(sanitizedEmail));
        
        // Create HR user with sanitized data
        User hrUser = createUser(
            sanitizedEmail,
            request.password(),
            sanitizedFirstName,
            sanitizedLastName,
            UserRole.HR
        );

        // Prepare HR employee context data as JSON for platform service
        String hrContext = String.format(
            "{\"companyId\":\"%s\",\"department\":\"%s\",\"position\":\"%s\",\"employeeId\":\"%s\"}",
            request.companyId(), sanitizedDepartment, sanitizedPosition, sanitizedEmployeeId
        );
        
        // Publish UserRegisteredEvent for platform service to create HREmployee entity
        authEventPublisherService.publishUserRegisteredEvent(hrUser, hrContext);

        return UserRegistrationResponse.createSuccessResponse(hrUser.getId(), "hr");
    }

    /**
     * Applicant registration - Phase 2 refactored.
     * Creates only User account and publishes UserRegisteredEvent.
     * Platform service consumes event to create Applicant domain entity.
     */
    @Transactional
    public UserRegistrationResponse registerApplicant(ApplicantRegistrationRequest request) {
        
        // Business rule validation
        request.validateBusinessRules();
        
        // Sanitize input fields
        String sanitizedFirstName = inputSanitizationService.sanitizeInput(request.personalInfo().firstName());
        String sanitizedLastName = inputSanitizationService.sanitizeInput(request.personalInfo().lastName());
        String sanitizedEmail = inputSanitizationService.sanitizeInput(request.personalInfo().email());
        String sanitizedPhoneNumber = inputSanitizationService.sanitizeInput(request.personalInfo().phoneNumber());
        String sanitizedCurrentLocation = inputSanitizationService.sanitizeInput(request.personalInfo().currentLocation());
        
        // Log applicant registration details
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

        // Prepare applicant profile context data as JSON for platform service
        String applicantContext = String.format(
            "{\"phoneNumber\":\"%s\",\"currentLocation\":\"%s\"}",
            sanitizedPhoneNumber, sanitizedCurrentLocation
        );
        
        // Publish UserRegisteredEvent for platform service to create Applicant entity
        authEventPublisherService.publishUserRegisteredEvent(applicantUser, applicantContext);

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
        
        // Phase 2: Domain data is in platform service, not available here
        
        return AuthResponse.builder()
            .accessToken(newTokenPair.accessToken())
            .refreshToken(newTokenPair.refreshToken())
            .expiresIn(newTokenPair.expiresIn())
            .userRole(user.getRole())
            .userId(user.getId())
            .companyId(null)  // Domain data in platform service
            .userContext(null)  // Domain data in platform service
            .requiresTwoFactor(false)
            .build();
    }

    /**
     * Logout user and invalidate tokens.
     * Phase 2: Tokens are managed in Redis, not in User entity.
     */
    @Transactional
    public void logout(String accessToken, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Blacklist access token (tokens are in Redis, not DB)
        tokenProvider.blacklistToken(accessToken);
        
        // Invalidate session
        if (user.getCurrentSessionId() != null) {
            tokenProvider.invalidateSession(user.getCurrentSessionId());
            securityService.removeActiveSession(userId, user.getCurrentSessionId());
        }
        
        // Clear session
        user.clearSession();
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

    /**
     * Initiates password reset process for a user.
     * @param email User's email address
     * @throws NotFoundException if user is not found (but returns success for security)
     */
    @Transactional
    public void forgotPassword(String email) {
        System.out.println("[DEBUG_LOG] Password reset requested for email: " + 
                          inputSanitizationService.encodeForHTML(email));
        
        // Find user by email - for security, don't reveal if email exists
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            System.out.println("[DEBUG_LOG] Password reset requested for non-existent email: " + 
                              inputSanitizationService.encodeForHTML(email));
            // For security reasons, don't reveal that email doesn't exist
            // Just return success but don't send email
            return;
        }
        
        // Check if user is enabled and email is verified
        if (!user.isEnabled()) {
            System.out.println("[DEBUG_LOG] Password reset requested for disabled user: " + 
                              inputSanitizationService.encodeForHTML(email));
            return; // Don't send reset email for disabled accounts
        }
        
        if (!user.isEmailVerified()) {
            System.out.println("[DEBUG_LOG] Password reset requested for unverified email: " + 
                              inputSanitizationService.encodeForHTML(email));
            return; // Don't send reset email for unverified accounts
        }
        
        // Generate password reset token (15 minutes expiry for security)
        String resetToken = tokenProvider.generateSecureToken();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);
        
        // Save reset token to user
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiry(expiryTime);
        userRepository.save(user);
        
        System.out.println("[DEBUG_LOG] Password reset token generated for user: " + 
                          inputSanitizationService.encodeForHTML(email));
        
        // Create reset URL (assuming frontend will handle this endpoint)
        String resetUrl = "http://localhost:8081/api/auth/reset-password?token=" + resetToken + "&email=" + email;
        
        // Publish password reset email event to Kafka
        try {
            emailEventProducer.publishPasswordResetEvent(user, resetUrl);
            System.out.println("[DEBUG_LOG] Password reset email event published for user: " + 
                              inputSanitizationService.encodeForHTML(email));
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Failed to publish password reset email event for user: " + 
                              inputSanitizationService.encodeForHTML(email) + ". Error: " + e.getMessage());
            // Clear the reset token if email sending fails
            user.setPasswordResetToken(null);
            user.setPasswordResetExpiry(null);
            userRepository.save(user);
        }
    }

    /**
     * Resets user password using reset token.
     * @param token Password reset token
     * @param newPassword New password to set
     * @throws ValidationException if reset fails
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        System.out.println("[DEBUG_LOG] Password reset attempted with token");
        
        // Validate new password
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ValidationException("New password is required and cannot be empty");
        }
        
        List<String> passwordErrors = passwordValidator.validate(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new ValidationException("Password validation failed: " + String.join(", ", passwordErrors));
        }
        
        // Find user by reset token
        User user = userRepository.findByPasswordResetToken(token)
            .orElseThrow(() -> new ValidationException("Invalid or expired reset token"));
        
        // Check if token is expired
        if (user.getPasswordResetExpiry() == null || 
            user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            System.out.println("[DEBUG_LOG] Expired reset token used for user: " + 
                              inputSanitizationService.encodeForHTML(user.getEmail()));
            throw new ValidationException("Reset token has expired");
        }
        
        // Check if user is enabled
        if (!user.isEnabled()) {
            System.out.println("[DEBUG_LOG] Password reset attempted for disabled user: " + 
                              inputSanitizationService.encodeForHTML(user.getEmail()));
            throw new ValidationException("Account is disabled");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        
        // Clear reset token
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        
        // Clear session to force re-login (tokens are in Redis, not DB)
        user.clearSession();
        
        // Reset failed login attempts
        user.setFailedLoginAttempts(0);
        user.setLastFailedLogin(null);
        user.setAccountLockedUntil(null);
        
        userRepository.save(user);
        
        System.out.println("[DEBUG_LOG] Password successfully reset for user: " + 
                          inputSanitizationService.encodeForHTML(user.getEmail()));
    }

    // Phase 2: Helper methods removed - domain data is in platform service
    // validateCompanyAccess, loadUserContext, and extractCompanyId are no longer needed
    // Auth service only manages identity, not domain entities
}