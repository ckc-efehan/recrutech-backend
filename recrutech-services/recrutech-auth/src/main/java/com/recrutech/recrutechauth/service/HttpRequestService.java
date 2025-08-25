package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.security.RateLimitingFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Service for handling HTTP request-related operations.
 * Extracts common functionality from controllers to improve code reusability.
 */
@Service
public class HttpRequestService {

    /**
     * Extracts the client IP address from the HTTP request.
     * Checks various headers commonly used by proxies and load balancers.
     *
     * @param request the HTTP servlet request
     * @return the client IP address
     */
    public String getClientIpAddress(HttpServletRequest request) {
        return RateLimitingFilter.getString(request);
    }

    /**
     * Extracts the Bearer token from the Authorization header.
     *
     * @param authHeader the Authorization header value
     * @return the extracted token
     * @throws IllegalArgumentException if the header is invalid or missing
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        return authHeader.substring(7).trim();
    }

    /**
     * Extracts the User-Agent header from the request.
     *
     * @param request the HTTP servlet request
     * @return the User-Agent header value, or null if not present
     */
    public String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Extracts the Accept-Language header from the request.
     *
     * @param request the HTTP servlet request
     * @return the Accept-Language header value, or null if not present
     */
    public String getAcceptLanguage(HttpServletRequest request) {
        return request.getHeader("Accept-Language");
    }
}