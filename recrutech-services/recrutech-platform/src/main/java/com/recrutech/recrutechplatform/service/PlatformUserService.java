package com.recrutech.recrutechplatform.service;

import com.recrutech.common.service.UserService;
import com.recrutech.recrutechplatform.dto.auth.AuthUserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Production-ready implementation of UserService for the platform service.
 * This implementation follows microservice best practices including:
 * - REST API calls to auth service for user data
 * - Distributed caching with Spring Cache and Redis for performance
 * - Circuit breaker pattern with Resilience4j for fault tolerance
 * - Retry mechanism for transient failures
 * - Proper error handling with fallback mechanisms
 * - Service isolation and loose coupling
 */
@Service
@Slf4j
public class PlatformUserService implements UserService {

    private final RestTemplate authServiceRestTemplate;

    public PlatformUserService(@Qualifier("authServiceRestTemplate") RestTemplate authServiceRestTemplate) {
        this.authServiceRestTemplate = authServiceRestTemplate;
    }

    /**
     * Retrieves user information from auth service with caching and circuit breaker.
     * This method is cached to avoid repeated API calls for the same user.
     *
     * @param userId the user ID
     * @return AuthUserResponse from auth service or fallback data
     */
    @Cacheable(value = "userInfo", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = "auth-service", fallbackMethod = "getUserInfoFallback")
    @Retry(name = "auth-service")
    public AuthUserResponse getUserInfo(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("getUserInfo called with null or empty userId");
            return AuthUserResponse.unknown(userId);
        }

        try {
            log.debug("Calling auth service for user: {}", userId);
            ResponseEntity<AuthUserResponse> response = authServiceRestTemplate.getForEntity(
                    "/api/users/{userId}", AuthUserResponse.class, userId);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Successfully retrieved user info for: {}", userId);
                return response.getBody();
            } else {
                log.warn("Auth service returned non-OK status for user {}: {}", userId, response.getStatusCode());
                return AuthUserResponse.unknown(userId);
            }
        } catch (RestClientException e) {
            log.error("Failed to call auth service for user {}: {}", userId, e.getMessage());
            throw e; // Let circuit breaker handle this
        }
    }

    /**
     * Fallback method for getUserInfo when auth service is unavailable.
     * This ensures the service remains functional even when auth service is down.
     *
     * @param userId the user ID
     * @param ex the exception that triggered the fallback
     * @return fallback AuthUserResponse
     */
    private AuthUserResponse getUserInfoFallback(String userId, Exception ex) {
        log.warn("Using fallback for user {}: {}", userId, ex.getMessage());
        return AuthUserResponse.unknown(userId);
    }

    @Override
    public String getUserFirstName(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "Unknown";
        }

        try {
            AuthUserResponse userInfo = getUserInfo(userId);
            return userInfo != null && userInfo.firstName() != null ? userInfo.firstName() : "Unknown";
        } catch (Exception e) {
            log.warn("Failed to get first name for user {}: {}", userId, e.getMessage());
            return "Unknown";
        }
    }

    @Override
    public String getUserLastName(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "Unknown";
        }

        try {
            AuthUserResponse userInfo = getUserInfo(userId);
            return userInfo != null && userInfo.lastName() != null ? userInfo.lastName() : "Unknown";
        } catch (Exception e) {
            log.warn("Failed to get last name for user {}: {}", userId, e.getMessage());
            return "Unknown";
        }
    }


    @Override
    public boolean userExists(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        try {
            AuthUserResponse userInfo = getUserInfo(userId);
            return userInfo != null && userInfo.active();
        } catch (Exception e) {
            log.warn("Failed to check if user exists {}: {}", userId, e.getMessage());
            // In case of error, assume user doesn't exist for security
            return false;
        }
    }
}