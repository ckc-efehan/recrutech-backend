package com.recrutech.recrutechauth.repository;

import com.recrutech.recrutechauth.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Company entity operations.
 * Provides methods for company management and verification.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    /**
     * Find company by admin user ID.
     * @param adminUserId the admin user ID
     * @return Optional containing the company if found
     */
    Optional<Company> findByAdminUserId(String adminUserId);


    /**
     * Check if business email exists in the system.
     * @param businessEmail the business email
     * @return true if email exists, false otherwise
     */
    boolean existsByBusinessEmail(String businessEmail);

    /**
     * Check if telephone number exists in the system.
     * @param telephone the telephone number
     * @return true if telephone exists, false otherwise
     */
    boolean existsByTelephone(String telephone);

}