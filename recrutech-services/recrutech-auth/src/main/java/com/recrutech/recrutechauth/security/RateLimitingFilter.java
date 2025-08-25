package com.recrutech.recrutechauth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * Global rate limiting filter that applies rate limiting to all endpoints.
 * Uses Redis for distributed rate limiting and configurable limits.
 */
@Component
@Order(3) // Execute after input sanitization and security headers
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final TokenProvider tokenProvider;
    private final int maxAttempts;
    private final int timeWindowSeconds;

    public RateLimitingFilter(
            RedisTemplate<String, String> redisTemplate,
            TokenProvider tokenProvider,
            SecurityService securityService,
            @Value("${security.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${security.rate-limit.time-window:900}") int timeWindowSeconds) {
        
        this.redisTemplate = redisTemplate;
        this.tokenProvider = tokenProvider;
        this.maxAttempts = maxAttempts;
        this.timeWindowSeconds = timeWindowSeconds;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RateLimitingFilter initialized with maxAttempts={}, timeWindow={}s", 
                   maxAttempts, timeWindowSeconds);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {

            if (isRateLimited(httpRequest, httpResponse)) {
                return; // Request blocked, response already sent
            }
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("RateLimitingFilter destroyed");
    }

    /**
     * Checks if the request should be rate limited.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @return true if request is blocked, false if allowed
     */
    private boolean isRateLimited(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String clientIdentifier = getClientIdentifier(request);
        String endpoint = getEndpointIdentifier(request);
        
        // Different rate limiting strategies based on endpoint sensitivity
        RateLimitConfig config = getRateLimitConfig(endpoint);
        
        // Check global rate limit
        if (isGlobalRateLimitExceeded(clientIdentifier, config)) {
            handleRateLimitExceeded(response, "Global rate limit exceeded", clientIdentifier);
            return true;
        }
        
        // Check endpoint-specific rate limit
        if (isEndpointRateLimitExceeded(clientIdentifier, endpoint, config)) {
            handleRateLimitExceeded(response, "Endpoint rate limit exceeded", clientIdentifier);
            return true;
        }
        
        // Check for suspicious activity patterns
        if (isSuspiciousActivity(request, clientIdentifier)) {
            handleRateLimitExceeded(response, "Suspicious activity detected", clientIdentifier);
            return true;
        }
        
        // Record the request
        recordRequest(clientIdentifier, endpoint, config);
        
        return false;
    }

    /**
     * Gets a unique identifier for the client making the request.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get user ID from JWT token first
        String userId = extractUserIdFromToken(request);
        if (userId != null) {
            return "user:" + userId;
        }
        
        // Fall back to IP address
        String clientIp = getClientIpAddress(request);
        return "ip:" + clientIp;
    }

    /**
     * Gets the client IP address, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        return getString(request);
    }

    public static String getString(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Extracts user ID from JWT token if present.
     */
    private String extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                // Use TokenProvider to extract user ID from JWT token
                String userId = tokenProvider.getUserIdFromToken(token);
                if (userId != null) {
                    logger.debug("Successfully extracted user ID from token: {}", userId);
                    return userId;
                }
            } catch (Exception e) {
                logger.debug("Failed to extract user ID from token: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets an identifier for the endpoint being accessed.
     */
    private String getEndpointIdentifier(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        
        // Normalize path to group similar endpoints
        if (path.startsWith("/api/auth/")) {
            return method + ":/api/auth/*";
        } else if (path.startsWith("/api/gdpr/")) {
            return method + ":/api/gdpr/*";
        } else {
            return method + ":" + path;
        }
    }

    /**
     * Gets rate limit configuration based on endpoint sensitivity.
     */
    private RateLimitConfig getRateLimitConfig(String endpoint) {
        if (endpoint.contains("/auth/login") || endpoint.contains("/auth/register")) {
            // Stricter limits for authentication endpoints
            return new RateLimitConfig(maxAttempts / 2, timeWindowSeconds, maxAttempts * 2, timeWindowSeconds * 2);
        } else if (endpoint.contains("/auth/")) {
            // Moderate limits for other auth endpoints
            return new RateLimitConfig(maxAttempts, timeWindowSeconds, maxAttempts * 3, timeWindowSeconds);
        } else {
            // Standard limits for general endpoints
            return new RateLimitConfig(maxAttempts * 2, timeWindowSeconds, maxAttempts * 5, timeWindowSeconds);
        }
    }

    /**
     * Checks if global rate limit is exceeded.
     */
    private boolean isGlobalRateLimitExceeded(String clientIdentifier, RateLimitConfig config) {
        String globalKey = "rate_limit:global:" + clientIdentifier;
        return isRateLimitExceeded(globalKey, config.globalLimit, config.globalWindow);
    }

    /**
     * Checks if endpoint-specific rate limit is exceeded.
     */
    private boolean isEndpointRateLimitExceeded(String clientIdentifier, String endpoint, RateLimitConfig config) {
        String endpointKey = "rate_limit:endpoint:" + clientIdentifier + ":" + endpoint;
        return isRateLimitExceeded(endpointKey, config.endpointLimit, config.endpointWindow);
    }

    /**
     * Generic rate limit check using Redis.
     */
    private boolean isRateLimitExceeded(String key, int limit, int windowSeconds) {
        try {
            String currentCount = redisTemplate.opsForValue().get(key);
            
            if (currentCount == null) {
                // First request in the window
                redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(windowSeconds));
                return false;
            }
            
            int count = Integer.parseInt(currentCount);
            if (count >= limit) {
                logger.warn("Rate limit exceeded for key: {}, count: {}, limit: {}", key, count, limit);
                return true;
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(key);
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for key: {}", key, e);
            // In case of Redis failure, allow the request (fail open)
            return false;
        }
    }

    /**
     * Checks for suspicious activity patterns.
     */
    private boolean isSuspiciousActivity(HttpServletRequest request, String clientIdentifier) {
        // Check for rapid requests from same client
        String rapidRequestKey = "rapid_requests:" + clientIdentifier;
        try {
            Long requestCount = redisTemplate.opsForValue().increment(rapidRequestKey);
            redisTemplate.expire(rapidRequestKey, Duration.ofMinutes(1));
            
            if (requestCount != null && requestCount > 100) { // 100 requests per minute
                logger.warn("Suspicious rapid requests detected for client: {}", clientIdentifier);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Error checking rapid requests: {}", e.getMessage());
        }
        
        // Check for suspicious user agents
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.trim().isEmpty() || isSuspiciousUserAgent(userAgent)) {
            logger.warn("Suspicious user agent detected: {}", userAgent);
            return true;
        }
        
        return false;
    }

    /**
     * Checks if user agent appears to be suspicious.
     */
    private boolean isSuspiciousUserAgent(String userAgent) {
        String lowerUserAgent = userAgent.toLowerCase();
        return lowerUserAgent.contains("bot") ||
               lowerUserAgent.contains("crawler") ||
               lowerUserAgent.contains("spider") ||
               lowerUserAgent.contains("scraper") ||
               lowerUserAgent.length() < 10 ||
               lowerUserAgent.length() > 500;
    }

    /**
     * Records a successful request for monitoring.
     */
    private void recordRequest(String clientIdentifier, String endpoint, RateLimitConfig config) {
        // This could be used for analytics or monitoring
        logger.debug("Request recorded for client: {}, endpoint: {}", clientIdentifier, endpoint);
    }

    /**
     * Handles rate limit exceeded scenarios.
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String reason, String clientIdentifier) 
            throws IOException {
        
        logger.warn("Rate limit exceeded - Reason: {}, Client: {}", reason, clientIdentifier);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxAttempts));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + (timeWindowSeconds * 1000L)));
        response.setHeader("Retry-After", String.valueOf(timeWindowSeconds));
        
        String jsonResponse = String.format(
            "{\"error\":\"Rate limit exceeded\",\"message\":\"%s\",\"retryAfter\":%d}",
            reason, timeWindowSeconds
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
         * Configuration class for rate limiting parameters.
         */
        private record RateLimitConfig(int endpointLimit, int endpointWindow, int globalLimit, int globalWindow) {
    }
}