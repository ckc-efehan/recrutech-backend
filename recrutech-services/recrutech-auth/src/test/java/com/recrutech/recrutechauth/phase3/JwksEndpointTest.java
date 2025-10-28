package com.recrutech.recrutechauth.phase3;

import com.recrutech.recrutechauth.security.KeyRotationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 3 JWKS Endpoint Tests.
 * Validates JWKS endpoint exposure for offline JWT validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwksEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeyRotationService keyRotationService;

    @Test
    @DisplayName("Phase 3.12: JWKS endpoint should be accessible")
    void testJwksEndpointAccessible() throws Exception {
        MvcResult result = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("[DEBUG_LOG] JWKS Response: " + response);
    }

    @Test
    @DisplayName("Phase 3.13: JWKS endpoint should return proper key metadata")
    void testJwksEndpointReturnsKeyMetadata() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("oct"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value("HS256"))
                .andExpect(jsonPath("$.keys[0].kid").exists())
                .andExpect(jsonPath("$.metadata.issuer").value("recrutech"))
                .andExpect(jsonPath("$.metadata.key_rotation_interval_hours").exists())
                .andExpect(jsonPath("$.metadata.key_overlap_hours").exists())
                .andExpect(jsonPath("$.metadata.valid_keys_count").exists());

        System.out.println("[DEBUG_LOG] JWKS endpoint returns proper key metadata");
    }

    @Test
    @DisplayName("Phase 3.14: JWKS endpoint should have proper cache headers")
    void testJwksEndpointCacheHeaders() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().string("Cache-Control", containsString("must-revalidate")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        System.out.println("[DEBUG_LOG] JWKS endpoint has proper cache headers (1 hour)");
    }

    @Test
    @DisplayName("Phase 3.15: OpenID Configuration endpoint should be accessible")
    void testOpenIdConfigurationEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.issuer").value("recrutech"))
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.refresh_endpoint").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("[DEBUG_LOG] OpenID Configuration Response: " + response);
    }

    @Test
    @DisplayName("Phase 3.16: OpenID Configuration should list supported claims")
    void testOpenIdConfigurationListsSupportedClaims() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claims_supported").isArray())
                .andExpect(jsonPath("$.claims_supported", hasItem("sub")))
                .andExpect(jsonPath("$.claims_supported", hasItem("iss")))
                .andExpect(jsonPath("$.claims_supported", hasItem("aud")))
                .andExpect(jsonPath("$.claims_supported", hasItem("exp")))
                .andExpect(jsonPath("$.claims_supported", hasItem("iat")))
                .andExpect(jsonPath("$.claims_supported", hasItem("nbf")))
                .andExpect(jsonPath("$.claims_supported", hasItem("jti")))
                .andExpect(jsonPath("$.claims_supported", hasItem("roles")))
                .andExpect(jsonPath("$.claims_supported", hasItem("scope")))
                .andExpect(jsonPath("$.claims_supported", hasItem("tenant")));

        System.out.println("[DEBUG_LOG] OpenID Configuration lists all standard JWT claims");
    }

    @Test
    @DisplayName("Phase 3.17: OpenID Configuration should list supported scopes")
    void testOpenIdConfigurationListsSupportedScopes() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopes_supported").isArray())
                .andExpect(jsonPath("$.scopes_supported", hasItem("openid")))
                .andExpect(jsonPath("$.scopes_supported", hasItem("profile")))
                .andExpect(jsonPath("$.scopes_supported", hasItem("email")))
                .andExpect(jsonPath("$.scopes_supported", hasItem("company:read")))
                .andExpect(jsonPath("$.scopes_supported", hasItem("company:write")))
                .andExpect(jsonPath("$.scopes_supported", hasItem("application:read")))
                .andExpect(jsonPath("$.scopes_supported", hasItem("jobposting:read")))
                .andExpect(jsonPath("$.scopes_supported", hasItem("interview:read")));

        System.out.println("[DEBUG_LOG] OpenID Configuration lists all supported scopes");
    }

    @Test
    @DisplayName("Phase 3.18: OpenID Configuration should indicate token rotation features")
    void testOpenIdConfigurationTokenRotationFeatures() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_rotation_enabled").value(true))
                .andExpect(jsonPath("$.refresh_token_rotation_enabled").value(true))
                .andExpect(jsonPath("$.refresh_token_reuse_detection_enabled").value(true));

        System.out.println("[DEBUG_LOG] OpenID Configuration indicates token rotation and reuse detection enabled");
    }

    @Test
    @DisplayName("Phase 3.19: JWKS health endpoint should be accessible")
    void testJwksHealthEndpoint() throws Exception {
        mockMvc.perform(get("/.well-known/jwks/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("jwks"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.key_rotation_enabled").exists())
                .andExpect(jsonPath("$.valid_keys_count").exists());

        System.out.println("[DEBUG_LOG] JWKS health endpoint is UP");
    }

    @Test
    @DisplayName("Phase 3.20: OpenID Configuration should have proper cache headers")
    void testOpenIdConfigurationCacheHeaders() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", containsString("max-age=86400"))); // 24 hours

        System.out.println("[DEBUG_LOG] OpenID Configuration has proper cache headers (24 hours)");
    }
}
