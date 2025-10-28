package com.recrutech.recrutechauth.controller;

import com.recrutech.recrutechauth.security.KeyRotationService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * JWKS (JSON Web Key Set) Controller.
 * Exposes public key information for offline JWT validation.
 * 
 * This endpoint allows resource servers and other services to validate JWTs
 * without making a call back to the authentication service, enabling true
 * offline validation as required by Phase 3.
 * 
 * Note: For HMAC-based signatures (HS256), the actual secret cannot be exposed.
 * This endpoint provides key metadata and rotation information. For true public
 * key cryptography, consider migrating to RS256 (RSA) or ES256 (ECDSA) in the future.
 */
@RestController
@RequestMapping("/.well-known")
public class JwksController {

    private final KeyRotationService keyRotationService;

    public JwksController(KeyRotationService keyRotationService) {
        this.keyRotationService = keyRotationService;
    }

    /**
     * JWKS endpoint exposing key metadata.
     * Returns key information following the JWK specification (RFC 7517).
     * 
     * @return JWKS response with key metadata
     */
    @GetMapping("/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks() {
        Map<String, Object> jwks = new HashMap<>();
        List<Map<String, Object>> keys = new ArrayList<>();

        // Get rotation status to include metadata
        Map<String, Object> rotationStatus = keyRotationService.getRotationStatus();
        
        // Create JWK entry for current key
        Map<String, Object> currentKey = new HashMap<>();
        currentKey.put("kty", "oct"); // Key Type: Octet sequence (symmetric key)
        currentKey.put("use", "sig"); // Public Key Use: signature
        currentKey.put("alg", "HS256"); // Algorithm: HMAC SHA-256
        currentKey.put("kid", rotationStatus.get("currentKey")); // Key ID from rotation service
        
        // Add key rotation metadata
        currentKey.put("rotation_enabled", rotationStatus.get("enabled"));
        currentKey.put("next_rotation", rotationStatus.get("nextRotation"));
        
        keys.add(currentKey);
        
        jwks.put("keys", keys);
        
        // Add metadata about the key set
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("issuer", "recrutech");
        metadata.put("key_rotation_interval_hours", rotationStatus.get("intervalHours"));
        metadata.put("key_overlap_hours", rotationStatus.get("overlapHours"));
        metadata.put("valid_keys_count", rotationStatus.get("validKeysCount"));
        metadata.put("note", "HMAC keys are symmetric and cannot be publicly exposed. " +
                "This endpoint provides key metadata for validation coordination. " +
                "For true offline validation, consider migrating to RS256/ES256.");
        
        jwks.put("metadata", metadata);
        
        // Set cache headers (cache for 1 hour, must revalidate)
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)
                        .mustRevalidate()
                        .cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(jwks);
    }

    /**
     * OpenID Connect Discovery endpoint.
     * Provides metadata about the OAuth 2.0 / OIDC authorization server.
     * 
     * @return OpenID Configuration
     */
    @GetMapping("/openid-configuration")
    public ResponseEntity<Map<String, Object>> getOpenIdConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Issuer
        config.put("issuer", "recrutech");
        
        // Endpoints
        config.put("authorization_endpoint", "http://localhost:8081/api/auth/authorize");
        config.put("token_endpoint", "http://localhost:8081/api/auth/token");
        config.put("jwks_uri", "http://localhost:8081/.well-known/jwks.json");
        config.put("refresh_endpoint", "http://localhost:8081/api/auth/refresh");
        
        // Supported features
        config.put("response_types_supported", Arrays.asList("code", "token"));
        config.put("subject_types_supported", Collections.singletonList("public"));
        config.put("id_token_signing_alg_values_supported", Collections.singletonList("HS256"));
        config.put("scopes_supported", Arrays.asList(
                "openid", "profile", "email",
                "company:read", "company:write", "company:manage",
                "hr:read", "hr:write", "hr:manage",
                "application:read", "application:write", "application:manage",
                "jobposting:read", "jobposting:write", "jobposting:manage",
                "interview:read", "interview:write", "interview:manage",
                "profile:read", "profile:write"
        ));
        config.put("token_endpoint_auth_methods_supported", Arrays.asList(
                "client_secret_basic", "client_secret_post"
        ));
        config.put("claims_supported", Arrays.asList(
                "sub", "iss", "aud", "exp", "iat", "nbf", "jti",
                "roles", "scope", "tenant", "email", "emailVerified", "mfaVerified"
        ));
        
        // Token rotation features
        config.put("token_rotation_enabled", true);
        config.put("refresh_token_rotation_enabled", true);
        config.put("refresh_token_reuse_detection_enabled", true);
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                .body(config);
    }

    /**
     * Health check endpoint for JWKS service.
     * 
     * @return Health status
     */
    @GetMapping("/jwks/health")
    public ResponseEntity<Map<String, Object>> getJwksHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "jwks");
        health.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> rotationStatus = keyRotationService.getRotationStatus();
        health.put("key_rotation_enabled", rotationStatus.get("enabled"));
        health.put("valid_keys_count", rotationStatus.get("validKeysCount"));
        
        return ResponseEntity.ok(health);
    }
}
