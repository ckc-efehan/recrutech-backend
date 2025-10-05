package com.recrutech.recrutechplatform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for MinIO object storage.
 * Maps to application.properties with prefix "minio".
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO server URL (e.g., http://localhost:9000).
     */
    private String url;

    /**
     * MinIO access key (username).
     */
    private String accessKey;

    /**
     * MinIO secret key (password).
     */
    private String secretKey;

    /**
     * Name of the bucket to store application documents.
     */
    private String bucketName;

    /**
     * Whether to automatically create the bucket if it doesn't exist.
     */
    private boolean autoCreateBucket = true;

    /**
     * Maximum file size in bytes (default 5MB).
     */
    private long maxFileSize = 5242880; // 5MB

    /**
     * Allowed file extensions for uploads.
     */
    private String[] allowedExtensions = {"pdf"};

}
