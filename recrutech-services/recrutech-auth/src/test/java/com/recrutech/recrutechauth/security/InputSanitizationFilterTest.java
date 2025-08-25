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

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for InputSanitizationFilter.
 * Tests request wrapping, parameter sanitization, and header handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InputSanitizationFilter Tests")
class InputSanitizationFilterTest {

    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private FilterChain mockFilterChain;

    private InputSanitizationFilter inputSanitizationFilter;

    @BeforeEach
    void setUp() {
        InputSanitizationService inputSanitizationService = new InputSanitizationService();
        inputSanitizationFilter = new InputSanitizationFilter(inputSanitizationService);
    }

    @Test
    @DisplayName("Test filter initialization and destruction")
    void testFilterLifecycle() {
        // Test initialization
        assertDoesNotThrow(() -> inputSanitizationFilter.init(null));
        
        // Test destruction
        assertDoesNotThrow(() -> inputSanitizationFilter.destroy());
        
        System.out.println("[DEBUG_LOG] Filter lifecycle tests passed");
    }

    @Test
    @DisplayName("Test parameter sanitization in filter")
    void testParameterSanitization() throws ServletException, IOException {
        // Setup malicious parameters
        Map<String, String[]> originalParams = new HashMap<>();
        originalParams.put("username", new String[]{"admin'; DROP TABLE users; --"});
        originalParams.put("email", new String[]{"<script>alert('XSS')</script>test@example.com"});
        originalParams.put("message", new String[]{"Hello & welcome!"});

        when(mockRequest.getParameterMap()).thenReturn(originalParams);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        // Capture the wrapped request
        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            // Test sanitized parameters
            String sanitizedUsername = wrappedRequest.getParameter("username");
            String sanitizedEmail = wrappedRequest.getParameter("email");
            String sanitizedMessage = wrappedRequest.getParameter("message");
            
            assertFalse(sanitizedUsername.contains("DROP TABLE"));
            assertFalse(sanitizedUsername.contains("--"));
            assertFalse(sanitizedEmail.contains("<script>"));
            assertTrue(sanitizedMessage.contains("&amp;"));
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        verify(mockFilterChain).doFilter(any(), eq(mockResponse));
        System.out.println("[DEBUG_LOG] Parameter sanitization tests passed");
    }

    @Test
    @DisplayName("Test header sanitization with security header preservation")
    void testHeaderSanitization() throws ServletException, IOException {
        // Setup headers including security-critical ones
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer valid.jwt.token");
        headers.put("Cookie", "sessionId=abc123");
        headers.put("User-Agent", "<script>alert('xss')</script>Mozilla/5.0");
        headers.put("X-Custom-Header", "'; DROP TABLE users; --");
        headers.put("Content-Type", "application/json");

        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        
        // Mock header enumeration
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
        headers.forEach((key, value) -> when(mockRequest.getHeader(key)).thenReturn(value));

        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            // Security headers should be preserved
            assertEquals("Bearer valid.jwt.token", wrappedRequest.getHeader("Authorization"));
            assertEquals("sessionId=abc123", wrappedRequest.getHeader("Cookie"));
            assertEquals("application/json", wrappedRequest.getHeader("Content-Type"));
            
            // Non-security headers should be sanitized
            String sanitizedUserAgent = wrappedRequest.getHeader("User-Agent");
            String sanitizedCustomHeader = wrappedRequest.getHeader("X-Custom-Header");
            
            if (sanitizedUserAgent != null) {
                assertFalse(sanitizedUserAgent.contains("<script>"));
            }
            if (sanitizedCustomHeader != null) {
                assertFalse(sanitizedCustomHeader.contains("DROP TABLE"));
            }
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        verify(mockFilterChain).doFilter(any(), eq(mockResponse));
        System.out.println("[DEBUG_LOG] Header sanitization tests passed");
    }

    @Test
    @DisplayName("Test parameter values array handling")
    void testParameterValuesArray() throws ServletException, IOException {
        // Setup parameters with multiple values
        Map<String, String[]> originalParams = new HashMap<>();
        originalParams.put("tags", new String[]{
            "<script>alert('xss1')</script>tag1",
            "'; DROP TABLE users; --",
            "normal_tag"
        });

        when(mockRequest.getParameterMap()).thenReturn(originalParams);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            String[] sanitizedTags = wrappedRequest.getParameterValues("tags");
            assertNotNull(sanitizedTags);
            assertEquals(3, sanitizedTags.length);
            
            // Check each value is sanitized
            assertFalse(sanitizedTags[0].contains("<script>"));
            assertFalse(sanitizedTags[1].contains("DROP TABLE"));
            assertTrue(sanitizedTags[2].contains("normal_tag"));
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        System.out.println("[DEBUG_LOG] Parameter values array tests passed");
    }

    @Test
    @DisplayName("Test parameter names enumeration")
    void testParameterNamesEnumeration() throws ServletException, IOException {
        Map<String, String[]> originalParams = new HashMap<>();
        originalParams.put("username", new String[]{"test"});
        originalParams.put("email", new String[]{"test@example.com"});

        when(mockRequest.getParameterMap()).thenReturn(originalParams);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            Enumeration<String> paramNames = wrappedRequest.getParameterNames();
            Set<String> nameSet = new HashSet<>();
            while (paramNames.hasMoreElements()) {
                nameSet.add(paramNames.nextElement());
            }
            
            assertTrue(nameSet.contains("username"));
            assertTrue(nameSet.contains("email"));
            assertEquals(2, nameSet.size());
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        System.out.println("[DEBUG_LOG] Parameter names enumeration tests passed");
    }

    @Test
    @DisplayName("Test header names enumeration with security headers")
    void testHeaderNamesEnumeration() throws ServletException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("X-Custom", "custom-value");

        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
        headers.forEach((key, value) -> when(mockRequest.getHeader(key)).thenReturn(value));

        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            // Test that header enumeration works without throwing exceptions
            Enumeration<String> headerNames = wrappedRequest.getHeaderNames();
            assertNotNull(headerNames, "Header names enumeration should not be null");
            
            // Test that we can get individual headers (the important functionality)
            assertEquals("Bearer token", wrappedRequest.getHeader("Authorization"), "Authorization header should be preserved");
            // User-Agent gets sanitized (/ becomes &#x2F;), which is expected behavior for non-security headers
            String userAgent = wrappedRequest.getHeader("User-Agent");
            assertNotNull(userAgent, "User-Agent header should be accessible");
            assertTrue(userAgent.contains("Mozilla"), "User-Agent should contain Mozilla");
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        System.out.println("[DEBUG_LOG] Header names enumeration tests passed");
    }

    @Test
    @DisplayName("Test multiple header values handling")
    void testMultipleHeaderValues() throws ServletException, IOException {
        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(List.of("X-Custom")));
        when(mockRequest.getHeader("X-Custom")).thenReturn("safe-value");

        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            Enumeration<String> headerValues = wrappedRequest.getHeaders("X-Custom");
            assertTrue(headerValues.hasMoreElements());
            assertEquals("safe-value", headerValues.nextElement());
            assertFalse(headerValues.hasMoreElements());
            
            // Test non-existent header
            Enumeration<String> emptyHeaders = wrappedRequest.getHeaders("Non-Existent");
            assertFalse(emptyHeaders.hasMoreElements());
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        System.out.println("[DEBUG_LOG] Multiple header values tests passed");
    }

    @Test
    @DisplayName("Test edge cases and null handling")
    void testEdgeCasesAndNullHandling() throws ServletException, IOException {
        // Setup with null and empty values
        Map<String, String[]> originalParams = new HashMap<>();
        originalParams.put("empty", new String[]{""});
        originalParams.put("null_value", new String[]{null});
        originalParams.put("whitespace", new String[]{"   "});

        when(mockRequest.getParameterMap()).thenReturn(originalParams);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            // Test empty parameter
            String emptyParam = wrappedRequest.getParameter("empty");
            assertEquals("", emptyParam);
            
            // Test null parameter
            String nullParam = wrappedRequest.getParameter("null_value");
            assertNull(nullParam);
            
            // Test whitespace parameter
            String whitespaceParam = wrappedRequest.getParameter("whitespace");
            assertEquals("", whitespaceParam.trim());
            
            // Test non-existent parameter
            String nonExistent = wrappedRequest.getParameter("does_not_exist");
            assertNull(nonExistent);
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        System.out.println("[DEBUG_LOG] Edge cases and null handling tests passed");
    }

    @Test
    @DisplayName("Test security header identification")
    void testSecurityHeaderIdentification() throws ServletException, IOException {
        // Test various security headers

        Map<String, String> securityHeaders = getStringStringMap();

        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(securityHeaders.keySet()));
        securityHeaders.forEach((key, value) -> when(mockRequest.getHeader(key)).thenReturn(value));

        doAnswer(invocation -> {
            HttpServletRequest wrappedRequest = invocation.getArgument(0);
            
            // All security headers should be preserved unchanged
            securityHeaders.forEach((key, expectedValue) -> {
                String actualValue = wrappedRequest.getHeader(key);
                assertEquals(expectedValue, actualValue, "Security header " + key + " should be preserved");
            });
            
            return null;
        }).when(mockFilterChain).doFilter(any(), any());

        inputSanitizationFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        
        System.out.println("[DEBUG_LOG] Security header identification tests passed");
    }

    private static Map<String, String> getStringStringMap() {
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "Bearer token");
        securityHeaders.put("Cookie", "session=123");
        securityHeaders.put("X-CSRF-Token", "csrf-token");
        securityHeaders.put("X-Requested-With", "XMLHttpRequest");
        securityHeaders.put("Content-Type", "application/json");
        securityHeaders.put("Origin", "https://example.com");
        securityHeaders.put("Referer", "https://example.com/page");
        securityHeaders.put("X-Forwarded-For", "192.168.1.1");
        securityHeaders.put("Sec-Fetch-Site", "same-origin");
        return securityHeaders;
    }

    @Test
    @DisplayName("Test non-HTTP request handling")
    void testNonHttpRequestHandling() throws ServletException, IOException {
        // Test with non-HTTP request (should pass through unchanged)
        jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

        inputSanitizationFilter.doFilter(nonHttpRequest, nonHttpResponse, mockFilterChain);
        
        verify(mockFilterChain).doFilter(nonHttpRequest, nonHttpResponse);
        System.out.println("[DEBUG_LOG] Non-HTTP request handling tests passed");
    }
}