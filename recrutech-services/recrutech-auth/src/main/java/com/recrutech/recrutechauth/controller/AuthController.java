package com.recrutech.recrutechauth.controller;

import com.recrutech.recrutechauth.service.AuthService;
import com.recrutech.recrutechauth.service.HttpRequestService;
import com.recrutech.recrutechauth.dto.auth.*;
import com.recrutech.recrutechauth.dto.registration.*;
import com.recrutech.recrutechauth.dto.common.*;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller using correct DTO references.
 * Provides unified endpoints for all user types.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final HttpRequestService httpRequestService;

    public AuthController(AuthService authService, HttpRequestService httpRequestService) {
        this.authService = authService;
        this.httpRequestService = httpRequestService;
    }

    /**
     * Unified login endpoint for all user types.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Validated(ValidationGroups.BasicValidation.class) @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate business rules before processing
        request.validateBusinessRules();
        
        String clientIp = httpRequestService.getClientIpAddress(httpRequest);
        String userAgent = httpRequestService.getUserAgent(httpRequest);
        String acceptLanguage = httpRequestService.getAcceptLanguage(httpRequest);
        
        AuthResponse response = authService.login(request, clientIp, userAgent, acceptLanguage);
        return ResponseEntity.ok(response);
    }

    /**
     * Company registration endpoint with automatic HR user creation.
     */
    @PostMapping("/register/company")
    public ResponseEntity<CompanyRegistrationResponse> registerCompany(
            @Validated({ValidationGroups.Create.class, ValidationGroups.StrictValidation.class}) @RequestBody CompanyRegistrationRequest request) {
        
        CompanyRegistrationResponse response = authService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * HR user registration endpoint.
     */
    @PostMapping("/register/hr")
    public ResponseEntity<UserRegistrationResponse> registerHR(
            @Validated(ValidationGroups.Create.class) @RequestBody HRRegistrationRequest request) {
        
        UserRegistrationResponse response = authService.registerHR(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Applicant registration endpoint.
     */
    @PostMapping("/register/applicant")
    public ResponseEntity<UserRegistrationResponse> registerApplicant(
            @Validated(ValidationGroups.Create.class) @RequestBody ApplicantRegistrationRequest request) {
        
        UserRegistrationResponse response = authService.registerApplicant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Token refresh endpoint.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Validated(ValidationGroups.BasicValidation.class) @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate token format before processing
        request.validateTokenFormat();
        
        String clientIp = httpRequestService.getClientIpAddress(httpRequest);
        
        AuthResponse response = authService.refreshToken(request.refreshToken(), clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint.
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader("Authorization") String authHeader,
            @Validated(ValidationGroups.BasicValidation.class) @RequestBody LogoutRequest request) {
        
        String accessToken = httpRequestService.extractTokenFromHeader(authHeader);
        authService.logout(accessToken, request.userId());
        
        // Use factory method to create success response
        LogoutResponse response = LogoutResponse.createSuccessResponse(
            request.logoutFromAllDevices() != null && request.logoutFromAllDevices()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Email verification endpoint.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @RequestParam("token") String token,
            @RequestParam("email") String email) {
        
        try {
            authService.verifyEmail(token, email);
            // Return success response or redirect to frontend success page
            return ResponseEntity.ok().body("E-Mail erfolgreich bestätigt! Sie können sich jetzt einloggen.");
        } catch (Exception e) {
            // Return error response or redirect to frontend error page
            return ResponseEntity.badRequest().body("Ungültiger oder abgelaufener Bestätigungslink.");
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        // Use factory method to create healthy response
        HealthResponse response = HealthResponse.createHealthyResponse();
        return ResponseEntity.ok(response);
    }

    /**
     * Forgot password endpoint - initiates password reset process.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ResetPasswordResponse> forgotPassword(
            @Validated(ValidationGroups.BasicValidation.class) @RequestBody ForgotPasswordRequest request) {
        
        // Validate business rules before processing
        request.validateBusinessRules();
        
        try {
            authService.forgotPassword(request.email());
            // Always return success for security (don't reveal if email exists)
            ResetPasswordResponse response = ResetPasswordResponse.createForgotPasswordSuccessResponse();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log error but still return success for security
            System.out.println("[DEBUG_LOG] Forgot password error: " + e.getMessage());
            ResetPasswordResponse response = ResetPasswordResponse.createForgotPasswordSuccessResponse();
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Password reset form endpoint - displays the password reset form.
     */
    @GetMapping("/reset-password")
    public String resetPasswordForm(
            @RequestParam("token") String token,
            @RequestParam("email") String email,
            Model model) {
        
        try {
            // Add token to model for form submission
            model.addAttribute("token", token);
            model.addAttribute("email", email);
            
            // Return the password reset form template
            return "reset-password-form";
        } catch (Exception e) {
            // If there's an error, redirect to an error page or show error message
            model.addAttribute("error", "Der Zurücksetzungslink ist ungültig oder abgelaufen.");
            return "reset-password-form";
        }
    }

    /**
     * Password reset confirmation endpoint - processes the password reset form submission.
     */
    @PostMapping("/reset-password-confirm")
    public ResponseEntity<ResetPasswordResponse> resetPasswordConfirm(
            @Validated(ValidationGroups.BasicValidation.class) @RequestBody ResetPasswordRequest request) {
        
        // Validate business rules before processing
        request.validateBusinessRules();
        
        try {
            authService.resetPassword(request.token(), request.newPassword());
            ResetPasswordResponse response = ResetPasswordResponse.createPasswordResetSuccessResponse();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Password reset error: " + e.getMessage());
            ResetPasswordResponse response = ResetPasswordResponse.createInvalidTokenResponse();
            return ResponseEntity.badRequest().body(response);
        }
    }

}