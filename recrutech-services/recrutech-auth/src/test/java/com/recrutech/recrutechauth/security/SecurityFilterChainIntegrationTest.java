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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test suite for the complete security filter chain.
 * Tests the interaction between InputSanitizationFilter, SecurityHeadersFilter, and RateLimitingFilter.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Security Filter Chain Integration Tests")
class SecurityFilterChainIntegrationTest {

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

    private InputSanitizationFilter inputSanitizationFilter;
    private SecurityHeadersFilter securityHeadersFilter;
    private RateLimitingFilter rateLimitingFilter;
    
    private Map<String, String> capturedHeaders;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize services and filters
        InputSanitizationService inputSanitizationService = new InputSanitizationService();
        inputSanitizationFilter = new InputSanitizationFilter(inputSanitizationService);
        securityHeadersFilter = new SecurityHeadersFilter();
        
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOperations);
        rateLimitingFilter = new RateLimitingFilter(
            mockRedisTemplate,
            mockTokenProvider,
            mockSecurityService,
            5, // maxAttempts
            900 // timeWindowSeconds
        );
        
        // Setup default header stubs for all tests
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        when(mockRequest.getHeader("Authorization")).thenReturn(null);
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        
        // Setup header names enumeration to include User-Agent so it gets processed by InputSanitizationFilter
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("User-Agent")));
        
        // Setup response capture
        capturedHeaders = new HashMap<>();
        doAnswer(invocation -> {
            String headerName = invocation.getArgument(0);
            String headerValue = invocation.getArgument(1);
            capturedHeaders.put(headerName, headerValue);
            return null;
        }).when(mockResponse).setHeader(anyString(), anyString());

        StringWriter responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);
        when(mockResponse.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("Test complete filter chain execution order")
    void testCompleteFilterChainExecutionOrder() throws ServletException, IOException {
        // Setup request with malicious input
        Map<String, String[]> maliciousParams = new HashMap<>();
        maliciousParams.put("username", new String[]{"<script>alert('xss')</script>admin"});
        
        when(mockRequest.getParameterMap()).thenReturn(maliciousParams);
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        // Setup Redis to allow request
        when(mockValueOperations.get(anyString())).thenReturn(null);

        // Create a chain that simulates the filter execution order
        FilterChain chainStep3 = mock(FilterChain.class);
        FilterChain chainStep2 = mock(FilterChain.class);
        FilterChain chainStep1 = mockFilterChain;

        // Step 3: Rate limiting filter (final filter)
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            
            // Verify input has been sanitized by previous filter
            String sanitizedUsername = request.getParameter("username");
            assertFalse(sanitizedUsername.contains("<script>"));
            
            // Verify security headers have been added by previous filter
            assertNotNull(capturedHeaders.get("X-Frame-Options"));
            assertNotNull(capturedHeaders.get("Content-Security-Policy"));
            
            // Rate limiting filter continues to final chain
            chainStep1.doFilter(request, response);
            return null;
        }).when(chainStep3).doFilter(any(), any());

        // Step 2: Security headers filter
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            
            // Verify input has been sanitized by previous filter
            String sanitizedUsername = request.getParameter("username");
            assertFalse(sanitizedUsername.contains("<script>"));
            
            // Apply security headers and continue to rate limiting
            securityHeadersFilter.doFilter(request, response, chainStep3);
            return null;
        }).when(chainStep2).doFilter(any(), any());

        // Step 1: Input sanitization filter (first filter)
        inputSanitizationFilter.doFilter(mockRequest, mockResponse, chainStep2);

        // Verify final chain was called (request completed successfully)
        verify(chainStep1).doFilter(any(), any());
        
        System.out.println("[DEBUG_LOG] Complete filter chain execution order tests passed");
    }

    @Test
    @DisplayName("Test input sanitization with security headers")
    void testInputSanitizationWithSecurityHeaders() throws ServletException, IOException {
        // Setup request with XSS attempt
        Map<String, String[]> xssParams = new HashMap<>();
        xssParams.put("comment", new String[]{"<script>document.location='https://evil.com'</script>"});
        
        when(mockRequest.getParameterMap()).thenReturn(xssParams);
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("POST");

        // Create filter chain: Input Sanitization -> Security Headers -> Final
        FilterChain securityHeadersChain = mock(FilterChain.class);
        
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            
            // Apply security headers
            securityHeadersFilter.doFilter(request, response, mockFilterChain);
            return null;
        }).when(securityHeadersChain).doFilter(any(), any());

        // Start with input sanitization
        inputSanitizationFilter.doFilter(mockRequest, mockResponse, securityHeadersChain);

        // Verify both filters executed
        verify(mockFilterChain).doFilter(any(), any());
        
        // Verify security headers were added
        assertEquals("DENY", capturedHeaders.get("X-Frame-Options"));
        assertEquals("1; mode=block", capturedHeaders.get("X-XSS-Protection"));
        assertNotNull(capturedHeaders.get("Content-Security-Policy"));
        
        System.out.println("[DEBUG_LOG] Input sanitization with security headers tests passed");
    }

    @Test
    @DisplayName("Test rate limiting blocks request before final processing")
    void testRateLimitingBlocksRequest() throws ServletException, IOException {
        // Setup request that will be rate limited
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        // Setup Redis to return rate limit exceeded
        when(mockValueOperations.get(anyString())).thenReturn("6"); // Exceeds limit

        // Create filter chain: Input Sanitization -> Security Headers -> Rate Limiting -> Final
        FilterChain rateLimitingChain = mock(FilterChain.class);
        FilterChain securityHeadersChain = mock(FilterChain.class);
        
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            
            // Rate limiting filter should block the request
            rateLimitingFilter.doFilter(request, response, mockFilterChain);
            return null;
        }).when(rateLimitingChain).doFilter(any(), any());

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            
            // Security headers filter continues to rate limiting
            securityHeadersFilter.doFilter(request, response, rateLimitingChain);
            return null;
        }).when(securityHeadersChain).doFilter(any(), any());

        // Start with input sanitization
        inputSanitizationFilter.doFilter(mockRequest, mockResponse, securityHeadersChain);

        // Verify final chain was NOT called (request was blocked)
        verify(mockFilterChain, never()).doFilter(any(), any());
        
        // Verify rate limiting response
        verify(mockResponse).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        assertEquals("5", capturedHeaders.get("X-RateLimit-Limit"));
        
        System.out.println("[DEBUG_LOG] Rate limiting blocks request tests passed");
    }

    @Test
    @DisplayName("Test JWT token extraction works through filter chain")
    void testJWTTokenExtractionThroughFilterChain() throws ServletException, IOException {
        // Setup authenticated request
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getRequestURI()).thenReturn("/api/user/profile");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");

        // Setup token provider
        when(mockTokenProvider.getUserIdFromToken("valid.jwt.token")).thenReturn("user-123");
        when(mockValueOperations.get(anyString())).thenReturn(null);

        // Create complete filter chain
        FilterChain rateLimitingChain = mock(FilterChain.class);
        FilterChain securityHeadersChain = mock(FilterChain.class);
        
        doAnswer(invocation -> {
            rateLimitingFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), mockFilterChain);
            return null;
        }).when(rateLimitingChain).doFilter(any(), any());

        doAnswer(invocation -> {
            securityHeadersFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), rateLimitingChain);
            return null;
        }).when(securityHeadersChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, securityHeadersChain);

        // Verify JWT token was extracted and used for rate limiting
        verify(mockTokenProvider).getUserIdFromToken("valid.jwt.token");
        verify(mockValueOperations, atLeastOnce()).set(contains("user:user-123"), eq("1"), any(Duration.class));
        verify(mockFilterChain).doFilter(any(), any());
        
        System.out.println("[DEBUG_LOG] JWT token extraction through filter chain tests passed");
    }

    @Test
    @DisplayName("Test malicious input sanitization with rate limiting")
    void testMaliciousInputSanitizationWithRateLimiting() throws ServletException, IOException {
        // Setup request with both XSS and SQL injection attempts
        Map<String, String[]> maliciousParams = new HashMap<>();
        maliciousParams.put("search", new String[]{"<script>alert('xss')</script>'; DROP TABLE users; --"});
        
        when(mockRequest.getParameterMap()).thenReturn(maliciousParams);
        when(mockRequest.getRequestURI()).thenReturn("/api/search");
        when(mockRequest.getMethod()).thenReturn("POST");

        // Setup Redis to allow request
        when(mockValueOperations.get(anyString())).thenReturn("2"); // Within limit

        // Create complete filter chain
        FilterChain rateLimitingChain = mock(FilterChain.class);
        FilterChain securityHeadersChain = mock(FilterChain.class);
        
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            
            // Verify input has been sanitized
            String sanitizedSearch = request.getParameter("search");
            assertFalse(sanitizedSearch.contains("<script>"));
            assertFalse(sanitizedSearch.contains("DROP TABLE"));
            assertFalse(sanitizedSearch.contains("--"));
            
            rateLimitingFilter.doFilter(request, invocation.getArgument(1), mockFilterChain);
            return null;
        }).when(rateLimitingChain).doFilter(any(), any());

        doAnswer(invocation -> {
            securityHeadersFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), rateLimitingChain);
            return null;
        }).when(securityHeadersChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, securityHeadersChain);

        // Verify request completed successfully with sanitized input
        verify(mockFilterChain).doFilter(any(), any());
        verify(mockValueOperations, atLeastOnce()).increment(anyString());
        
        System.out.println("[DEBUG_LOG] Malicious input sanitization with rate limiting tests passed");
    }

    @Test
    @DisplayName("Test security headers for sensitive endpoints")
    void testSecurityHeadersForSensitiveEndpoints() throws ServletException, IOException {
        // Setup request to sensitive endpoint
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        // Setup Redis to allow request
        when(mockValueOperations.get(anyString())).thenReturn("1");

        // Create complete filter chain
        FilterChain rateLimitingChain = mock(FilterChain.class);
        FilterChain securityHeadersChain = mock(FilterChain.class);
        
        doAnswer(invocation -> {
            rateLimitingFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), mockFilterChain);
            return null;
        }).when(rateLimitingChain).doFilter(any(), any());

        doAnswer(invocation -> {
            securityHeadersFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), rateLimitingChain);
            return null;
        }).when(securityHeadersChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, securityHeadersChain);

        // Verify sensitive endpoint security headers
        assertEquals("no-cache, no-store, must-revalidate, private", capturedHeaders.get("Cache-Control"));
        assertEquals("no-cache", capturedHeaders.get("Pragma"));
        assertEquals("0", capturedHeaders.get("Expires"));
        assertEquals("required", capturedHeaders.get("X-CSRF-Protection"));
        assertEquals("stateless", capturedHeaders.get("X-Session-Management"));
        
        verify(mockFilterChain).doFilter(any(), any());
        
        System.out.println("[DEBUG_LOG] Security headers for sensitive endpoints tests passed");
    }

    @Test
    @DisplayName("Test filter chain with suspicious activity detection")
    void testFilterChainWithSuspiciousActivityDetection() throws ServletException, IOException {
        // Setup request with suspicious user agent
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getHeader("User-Agent")).thenReturn("bot-crawler");

        // Create complete filter chain
        FilterChain rateLimitingChain = mock(FilterChain.class);
        FilterChain securityHeadersChain = mock(FilterChain.class);
        
        doAnswer(invocation -> {
            // Rate limiting should block suspicious activity
            rateLimitingFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), mockFilterChain);
            return null;
        }).when(rateLimitingChain).doFilter(any(), any());

        doAnswer(invocation -> {
            securityHeadersFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), rateLimitingChain);
            return null;
        }).when(securityHeadersChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, securityHeadersChain);

        // Verify request was blocked due to suspicious activity
        verify(mockFilterChain, never()).doFilter(any(), any());
        verify(mockResponse).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        
        System.out.println("[DEBUG_LOG] Filter chain with suspicious activity detection tests passed");
    }

    @Test
    @DisplayName("Test complete security filter chain performance")
    void testCompleteSecurityFilterChainPerformance() throws ServletException, IOException {
        // Setup normal request
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");

        when(mockValueOperations.get(anyString())).thenReturn(null);

        // Create complete filter chain
        FilterChain rateLimitingChain = mock(FilterChain.class);
        FilterChain securityHeadersChain = mock(FilterChain.class);
        
        doAnswer(invocation -> {
            rateLimitingFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), mockFilterChain);
            return null;
        }).when(rateLimitingChain).doFilter(any(), any());

        doAnswer(invocation -> {
            securityHeadersFilter.doFilter(invocation.getArgument(0), invocation.getArgument(1), rateLimitingChain);
            return null;
        }).when(securityHeadersChain).doFilter(any(), any());

        // Measure performance
        long startTime = System.currentTimeMillis();
        inputSanitizationFilter.doFilter(mockRequest, mockResponse, securityHeadersChain);
        long endTime = System.currentTimeMillis();

        // Verify request completed successfully
        verify(mockFilterChain).doFilter(any(), any());
        
        // Verify performance (should complete quickly)
        assertTrue((endTime - startTime) < 100, "Complete filter chain should execute within 100ms");
        
        System.out.println("[DEBUG_LOG] Complete security filter chain performance tests passed - execution time: " + (endTime - startTime) + "ms");
    }
}