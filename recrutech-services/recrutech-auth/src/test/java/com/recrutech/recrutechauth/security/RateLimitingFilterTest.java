package com.recrutech.recrutechauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive test suite for RateLimitingFilter.
 * Tests rate limiting logic, JWT token extraction, and suspicious activity detection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter Tests")
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private FilterChain mockFilterChain;
    
    @Mock
    private RedisTemplate<String, String> mockRedisTemplate;
    
    @Mock
    private ValueOperations<String, String> mockValueOperations;
    
    @Mock
    private TokenProvider mockTokenProvider;
    
    @Mock
    private SecurityService mockSecurityService;
    
    private RateLimitingFilter rateLimitingFilter;
    private StringWriter responseWriter;
    private Map<String, String> capturedHeaders;

    @BeforeEach
    void setUp() throws IOException {
        lenient().when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);
        
        rateLimitingFilter = new RateLimitingFilter(
            mockRedisTemplate,
            mockTokenProvider,
            mockSecurityService,
            5, // maxAttempts
            900 // timeWindowSeconds
        );
        
        // Setup response writer and header capture
        responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        lenient().when(mockResponse.getWriter()).thenReturn(printWriter);
        
        capturedHeaders = new HashMap<>();
        lenient().doAnswer(invocation -> {
            String headerName = invocation.getArgument(0);
            String headerValue = invocation.getArgument(1);
            capturedHeaders.put(headerName, headerValue);
            return null;
        }).when(mockResponse).setHeader(anyString(), anyString());
        
        // Setup default header responses to avoid stubbing issues
        lenient().when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);
    }

    @Test
    @DisplayName("Test filter initialization and destruction")
    void testFilterLifecycle() {
        // Test initialization
        assertDoesNotThrow(() -> rateLimitingFilter.init(null));
        
        // Test destruction
        assertDoesNotThrow(() -> rateLimitingFilter.destroy());
        
        System.out.println("[DEBUG_LOG] RateLimitingFilter lifecycle tests passed");
    }

    @Test
    @DisplayName("Test successful request within rate limit")
    void testSuccessfulRequestWithinRateLimit() throws ServletException, IOException {
        // Setup request
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return no existing rate limit
        when(mockValueOperations.get(anyString())).thenReturn(null);

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was allowed through
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        verify(mockValueOperations, atLeastOnce()).set(anyString(), eq("1"), any(Duration.class));
        
        System.out.println("[DEBUG_LOG] Successful request within rate limit tests passed");
    }

    @Test
    @DisplayName("Test rate limit exceeded")
    void testRateLimitExceeded() throws ServletException, IOException {
        // Setup request
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/test");
        lenient().when(mockRequest.getMethod()).thenReturn("GET");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return rate limit exceeded
        // For general endpoints, endpointLimit = maxAttempts * 2 = 10, so count of 11 should exceed limit
        when(mockValueOperations.get("rate_limit:global:ip:192.168.1.1")).thenReturn("20"); // Below global limit of 25
        when(mockValueOperations.get("rate_limit:endpoint:ip:192.168.1.1:GET:/api/test")).thenReturn("11"); // Exceeds endpoint limit of 10

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was blocked
        verify(mockFilterChain, never()).doFilter(any(), any());
        verify(mockResponse).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(mockResponse).setContentType("application/json");
        
        // Verify rate limit headers
        assertEquals("5", capturedHeaders.get("X-RateLimit-Limit"));
        assertEquals("0", capturedHeaders.get("X-RateLimit-Remaining"));
        assertNotNull(capturedHeaders.get("X-RateLimit-Reset"));
        assertNotNull(capturedHeaders.get("Retry-After"));
        
        System.out.println("[DEBUG_LOG] Rate limit exceeded tests passed");
    }

    @Test
    @DisplayName("Test JWT token extraction for user identification")
    void testJWTTokenExtraction() throws ServletException, IOException {
        // Setup request with JWT token
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/test");
        lenient().when(mockRequest.getMethod()).thenReturn("GET");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");

        // Setup token provider to return user ID
        when(mockTokenProvider.getUserIdFromToken("valid.jwt.token")).thenReturn("user-123");

        // Setup Redis to return no existing rate limit
        when(mockValueOperations.get(anyString())).thenReturn(null);

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was allowed through
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        verify(mockTokenProvider).getUserIdFromToken("valid.jwt.token");
        
        // Verify rate limiting key uses user ID instead of IP
        verify(mockValueOperations, atLeastOnce()).set(contains("user:user-123"), eq("1"), any(Duration.class));
        
        System.out.println("[DEBUG_LOG] JWT token extraction tests passed");
    }

    @Test
    @DisplayName("Test fallback to IP-based rate limiting when JWT extraction fails")
    void testFallbackToIPBasedRateLimiting() throws ServletException, IOException {
        // Setup request with invalid JWT token
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer invalid.jwt.token");

        // Setup token provider to return null (invalid token)
        when(mockTokenProvider.getUserIdFromToken("invalid.jwt.token")).thenReturn(null);

        // Setup Redis to return no existing rate limit
        when(mockValueOperations.get(anyString())).thenReturn(null);

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was allowed through
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        
        // Verify rate limiting key uses IP instead of user ID
        verify(mockValueOperations, atLeastOnce()).set(contains("ip:192.168.1.1"), eq("1"), any(Duration.class));
        
        System.out.println("[DEBUG_LOG] Fallback to IP-based rate limiting tests passed");
    }

    @Test
    @DisplayName("Test X-Forwarded-For header IP extraction")
    void testXForwardedForIPExtraction() throws ServletException, IOException {
        // Setup request with X-Forwarded-For header
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/test");
        lenient().when(mockRequest.getMethod()).thenReturn("GET");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        lenient().when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return no existing rate limit
        when(mockValueOperations.get(anyString())).thenReturn(null);

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was allowed through
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        
        // Verify rate limiting key uses first IP from X-Forwarded-For
        verify(mockValueOperations, atLeastOnce()).set(contains("ip:203.0.113.1"), eq("1"), any(Duration.class));
        
        System.out.println("[DEBUG_LOG] X-Forwarded-For IP extraction tests passed");
    }

    @Test
    @DisplayName("Test different rate limits for different endpoints")
    void testDifferentRateLimitsForEndpoints() throws ServletException, IOException {
        // Test auth endpoint (stricter limits)
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        lenient().when(mockRequest.getMethod()).thenReturn("POST");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return rate limit at auth endpoint threshold
        // For auth endpoints, endpointLimit = maxAttempts = 5 (not maxAttempts/2), so count of 6 should exceed limit
        when(mockValueOperations.get("rate_limit:global:ip:192.168.1.1")).thenReturn("10"); // Below global limit of 15
        when(mockValueOperations.get("rate_limit:endpoint:ip:192.168.1.1:POST:/api/auth/*")).thenReturn("6"); // Exceeds endpoint limit of 5

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was blocked due to stricter auth endpoint limits
        verify(mockFilterChain, never()).doFilter(any(), any());
        verify(mockResponse).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        
        System.out.println("[DEBUG_LOG] Different rate limits for endpoints tests passed");
    }

    @Test
    @DisplayName("Test suspicious user agent detection")
    void testSuspiciousUserAgentDetection() throws ServletException, IOException {
        String[] suspiciousUserAgents = {
            "bot",
            "crawler",
            "spider",
            "scraper",
            "ab", // Too short
            null, // Null user agent
            "" // Empty user agent
        };

        for (String userAgent : suspiciousUserAgents) {
            // Reset mocks for each test
            reset(mockFilterChain, mockResponse);
            capturedHeaders.clear();
            responseWriter.getBuffer().setLength(0);
            
            // Setup response writer again
            StringWriter newResponseWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(newResponseWriter);
            when(mockResponse.getWriter()).thenReturn(printWriter);

            when(mockRequest.getRequestURI()).thenReturn("/api/test");
            when(mockRequest.getMethod()).thenReturn("GET");
            when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
            when(mockRequest.getHeader("User-Agent")).thenReturn(userAgent);
            when(mockRequest.getHeader("Authorization")).thenReturn(null);

            rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

            // Verify request was blocked due to suspicious user agent
            verify(mockFilterChain, never()).doFilter(any(), any());
            verify(mockResponse).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }
        
        System.out.println("[DEBUG_LOG] Suspicious user agent detection tests passed");
    }

    @Test
    @DisplayName("Test rapid requests detection")
    void testRapidRequestsDetection() throws ServletException, IOException {
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/test");
        lenient().when(mockRequest.getMethod()).thenReturn("GET");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return high rapid request count
        when(mockValueOperations.increment(contains("rapid_requests"))).thenReturn(101L); // Exceeds limit of 100

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was blocked due to rapid requests
        verify(mockFilterChain, never()).doFilter(any(), any());
        verify(mockResponse).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        
        System.out.println("[DEBUG_LOG] Rapid requests detection tests passed");
    }

    @Test
    @DisplayName("Test Redis failure handling (fail open)")
    void testRedisFailureHandling() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to throw exception
        when(mockValueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was allowed through (fail open)
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        
        System.out.println("[DEBUG_LOG] Redis failure handling tests passed");
    }

    @Test
    @DisplayName("Test endpoint identifier normalization")
    void testEndpointIdentifierNormalization() throws ServletException, IOException {
        String[] authEndpoints = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
        };
        
        String[] gdprEndpoints = {
            "/api/gdpr/delete-data",
            "/api/gdpr/export-data"
        };

        // Test auth endpoints normalization
        for (String endpoint : authEndpoints) {
            when(mockRequest.getRequestURI()).thenReturn(endpoint);
            when(mockRequest.getMethod()).thenReturn("POST");
            when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
            when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
            when(mockRequest.getHeader("Authorization")).thenReturn(null);
            when(mockValueOperations.get(anyString())).thenReturn(null);

            rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

            // Verify endpoint is normalized to POST:/api/auth/*
            verify(mockValueOperations, atLeastOnce()).set(contains("POST:/api/auth/*"), eq("1"), any(Duration.class));
        }

        // Test GDPR endpoints normalization
        for (String endpoint : gdprEndpoints) {
            when(mockRequest.getRequestURI()).thenReturn(endpoint);
            when(mockRequest.getMethod()).thenReturn("POST");
            when(mockValueOperations.get(anyString())).thenReturn(null);

            rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

            // Verify endpoint is normalized to POST:/api/gdpr/*
            verify(mockValueOperations, atLeastOnce()).set(contains("POST:/api/gdpr/*"), eq("1"), any(Duration.class));
        }
        
        System.out.println("[DEBUG_LOG] Endpoint identifier normalization tests passed");
    }

    @Test
    @DisplayName("Test rate limit increment")
    void testRateLimitIncrement() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return existing count
        when(mockValueOperations.get(anyString())).thenReturn("3");

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify request was allowed through
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        
        // Verify counter was incremented
        verify(mockValueOperations, atLeastOnce()).increment(anyString());
        
        System.out.println("[DEBUG_LOG] Rate limit increment tests passed");
    }

    @Test
    @DisplayName("Test non-HTTP request handling")
    void testNonHttpRequestHandling() throws ServletException, IOException {
        // Test with non-HTTP request (should pass through unchanged)
        jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

        rateLimitingFilter.doFilter(nonHttpRequest, nonHttpResponse, mockFilterChain);
        
        verify(mockFilterChain).doFilter(nonHttpRequest, nonHttpResponse);
        
        System.out.println("[DEBUG_LOG] Non-HTTP request handling tests passed");
    }

    @Test
    @DisplayName("Test rate limit response JSON format")
    void testRateLimitResponseJSONFormat() throws ServletException, IOException {
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/test");
        lenient().when(mockRequest.getMethod()).thenReturn("GET");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return rate limit exceeded
        // For general endpoints, endpointLimit = maxAttempts * 2 = 10, so count of 11 should exceed limit
        when(mockValueOperations.get("rate_limit:global:ip:192.168.1.1")).thenReturn("20"); // Below global limit of 25
        when(mockValueOperations.get("rate_limit:endpoint:ip:192.168.1.1:GET:/api/test")).thenReturn("11"); // Exceeds endpoint limit of 10

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify JSON response format
        verify(mockResponse).setContentType("application/json");
        
        // The response should contain error, message, and retryAfter fields
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("\"error\":\"Rate limit exceeded\""));
        assertTrue(responseContent.contains("\"retryAfter\":900"));
        
        System.out.println("[DEBUG_LOG] Rate limit response JSON format tests passed");
    }

    @Test
    @DisplayName("Test global vs endpoint rate limiting")
    void testGlobalVsEndpointRateLimiting() throws ServletException, IOException {
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/test");
        lenient().when(mockRequest.getMethod()).thenReturn("GET");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);

        // Setup Redis to return different values for global vs endpoint limits
        when(mockValueOperations.get(contains("global"))).thenReturn("8"); // Below global limit
        when(mockValueOperations.get(contains("endpoint"))).thenReturn("12"); // Exceeds endpoint limit

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify both global and endpoint rate limits are checked
        verify(mockValueOperations, atLeastOnce()).get(contains("rate_limit:global:"));
        verify(mockValueOperations, atLeastOnce()).get(contains("rate_limit:endpoint:"));
        
        System.out.println("[DEBUG_LOG] Global vs endpoint rate limiting tests passed");
    }

    @Test
    @DisplayName("Test X-Real-IP header handling")
    void testXRealIPHeaderHandling() throws ServletException, IOException {
        lenient().when(mockRequest.getRequestURI()).thenReturn("/api/test");
        lenient().when(mockRequest.getMethod()).thenReturn("GET");
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        lenient().when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Real-IP")).thenReturn("203.0.113.2");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        lenient().when(mockRequest.getHeader("Authorization")).thenReturn(null);

        when(mockValueOperations.get(anyString())).thenReturn(null);

        rateLimitingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify rate limiting key uses X-Real-IP
        verify(mockValueOperations, atLeastOnce()).set(contains("ip:203.0.113.2"), eq("1"), any(Duration.class));
        
        System.out.println("[DEBUG_LOG] X-Real-IP header handling tests passed");
    }
}