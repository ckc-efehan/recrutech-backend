package com.recrutech.recrutechplatform;

import com.recrutech.recrutechplatform.config.MinioTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for the Recrutech Platform application.
 * This test verifies that the Spring application context can load successfully
 * with all beans, including MinIO via Testcontainers.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MinioTestConfiguration.class)
class RecrutechPlatformApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the application context loads successfully
        // with all beans including MinIO container
    }

}
