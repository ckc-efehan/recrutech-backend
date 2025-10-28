package com.recrutech.recrutechplatform.domain.applicant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Applicant entity operations.
 * Provides database access methods for applicant domain data.
 */
@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, String> {

    /**
     * Find applicant by account ID.
     * @param accountId the account ID from auth service
     * @return Optional containing the applicant if found
     */
    Optional<Applicant> findByAccountId(String accountId);

    /**
     * Check if an applicant exists for the given account ID.
     * @param accountId the account ID from auth service
     * @return true if applicant exists, false otherwise
     */
    boolean existsByAccountId(String accountId);

    /**
     * Delete applicant by account ID.
     * @param accountId the account ID from auth service
     */
    void deleteByAccountId(String accountId);
}
