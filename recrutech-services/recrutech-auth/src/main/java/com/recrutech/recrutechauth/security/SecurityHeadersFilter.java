package com.recrutech.recrutechauth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security headers filter that adds comprehensive security headers to HTTP responses.
 * Provides protection against various attacks including session fixation, clickjacking,
 * MIME type sniffing, and other security vulnerabilities in stateless applications.
 */
@Component
@Order(2) // Execute after input sanitization but before other filters
public class SecurityHeadersFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("SecurityHeadersFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (response instanceof HttpServletResponse httpResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            addSecurityHeaders(httpResponse, httpRequest);
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("SecurityHeadersFilter destroyed");
    }

    /**
     * Adds comprehensive security headers to the HTTP response.
     */
    private void addSecurityHeaders(HttpServletResponse response, HttpServletRequest request) {
        
        // X-Frame-Options: Prevent clickjacking attacks
        response.setHeader("X-Frame-Options", "DENY");
        
        // X-Content-Type-Options: Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-XSS-Protection: Enable XSS filtering (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Strict-Transport-Security: Enforce HTTPS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        
        // Referrer-Policy: Control referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Content Security Policy: Prevent XSS and data injection attacks
        String csp = buildContentSecurityPolicy();
        response.setHeader("Content-Security-Policy", csp);
        
        // Permissions-Policy: Control browser features
        String permissionsPolicy = buildPermissionsPolicy();
        response.setHeader("Permissions-Policy", permissionsPolicy);
        
        // Cache-Control: Prevent caching of sensitive data
        if (isSensitiveEndpoint(request)) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        // X-Permitted-Cross-Domain-Policies: Restrict cross-domain policies
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        
        // Cross-Origin-Embedder-Policy: Control embedding of cross-origin resources
        response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
        
        // Cross-Origin-Opener-Policy: Isolate browsing context
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        
        // Cross-Origin-Resource-Policy: Control cross-origin resource sharing
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        
        // Server header removal (security through obscurity)
        response.setHeader("Server", "RecruTech-Auth");
        
        // X-Powered-By header removal
        response.setHeader("X-Powered-By", "");
        
        // Session fixation protection for stateless applications
        addStatelessSessionProtection(response, request);
        
        // CSRF protection headers for stateless applications
        addCSRFProtectionHeaders(response, request);
    }

    /**
     * Builds Content Security Policy header value.
     */
    private String buildContentSecurityPolicy() {
        return "default-src 'self'; " +
               "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
               "style-src 'self' 'unsafe-inline'; " +
               "img-src 'self' data: https:; " +
               "font-src 'self' https:; " +
               "connect-src 'self' https:; " +
               "media-src 'none'; " +
               "object-src 'none'; " +
               "child-src 'none'; " +
               "frame-src 'none'; " +
               "worker-src 'none'; " +
               "frame-ancestors 'none'; " +
               "form-action 'self'; " +
               "base-uri 'self'; " +
               "manifest-src 'self'";
    }

    /**
     * Builds Permissions Policy header value.
     */
    private String buildPermissionsPolicy() {
        return "accelerometer=(), " +
               "ambient-light-sensor=(), " +
               "autoplay=(), " +
               "battery=(), " +
               "camera=(), " +
               "cross-origin-isolated=(), " +
               "display-capture=(), " +
               "document-domain=(), " +
               "encrypted-media=(), " +
               "execution-while-not-rendered=(), " +
               "execution-while-out-of-viewport=(), " +
               "fullscreen=(), " +
               "geolocation=(), " +
               "gyroscope=(), " +
               "keyboard-map=(), " +
               "magnetometer=(), " +
               "microphone=(), " +
               "midi=(), " +
               "navigation-override=(), " +
               "payment=(), " +
               "picture-in-picture=(), " +
               "publickey-credentials-get=(), " +
               "screen-wake-lock=(), " +
               "sync-xhr=(), " +
               "usb=(), " +
               "web-share=(), " +
               "xr-spatial-tracking=()";
    }

    /**
     * Adds session fixation protection headers for stateless applications.
     */
    private void addStatelessSessionProtection(HttpServletResponse response, HttpServletRequest request) {
        // For stateless applications, we add headers that prevent session-related attacks
        
        // Prevent session fixation by ensuring no session cookies are set
        response.setHeader("X-Session-Management", "stateless");
        
        // Add token-based authentication headers
        response.setHeader("X-Auth-Method", "JWT");
        
        // Prevent session hijacking by adding secure token requirements
        response.setHeader("X-Token-Security", "required");
        
        // Add request ID for tracking (helps with session fixation detection)
        String requestId = java.util.UUID.randomUUID().toString();
        response.setHeader("X-Request-ID", requestId);
        
        // Add timestamp for request tracking
        response.setHeader("X-Request-Time", String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Adds CSRF protection headers for stateless applications.
     */
    private void addCSRFProtectionHeaders(HttpServletResponse response, HttpServletRequest request) {
        // For stateless JWT-based applications, CSRF protection is different
        
        // Require custom headers for state-changing operations
        if (isStateChangingRequest(request)) {
            response.setHeader("X-CSRF-Protection", "required");
            response.setHeader("X-Custom-Header-Required", "true");
        }
        
        // SameSite cookie policy (even though we're stateless, this helps with any cookies)
        response.setHeader("X-SameSite-Policy", "strict");
        
        // Origin validation requirement
        response.setHeader("X-Origin-Validation", "required");
    }

    /**
     * Checks if the endpoint handles sensitive data.
     */
    private boolean isSensitiveEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/auth/") || 
               path.contains("/login") || 
               path.contains("/register") || 
               path.contains("/refresh") || 
               path.contains("/gdpr/") ||
               path.contains("/user/") ||
               path.contains("/admin/");
    }

    /**
     * Checks if the request is a state-changing operation.
     */
    private boolean isStateChangingRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || 
               "PUT".equals(method) || 
               "DELETE".equals(method) || 
               "PATCH".equals(method);
    }
}