package com.recrutech.recrutechplatform.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Resilience4j Circuit Breaker.
 * This configuration provides:
 * - Event logging for circuit breaker state transitions
 * - Monitoring of circuit breaker events for observability
 * - Clean separation of cross-cutting concerns (fault tolerance)
 * Best practices implemented:
 * - Centralized configuration
 * - Comprehensive logging for debugging and monitoring
 * - Event-driven architecture for circuit breaker state changes
 * - Automatic registration of event listeners for all circuit breakers
 */
@Configuration
public class Resilience4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfig.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public Resilience4jConfig(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Initializes event listeners for all circuit breakers in the registry.
     * This method runs after bean construction and sets up comprehensive logging
     * for circuit breaker state changes, failures, and slow calls.
     */
    @PostConstruct
    public void registerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerEventListener);
        
        // Register listener for newly added circuit breakers
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> {
                    log.info("Circuit breaker '{}' has been registered", event.getAddedEntry().getName());
                    registerEventListener(event.getAddedEntry());
                });
        
        log.info("Circuit breaker event listeners initialized successfully");
    }

    /**
     * Registers comprehensive event listeners for a single circuit breaker.
     * Logs all important events including state transitions, failures, and slow calls.
     *
     * @param circuitBreaker the circuit breaker to monitor
     */
    private void registerEventListener(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> 
                    log.debug("Circuit breaker '{}' - Call succeeded", circuitBreaker.getName()))
                .onError(event -> 
                    log.warn("Circuit breaker '{}' - Call failed: {}", 
                            circuitBreaker.getName(), event.getThrowable().getMessage()))
                .onStateTransition(event -> 
                    log.warn("Circuit breaker '{}' changed state from {} to {}", 
                            circuitBreaker.getName(), 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()))
                .onSlowCallRateExceeded(event -> 
                    log.warn("Circuit breaker '{}' - Slow call rate exceeded: {}%", 
                            circuitBreaker.getName(), event.getSlowCallRate()))
                .onFailureRateExceeded(event -> 
                    log.error("Circuit breaker '{}' - Failure rate exceeded: {}%", 
                            circuitBreaker.getName(), event.getFailureRate()))
                .onCallNotPermitted(event -> 
                    log.error("Circuit breaker '{}' - Call not permitted (circuit is OPEN)", 
                            circuitBreaker.getName()))
                .onReset(event -> 
                    log.info("Circuit breaker '{}' has been reset", circuitBreaker.getName()));
    }
}
