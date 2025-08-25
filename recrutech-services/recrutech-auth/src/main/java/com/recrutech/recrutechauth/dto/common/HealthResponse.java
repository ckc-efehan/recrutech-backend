package com.recrutech.recrutechauth.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * Enterprise-grade health check response record.
 * Features:
 * - Immutable design for thread safety
 * - System health monitoring
 * - Component health tracking
 * - Version and uptime information
 * - Factory methods for common responses
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
    
    @JsonProperty("status")
    @NotNull
    HealthStatus status,
    
    @JsonProperty("message")
    @NotNull
    String message,
    
    @JsonProperty("timestamp")
    Long timestamp,
    
    @JsonProperty("version")
    String version,
    
    @JsonProperty("uptime")
    String uptime,
    
    @JsonProperty("checks")
    Map<String, ComponentHealth> checks
) {
    
    /**
     * Default constructor with default values
     */
    public HealthResponse {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Health status enumeration
     */
    public enum HealthStatus {
        UP,
        DOWN,
        DEGRADED,
        MAINTENANCE
    }
    
    /**
     * Component health information record
     */
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ComponentHealth(
        @JsonProperty("status")
        HealthStatus status,
        
        @JsonProperty("responseTime")
        Long responseTime,
        
        @JsonProperty("details")
        String details
    ) {}
    
    /**
     * Creates a healthy system response
     */
    public static HealthResponse createHealthyResponse() {
        return HealthResponse.builder()
            .status(HealthStatus.UP)
            .message("Authentication service is running normally")
            .version("2.0.0")
            .uptime("Available")
            .checks(Map.of(
                "database", ComponentHealth.builder()
                    .status(HealthStatus.UP)
                    .responseTime(50L)
                    .details("Connection pool healthy")
                    .build(),
                "redis", ComponentHealth.builder()
                    .status(HealthStatus.UP)
                    .responseTime(10L)
                    .details("Cache operational")
                    .build()
            ))
            .build();
    }
    
}