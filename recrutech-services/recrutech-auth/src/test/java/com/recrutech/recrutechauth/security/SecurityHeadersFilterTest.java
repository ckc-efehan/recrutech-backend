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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for SecurityHeadersFilter.
 * Tests security header addition, CSP generation, and endpoint-specific behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityHeadersFilter Tests")
class SecurityHeadersFilterTest {

    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private FilterChain mockFilterChain;
    
    private SecurityHeadersFilter securityHeadersFilter;
    private Map<String, String> capturedHeaders;

    @BeforeEach
    void setUp() {
        securityHeadersFilter = new SecurityHeadersFilter();
        capturedHeaders = new HashMap<>();
        
        // Capture all headers set on the response (lenient to avoid UnnecessaryStubbingException)
        lenient().doAnswer(invocation -> {
            String headerName = invocation.getArgument(0);
            String headerValue = invocation.getArgument(1);
            capturedHeaders.put(headerName, headerValue);
            return null;
        }).when(mockResponse).setHeader(anyString(), anyString());
    }

    @Test
    @DisplayName("Test filter initialization and destruction")
    void testFilterLifecycle() {
        // Test initialization
        assertDoesNotThrow(() -> securityHeadersFilter.init(null));
        
        // Test destruction
        assertDoesNotThrow(() -> securityHeadersFilter.destroy());
        
        System.out.println("[DEBUG_LOG] SecurityHeadersFilter lifecycle tests passed");
    }

    @Test
    @DisplayName("Test basic security headers addition")
    void testBasicSecurityHeaders() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify basic security headers
        assertEquals("DENY", capturedHeaders.get("X-Frame-Options"));
        assertEquals("nosniff", capturedHeaders.get("X-Content-Type-Options"));
        assertEquals("1; mode=block", capturedHeaders.get("X-XSS-Protection"));
        assertEquals("max-age=31536000; includeSubDomains; preload", capturedHeaders.get("Strict-Transport-Security"));
        assertEquals("strict-origin-when-cross-origin", capturedHeaders.get("Referrer-Policy"));
        
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        System.out.println("[DEBUG_LOG] Basic security headers tests passed");
    }

    @Test
    @DisplayName("Test Content Security Policy generation")
    void testContentSecurityPolicy() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        String csp = capturedHeaders.get("Content-Security-Policy");
        assertNotNull(csp);
        
        // Verify CSP contains expected directives
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("script-src 'self' 'unsafe-inline' 'unsafe-eval'"));
        assertTrue(csp.contains("style-src 'self' 'unsafe-inline'"));
        assertTrue(csp.contains("object-src 'none'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
        assertTrue(csp.contains("form-action 'self'"));
        
        System.out.println("[DEBUG_LOG] Content Security Policy tests passed");
    }

    @Test
    @DisplayName("Test Permissions Policy generation")
    void testPermissionsPolicy() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        String permissionsPolicy = capturedHeaders.get("Permissions-Policy");
        assertNotNull(permissionsPolicy);
        
        // Verify Permissions Policy contains expected features
        assertTrue(permissionsPolicy.contains("camera=()"));
        assertTrue(permissionsPolicy.contains("microphone=()"));
        assertTrue(permissionsPolicy.contains("geolocation=()"));
        assertTrue(permissionsPolicy.contains("payment=()"));
        assertTrue(permissionsPolicy.contains("usb=()"));
        
        System.out.println("[DEBUG_LOG] Permissions Policy tests passed");
    }

    @Test
    @DisplayName("Test sensitive endpoint cache control")
    void testSensitiveEndpointCacheControl() throws ServletException, IOException {
        // Test auth endpoint
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        assertEquals("no-cache, no-store, must-revalidate, private", capturedHeaders.get("Cache-Control"));
        assertEquals("no-cache", capturedHeaders.get("Pragma"));
        assertEquals("0", capturedHeaders.get("Expires"));
        
        System.out.println("[DEBUG_LOG] Sensitive endpoint cache control tests passed");
    }

    @Test
    @DisplayName("Test non-sensitive endpoint cache control")
    void testNonSensitiveEndpointCacheControl() throws ServletException, IOException {
        // Test non-sensitive endpoint
        when(mockRequest.getRequestURI()).thenReturn("/api/public/info");
        when(mockRequest.getMethod()).thenReturn("GET");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Cache control headers should not be set for non-sensitive endpoints
        assertNull(capturedHeaders.get("Cache-Control"));
        assertNull(capturedHeaders.get("Pragma"));
        assertNull(capturedHeaders.get("Expires"));
        
        System.out.println("[DEBUG_LOG] Non-sensitive endpoint cache control tests passed");
    }

    @Test
    @DisplayName("Test cross-origin security headers")
    void testCrossOriginSecurityHeaders() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        assertEquals("none", capturedHeaders.get("X-Permitted-Cross-Domain-Policies"));
        assertEquals("require-corp", capturedHeaders.get("Cross-Origin-Embedder-Policy"));
        assertEquals("same-origin", capturedHeaders.get("Cross-Origin-Opener-Policy"));
        assertEquals("same-origin", capturedHeaders.get("Cross-Origin-Resource-Policy"));
        
        System.out.println("[DEBUG_LOG] Cross-origin security headers tests passed");
    }

    @Test
    @DisplayName("Test server header customization")
    void testServerHeaderCustomization() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        assertEquals("RecruTech-Auth", capturedHeaders.get("Server"));
        assertEquals("", capturedHeaders.get("X-Powered-By"));
        
        System.out.println("[DEBUG_LOG] Server header customization tests passed");
    }

    @Test
    @DisplayName("Test stateless session protection headers")
    void testStatelessSessionProtection() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        assertEquals("stateless", capturedHeaders.get("X-Session-Management"));
        assertEquals("JWT", capturedHeaders.get("X-Auth-Method"));
        assertEquals("required", capturedHeaders.get("X-Token-Security"));
        
        // Verify request ID and timestamp are set
        assertNotNull(capturedHeaders.get("X-Request-ID"));
        assertNotNull(capturedHeaders.get("X-Request-Time"));
        
        // Verify request ID is a valid UUID format
        String requestId = capturedHeaders.get("X-Request-ID");
        assertTrue(requestId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
        
        System.out.println("[DEBUG_LOG] Stateless session protection tests passed");
    }

    @Test
    @DisplayName("Test CSRF protection headers for state-changing requests")
    void testCSRFProtectionHeaders() throws ServletException, IOException {
        // Test POST request (state-changing)
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/register");
        when(mockRequest.getMethod()).thenReturn("POST");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        assertEquals("required", capturedHeaders.get("X-CSRF-Protection"));
        assertEquals("true", capturedHeaders.get("X-Custom-Header-Required"));
        assertEquals("strict", capturedHeaders.get("X-SameSite-Policy"));
        assertEquals("required", capturedHeaders.get("X-Origin-Validation"));
        
        System.out.println("[DEBUG_LOG] CSRF protection headers tests passed");
    }

    @Test
    @DisplayName("Test CSRF protection headers for non-state-changing requests")
    void testCSRFProtectionHeadersForGetRequests() throws ServletException, IOException {
        // Test GET request (non-state-changing)
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/health");
        when(mockRequest.getMethod()).thenReturn("GET");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // CSRF-specific headers should not be set for GET requests
        assertNull(capturedHeaders.get("X-CSRF-Protection"));
        assertNull(capturedHeaders.get("X-Custom-Header-Required"));
        
        // But general security headers should still be set
        assertEquals("strict", capturedHeaders.get("X-SameSite-Policy"));
        assertEquals("required", capturedHeaders.get("X-Origin-Validation"));
        
        System.out.println("[DEBUG_LOG] CSRF protection for GET requests tests passed");
    }

    @Test
    @DisplayName("Test sensitive endpoint detection")
    void testSensitiveEndpointDetection() throws ServletException, IOException {
        String[] sensitiveEndpoints = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/gdpr/delete-data",
            "/api/user/profile",
            "/api/admin/settings"
        };

        for (String endpoint : sensitiveEndpoints) {
            capturedHeaders.clear(); // Reset for each test
            when(mockRequest.getRequestURI()).thenReturn(endpoint);
            when(mockRequest.getMethod()).thenReturn("POST");

            securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

            // Sensitive endpoints should have cache control headers
            assertEquals("no-cache, no-store, must-revalidate, private", capturedHeaders.get("Cache-Control"),
                        "Endpoint " + endpoint + " should have cache control headers");
        }
        
        System.out.println("[DEBUG_LOG] Sensitive endpoint detection tests passed");
    }

    @Test
    @DisplayName("Test state-changing request detection")
    void testStateChangingRequestDetection() throws ServletException, IOException {
        String[] stateChangingMethods = {"POST", "PUT", "DELETE", "PATCH"};
        String[] nonStateChangingMethods = {"GET", "HEAD", "OPTIONS"};

        // Test state-changing methods
        for (String method : stateChangingMethods) {
            capturedHeaders.clear();
            when(mockRequest.getRequestURI()).thenReturn("/api/test");
            when(mockRequest.getMethod()).thenReturn(method);

            securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

            assertEquals("required", capturedHeaders.get("X-CSRF-Protection"),
                        "Method " + method + " should have CSRF protection");
        }

        // Test non-state-changing methods
        for (String method : nonStateChangingMethods) {
            capturedHeaders.clear();
            when(mockRequest.getRequestURI()).thenReturn("/api/test");
            when(mockRequest.getMethod()).thenReturn(method);

            securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

            assertNull(capturedHeaders.get("X-CSRF-Protection"),
                      "Method " + method + " should not have CSRF protection");
        }
        
        System.out.println("[DEBUG_LOG] State-changing request detection tests passed");
    }

    @Test
    @DisplayName("Test non-HTTP request handling")
    void testNonHttpRequestHandling() throws ServletException, IOException {
        // Test with non-HTTP request (should pass through unchanged)
        jakarta.servlet.ServletRequest nonHttpRequest = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse nonHttpResponse = mock(jakarta.servlet.ServletResponse.class);

        securityHeadersFilter.doFilter(nonHttpRequest, nonHttpResponse, mockFilterChain);
        
        verify(mockFilterChain).doFilter(nonHttpRequest, nonHttpResponse);
        // No headers should be captured since it's not an HTTP response
        assertTrue(capturedHeaders.isEmpty());
        
        System.out.println("[DEBUG_LOG] Non-HTTP request handling tests passed");
    }

    @Test
    @DisplayName("Test all security headers are present")
    void testAllSecurityHeadersPresent() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify all expected security headers are present
        String[] expectedHeaders = {
            "X-Frame-Options",
            "X-Content-Type-Options", 
            "X-XSS-Protection",
            "Strict-Transport-Security",
            "Referrer-Policy",
            "Content-Security-Policy",
            "Permissions-Policy",
            "Cache-Control",
            "Pragma",
            "Expires",
            "X-Permitted-Cross-Domain-Policies",
            "Cross-Origin-Embedder-Policy",
            "Cross-Origin-Opener-Policy",
            "Cross-Origin-Resource-Policy",
            "Server",
            "X-Powered-By",
            "X-Session-Management",
            "X-Auth-Method",
            "X-Token-Security",
            "X-Request-ID",
            "X-Request-Time",
            "X-CSRF-Protection",
            "X-Custom-Header-Required",
            "X-SameSite-Policy",
            "X-Origin-Validation"
        };

        for (String header : expectedHeaders) {
            assertNotNull(capturedHeaders.get(header), "Header " + header + " should be present");
        }

        assertEquals(expectedHeaders.length, capturedHeaders.size(), 
                    "All expected headers should be present");
        
        System.out.println("[DEBUG_LOG] All security headers presence tests passed");
    }

    @Test
    @DisplayName("Test header values are not empty")
    void testHeaderValuesNotEmpty() throws ServletException, IOException {
        when(mockRequest.getRequestURI()).thenReturn("/api/auth/login");
        when(mockRequest.getMethod()).thenReturn("POST");

        securityHeadersFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Verify header values are not null or empty (except X-Powered-By which is intentionally empty)
        capturedHeaders.forEach((headerName, headerValue) -> {
            if (!"X-Powered-By".equals(headerName)) {
                assertNotNull(headerValue, "Header " + headerName + " should not be null");
                assertFalse(headerValue.trim().isEmpty(), "Header " + headerName + " should not be empty");
            }
        });
        
        System.out.println("[DEBUG_LOG] Header values not empty tests passed");
    }
}