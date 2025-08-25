package com.recrutech.recrutechauth.model;

import com.recrutech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Entity representing a user in the system with comprehensive security features.
 * This class extends BaseEntity to inherit common fields like ID and creation timestamp.
 * Supports automatic token generation and advanced security tracking.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column
    private String emailVerificationToken;

    @Column
    private LocalDateTime emailVerificationExpiry;

    // Security fields for account protection
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column
    private LocalDateTime lastFailedLogin;

    @Column
    private LocalDateTime accountLockedUntil;

    @Column
    private String passwordResetToken;

    @Column
    private LocalDateTime passwordResetExpiry;

    @Column
    private LocalDateTime lastPasswordChange;

    // Two-factor authentication fields
    @Column
    private String twoFactorSecret;

    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    @Column
    private String backupCodes; // JSON Array as String

    // Session management for automatic token generation
    @Column
    private String currentSessionId;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private String lastLoginIp;

    // Token management fields
    @Column
    private String currentAccessToken;

    @Column
    private String currentRefreshToken;

    @Column
    private LocalDateTime tokenExpiresAt;


    /**
     * Initializes the entity by ensuring it has an ID and creation timestamp.
     * This method should be called before persisting the entity.
     */
    @PrePersist
    public void prePersist() {
        initializeEntity();
        if (lastPasswordChange == null) {
            lastPasswordChange = LocalDateTime.now();
        }
    }

    /**
     * Updates the entity before it's updated in the database.
     * This method is called automatically by JPA before an update operation.
     */
    @PreUpdate
    public void preUpdate() {
        // Update logic if needed
    }

    // Utility methods for security checks
    
    /**
     * Checks if the account is currently locked due to failed login attempts.
     * @return true if account is locked, false otherwise
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Checks if the password has expired and needs to be changed.
     * @return true if password is expired, false otherwise
     */
    public boolean isPasswordExpired() {
        if (lastPasswordChange == null) return false;
        return lastPasswordChange.isBefore(LocalDateTime.now().minusDays(90)); // 90 days expiry
    }

    /**
     * Checks if the current access token is expired.
     * @return true if token is expired or null, false otherwise
     */
    public boolean isTokenExpired() {
        return tokenExpiresAt == null || tokenExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Checks if the user needs a new token (no token exists or current token is expired).
     * @return true if new token is needed, false otherwise
     */
    public boolean needsNewToken() {
        return currentAccessToken == null || currentRefreshToken == null || isTokenExpired();
    }

    /**
     * Updates token information for automatic token management.
     * @param accessToken the new access token
     * @param refreshToken the new refresh token
     * @param expiresAt when the token expires
     */
    public void updateTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.currentAccessToken = accessToken;
        this.currentRefreshToken = refreshToken;
        this.tokenExpiresAt = expiresAt;
    }

    /**
     * Clears all token information (used during logout).
     */
    public void clearTokens() {
        this.currentAccessToken = null;
        this.currentRefreshToken = null;
        this.tokenExpiresAt = null;
        this.currentSessionId = null;
    }

}