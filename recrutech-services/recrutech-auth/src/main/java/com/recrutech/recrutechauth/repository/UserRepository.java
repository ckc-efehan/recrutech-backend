package com.recrutech.recrutechauth.repository;

import com.recrutech.recrutechauth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides methods for authentication and user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email address.
     * @param email the email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find users with expired tokens that need cleanup.
     * @param now current timestamp
     * @return list of users with expired tokens
     */
    @Query("SELECT u FROM User u WHERE u.tokenExpiresAt < :now AND u.currentAccessToken IS NOT NULL")
    List<User> findUsersWithExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Find users who need new tokens (no tokens or expired tokens).
     * @param now current timestamp
     * @return list of users who need new tokens
     */
    @Query("SELECT u FROM User u WHERE u.currentAccessToken IS NULL OR u.currentRefreshToken IS NULL OR u.tokenExpiresAt < :now")
    List<User> findUsersNeedingNewTokens(@Param("now") LocalDateTime now);

    /**
     * Check if email exists in the system.
     * @param email the email address
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find user by password reset token.
     * @param passwordResetToken the password reset token
     * @return Optional containing the user if found
     */
    Optional<User> findByPasswordResetToken(String passwordResetToken);

}