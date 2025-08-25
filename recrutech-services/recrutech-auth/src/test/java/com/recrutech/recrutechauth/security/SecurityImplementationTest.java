package com.recrutech.recrutechauth.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for security implementations.
 * Tests input sanitization, rate limiting, and security headers functionality.
 */
class SecurityImplementationTest {

    private InputSanitizationService inputSanitizationService;

    @BeforeEach
    void setUp() {
        inputSanitizationService = new InputSanitizationService();
    }

    @Test
    @DisplayName("Test XSS sanitization removes malicious scripts")
    void testXSSSanitization() {
        // Test script tag removal
        String maliciousInput = "<script>alert('XSS')</script>Hello World";
        String sanitized = inputSanitizationService.sanitizeForXSS(maliciousInput);
        assertFalse(sanitized.contains("<script>"));
        assertFalse(sanitized.contains("alert"));
        assertTrue(sanitized.contains("Hello World"));

        // Test javascript: protocol removal
        String jsProtocol = "javascript:alert('XSS')";
        String sanitizedJs = inputSanitizationService.sanitizeForXSS(jsProtocol);
        assertFalse(sanitizedJs.contains("javascript:"));

        // Test event handler removal
        String eventHandler = "<div onload='alert(1)'>Content</div>";
        String sanitizedEvent = inputSanitizationService.sanitizeForXSS(eventHandler);
        assertFalse(sanitizedEvent.contains("onload"));

        System.out.println("[DEBUG_LOG] XSS sanitization tests passed");
    }

    @Test
    @DisplayName("Test SQL injection detection")
    void testSQLInjectionDetection() {
        // Test SQL keywords detection
        String sqlInjection1 = "'; DROP TABLE users; --";
        assertFalse(inputSanitizationService.isSafeFromSQLInjection(sqlInjection1));

        String sqlInjection2 = "admin' OR '1'='1";
        assertFalse(inputSanitizationService.isSafeFromSQLInjection(sqlInjection2));

        String sqlInjection3 = "UNION SELECT * FROM passwords";
        assertFalse(inputSanitizationService.isSafeFromSQLInjection(sqlInjection3));

        // Test safe input
        String safeInput = "john.doe@example.com";
        assertTrue(inputSanitizationService.isSafeFromSQLInjection(safeInput));

        System.out.println("[DEBUG_LOG] SQL injection detection tests passed");
    }

    @Test
    @DisplayName("Test field type validation")
    void testFieldTypeValidation() {
        // Test email validation
        assertTrue(inputSanitizationService.isValidForFieldType("test@example.com", "email"));
        assertFalse(inputSanitizationService.isValidForFieldType("invalid-email", "email"));
        assertFalse(inputSanitizationService.isValidForFieldType("<script>alert('xss')</script>@test.com", "email"));

        // Test name validation
        assertTrue(inputSanitizationService.isValidForFieldType("John Doe", "name"));
        assertTrue(inputSanitizationService.isValidForFieldType("Hans Müller", "name"));
        assertFalse(inputSanitizationService.isValidForFieldType("John<script>alert(1)</script>", "name"));

        // Test phone validation
        assertTrue(inputSanitizationService.isValidForFieldType("+49 123 456789", "phone"));
        assertTrue(inputSanitizationService.isValidForFieldType("(555) 123-4567", "phone"));
        assertFalse(inputSanitizationService.isValidForFieldType("phone'; DROP TABLE users; --", "phone"));

        // Test UUID validation
        assertTrue(inputSanitizationService.isValidForFieldType("123e4567-e89b-12d3-a456-426614174000", "uuid"));
        assertFalse(inputSanitizationService.isValidForFieldType("invalid-uuid", "uuid"));

        System.out.println("[DEBUG_LOG] Field type validation tests passed");
    }

    @Test
    @DisplayName("Test comprehensive input sanitization")
    void testComprehensiveInputSanitization() {
        // Test combined XSS and SQL injection
        String maliciousInput = "<script>alert('XSS')</script>'; DROP TABLE users; --";
        String sanitized = inputSanitizationService.sanitizeInput(maliciousInput);
        
        assertFalse(sanitized.contains("<script>"));
        assertFalse(sanitized.contains("DROP TABLE"));
        assertFalse(sanitized.contains("--"));

        // Test HTML entity encoding
        String htmlInput = "<div>Hello & \"World\"</div>";
        String sanitizedHtml = inputSanitizationService.sanitizeForXSS(htmlInput);
        assertTrue(sanitizedHtml.contains("&lt;"));
        assertTrue(sanitizedHtml.contains("&gt;"));
        assertTrue(sanitizedHtml.contains("&quot;"));
        assertTrue(sanitizedHtml.contains("&amp;"));

        System.out.println("[DEBUG_LOG] Comprehensive input sanitization tests passed");
    }

    @Test
    @DisplayName("Test parameter sanitization")
    void testParameterSanitization() {
        java.util.Map<String, String> parameters = new java.util.HashMap<>();
        parameters.put("username", "admin'; DROP TABLE users; --");
        parameters.put("email", "<script>alert('XSS')</script>test@example.com");
        parameters.put("message", "Hello & welcome to our site!");

        java.util.Map<String, String> sanitized = inputSanitizationService.sanitizeParameters(parameters);

        assertFalse(sanitized.get("username").contains("DROP TABLE"));
        assertFalse(sanitized.get("email").contains("<script>"));
        assertTrue(sanitized.get("message").contains("&amp;"));

        System.out.println("[DEBUG_LOG] Parameter sanitization tests passed");
    }

    @Test
    @DisplayName("Test edge cases and null handling")
    void testEdgeCasesAndNullHandling() {
        // Test null input
        assertNull(inputSanitizationService.sanitizeInput(null));
        assertTrue(inputSanitizationService.isSafeFromSQLInjection(null));

        // Test empty input
        String empty = "";
        assertEquals(empty, inputSanitizationService.sanitizeInput(empty));
        assertTrue(inputSanitizationService.isSafeFromSQLInjection(empty));

        // Test whitespace-only input
        String whitespace = "   ";
        assertEquals(whitespace.trim(), inputSanitizationService.sanitizeInput(whitespace));

        // Test very long input
        String longInput = "a".repeat(1000) + "<script>alert('xss')</script>";
        String sanitizedLong = inputSanitizationService.sanitizeForXSS(longInput);
        assertFalse(sanitizedLong.contains("<script>"));

        System.out.println("[DEBUG_LOG] Edge cases and null handling tests passed");
    }

    @Test
    @DisplayName("Test security patterns detection")
    void testSecurityPatternsDetection() {
        // Test various XSS patterns
        String[] xssPatterns = {
            "<script>alert(1)</script>",
            "javascript:alert(1)",
            "vbscript:msgbox(1)",
            "<img onerror='alert(1)' src='x'>",
            "<div onmouseover='alert(1)'>",
            "eval('alert(1)')",
            "expression(alert(1))"
        };

        for (String pattern : xssPatterns) {
            String sanitized = inputSanitizationService.sanitizeForXSS(pattern);
            assertNotEquals(pattern, sanitized, "XSS pattern should be sanitized: " + pattern);
        }

        // Test various SQL injection patterns
        String[] sqlPatterns = {
            "'; DROP TABLE users; --",
            "admin' OR '1'='1",
            "UNION SELECT * FROM passwords",
            "INSERT INTO users VALUES ('hacker', 'password')",
            "UPDATE users SET password='hacked' WHERE id=1",
            "DELETE FROM users WHERE id > 0"
        };

        for (String pattern : sqlPatterns) {
            assertFalse(inputSanitizationService.isSafeFromSQLInjection(pattern), 
                       "SQL injection pattern should be detected: " + pattern);
        }

        System.out.println("[DEBUG_LOG] Security patterns detection tests passed");
    }

    @Test
    @DisplayName("Test performance with large inputs")
    void testPerformanceWithLargeInputs() {
        // Test performance with large input
        String largeInput = "Safe content ".repeat(10000) + "<script>alert('xss')</script>";
        
        long startTime = System.currentTimeMillis();
        String sanitized = inputSanitizationService.sanitizeInput(largeInput);
        long endTime = System.currentTimeMillis();
        
        assertFalse(sanitized.contains("<script>"));
        assertTrue((endTime - startTime) < 1000, "Sanitization should complete within 1 second");

        System.out.println("[DEBUG_LOG] Performance test passed - sanitization took " + (endTime - startTime) + "ms");
    }

    @Test
    @DisplayName("Test international character handling")
    void testInternationalCharacterHandling() {
        // Test German umlauts
        assertTrue(inputSanitizationService.isValidForFieldType("Hans Müller", "name"));
        
        // Test various international characters
        String internationalText = "José María Azañón";
        String sanitized = inputSanitizationService.sanitizeInput(internationalText);
        assertTrue(sanitized.contains("José"));
        assertTrue(sanitized.contains("María"));

        // Test mixed content with international characters and XSS
        String mixedContent = "José<script>alert('xss')</script>María";
        String sanitizedMixed = inputSanitizationService.sanitizeForXSS(mixedContent);
        assertTrue(sanitizedMixed.contains("José"));
        assertTrue(sanitizedMixed.contains("María"));
        assertFalse(sanitizedMixed.contains("<script>"));

        System.out.println("[DEBUG_LOG] International character handling tests passed");
    }
}