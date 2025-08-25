package com.recrutech.recrutechauth.exception;

import com.recrutech.recrutechauth.dto.auth.AuthResponse;
import com.recrutech.recrutechauth.dto.auth.LogoutResponse;
import com.recrutech.recrutechauth.dto.registration.UserRegistrationResponse;
import com.recrutech.recrutechauth.dto.common.ErrorResponse;
import com.recrutech.common.exception.NotFoundException;
import com.recrutech.common.exception.ValidationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Centralized exception handler for authentication-related exceptions.
 * Provides consistent error responses across all auth endpoints.
 */
@ControllerAdvice(basePackages = "com.recrutech.recrutechauth")
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse errorResponse = ErrorResponse.createAuthenticationError("Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<AuthResponse> handleAccountDisabled(AccountDisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(AuthResponse.createErrorResponse("Account is disabled"));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<AuthResponse> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED)
            .body(AuthResponse.createErrorResponse("Account is temporarily locked"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<AuthResponse> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(AuthResponse.createErrorResponse("Email address not verified"));
    }

    @ExceptionHandler(PasswordExpiredException.class)
    public ResponseEntity<AuthResponse> handlePasswordExpired(PasswordExpiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(AuthResponse.createErrorResponse("Password has expired"));
    }

    @ExceptionHandler(SuspiciousActivityException.class)
    public ResponseEntity<AuthResponse> handleSuspiciousActivity(SuspiciousActivityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(AuthResponse.createErrorResponse("Suspicious activity detected"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<AuthResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(AuthResponse.createErrorResponse("Invalid or expired token"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponse errorResponse = ErrorResponse.createAuthenticationError(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<Object> handleRegistrationException(RegistrationException ex) {
        // Return appropriate response type based on context
        // For now, return a generic UserRegistrationResponse
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(UserRegistrationResponse.builder()
                .message("Registration failed: " + ex.getMessage())
                .build());
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<AuthResponse> handleAuthServiceException(AuthServiceException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(AuthResponse.createErrorResponse("Authentication service error"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        ErrorResponse errorResponse = ErrorResponse.createRateLimitError(ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.createError("VALIDATION_ERROR", ex.getMessage(), 400);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<LogoutResponse> handleLogoutFailure(RuntimeException ex) {
        // Handle logout-related runtime exceptions
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("logout")) {
            LogoutResponse response = LogoutResponse.createFailureResponse(ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        // For other runtime exceptions, rethrow to be handled by default handler
        throw ex;
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        ErrorResponse errorResponse = ErrorResponse.createConflictError(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.createNotFoundError("Resource");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse errorResponse = ErrorResponse.createValidationError(List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
        ErrorResponse errorResponse = ErrorResponse.createValidationError(validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.createAuthorizationError("Access denied: Insufficient permissions");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.createInternalServerError("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

}