package com.recrutech.recrutechplatform.domain.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Company entity operations.
 * Provides database access methods for company domain data.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    /**
     * Find company by account ID.
     * @param accountId the account ID from auth service
     * @return Optional containing the company if found
     */
    Optional<Company> findByAccountId(String accountId);

    /**
     * Find company by admin account ID.
     * @param adminAccountId the admin account ID from auth service
     * @return Optional containing the company if found
     */
    Optional<Company> findByAdminAccountId(String adminAccountId);

    /**
     * Find company by business email.
     * @param businessEmail the unique business email
     * @return Optional containing the company if found
     */
    Optional<Company> findByBusinessEmail(String businessEmail);

    /**
     * Find company by telephone.
     * @param telephone the unique telephone number
     * @return Optional containing the company if found
     */
    Optional<Company> findByTelephone(String telephone);

    /**
     * Check if a company exists for the given admin account ID.
     * @param adminAccountId the admin account ID from auth service
     * @return true if company exists, false otherwise
     */
    boolean existsByAdminAccountId(String adminAccountId);

    /**
     * Check if a company exists with the given business email.
     * @param businessEmail the business email to check
     * @return true if company exists, false otherwise
     */
    boolean existsByBusinessEmail(String businessEmail);

    /**
     * Check if a company exists with the given telephone.
     * @param telephone the telephone to check
     * @return true if company exists, false otherwise
     */
    boolean existsByTelephone(String telephone);
}
