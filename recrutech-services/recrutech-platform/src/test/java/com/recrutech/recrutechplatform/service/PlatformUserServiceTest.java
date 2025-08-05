package com.recrutech.recrutechplatform.service;

import com.recrutech.recrutechplatform.dto.auth.AuthUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlatformUserService.
 * Tests REST client calls, error handling, and fallback mechanisms.
 */
@ExtendWith(MockitoExtension.class)
class PlatformUserServiceTest {

    @Mock
    private RestTemplate authServiceRestTemplate;

    private PlatformUserService platformUserService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";

    @BeforeEach
    void setUp() {
        platformUserService = new PlatformUserService(authServiceRestTemplate);
    }

    @Test
    void testGetUserFirstName_Success() {
        // Given
        AuthUserResponse mockResponse = new AuthUserResponse(
                TEST_USER_ID, TEST_FIRST_NAME, TEST_LAST_NAME, "john.doe@example.com", true);
        ResponseEntity<AuthUserResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenReturn(responseEntity);

        // When
        String firstName = platformUserService.getUserFirstName(TEST_USER_ID);

        // Then
        assertEquals(TEST_FIRST_NAME, firstName);
        verify(authServiceRestTemplate).getForEntity("/api/users/{userId}", AuthUserResponse.class, TEST_USER_ID);
    }

    @Test
    void testGetUserLastName_Success() {
        // Given
        AuthUserResponse mockResponse = new AuthUserResponse(
                TEST_USER_ID, TEST_FIRST_NAME, TEST_LAST_NAME, "john.doe@example.com", true);
        ResponseEntity<AuthUserResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenReturn(responseEntity);

        // When
        String lastName = platformUserService.getUserLastName(TEST_USER_ID);

        // Then
        assertEquals(TEST_LAST_NAME, lastName);
    }


    @Test
    void testUserExists_ActiveUser() {
        // Given
        AuthUserResponse mockResponse = new AuthUserResponse(
                TEST_USER_ID, TEST_FIRST_NAME, TEST_LAST_NAME, "john.doe@example.com", true);
        ResponseEntity<AuthUserResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenReturn(responseEntity);

        // When
        boolean exists = platformUserService.userExists(TEST_USER_ID);

        // Then
        assertTrue(exists);
    }

    @Test
    void testUserExists_InactiveUser() {
        // Given
        AuthUserResponse mockResponse = new AuthUserResponse(
                TEST_USER_ID, TEST_FIRST_NAME, TEST_LAST_NAME, "john.doe@example.com", false);
        ResponseEntity<AuthUserResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenReturn(responseEntity);

        // When
        boolean exists = platformUserService.userExists(TEST_USER_ID);

        // Then
        assertFalse(exists);
    }

    @Test
    void testGetUserFirstName_NullUserId() {
        // When
        String firstName = platformUserService.getUserFirstName(null);

        // Then
        assertEquals("Unknown", firstName);
        verifyNoInteractions(authServiceRestTemplate);
    }

    @Test
    void testGetUserFirstName_EmptyUserId() {
        // When
        String firstName = platformUserService.getUserFirstName("   ");

        // Then
        assertEquals("Unknown", firstName);
        verifyNoInteractions(authServiceRestTemplate);
    }

    @Test
    void testGetUserFirstName_RestClientException() {
        // Given
        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenThrow(new RestClientException("Connection failed"));

        // When
        String firstName = platformUserService.getUserFirstName(TEST_USER_ID);

        // Then
        assertEquals("Unknown", firstName);
    }

    @Test
    void testGetUserFirstName_NonOkResponse() {
        // Given
        ResponseEntity<AuthUserResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenReturn(responseEntity);

        // When
        String firstName = platformUserService.getUserFirstName(TEST_USER_ID);

        // Then
        assertEquals("Unknown", firstName);
    }

    @Test
    void testGetUserInfo_Success() {
        // Given
        AuthUserResponse mockResponse = new AuthUserResponse(
                TEST_USER_ID, TEST_FIRST_NAME, TEST_LAST_NAME, "john.doe@example.com", true);
        ResponseEntity<AuthUserResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenReturn(responseEntity);

        // When
        AuthUserResponse result = platformUserService.getUserInfo(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.id());
        assertEquals(TEST_FIRST_NAME, result.firstName());
        assertEquals(TEST_LAST_NAME, result.lastName());
        assertTrue(result.active());
    }

    @Test
    void testGetUserInfo_NullUserId() {
        // When
        AuthUserResponse result = platformUserService.getUserInfo(null);

        // Then
        assertNotNull(result);
        assertEquals("Unknown", result.firstName());
        assertEquals("User", result.lastName());
        assertFalse(result.active());
        verifyNoInteractions(authServiceRestTemplate);
    }

    @Test
    void testUserExists_NullUserId() {
        // When
        boolean exists = platformUserService.userExists(null);

        // Then
        assertFalse(exists);
        verifyNoInteractions(authServiceRestTemplate);
    }

    @Test
    void testUserExists_EmptyUserId() {
        // When
        boolean exists = platformUserService.userExists("");

        // Then
        assertFalse(exists);
        verifyNoInteractions(authServiceRestTemplate);
    }

    @Test
    void testUserExists_RestClientException() {
        // Given
        when(authServiceRestTemplate.getForEntity(anyString(), eq(AuthUserResponse.class), eq(TEST_USER_ID)))
                .thenThrow(new RestClientException("Service unavailable"));

        // When
        boolean exists = platformUserService.userExists(TEST_USER_ID);

        // Then
        assertFalse(exists); // Should return false for security in case of error
    }

}