package com.recrutech.recrutechauth.config;

import com.recrutech.recrutechauth.security.InputSanitizationFilter;
import com.recrutech.recrutechauth.security.SecurityHeadersFilter;
import com.recrutech.recrutechauth.security.RateLimitingFilter;
import com.recrutech.recrutechauth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for the authentication service.
 * Provides password encoding and basic security setup.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final InputSanitizationFilter inputSanitizationFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(InputSanitizationFilter inputSanitizationFilter,
                         SecurityHeadersFilter securityHeadersFilter,
                         RateLimitingFilter rateLimitingFilter,
                         JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.inputSanitizationFilter = inputSanitizationFilter;
        this.securityHeadersFilter = securityHeadersFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Password encoder bean with high security strength.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // High security strength
    }

    /**
     * CORS configuration with restricted origins for enhanced security.
     * Replaces the permissive "*" configuration in controllers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Restrict allowed origins - replace with your actual frontend domains
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://recrutech.com",
            "https://*.recrutech.com",
            "https://app.recrutech.com",
            "http://localhost:3000", // For development
            "http://localhost:5173", // For development (Vite)
            "http://localhost:8080"  // For development
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-CSRF-Token",
            "X-Custom-Header"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset",
            "X-Request-ID"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Enhanced security filter chain configuration with comprehensive security measures.
     * Includes input sanitization, security headers, rate limiting, and session protection.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CSRF protection - disabled for stateless JWT authentication
            .csrf(AbstractHttpConfigurer::disable)
            
            // Session management - stateless for JWT
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                       .sessionFixation().none() // Additional session fixation protection
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public API endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register/**", "/api/auth/refresh", "/api/auth/health", "/api/auth/verify-email", "/api/auth/forgot-password").permitAll()
                .requestMatchers("/api/auth/reset-password", "/api/auth/reset-password-confirm").permitAll()
                .requestMatchers("/api/gdpr/info", "/api/gdpr/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Swagger UI and OpenAPI documentation
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Static resources
                .requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .anyRequest().authenticated())
            
            // Enhanced security headers (additional to our custom filter)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(content -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true))
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            
            // Add custom security filters in the correct order
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(inputSanitizationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(securityHeadersFilter, inputSanitizationFilter.getClass())
            .addFilterAfter(rateLimitingFilter, securityHeadersFilter.getClass());

        return http.build();
    }
}