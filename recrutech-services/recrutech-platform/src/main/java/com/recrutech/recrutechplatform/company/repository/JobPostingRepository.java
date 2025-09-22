package com.recrutech.recrutechplatform.company.repository;

import com.recrutech.recrutechplatform.company.model.JobPosting;
import com.recrutech.recrutechplatform.company.model.JobPostingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, String> {

    Optional<JobPosting> findByIdAndCompanyIdAndIsDeletedFalse(String id, String companyId);

    Page<JobPosting> findAllByCompanyIdAndIsDeletedFalse(String companyId, Pageable pageable);

    Page<JobPosting> findAllByCompanyIdAndStatusAndIsDeletedFalse(String companyId, JobPostingStatus status, Pageable pageable);

    boolean existsByIdAndCompanyIdAndIsDeletedFalse(String id, String companyId);
}
