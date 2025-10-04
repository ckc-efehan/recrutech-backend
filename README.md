# RecruTech Backend

[![Java CI with Maven](https://github.com/ckc-efehan/recrutech-backend/actions/workflows/maven.yml/badge.svg)](https://github.com/ckc-efehan/recrutech-backend/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green.svg)

A modern, modular backend system for recruiting technology, built with Spring Boot and Java 21.

## Project Overview

RecruTech Backend is a platform for recruitment and applicant management. It provides secure authentication, user management, and GDPR-compliant data processing for different user roles (companies, HR staff, applicants).

## Architecture

The project follows a microservice architecture with separate modules:

```
recrutech-backend/
├── recrutech-services/
│   ├── recrutech-auth/          # Authentication & user management, JWT, registration, login
│   ├── recrutech-common/        # Shared library (DTOs, utilities)
│   ├── recrutech-notification/  # Notifications (email templates, delivery)
│   └── recrutech-platform/      # Company-scoped platform APIs (Job Postings, etc.)
├── docker-compose.yml           # Orchestration (includes services/docker-compose.yml)
└── pom.xml                      # Root Maven configuration
```

## Technology Stack

### Backend
- Spring Boot 3.5.5
- Java 21
- Maven

### Security & Authentication
- Spring Security
- JWT (JSON Web Tokens), implementation: JJWT 0.12.3

### Data & Persistence
- MySQL 8.0
- Spring Data JPA
- Liquibase (migrations)
- Redis (caching/sessions)

### Development & Testing
- Lombok, Spring Boot DevTools
- H2 in-memory database (tests)
- Spring Boot Test, Spring Security Test

## Prerequisites
- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose

## Quick Start

### 1) Clone repository
```powershell
git clone <repository-url>
Set-Location recrutech-backend
```

### 2) Start infrastructure (Docker)
By default, MySQL, Redis, Zookeeper, and Kafka are provided.

```powershell
# Full system (from project root)
docker compose up -d

# Or only the database
docker compose up -d mysql
```
Note: The root `docker-compose.yml` includes `recrutech-services/docker-compose.yml`.

### 3) Build
```powershell
mvn clean compile
```

### 4) Start authentication service
```powershell
Set-Location .\recrutech-services\recrutech-auth
mvn spring-boot:run
```
The application will be available at `http://localhost:8080`.

### 5) Start platform service (Job Postings)
```powershell
Set-Location ..\recrutech-platform
mvn spring-boot:run
```
By default, the platform service also uses port 8080. If you want to run both services at once, override the port, for example:
```powershell
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```
Base URL: `http://localhost:8080` (or `8081` if overridden).

## Configuration

### Important environment variables (Docker)
- `MYSQL_ROOT_PASSWORD` – Root password
- `MYSQL_DATABASE` – Database name (default: `recrutech`)
- `MYSQL_USER` – Username
- `MYSQL_PASSWORD` – Password

### Application configuration
- `recrutech-services/recrutech-auth/src/main/resources/application.yml`
- `recrutech-services/recrutech-platform/src/main/resources/application.properties`

## API Overview (Auth)

Base path: `/api/auth`

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

### Registration

Register company:
```http
POST /api/auth/register/company
Content-Type: application/json

{
  "companyName": "Example Corp",
  "email": "admin@example.com",
  "password": "securePassword"
}
```

Register HR user:
```http
POST /api/auth/register/hr
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "hr@example.com",
  "password": "securePassword"
}
```

Register applicant:
```http
POST /api/auth/register/applicant
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "applicant@example.com",
  "password": "securePassword"
}
```

### Refresh token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

### Logout
```http
POST /api/auth/logout
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "userId": "user-id",
  "logoutFromAllDevices": false
}
```

### Health Check
```http
GET /api/auth/health
```

## API Overview (Platform – Job Postings)

Base path: `/companies/{companyId}/job-postings`

Headers:
- For write operations: `X-User-Id: <uuid>`
- `Content-Type: application/json`

Notes:
- Locally no Authorization header is required; security is configured to permit all for development.
- Foreign keys: `companyId` and `X-User-Id` must reference existing rows in `companies` and `users` when DB constraints are enforced.

### Create
```http
POST /companies/{companyId}/job-postings
X-User-Id: <uuid>
Content-Type: application/json

{
  "title": "Senior Backend Engineer",
  "description": "Wir suchen eine/n Senior Backend Engineer (Java, Spring).",
  "location": "Berlin, DE",
  "employmentType": "FULL_TIME",
  "salaryMin": 65000,
  "salaryMax": 85000,
  "currency": "EUR",
  "expiresAt": "2030-12-31T23:59:59"
}
```

### Get by ID
```http
GET /companies/{companyId}/job-postings/{id}
```

### List (optional filters)
```http
GET /companies/{companyId}/job-postings?status=&page=0&size=20&sort=createdAt,desc
```

### Update
```http
PUT /companies/{companyId}/job-postings/{id}
X-User-Id: <uuid>
Content-Type: application/json
{ ...same fields as create... }
```

### Publish
```http
POST /companies/{companyId}/job-postings/{id}/publish
X-User-Id: <uuid>
```

### Close
```http
POST /companies/{companyId}/job-postings/{id}/close
X-User-Id: <uuid>
```

### Delete (soft)
```http
DELETE /companies/{companyId}/job-postings/{id}
X-User-Id: <uuid>
```

Sample success response (excerpt):
```json
{
  "id": "<uuid>",
  "companyId": "<uuid>",
  "title": "Senior Backend Engineer",
  "status": "DRAFT",
  "createdAt": "2025-09-22T18:17:00",
  "isDeleted": false
}
```

## Application Management System

The Application Management System is the core of the recruiting platform, connecting **Job Postings**, **Applicants**, and **Companies** through a complete application workflow.

### Features
- ✅ Complete application lifecycle management (SUBMITTED → UNDER_REVIEW → INTERVIEW_SCHEDULED → INTERVIEWED → OFFER_EXTENDED → ACCEPTED/REJECTED/WITHDRAWN)
- ✅ Duplicate application prevention
- ✅ Status transition validation
- ✅ Event-driven architecture for notifications
- ✅ Soft-delete with audit trail
- ✅ Pagination and filtering
- ✅ Comprehensive test coverage (59 tests)

### Application Status Workflow

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
    ↓
ACCEPTED / REJECTED / WITHDRAWN
```

**Status Transitions:**
- From any non-final status: Can transition to `REJECTED` or `WITHDRAWN`
- Final statuses (`ACCEPTED`, `REJECTED`, `WITHDRAWN`) cannot be changed
- Only valid transitions are allowed (enforced by business logic)

## API Overview (Applications)

Base path: `/applications`

Headers:
- `X-User-Id: <uuid>` (required for write operations)
- `Content-Type: application/json`

### Submit Application
```http
POST /applications
X-User-Id: <uuid>
Content-Type: application/json

{
  "applicantId": "<uuid>",
  "jobPostingId": "<uuid>",
  "coverLetter": "I am very interested in this position...",
  "resumeUrl": "https://example.com/resume.pdf",
  "portfolioUrl": "https://example.com/portfolio"
}
```

**Response (201 Created):**
```json
{
  "id": "<uuid>",
  "applicantId": "<uuid>",
  "jobPostingId": "<uuid>",
  "status": "SUBMITTED",
  "submittedAt": "2025-10-04T19:42:00",
  "coverLetter": "I am very interested...",
  "resumeUrl": "https://example.com/resume.pdf",
  "portfolioUrl": "https://example.com/portfolio",
  "isDeleted": false,
  "createdAt": "2025-10-04T19:42:00"
}
```

**Business Rules:**
- Prevents duplicate applications (one applicant can only apply once per job posting)
- Automatically sets status to `SUBMITTED`
- Sets `submittedAt` timestamp
- Publishes `ApplicationSubmittedEvent`

### Get Application by ID
```http
GET /applications/{id}
```

### Get Applications by Applicant
Get all applications for a specific applicant (with optional status filter).

```http
GET /applications/applicant/{applicantId}?status=SUBMITTED&page=0&size=20&sort=submittedAt,desc
```

### Get Applications by Job Posting
Get all applications for a specific job posting (for HR/recruiters).

```http
GET /applications/job-posting/{jobPostingId}?status=UNDER_REVIEW&page=0&size=20
```

### Get Applications by Company
Get all applications for a company's job postings.

```http
GET /applications/company/{companyId}?status=INTERVIEWED&page=0&size=20
```

### Update Application Status
Update the status of an application (for HR/recruiters).

```http
PUT /applications/{id}/status
X-User-Id: <uuid>
Content-Type: application/json

{
  "status": "UNDER_REVIEW",
  "hrNotes": "Good candidate, proceeding to interview",
  "rejectionReason": null
}
```

**Valid Status Transitions:**
- `SUBMITTED` → `UNDER_REVIEW`, `REJECTED`, `WITHDRAWN`
- `UNDER_REVIEW` → `INTERVIEW_SCHEDULED`, `REJECTED`, `WITHDRAWN`
- `INTERVIEW_SCHEDULED` → `INTERVIEWED`, `REJECTED`, `WITHDRAWN`
- `INTERVIEWED` → `OFFER_EXTENDED`, `REJECTED`, `WITHDRAWN`
- `OFFER_EXTENDED` → `ACCEPTED`, `REJECTED`, `WITHDRAWN`

**Publishes:** `ApplicationStatusChangedEvent`

### Withdraw Application
Allows an applicant to withdraw their application.

```http
POST /applications/{id}/withdraw?applicantId={applicantId}
X-User-Id: <uuid>
```

**Business Rules:**
- Only the applicant who submitted the application can withdraw it
- Cannot withdraw finalized applications (ACCEPTED, REJECTED, WITHDRAWN)
- Sets status to `WITHDRAWN` and `finalizedAt` timestamp

### Delete Application (Soft Delete)
```http
DELETE /applications/{id}
X-User-Id: <uuid>
```

## Event Infrastructure

The system uses an event-driven architecture for loose coupling and asynchronous processing.

### Base Event
All events extend `BaseEvent` which provides:
- `eventId` (UUID)
- `occurredAt` (timestamp)
- `eventType` (string identifier)

### Application Events

#### ApplicationSubmittedEvent
Published when a new application is submitted.

**Fields:**
- `applicationId`
- `applicantId`
- `jobPostingId`
- `companyId`

**Use Cases:**
- Send confirmation email to applicant
- Notify HR team of new application
- Update analytics/metrics

#### ApplicationStatusChangedEvent
Published when an application's status changes.

**Fields:**
- `applicationId`
- `applicantId`
- `jobPostingId`
- `companyId`
- `previousStatus`
- `newStatus`
- `updatedByUserId`

**Use Cases:**
- Notify applicant of status change
- Trigger workflow automation (e.g., send interview invitation)
- Maintain audit trail

## Database Schema

### Core Tables

#### users
- `id` (CHAR(36), PK)
- `email` (VARCHAR, unique)
- `password_hash` (VARCHAR)
- `role` (ENUM: COMPANY_ADMIN, HR, APPLICANT)
- `created_at` (DATETIME)
- Audit fields: `is_deleted`, `deleted_at`

#### companies
- `id` (CHAR(36), PK)
- `name` (VARCHAR)
- `location` (VARCHAR)
- `business_email` (VARCHAR, unique)
- `admin_user_id` (CHAR(36), FK → users.id)
- `verified` (BOOLEAN)
- `created_at` (DATETIME)

#### applicants
- `id` (CHAR(36), PK)
- `user_id` (CHAR(36), FK → users.id)
- `phone_number` (VARCHAR)
- `date_of_birth` (DATE)
- `linkedin_profile` (VARCHAR)
- `resume_url` (VARCHAR)
- `current_location` (VARCHAR)
- `profile_complete` (BOOLEAN)
- `created_at` (DATETIME)

#### job_postings
- `id` (CHAR(36), PK)
- `company_id` (CHAR(36), FK → companies.id)
- `title` (VARCHAR(200))
- `description` (TEXT)
- `location` (VARCHAR(200))
- `employment_type` (VARCHAR(50))
- `salary_min`, `salary_max` (DECIMAL(12,2))
- `currency` (VARCHAR(3))
- `status` (ENUM: DRAFT, PUBLISHED, ARCHIVED)
- `published_at`, `expires_at` (DATETIME)
- Audit fields: `created_by_user_id`, `updated_by_user_id`, `deleted_by_user_id`
- Soft delete: `is_deleted`, `deleted_at`
- `created_at` (DATETIME)

**Indexes:**
- `idx_job_postings_company` (company_id)
- `idx_job_postings_company_status` (company_id, status)
- `idx_job_postings_published` (status, is_deleted)

#### applications
- `id` (CHAR(36), PK)
- `applicant_id` (CHAR(36), FK → applicants.id)
- `job_posting_id` (CHAR(36), FK → job_postings.id)
- `cover_letter` (TEXT)
- `resume_url` (VARCHAR(500))
- `portfolio_url` (VARCHAR(500))
- `status` (ENUM: SUBMITTED, UNDER_REVIEW, INTERVIEW_SCHEDULED, INTERVIEWED, OFFER_EXTENDED, ACCEPTED, REJECTED, WITHDRAWN)
- Lifecycle timestamps: `submitted_at`, `reviewed_at`, `interview_scheduled_at`, `offer_extended_at`, `finalized_at`
- `hr_notes`, `rejection_reason` (TEXT)
- Audit fields: `created_by_user_id`, `updated_by_user_id`, `deleted_by_user_id`
- Soft delete: `is_deleted`, `deleted_at`
- `created_at` (DATETIME)

**Indexes:**
- `idx_applications_applicant` (applicant_id)
- `idx_applications_job_posting` (job_posting_id)
- `idx_applications_applicant_status` (applicant_id, status)
- `idx_applications_job_posting_status` (job_posting_id, status)
- `idx_applications_applicant_job_posting` (applicant_id, job_posting_id, is_deleted) - for duplicate prevention
- `idx_applications_is_deleted` (is_deleted)

**Foreign Key Constraints:**
- All foreign keys have `ON DELETE` and `ON UPDATE` rules
- `applicant_id`, `job_posting_id`: `CASCADE`
- `created_by_user_id`: `RESTRICT`
- `updated_by_user_id`, `deleted_by_user_id`: `SET NULL`

## Development

### Local development
1. Start infrastructure via Docker Compose
2. Configure properties in `application.yml`
3. Run with the dev profile:
   ```powershell
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Run tests
```powershell
# All modules
mvn test

# Auth service tests only
Set-Location .\recrutech-services\recrutech-auth
mvn test

# Platform service tests only (includes Testcontainers MySQL)
Set-Location ..\recrutech-platform
mvn test
# Or from project root:
mvn -pl recrutech-services/recrutech-platform test

# Common module tests (Event infrastructure)
Set-Location ..\recrutech-common
mvn test
```

Note: Ensure Docker is running; repository integration tests in the platform module use Testcontainers (MySQL).

### Test Coverage

The project has comprehensive test coverage across all modules:

#### Platform Module Tests (59 tests total)

**ApplicationServiceTest (30 tests)** - Service layer business logic
- Submit application tests (5 tests)
  - Success scenario with all fields
  - Duplicate application prevention
  - Invalid UUID validation (applicantId, jobPostingId, userId)
- Get application tests (8 tests)
  - Get by ID (success and not found)
  - Get by applicant (with/without status filter)
  - Get by job posting (with/without status filter)
  - Get by company (with/without status filter)
- Update status tests (13 tests)
  - Valid transitions through entire workflow
  - Invalid transition validation
  - Finalized application protection (ACCEPTED, REJECTED, WITHDRAWN)
  - Idempotent status updates
- Withdraw tests (3 tests)
  - Success scenario
  - Ownership validation
  - Cannot withdraw finalized applications
- Soft delete test (1 test)

**ApplicationControllerTest (21 tests)** - REST API endpoints
- Submit endpoint (4 tests)
  - Success (201 Created)
  - Missing X-User-Id header (400)
  - Missing applicantId (400)
  - Missing jobPostingId (400)
- Get by ID endpoint (1 test)
- Get by applicant endpoint (3 tests)
  - Without status filter
  - With status filter
  - Pageable parameters validation
- Get by job posting endpoint (2 tests)
  - Without status filter
  - With status filter
- Get by company endpoint (3 tests)
  - Without status filter
  - With status filter
  - Pageable parameters validation
- Update status endpoint (3 tests)
  - Success (200 OK)
  - Missing X-User-Id header (400)
  - Missing status field (400)
- Withdraw endpoint (3 tests)
  - Success (200 OK)
  - Missing X-User-Id header (400)
  - Missing applicantId parameter (400)
- Delete endpoint (2 tests)
  - Success (204 No Content)
  - Missing X-User-Id header (400)

**ApplicationMapperTest (2 tests)** - Entity/DTO mapping
- Maps all fields correctly
- Handles null optional fields

**JobPostingServiceTest (12 tests)** - Job postings business logic
- Create, read, update operations
- Publish and close workflows
- Soft delete
- Business rule validation (salary, currency, dates)

**JobPostingControllerTest (7 tests)** - Job postings REST API
- All CRUD operations
- Pagination and filtering

#### Common Module Tests (6 tests)

**ApplicationSubmittedEventTest (3 tests)** - Event creation and properties
- Constructor initializes all fields
- toString contains all information
- Event IDs are unique

**ApplicationStatusChangedEventTest (3 tests)** - Event creation and properties
- Constructor initializes all fields
- ToString contains all information
- Event IDs are unique

#### Test Technologies
- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions
- **Spring Boot Test** - Integration testing
- **MockMvc** - REST API testing
- **Testcontainers** - Database integration tests (MySQL)
- **H2** - In-memory database for unit tests

#### Running Specific Test Classes
```powershell
# Run ApplicationServiceTest
mvn -Dtest=ApplicationServiceTest test

# Run ApplicationControllerTest
mvn -Dtest=ApplicationControllerTest test

# Run all Application tests
mvn -Dtest=Application*Test test
```

### Code style
The project uses Lombok. Please enable the Lombok plugin in your IDE.

## Docker deployment

### Start complete system
```powershell
docker compose up -d
```

### Start database only
```powershell
docker compose up -d mysql
```

## Security
- JWT-based authentication (access and refresh tokens)
- Password hashing with secure algorithms
- IP and user-agent logging for security monitoring
- CORS configuration for secure cross-origin requests
- Input validation (Bean Validation)
- GDPR support via dedicated endpoints

## Monitoring & Logging
- Health-check endpoints
- Comprehensive logging for debugging and audit
- Security monitoring of suspicious activities

## Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/NewFeature`)
3. Commit your changes (`git commit -m "Add NewFeature"`)
4. Push to the branch (`git push origin feature/NewFeature`)
5. Open a Pull Request

## License
This project is licensed under the [MIT License](LICENSE).