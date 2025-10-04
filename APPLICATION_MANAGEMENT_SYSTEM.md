# Application Management System - Implementation Summary

## Overview
Implemented a complete Application Management System for the RecruTech recruiting platform that connects JobPostings ↔ Applicants ↔ Companies.

## Why This Feature?
- **Connects Existing Components**: Links JobPostings, Applicants, and Companies
- **Core Functionality**: Essential for a recruiting platform
- **Complete Workflow**: From application submission to hiring decision
- **Event-Driven**: Ready for notification service integration

## Implementation Details

### 1. Database Layer (Platform Module)

#### Entities & Enums
- **ApplicationStatus Enum** (`application/model/ApplicationStatus.java`)
  - Status flow: SUBMITTED → UNDER_REVIEW → INTERVIEW_SCHEDULED → INTERVIEWED → OFFER_EXTENDED → ACCEPTED/REJECTED/WITHDRAWN
  - Supports complete hiring workflow

- **Application Entity** (`application/model/Application.java`)
  - Relationships: applicantId (FK to applicants), jobPostingId (FK to job_postings)
  - Audit fields: createdByUserId, updatedByUserId, deletedByUserId
  - Content: coverLetter, resumeUrl, portfolioUrl
  - Lifecycle timestamps: submittedAt, reviewedAt, interviewScheduledAt, offerExtendedAt, finalizedAt
  - HR fields: hrNotes, rejectionReason
  - Soft delete support

#### Repository Layer
- **ApplicationRepository** (`application/repository/ApplicationRepository.java`)
  - Find by applicant (with optional status filter)
  - Find by job posting (with optional status filter)
  - Find by company (JOIN with job_postings, with optional status filter)
  - Duplicate application check
  - All queries support pagination and filter soft-deleted records

#### Database Migrations
- **Liquibase Migration** (`db.changelog/v0.1/06-create-applications-table.xml`)
  - Complete table structure with all fields
  - Foreign key constraints with proper CASCADE/SET NULL behaviors
  - Performance indexes: applicantId, jobPostingId, status combinations
  - Unique index for duplicate prevention
  - Updated master changelog to include new migration

### 2. Service Layer (Platform Module)

#### ApplicationService (`application/service/ApplicationService.java`)
- **Submission**: 
  - Duplicate application prevention
  - Automatic timestamp setting
  - Validation of all IDs

- **Status Management**:
  - Validates status transitions (enforces allowed workflow paths)
  - Prevents modification of finalized applications (ACCEPTED/REJECTED/WITHDRAWN)
  - Updates lifecycle timestamps automatically
  - Supports HR notes and rejection reasons

- **Query Methods**:
  - By applicant (with pagination & status filter)
  - By job posting (with pagination & status filter)
  - By company (with pagination & status filter)

- **Withdrawal**:
  - Applicant can withdraw own applications
  - Ownership verification
  - Cannot withdraw finalized applications

- **Business Rules**:
  - Status transition validation with clear error messages
  - Duplicate application prevention
  - Finalized state protection
  - Comprehensive audit trail

### 3. Event Infrastructure (Common Module)

#### Event Classes
- **BaseEvent** (`common/event/BaseEvent.java`)
  - Base class for all domain events
  - Contains: eventId, occurredAt, eventType
  - Immutable design for event integrity

- **ApplicationSubmittedEvent** (`common/event/ApplicationSubmittedEvent.java`)
  - Published when new application is submitted
  - Contains: applicationId, applicantId, jobPostingId, companyId
  - Ready for notification service integration

- **ApplicationStatusChangedEvent** (`common/event/ApplicationStatusChangedEvent.java`)
  - Published when application status changes
  - Contains: applicationId, applicantId, jobPostingId, companyId, previousStatus, newStatus, updatedByUserId
  - Enables notifications for status updates

### 4. REST API Layer (Platform Module)

#### DTOs (`application/dto/`)
- **ApplicationSubmitRequest**: For submitting new applications
  - Validation: applicantId and jobPostingId required
  - Optional: coverLetter, resumeUrl, portfolioUrl

- **ApplicationResponse**: Complete application data
  - All entity fields including status, timestamps, audit info

- **ApplicationUpdateStatusRequest**: For HR status updates
  - Validation: status required
  - Optional: hrNotes, rejectionReason

#### Mapper
- **ApplicationMapper** (`application/mapper/ApplicationMapper.java`)
  - Static utility class for entity ↔ DTO conversion
  - toResponse() method for clean API responses

#### Controller
- **ApplicationController** (`application/controller/ApplicationController.java`)
  - **POST /applications** - Submit new application
  - **GET /applications/{id}** - Get application by ID
  - **GET /applications/applicant/{applicantId}** - List applicant's applications
  - **GET /applications/job-posting/{jobPostingId}** - List applications for job posting
  - **GET /applications/company/{companyId}** - List applications for company's jobs
  - **PUT /applications/{id}/status** - Update application status (HR)
  - **POST /applications/{id}/withdraw** - Withdraw application (applicant)
  - **DELETE /applications/{id}** - Soft delete application
  - All list endpoints support pagination (page, size, sort) and optional status filtering

## Best Practices Applied

### Clean Code
✓ Clear separation of concerns (controller → service → repository → entity)
✓ Single Responsibility Principle (each class has one purpose)
✓ Comprehensive Javadoc documentation
✓ Meaningful naming conventions
✓ Immutable DTOs using Java records
✓ Constants for error messages and magic numbers

### Architecture
✓ Layered architecture (controller → service → repository → entity)
✓ Repository pattern for data access
✓ DTO pattern for API layer
✓ Mapper pattern for entity/DTO conversion
✓ Event-driven design ready for notifications
✓ Pagination support for scalability

### Data Integrity
✓ Foreign key constraints in database
✓ Soft delete pattern (preserves history)
✓ Duplicate prevention (can't apply twice to same job)
✓ Status transition validation (enforces workflow)
✓ Comprehensive audit trail (who, when, what)
✓ Lifecycle timestamps for tracking

### Validation & Error Handling
✓ Jakarta validation annotations on DTOs
✓ UUID validation for all IDs
✓ Business rule validation in service layer
✓ Custom exceptions (NotFoundException, ValidationException)
✓ Clear, user-friendly error messages

### Performance
✓ Database indexes on frequently queried fields
✓ Pagination support on all list endpoints
✓ Optimized queries (JOIN for company lookups)
✓ Soft delete filtering in queries

## API Endpoints Summary

### Application Management
```
POST   /applications                              - Submit application
GET    /applications/{id}                         - Get by ID
GET    /applications/applicant/{applicantId}      - List by applicant
GET    /applications/job-posting/{jobPostingId}   - List by job posting
GET    /applications/company/{companyId}          - List by company
PUT    /applications/{id}/status                  - Update status (HR)
POST   /applications/{id}/withdraw                - Withdraw (applicant)
DELETE /applications/{id}                         - Delete
```

### Query Parameters (for list endpoints)
- `status`: Filter by ApplicationStatus (optional)
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (default: submittedAt,desc)

### Headers
- `X-User-Id`: Current user's ID (required for mutations)

## Status Flow Diagram

```
                    SUBMITTED
                        ↓
                  UNDER_REVIEW
                        ↓
              INTERVIEW_SCHEDULED
                        ↓
                   INTERVIEWED
                        ↓
                 OFFER_EXTENDED
                   ↙        ↘
              ACCEPTED    REJECTED
                   
(WITHDRAWN can happen at any non-final state)
```

## Database Schema

### applications Table
- **Primary Key**: id (CHAR(36) - UUID)
- **Foreign Keys**: 
  - applicant_id → applicants.id (CASCADE)
  - job_posting_id → job_postings.id (CASCADE)
  - created_by_user_id → users.id (RESTRICT)
  - updated_by_user_id → users.id (SET NULL)
  - deleted_by_user_id → users.id (SET NULL)
- **Indexes**: 
  - applicant_id
  - job_posting_id
  - applicant_id + status
  - job_posting_id + status
  - applicant_id + job_posting_id + is_deleted (for duplicate check)
  - is_deleted

## Integration Points

### Existing Systems
✓ **Applicants** (auth module): Links to applicant profiles
✓ **JobPostings** (platform module): Links to job postings
✓ **Companies** (auth module): Accessible via JobPosting.companyId
✓ **Users** (auth module): Audit trail via userId fields

### Future Integrations
🔄 **Notification Service**: Event infrastructure ready
  - ApplicationSubmittedEvent → Notify HR about new applications
  - ApplicationStatusChangedEvent → Notify applicants about status updates

## Testing Recommendations

### Unit Tests
- ApplicationService business logic
- Status transition validation
- Duplicate application prevention
- Lifecycle timestamp updates

### Integration Tests
- ApplicationController endpoints
- Repository queries
- Database constraints
- Pagination and filtering

## Files Created/Modified

### Platform Module
```
src/main/java/com/recrutech/recrutechplatform/application/
├── model/
│   ├── ApplicationStatus.java
│   └── Application.java
├── repository/
│   └── ApplicationRepository.java
├── service/
│   └── ApplicationService.java
├── dto/
│   ├── ApplicationSubmitRequest.java
│   ├── ApplicationResponse.java
│   └── ApplicationUpdateStatusRequest.java
├── mapper/
│   └── ApplicationMapper.java
└── controller/
    └── ApplicationController.java

src/main/resources/db.changelog/
├── liquibase-changelog.xml (modified)
└── v0.1/
    └── 06-create-applications-table.xml
```

### Common Module
```
src/main/java/com/recrutech/common/event/
├── BaseEvent.java
├── ApplicationSubmittedEvent.java
└── ApplicationStatusChangedEvent.java
```

## Summary

The Application Management System is now fully implemented and provides:
✅ Complete workflow from application to hiring
✅ Robust business logic with validation
✅ Clean, maintainable code following best practices
✅ Event-driven architecture ready for notifications
✅ Comprehensive API for all use cases
✅ Performance-optimized with proper indexing
✅ Data integrity with constraints and soft deletes

The system successfully connects the three core entities (JobPostings ↔ Applicants ↔ Companies) and provides the essential recruiting platform functionality.
