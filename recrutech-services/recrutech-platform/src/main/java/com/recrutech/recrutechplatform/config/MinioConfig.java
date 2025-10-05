package com.recrutech.recrutechplatform.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MinIO client.
 * Creates MinioClient bean with lazy bucket initialization.
 * 
 * Best Practice: The MinIO client is created without attempting connection during
 * Spring context initialization. Bucket creation is handled lazily by the 
 * MinioStorageService on first use, preventing startup issues with MinIO availability.
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private final MinioProperties minioProperties;

    public MinioConfig(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    /**
     * Creates MinIO client bean.
     * 
     * Note: This method only creates the client configuration without attempting
     * to connect to MinIO. Actual connection and bucket initialization happens
     * lazily when the MinioStorageService is first used.
     *
     * @return configured MinioClient instance
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
    
    /**
     * Exposes MinioProperties as a bean for services that need bucket configuration.
     * 
     * @return MinIO configuration properties
     */
    @Bean
    public MinioProperties minioProperties() {
        return minioProperties;
    }
}
