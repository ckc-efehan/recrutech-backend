package com.recrutech.recrutechplatform.domain.hremployee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for HREmployee entity operations.
 * Provides database access methods for HR employee domain data.
 */
@Repository
public interface HREmployeeRepository extends JpaRepository<HREmployee, String> {

    /**
     * Find HR employee by account ID.
     * @param accountId the account ID from auth service
     * @return Optional containing the HR employee if found
     */
    Optional<HREmployee> findByAccountId(String accountId);

    /**
     * Find all HR employees for a specific company.
     * @param companyId the company ID
     * @return List of HR employees for the company
     */
    List<HREmployee> findByCompanyId(String companyId);

    /**
     * Find all active HR employees for a specific company.
     * @param companyId the company ID
     * @param active the active status
     * @return List of active HR employees for the company
     */
    List<HREmployee> findByCompanyIdAndActive(String companyId, boolean active);

    /**
     * Check if an HR employee exists for the given account ID.
     * @param accountId the account ID from auth service
     * @return true if HR employee exists, false otherwise
     */
    boolean existsByAccountId(String accountId);

    /**
     * Check if an HR employee exists for the given company and employee ID.
     * @param companyId the company ID
     * @param employeeId the employee ID
     * @return true if HR employee exists, false otherwise
     */
    boolean existsByCompanyIdAndEmployeeId(String companyId, String employeeId);

    /**
     * Delete HR employee by account ID.
     * @param accountId the account ID from auth service
     */
    void deleteByAccountId(String accountId);

    /**
     * Count active HR employees for a specific company.
     * @param companyId the company ID
     * @param active the active status
     * @return count of active HR employees
     */
    long countByCompanyIdAndActive(String companyId, boolean active);
}
