package com.recrutech.recrutechplatform.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileMetadata model class.
 * Tests getters, setters, and lifecycle methods.
 */
class FileMetadataTest {

    private FileMetadata fileMetadata;

    @BeforeEach
    void setUp() {
        fileMetadata = new FileMetadata();
    }

    @Test
    void testFileNameGetterAndSetter() {
        // Given
        String fileName = "test-document.pdf";

        // When
        fileMetadata.setFileName(fileName);

        // Then
        assertEquals(fileName, fileMetadata.getFileName());
    }

    @Test
    void testContentTypeGetterAndSetter() {
        // Given
        String contentType = "application/pdf";

        // When
        fileMetadata.setContentType(contentType);

        // Then
        assertEquals(contentType, fileMetadata.getContentType());
    }

    @Test
    void testSizeGetterAndSetter() {
        // Given
        Long size = 1024L;

        // When
        fileMetadata.setSize(size);

        // Then
        assertEquals(size, fileMetadata.getSize());
    }

    @Test
    void testFilePathGetterAndSetter() {
        // Given
        String filePath = "/uploads/documents/test-document.pdf";

        // When
        fileMetadata.setFilePath(filePath);

        // Then
        assertEquals(filePath, fileMetadata.getFilePath());
    }

    @Test
    void testOnCreateMethod() {
        // Given
        FileMetadata newFileMetadata = new FileMetadata();
        
        // When
        newFileMetadata.onCreate();

        // Then
        // The onCreate method calls initializeEntity() from BaseEntity
        // This should set the ID and timestamps
        assertNotNull(newFileMetadata.getId());
        assertNotNull(newFileMetadata.getCreatedAt());
    }

    @Test
    void testAllFieldsSetCorrectly() {
        // Given
        String fileName = "resume.pdf";
        String contentType = "application/pdf";
        Long size = 2048L;
        String filePath = "/files/resumes/resume.pdf";

        // When
        fileMetadata.setFileName(fileName);
        fileMetadata.setContentType(contentType);
        fileMetadata.setSize(size);
        fileMetadata.setFilePath(filePath);

        // Then
        assertEquals(fileName, fileMetadata.getFileName());
        assertEquals(contentType, fileMetadata.getContentType());
        assertEquals(size, fileMetadata.getSize());
        assertEquals(filePath, fileMetadata.getFilePath());
    }

    @Test
    void testFileMetadataCreation() {
        // Given & When
        FileMetadata newFile = new FileMetadata();

        // Then
        assertNotNull(newFile);
        assertNull(newFile.getFileName());
        assertNull(newFile.getContentType());
        assertNull(newFile.getSize());
        assertNull(newFile.getFilePath());
    }

    @Test
    void testNullValues() {
        // When
        fileMetadata.setFileName(null);
        fileMetadata.setContentType(null);
        fileMetadata.setSize(null);
        fileMetadata.setFilePath(null);

        // Then
        assertNull(fileMetadata.getFileName());
        assertNull(fileMetadata.getContentType());
        assertNull(fileMetadata.getSize());
        assertNull(fileMetadata.getFilePath());
    }
}