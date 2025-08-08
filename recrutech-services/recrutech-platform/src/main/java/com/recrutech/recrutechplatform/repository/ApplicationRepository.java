package com.recrutech.recrutechplatform.repository;

import com.recrutech.recrutechplatform.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Application entities
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {
    
    /**
     * Find all applications for a specific job.
     *
     * @param jobId the ID of the job
     * @return list of applications for the specified job
     */
    List<Application> findByJob_Id(String jobId);
    
    /**
     * Find an application by its ID and job ID.
     * This ensures the application belongs to the specified job.
     *
     * @param applicationId the ID of the application
     * @param jobId the ID of the job
     * @return optional containing the application if found and belongs to the job
     */
    Optional<Application> findByIdAndJob_Id(String applicationId, String jobId);
}