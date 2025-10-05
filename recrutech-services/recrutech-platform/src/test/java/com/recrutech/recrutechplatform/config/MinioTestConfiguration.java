package com.recrutech.recrutechplatform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base test configuration for MinIO integration tests.
 * Provides a MinIO Testcontainer that runs during tests.
 * 
 * Usage: Annotate your test class with @Import(MinioTestConfiguration.class)
 * or extend a base test class that includes this configuration.
 * 
 * The MinIO container will be automatically started before tests and stopped after.
 * Connection properties are dynamically injected into the Spring test context.
 */
@TestConfiguration
@Testcontainers
public class MinioTestConfiguration {

    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2024-01-16T16-07-38Z";
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final int MINIO_PORT = 9000;

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
     * This overrides the properties in application-test.properties with the
     * actual container connection details.
     */
    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.url", () -> 
            String.format("http://%s:%d", 
                minioContainer.getHost(), 
                minioContainer.getMappedPort(MINIO_PORT)));
        registry.add("minio.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("minio.secret-key", () -> MINIO_SECRET_KEY);
        registry.add("minio.bucket-name", () -> "test-bucket");
        registry.add("minio.auto-create-bucket", () -> "true");
    }
}
