package com.recrutech.recrutechauth.service;

import com.recrutech.recrutechauth.exception.TokenException;
import com.recrutech.recrutechauth.model.RefreshToken;
import com.recrutech.recrutechauth.model.User;
import com.recrutech.recrutechauth.repository.RefreshTokenRepository;
import com.recrutech.recrutechauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for JWT token generation and validation.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final String jwtId;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshExpiration;
    
    @Value("${jwt.audience:recrutech-api}")
    private String audience;

    /**
     * Constructor for JwtService.
     *
     * @param encoder the JWT encoder
     * @param decoder the JWT decoder
     * @param refreshTokenRepository the refresh token repository
     * @param userRepository the user repository
     * @param jwtId the JWT ID for token identification
     */
    public JwtService(
            JwtEncoder encoder, 
            JwtDecoder decoder, 
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            String jwtId) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtId = jwtId;
    }

    /**
     * Generates a JWT token for the given authentication.
     *
     * @param authentication the authentication object
     * @return the JWT token
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        // Get user ID from database
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("recrutech-auth")
                .issuedAt(now)
                .expiresAt(now.plus(jwtExpiration, ChronoUnit.MILLIS))
                .subject(user.getId()) // Use user ID instead of username
                .audience(java.util.List.of(audience))
                .claim("scope", scope)
                .claim("username", authentication.getName()) // Keep username as separate claim
                .id(UUID.randomUUID().toString())
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Generates a refresh token for the given authentication and stores it in the database.
     *
     * @param authentication the authentication object
     * @return the refresh token
     */
    @Transactional
    public String generateRefreshToken(Authentication authentication) {
        Instant now = Instant.now();
        String tokenId = UUID.randomUUID().toString();

        // Get user ID from database
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("recrutech-auth")
                .issuedAt(now)
                .expiresAt(now.plus(refreshExpiration, ChronoUnit.MILLIS))
                .subject(user.getId()) // Use user ID instead of username
                .audience(java.util.List.of(audience))
                .claim("token_type", "refresh")
                .claim("username", authentication.getName()) // Keep username as separate claim
                .id(tokenId)
                .build();

        String tokenValue = this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        
        // Store refresh token in database for revocation capability
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(now.plus(refreshExpiration, ChronoUnit.MILLIS));
        refreshToken.setTokenId(tokenId);
        refreshToken.setRevoked(false);
        
        refreshTokenRepository.save(refreshToken);
        
        return tokenValue;
    }
    
    /**
     * Validates a JWT token and returns the Jwt object.
     *
     * @param token the JWT token to validate
     * @return the validated Jwt object
     * @throws TokenException if the token is invalid
     */
    public Jwt validateToken(String token) {
        try {
            return decoder.decode(token);
        } catch (JwtException e) {
            throw new TokenException("Invalid token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts the username from a JWT token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        Jwt jwt = validateToken(token);
        // Extract from username claim instead of subject (subject now contains user ID)
        return jwt.getClaimAsString("username");
    }
    
    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public String extractUserId(String token) {
        Jwt jwt = validateToken(token);
        return jwt.getSubject(); // Now returns user ID
    }
    
    /**
     * Checks if a JWT token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        Jwt jwt = validateToken(token);
        return jwt.getExpiresAt().isBefore(Instant.now());
    }
    
    /**
     * Checks if a JWT token is a refresh token.
     *
     * @param token the JWT token
     * @return true if the token is a refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        Jwt jwt = validateToken(token);
        Map<String, Object> claims = jwt.getClaims();
        return "refresh".equals(claims.get("token_type"));
    }
    
    /**
     * Checks if a refresh token is revoked.
     *
     * @param token the refresh token
     * @return true if the token is revoked, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isRefreshTokenRevoked(String token) {
        Jwt jwt = validateToken(token);
        String tokenId = jwt.getId();
        
        return refreshTokenRepository.findByTokenId(tokenId)
                .map(RefreshToken::isRevoked)
                .orElse(true); // If token not found in database, consider it revoked
    }
    
    /**
     * Revokes a refresh token.
     *
     * @param token the refresh token
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        Jwt jwt = validateToken(token);
        String tokenId = jwt.getId();
        
        refreshTokenRepository.findByTokenId(tokenId)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }
    
    /**
     * Generates a new access token and refresh token from a refresh token.
     * The old refresh token is revoked.
     *
     * @param refreshToken the refresh token
     * @return the new access token
     * @throws TokenException if the refresh token is invalid
     */
    @Transactional
    public String generateTokenFromRefreshToken(String refreshToken) {
        try {
            // Validate the refresh token
            Jwt jwt = validateToken(refreshToken);
            
            // Check if it's a refresh token
            if (!isRefreshToken(refreshToken)) {
                throw new TokenException("Not a refresh token");
            }
            
            // Check if it's expired
            if (isTokenExpired(refreshToken)) {
                throw new TokenException("Refresh token expired");
            }
            
            // Check if it's revoked
            if (isRefreshTokenRevoked(refreshToken)) {
                throw new TokenException("Refresh token has been revoked");
            }
            
            // Extract username and get user details
            String username = jwt.getSubject();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new TokenException("User not found for token"));
            
            // Get user authorities
            String scope = user.getRoles().stream()
                    .map(role -> "ROLE_" + role.getName())
                    .collect(Collectors.joining(" "));
            
            // Generate new access token
            Instant now = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("recrutech-auth")
                    .issuedAt(now)
                    .expiresAt(now.plus(jwtExpiration, ChronoUnit.MILLIS))
                    .subject(username)
                    .audience(java.util.List.of(audience))
                    .claim("scope", scope)
                    .id(UUID.randomUUID().toString())
                    .build();
            
            // Revoke the old refresh token
            revokeRefreshToken(refreshToken);
            
            return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenException("Error generating token from refresh token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generates a new refresh token from an old refresh token.
     * The old refresh token is revoked.
     *
     * @param oldRefreshToken the old refresh token
     * @return the new refresh token
     * @throws TokenException if the refresh token is invalid
     */
    @Transactional
    public String rotateRefreshToken(String oldRefreshToken) {
        try {
            // Validate the refresh token
            Jwt jwt = validateToken(oldRefreshToken);
            
            // Check if it's a refresh token
            if (!isRefreshToken(oldRefreshToken)) {
                throw new TokenException("Not a refresh token");
            }
            
            // Check if it's expired
            if (isTokenExpired(oldRefreshToken)) {
                throw new TokenException("Refresh token expired");
            }
            
            // Check if it's revoked
            if (isRefreshTokenRevoked(oldRefreshToken)) {
                throw new TokenException("Refresh token has been revoked");
            }
            
            // Extract username and get user details
            String username = jwt.getSubject();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new TokenException("User not found for token"));
            
            // Generate new refresh token
            Instant now = Instant.now();
            String tokenId = UUID.randomUUID().toString();
            
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("recrutech-auth")
                    .issuedAt(now)
                    .expiresAt(now.plus(refreshExpiration, ChronoUnit.MILLIS))
                    .subject(username)
                    .audience(java.util.List.of(audience))
                    .claim("token_type", "refresh")
                    .id(tokenId)
                    .build();
            
            String tokenValue = this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
            
            // Store new refresh token in database
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(tokenValue);
            refreshToken.setUser(user);
            refreshToken.setExpiryDate(now.plus(refreshExpiration, ChronoUnit.MILLIS));
            refreshToken.setTokenId(tokenId);
            refreshToken.setRevoked(false);
            
            refreshTokenRepository.save(refreshToken);
            
            // Revoke the old refresh token
            revokeRefreshToken(oldRefreshToken);
            
            return tokenValue;
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenException("Error rotating refresh token: " + e.getMessage(), e);
        }
    }
}