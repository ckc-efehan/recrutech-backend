package com.recrutech.recrutechplatform.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MinIO client.
 * Creates MinioClient bean and automatically creates bucket if needed.
 */
@Configuration
public class MinioConfig {

    private final MinioProperties minioProperties;

    public MinioConfig(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    /**
     * Creates and configures MinIO client bean.
     * Automatically creates the configured bucket if it doesn't exist.
     *
     * @return configured MinioClient instance
     */
    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

        // Auto-create bucket if enabled
        if (minioProperties.isAutoCreateBucket()) {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .build()
                );

                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(minioProperties.getBucketName())
                                    .build()
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create MinIO bucket: " + minioProperties.getBucketName(), e);
            }
        }

        return minioClient;
    }
}
