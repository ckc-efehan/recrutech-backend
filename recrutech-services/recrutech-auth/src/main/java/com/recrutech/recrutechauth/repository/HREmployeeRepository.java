package com.recrutech.recrutechauth.repository;

import com.recrutech.recrutechauth.model.HREmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for HREmployee entity operations.
 * Provides methods for HR employee management and company associations.
 */
@Repository
public interface HREmployeeRepository extends JpaRepository<HREmployee, String> {

    /**
     * Find HR employee by user ID.
     * @param userId the user ID
     * @return Optional containing the HR employee if found
     */
    Optional<HREmployee> findByUserId(String userId);

}