package com.recrutech.recrutechplatform.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for MinIO integration tests.
 * Provides a MinIO Testcontainer and a properly configured MinioClient bean.
 * 
 * This configuration ensures:
 * - MinIO container starts before bean initialization
 * - Test MinioClient bean overrides production MinioConfig bean with @Primary
 * - Bucket is created safely after container is fully ready
 * 
 * Usage: Annotate your test class with @Import(MinioTestConfiguration.class)
 */
@TestConfiguration
@Testcontainers
public class MinioTestConfiguration {

    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2024-01-16T16-07-38Z";
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final int MINIO_PORT = 9000;
    private static final String TEST_BUCKET = "test-bucket";

    /**
     * MinIO container configured with default credentials.
     * The container is shared across all tests in the same JVM (singleton).
     */
    @Container
    public static final GenericContainer<?> minioContainer = new GenericContainer<>(
            DockerImageName.parse(MINIO_IMAGE))
            .withExposedPorts(MINIO_PORT)
            .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
            .withCommand("server", "/data");

    /**
     * Dynamically configures MinIO properties for the Spring test context.
     * This ensures container is started before properties are accessed.
     */
    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        // Ensure container is started before registering properties
        if (!minioContainer.isRunning()) {
            minioContainer.start();
        }
        
        registry.add("minio.url", () -> 
            String.format("http://%s:%d", 
                minioContainer.getHost(), 
                minioContainer.getMappedPort(MINIO_PORT)));
        registry.add("minio.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("minio.secret-key", () -> MINIO_SECRET_KEY);
        registry.add("minio.bucket-name", () -> TEST_BUCKET);
        registry.add("minio.auto-create-bucket", () -> "false"); // We handle bucket creation in the bean
    }

    /**
     * Provides a MinioClient bean for tests that connects to the Testcontainer.
     * This overrides the production MinioConfig bean with @Primary.
     * 
     * The bean ensures:
     * - Container is running before client creation
     * - Bucket is created after successful connection
     * - No race conditions with container startup
     *
     * @return configured MinioClient for testing
     */
    @Bean
    @Primary
    public MinioClient minioClient() {
        // Ensure container is running
        if (!minioContainer.isRunning()) {
            minioContainer.start();
        }

        String endpoint = String.format("http://%s:%d", 
            minioContainer.getHost(), 
            minioContainer.getMappedPort(MINIO_PORT));

        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
                .build();

        // Create test bucket
        try {
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(TEST_BUCKET).build());
            
            if (!exists) {
                client.makeBucket(
                    MakeBucketArgs.builder().bucket(TEST_BUCKET).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test MinIO bucket: " + TEST_BUCKET, e);
        }

        return client;
    }
}
