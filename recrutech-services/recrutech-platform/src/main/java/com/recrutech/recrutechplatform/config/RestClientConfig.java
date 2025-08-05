package com.recrutech.recrutechplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration class for REST clients and Redis caching.
 * This class provides beans for inter-service communication
 * and enables distributed Redis caching for performance optimization.
 */
@Configuration
@EnableCaching
public class RestClientConfig {

    @Value("${app.auth.service.url}")
    private String authServiceUrl;

    /**
     * Creates a RestTemplate bean configured for auth service communication.
     * Includes timeout configuration for resilience.
     *
     * @param builder the RestTemplateBuilder
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate authServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(authServiceUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}