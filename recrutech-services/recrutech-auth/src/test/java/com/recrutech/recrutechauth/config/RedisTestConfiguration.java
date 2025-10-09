package com.recrutech.recrutechauth.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for Redis-dependent beans.
 * Provides a mock RedisTemplate to satisfy dependency injection in tests.
 * 
 * This configuration ensures that tests can load the ApplicationContext
 * without requiring a real Redis instance.
 * 
 * Usage: Annotate your test class with @Import(RedisTestConfiguration.class)
 */
@TestConfiguration
public class RedisTestConfiguration {

    /**
     * Provides a mock RedisTemplate bean for tests.
     * This overrides any Redis auto-configuration with @Primary.
     *
     * @return mocked RedisTemplate for testing
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return mock(RedisTemplate.class);
    }
}
