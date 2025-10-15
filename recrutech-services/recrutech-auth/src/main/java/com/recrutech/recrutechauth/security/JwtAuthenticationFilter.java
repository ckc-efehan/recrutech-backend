package com.recrutech.recrutechauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter that validates Bearer tokens from the Authorization header
 * and populates the Spring SecurityContext for downstream authorization decisions.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // Reuse existing helper to get a stable client identifier (IP/User-Agent hash)
                String clientIp = RateLimitingFilter.getString(request);

                if (tokenProvider.isTokenValid(token, clientIp)) {
                    // At minimum we set an authenticated principal. Roles/authorities can be extracted from claims if needed.
                    String userId = safeGetUserId(token);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userId != null ? userId : "jwt-user",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            // Do not block the request on filter errors; just log and continue without authentication
            log.debug("JWT filter processing failed: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String safeGetUserId(String token) {
        try {
            return tokenProvider.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}
