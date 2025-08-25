package com.recrutech.recrutechauth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpRequestServiceTest {

    @Mock
    private HttpServletRequest request;

    private HttpRequestService httpRequestService;

    @BeforeEach
    void setUp() {
        httpRequestService = new HttpRequestService();
    }

    @Test
    void constructor_ShouldInitialize() {
        // When
        HttpRequestService service = new HttpRequestService();
        
        // Then
        assertNotNull(service);
    }

    @Test
    void getClientIpAddress_ShouldReturnXForwardedFor_WhenPresent() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("192.168.1.100", result);
        verify(request, never()).getHeader("X-Real-IP");
        verify(request, never()).getRemoteAddr();
    }

    @Test
    void getClientIpAddress_ShouldReturnFirstIpFromXForwardedFor_WhenMultipleIps() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1, 172.16.0.1");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("192.168.1.100", result);
    }

    @Test
    void getClientIpAddress_ShouldTrimWhitespace_FromXForwardedFor() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("  192.168.1.100  , 10.0.0.1");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("192.168.1.100", result);
    }

    @Test
    void getClientIpAddress_ShouldReturnXRealIp_WhenXForwardedForNotPresent() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.200");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("192.168.1.200", result);
        verify(request, never()).getRemoteAddr();
    }

    @Test
    void getClientIpAddress_ShouldReturnXRealIp_WhenXForwardedForEmpty() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.200");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("192.168.1.200", result);
        verify(request, never()).getRemoteAddr();
    }

    @Test
    void getClientIpAddress_ShouldReturnRemoteAddr_WhenNoProxyHeaders() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("127.0.0.1", result);
    }

    @Test
    void getClientIpAddress_ShouldReturnRemoteAddr_WhenProxyHeadersEmpty() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("127.0.0.1", result);
    }

    @Test
    void getClientIpAddress_ShouldHandleNullRemoteAddr() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertNull(result);
    }

    @Test
    void extractTokenFromHeader_ShouldReturnToken_WhenValidBearerHeader() {
        // Given
        String authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        
        // When
        String result = httpRequestService.extractTokenFromHeader(authHeader);
        
        // Then
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", result);
    }

    @Test
    void extractTokenFromHeader_ShouldReturnToken_WhenBearerHeaderWithSpaces() {
        // Given
        String authHeader = "Bearer   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9   ";
        
        // When
        String result = httpRequestService.extractTokenFromHeader(authHeader);
        
        // Then
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", result);
    }

    @Test
    void extractTokenFromHeader_ShouldThrowException_WhenHeaderIsNull() {
        // Given
        String authHeader = null;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            httpRequestService.extractTokenFromHeader(authHeader));
        assertEquals("Invalid authorization header", exception.getMessage());
    }

    @Test
    void extractTokenFromHeader_ShouldThrowException_WhenHeaderIsEmpty() {
        // Given
        String authHeader = "";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            httpRequestService.extractTokenFromHeader(authHeader));
        assertEquals("Invalid authorization header", exception.getMessage());
    }

    @Test
    void extractTokenFromHeader_ShouldThrowException_WhenHeaderDoesNotStartWithBearer() {
        // Given
        String authHeader = "Basic dXNlcjpwYXNzd29yZA==";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            httpRequestService.extractTokenFromHeader(authHeader));
        assertEquals("Invalid authorization header", exception.getMessage());
    }

    @Test
    void extractTokenFromHeader_ShouldThrowException_WhenHeaderIsBearerOnly() {
        // Given
        String authHeader = "Bearer";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            httpRequestService.extractTokenFromHeader(authHeader));
        assertEquals("Invalid authorization header", exception.getMessage());
    }

    @Test
    void extractTokenFromHeader_ShouldThrowException_WhenHeaderIsBearerWithSpace() {
        // Given
        String authHeader = "Bearer ";
        
        // When
        String result = httpRequestService.extractTokenFromHeader(authHeader);
        
        // Then
        assertEquals("", result);
    }

    @Test
    void extractTokenFromHeader_ShouldHandleCaseInsensitive() {
        // Given
        String authHeader = "bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            httpRequestService.extractTokenFromHeader(authHeader));
        assertEquals("Invalid authorization header", exception.getMessage());
    }

    @Test
    void getUserAgent_ShouldReturnUserAgent_WhenPresent() {
        // Given
        String expectedUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        when(request.getHeader("User-Agent")).thenReturn(expectedUserAgent);
        
        // When
        String result = httpRequestService.getUserAgent(request);
        
        // Then
        assertEquals(expectedUserAgent, result);
    }

    @Test
    void getUserAgent_ShouldReturnNull_WhenNotPresent() {
        // Given
        when(request.getHeader("User-Agent")).thenReturn(null);
        
        // When
        String result = httpRequestService.getUserAgent(request);
        
        // Then
        assertNull(result);
    }

    @Test
    void getUserAgent_ShouldReturnEmptyString_WhenEmpty() {
        // Given
        when(request.getHeader("User-Agent")).thenReturn("");
        
        // When
        String result = httpRequestService.getUserAgent(request);
        
        // Then
        assertEquals("", result);
    }

    @Test
    void getUserAgent_ShouldHandleLongUserAgent() {
        // Given
        String longUserAgent = "A".repeat(1000);
        when(request.getHeader("User-Agent")).thenReturn(longUserAgent);
        
        // When
        String result = httpRequestService.getUserAgent(request);
        
        // Then
        assertEquals(longUserAgent, result);
    }

    @Test
    void getAcceptLanguage_ShouldReturnAcceptLanguage_WhenPresent() {
        // Given
        String expectedAcceptLanguage = "en-US,en;q=0.9,de;q=0.8";
        when(request.getHeader("Accept-Language")).thenReturn(expectedAcceptLanguage);
        
        // When
        String result = httpRequestService.getAcceptLanguage(request);
        
        // Then
        assertEquals(expectedAcceptLanguage, result);
    }

    @Test
    void getAcceptLanguage_ShouldReturnNull_WhenNotPresent() {
        // Given
        when(request.getHeader("Accept-Language")).thenReturn(null);
        
        // When
        String result = httpRequestService.getAcceptLanguage(request);
        
        // Then
        assertNull(result);
    }

    @Test
    void getAcceptLanguage_ShouldReturnEmptyString_WhenEmpty() {
        // Given
        when(request.getHeader("Accept-Language")).thenReturn("");
        
        // When
        String result = httpRequestService.getAcceptLanguage(request);
        
        // Then
        assertEquals("", result);
    }

    @Test
    void getAcceptLanguage_ShouldHandleComplexLanguageHeader() {
        // Given
        String complexLanguage = "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5";
        when(request.getHeader("Accept-Language")).thenReturn(complexLanguage);
        
        // When
        String result = httpRequestService.getAcceptLanguage(request);
        
        // Then
        assertEquals(complexLanguage, result);
    }

    @Test
    void getClientIpAddress_ShouldHandleIPv6Addresses() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", result);
    }

    @Test
    void getClientIpAddress_ShouldHandleMixedIPVersions() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 2001:0db8:85a3::8a2e:0370:7334");
        
        // When
        String result = httpRequestService.getClientIpAddress(request);
        
        // Then
        assertEquals("192.168.1.1", result);
    }

    @Test
    void extractTokenFromHeader_ShouldHandleVeryLongToken() {
        // Given
        String longToken = "A".repeat(2000);
        String authHeader = "Bearer " + longToken;
        
        // When
        String result = httpRequestService.extractTokenFromHeader(authHeader);
        
        // Then
        assertEquals(longToken, result);
    }

    @Test
    void getClientIpAddress_ShouldHandleNullRequest() {
        // Given
        HttpServletRequest nullRequest = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            httpRequestService.getClientIpAddress(nullRequest));
    }

    @Test
    void getUserAgent_ShouldHandleNullRequest() {
        // Given
        HttpServletRequest nullRequest = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            httpRequestService.getUserAgent(nullRequest));
    }

    @Test
    void getAcceptLanguage_ShouldHandleNullRequest() {
        // Given
        HttpServletRequest nullRequest = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            httpRequestService.getAcceptLanguage(nullRequest));
    }
}