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
