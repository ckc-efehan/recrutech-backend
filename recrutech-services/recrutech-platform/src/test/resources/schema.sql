-- Test schema for H2 database
-- Uses VARCHAR instead of CHAR for UUID columns because H2 has issues with CHAR type

CREATE TABLE IF NOT EXISTS job_postings (
    id VARCHAR(36) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    company_id VARCHAR(36) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    updated_by_user_id VARCHAR(36),
    deleted_by_user_id VARCHAR(36),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(200),
    employment_type VARCHAR(50),
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    currency VARCHAR(3),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    expires_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_job_postings_company ON job_postings(company_id);
CREATE INDEX IF NOT EXISTS idx_job_postings_company_status ON job_postings(company_id, status);
CREATE INDEX IF NOT EXISTS idx_job_postings_published ON job_postings(status, is_deleted);

-- Applications table
CREATE TABLE IF NOT EXISTS applications (
    id VARCHAR(36) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    
    -- Core relationships
    applicant_id VARCHAR(36) NOT NULL,
    job_posting_id VARCHAR(36) NOT NULL,
    
    -- Audit fields
    created_by_user_id VARCHAR(36) NOT NULL,
    updated_by_user_id VARCHAR(36),
    deleted_by_user_id VARCHAR(36),
    
    -- Application content (MinIO paths)
    cover_letter_path VARCHAR(500),
    resume_path VARCHAR(500),
    portfolio_path VARCHAR(500),
    
    -- Status and lifecycle
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    interview_scheduled_at TIMESTAMP,
    offer_extended_at TIMESTAMP,
    finalized_at TIMESTAMP,
    
    -- Additional notes
    hr_notes TEXT,
    rejection_reason TEXT,
    
    -- Soft delete
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Indexes for applications table
CREATE INDEX IF NOT EXISTS idx_applications_applicant ON applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_applications_job_posting ON applications(job_posting_id);
CREATE INDEX IF NOT EXISTS idx_applications_applicant_status ON applications(applicant_id, status);
CREATE INDEX IF NOT EXISTS idx_applications_job_posting_status ON applications(job_posting_id, status);
CREATE INDEX IF NOT EXISTS idx_applications_applicant_job_posting ON applications(applicant_id, job_posting_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_applications_is_deleted ON applications(is_deleted);
