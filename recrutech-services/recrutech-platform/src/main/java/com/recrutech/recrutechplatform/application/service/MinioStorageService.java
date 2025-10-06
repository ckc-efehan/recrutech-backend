package com.recrutech.recrutechplatform.application.service;

import com.recrutech.common.exception.FileStorageException;
import com.recrutech.common.exception.ValidationException;
import com.recrutech.recrutechplatform.config.MinioProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling document storage operations with MinIO.
 * Provides S3-compatible object storage for PDF documents.
 * Best practices applied:
 * - Lazy bucket initialization on service startup
 * - File validation (type, size, name)
 * - Unique object key generation
 * - Pre-signed URLs for temporary access
 * - Secure file storage with proper error handling
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioStorageService(
            MinioClient minioClient,
            MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * Initializes the MinIO bucket lazily after service construction.
     * This ensures the bucket exists before any storage operations are performed.
     * Bucket creation only happens if auto-create is enabled in properties.
     * Protected by circuit breaker to prevent cascading failures during startup.
     */
    @PostConstruct
    @CircuitBreaker(name = "minioService", fallbackMethod = "initializeBucketFallback")
    public void initializeBucket() {
        if (!minioProperties.isAutoCreateBucket()) {
            log.info("Auto-create bucket is disabled. Skipping bucket initialization.");
            return;
        }

        try {
            String bucketName = minioProperties.getBucketName();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                log.info("Bucket '{}' does not exist. Creating...", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Bucket '{}' created successfully.", bucketName);
            } else {
                log.info("Bucket '{}' already exists.", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", minioProperties.getBucketName(), e);
            throw new RuntimeException("Failed to initialize MinIO bucket: " + minioProperties.getBucketName(), e);
        }
    }

    /**
     * Stores a file in MinIO and returns the object key.
     * Object key format: applicantId/fileType/UUID.pdf
     * Protected by circuit breaker to prevent cascading failures.
     *
     * @param file the multipart file to store
     * @param fileType the type of document (coverLetter, resume, portfolio)
     * @param applicantId the ID of the applicant
     * @return the MinIO object key
     */
    @CircuitBreaker(name = "minioService", fallbackMethod = "storeFileFallback")
    public String storeFile(MultipartFile file, String fileType, String applicantId) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = getFileExtension(originalFilename);
        
        // Generate unique object key: applicantId/fileType/UUID.pdf
        String objectKey = String.format("%s/%s/%s.%s",
                applicantId,
                fileType,
                UUID.randomUUID(),
                fileExtension);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Successfully stored file: {}", objectKey);
            return objectKey;
        } catch (Exception ex) {
            log.error("Failed to store file in MinIO: {}", objectKey, ex);
            throw new FileStorageException("Could not store file in MinIO: " + objectKey, ex);
        }
    }

    /**
     * Loads a file from MinIO as a Resource.
     * Protected by circuit breaker to prevent cascading failures.
     *
     * @param objectKey the MinIO object key
     * @return the file as a Resource
     */
    @CircuitBreaker(name = "minioService", fallbackMethod = "loadFileAsResourceFallback")
    public Resource loadFileAsResource(String objectKey) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .build()
            );

            log.debug("Successfully loaded file: {}", objectKey);
            return new InputStreamResource(inputStream);
        } catch (Exception ex) {
            log.error("Failed to load file from MinIO: {}", objectKey, ex);
            throw new FileStorageException("File not found in MinIO: " + objectKey, ex);
        }
    }

    /**
     * Generates a pre-signed URL for temporary file access.
     * URL is valid for a specified duration.
     * Protected by circuit breaker to prevent cascading failures.
     *
     * @param objectKey the MinIO object key
     * @param expiryMinutes URL expiry time in minutes
     * @return pre-signed URL
     */
    @CircuitBreaker(name = "minioService", fallbackMethod = "generatePresignedUrlFallback")
    public String generatePresignedUrl(String objectKey, int expiryMinutes) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
            log.debug("Successfully generated presigned URL for: {}", objectKey);
            return url;
        } catch (Exception ex) {
            log.error("Failed to generate presigned URL for: {}", objectKey, ex);
            throw new FileStorageException("Could not generate presigned URL: " + objectKey, ex);
        }
    }

    /**
     * Deletes a file from MinIO.
     * Protected by circuit breaker to prevent cascading failures.
     *
     * @param objectKey the MinIO object key
     */
    @CircuitBreaker(name = "minioService", fallbackMethod = "deleteFileFallback")
    public void deleteFile(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            log.debug("Skipping file deletion - object key is empty");
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .build()
            );
            log.info("Successfully deleted file: {}", objectKey);
        } catch (Exception ex) {
            log.error("Failed to delete file from MinIO: {}", objectKey, ex);
            throw new FileStorageException("Could not delete file from MinIO: " + objectKey, ex);
        }
    }

    /**
     * Validates the uploaded file.
     * Checks file size, extension, and emptiness.
     *
     * @param file the file to validate
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty or null");
        }

        if (file.getSize() > minioProperties.getMaxFileSize()) {
            throw new ValidationException(
                    String.format("File size exceeds maximum allowed size of %d bytes",
                            minioProperties.getMaxFileSize()));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new ValidationException("Filename is invalid");
        }

        String fileExtension = getFileExtension(originalFilename);
        if (!isAllowedExtension(fileExtension)) {
            throw new ValidationException(
                    String.format("File type '%s' is not allowed. Only PDF files are accepted.", 
                            fileExtension));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new ValidationException("File must be a PDF document");
        }
    }

    /**
     * Extracts the file extension from a filename.
     *
     * @param filename the filename
     * @return the file extension in lowercase
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Checks if a file extension is allowed.
     *
     * @param extension the file extension to check
     * @return true if allowed, false otherwise
     */
    private boolean isAllowedExtension(String extension) {
        for (String allowedExt : minioProperties.getAllowedExtensions()) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    // ========== Circuit Breaker Fallback Methods ==========

    /**
     * Fallback method for bucket initialization when circuit is open.
     * Logs the failure but allows the application to start without MinIO connectivity.
     *
     * @param ex the exception that triggered the fallback
     */
    private void initializeBucketFallback(Exception ex) {
        log.error("Circuit breaker OPEN: MinIO bucket initialization failed. " +
                "Application will continue but file operations may fail. Error: {}", ex.getMessage());
    }

    /**
     * Fallback method for file storage when circuit is open.
     * Prevents cascading failures by throwing a specific exception.
     *
     * @param file the file that was being stored
     * @param fileType the type of file
     * @param applicantId the applicant ID
     * @param ex the exception that triggered the fallback
     * @return never returns, always throws exception
     * @throws FileStorageException indicating the service is unavailable
     */
    private String storeFileFallback(MultipartFile file, String fileType, String applicantId, Exception ex) {
        log.error("Circuit breaker OPEN: Cannot store file '{}' for applicant '{}'. MinIO service unavailable. Error: {}",
                fileType, applicantId, ex.getMessage());
        throw new FileStorageException("File storage service is temporarily unavailable. Please try again later.", ex);
    }

    /**
     * Fallback method for file loading when circuit is open.
     * Prevents cascading failures by throwing a specific exception.
     *
     * @param objectKey the object key that was being loaded
     * @param ex the exception that triggered the fallback
     * @return never returns, always throws exception
     * @throws FileStorageException indicating the service is unavailable
     */
    private Resource loadFileAsResourceFallback(String objectKey, Exception ex) {
        log.error("Circuit breaker OPEN: Cannot load file '{}'. MinIO service unavailable. Error: {}",
                objectKey, ex.getMessage());
        throw new FileStorageException("File retrieval service is temporarily unavailable. Please try again later.", ex);
    }

    /**
     * Fallback method for presigned URL generation when circuit is open.
     * Prevents cascading failures by throwing a specific exception.
     *
     * @param objectKey the object key for which URL was being generated
     * @param expiryMinutes the expiry time
     * @param ex the exception that triggered the fallback
     * @return never returns, always throws exception
     * @throws FileStorageException indicating the service is unavailable
     */
    private String generatePresignedUrlFallback(String objectKey, int expiryMinutes, Exception ex) {
        log.error("Circuit breaker OPEN: Cannot generate presigned URL for '{}'. MinIO service unavailable. Error: {}",
                objectKey, ex.getMessage());
        throw new FileStorageException("URL generation service is temporarily unavailable. Please try again later.", ex);
    }

    /**
     * Fallback method for file deletion when circuit is open.
     * Logs the failure but does not throw exception to allow graceful degradation.
     *
     * @param objectKey the object key that was being deleted
     * @param ex the exception that triggered the fallback
     */
    private void deleteFileFallback(String objectKey, Exception ex) {
        log.error("Circuit breaker OPEN: Cannot delete file '{}'. MinIO service unavailable. " +
                "File deletion will be skipped. Error: {}", objectKey, ex.getMessage());
        // Don't throw exception - allow the application deletion to proceed even if file cleanup fails
    }
}
