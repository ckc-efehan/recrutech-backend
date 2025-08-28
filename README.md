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
│   └── recrutech-notification/  # Notifications (email templates, delivery)
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

## Configuration

### Important environment variables (Docker)
- `MYSQL_ROOT_PASSWORD` – Root password
- `MYSQL_DATABASE` – Database name (default: `recrutech`)
- `MYSQL_USER` – Username
- `MYSQL_PASSWORD` – Password

### Application configuration
- `recrutech-services/recrutech-auth/src/main/resources/application.yml`

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
# All tests
mvn test

# Auth service tests only
Set-Location .\recrutech-services\recrutech-auth
mvn test
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