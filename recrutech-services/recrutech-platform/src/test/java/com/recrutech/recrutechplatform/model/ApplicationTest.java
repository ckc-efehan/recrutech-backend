package com.recrutech.recrutechplatform.model;

import com.recrutech.recrutechplatform.enums.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Application model class.
 * Tests getters, setters, and lifecycle methods.
 */
class ApplicationTest {

    private Application application;
    private Job job;

    @BeforeEach
    void setUp() {
        application = new Application();
        job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setTitle("Software Engineer");
        job.setDescription("Job description");
        job.setLocation("Berlin");
    }

    @Test
    void testCvFileIdGetterAndSetter() {
        // Given
        String cvFileId = UUID.randomUUID().toString();

        // When
        application.setCvFileId(cvFileId);

        // Then
        assertEquals(cvFileId, application.getCvFileId());
    }

    @Test
    void testUserIdGetterAndSetter() {
        // Given
        String userId = UUID.randomUUID().toString();

        // When
        application.setUserId(userId);

        // Then
        assertEquals(userId, application.getUserId());
    }

    @Test
    void testStatusGetterAndSetter() {
        // Given
        ApplicationStatus status = ApplicationStatus.RECEIVED;

        // When
        application.setStatus(status);

        // Then
        assertEquals(status, application.getStatus());
    }

    @Test
    void testViewedByHrGetterAndSetter() {
        // Given
        boolean viewedByHr = true;

        // When
        application.setViewedByHr(viewedByHr);

        // Then
        assertTrue(application.isViewedByHr());

        // Test false case
        application.setViewedByHr(false);
        assertFalse(application.isViewedByHr());
    }

    @Test
    void testJobGetterAndSetter() {
        // When
        application.setJob(job);

        // Then
        assertEquals(job, application.getJob());
        assertEquals(job.getId(), application.getJob().getId());
        assertEquals(job.getTitle(), application.getJob().getTitle());
    }

    @Test
    void testOnCreateMethod() {
        // Given
        Application newApplication = new Application();
        
        // When
        newApplication.onCreate();

        // Then
        // The onCreate method calls initializeEntity() from BaseEntity
        // This should set the ID and timestamps
        assertNotNull(newApplication.getId());
        assertNotNull(newApplication.getCreatedAt());
    }

    @Test
    void testAllFieldsSetCorrectly() {
        // Given
        String cvFileId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        ApplicationStatus status = ApplicationStatus.RECEIVED;
        boolean viewedByHr = true;

        // When
        application.setCvFileId(cvFileId);
        application.setUserId(userId);
        application.setStatus(status);
        application.setViewedByHr(viewedByHr);
        application.setJob(job);

        // Then
        assertEquals(cvFileId, application.getCvFileId());
        assertEquals(userId, application.getUserId());
        assertEquals(status, application.getStatus());
        assertTrue(application.isViewedByHr());
        assertEquals(job, application.getJob());
    }

    @Test
    void testApplicationCreation() {
        // Given & When
        Application newApplication = new Application();

        // Then
        assertNotNull(newApplication);
        assertNull(newApplication.getCvFileId());
        assertNull(newApplication.getUserId());
        assertNull(newApplication.getStatus());
        assertFalse(newApplication.isViewedByHr()); // boolean defaults to false
        assertNull(newApplication.getJob());
    }

    @Test
    void testNullValues() {
        // When
        application.setCvFileId(null);
        application.setUserId(null);
        application.setStatus(null);
        application.setJob(null);

        // Then
        assertNull(application.getCvFileId());
        assertNull(application.getUserId());
        assertNull(application.getStatus());
        assertNull(application.getJob());
    }

    @Test
    void testApplicationStatusValues() {
        // Test all enum values
        application.setStatus(ApplicationStatus.RECEIVED);
        assertEquals(ApplicationStatus.RECEIVED, application.getStatus());

        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        assertEquals(ApplicationStatus.UNDER_REVIEW, application.getStatus());

        application.setStatus(ApplicationStatus.INVITED);
        assertEquals(ApplicationStatus.INVITED, application.getStatus());

        application.setStatus(ApplicationStatus.REJECTED);
        assertEquals(ApplicationStatus.REJECTED, application.getStatus());

        application.setStatus(ApplicationStatus.WITHDRAWN);
        assertEquals(ApplicationStatus.WITHDRAWN, application.getStatus());
    }

    @Test
    void testViewedByHrDefaultValue() {
        // Given
        Application newApplication = new Application();

        // Then
        // Boolean primitive defaults to false
        assertFalse(newApplication.isViewedByHr());
    }
}