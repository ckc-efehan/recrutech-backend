package com.recrutech.recrutechauth.controller;

import com.recrutech.recrutechauth.service.AuthService;
import com.recrutech.recrutechauth.service.HttpRequestService;
import com.recrutech.recrutechauth.dto.auth.*;
import com.recrutech.recrutechauth.dto.registration.*;
import com.recrutech.recrutechauth.dto.common.*;
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
@CrossOrigin(origins = "*", maxAge = 3600)
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
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        // Use factory method to create healthy response
        HealthResponse response = HealthResponse.createHealthyResponse();
        return ResponseEntity.ok(response);
    }

}