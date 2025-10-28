package com.recrutech.recrutechauth.repository;

import com.recrutech.recrutechauth.model.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RefreshToken entity operations in Redis.
 * Provides methods for token management, rotation, and family-based invalidation.
 */
@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    /**
     * Find all refresh tokens for a specific user.
     * Used for user logout (invalidate all tokens) or security audits.
     * @param userId the user ID
     * @return List of refresh tokens for the user
     */
    List<RefreshToken> findByUserId(String userId);

    /**
     * Find all refresh tokens in a specific family.
     * Used for invalidating entire token family when reuse is detected.
     * @param familyId the family ID
     * @return List of refresh tokens in the family
     */
    List<RefreshToken> findByFamilyId(String familyId);

    /**
     * Find refresh token by token value.
     * This is the primary lookup method for token validation.
     * @param token the token value
     * @return Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all tokens for a user that have been revoked.
     * Used for security monitoring and audit trails.
     * @param userId the user ID
     * @param revoked the revoked status
     * @return List of tokens matching the criteria
     */
    List<RefreshToken> findByUserIdAndRevoked(String userId, boolean revoked);

    /**
     * Delete all refresh tokens for a specific user.
     * Used during logout or account deletion.
     * @param userId the user ID
     */
    void deleteByUserId(String userId);

    /**
     * Delete all refresh tokens in a specific family.
     * Used when token reuse is detected to invalidate entire chain.
     * @param familyId the family ID
     */
    void deleteByFamilyId(String familyId);

    /**
     * Count active (non-revoked) tokens for a user.
     * Used for monitoring and limiting concurrent sessions.
     * @param userId the user ID
     * @param revoked the revoked status
     * @return count of active tokens
     */
    long countByUserIdAndRevoked(String userId, boolean revoked);
}
