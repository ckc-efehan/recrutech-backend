package com.recrutech.recrutechauth.repository;

import com.recrutech.recrutechauth.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for Applicant entity operations.
 * Provides methods for applicant management and profile tracking.
 */
@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, String> {

    /**
     * Find applicant by user ID.
     * @param userId the user ID
     * @return Optional containing the applicant if found
     */
    Optional<Applicant> findByUserId(String userId);
}