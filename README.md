# RecruTech Backend

A modern, microservice-based backend system for recruitment technology, built with Spring Boot and Java 21.

## Project Overview

RecruTech Backend is a comprehensive platform for recruitment and applicant management. The system provides secure authentication, user management, and GDPR-compliant data processing for different user types (companies, HR employees, applicants).

## Architecture

The project follows a modular microservice architecture:

```
recrutech-backend/
├── recrutech-services/
│   ├── recrutech-auth/          # Authentication service
│   └── recrutech-common/        # Shared utilities and DTOs
├── docker-compose.yml           # Container orchestration
└── pom.xml                     # Root Maven configuration
```

## Technology Stack

### Backend Framework
- **Spring Boot 3.5.5** - Main framework
- **Java 21** - Programming language
- **Maven** - Build management

### Security & Authentication
- **Spring Security** - Security framework
- **JWT (JSON Web Tokens)** - Token-based authentication
- **JJWT 0.12.3** - JWT implementation

### Database & Persistence
- **MySQL 8.0** - Primary database
- **Spring Data JPA** - ORM framework
- **Liquibase** - Database migrations
- **Redis** - Caching and session management

### Development Tools
- **Lombok** - Code generation
- **Spring Boot DevTools** - Development support
- **H2 Database** - In-memory database for tests

### Testing
- **Spring Boot Test** - Test framework
- **Spring Security Test** - Security tests

## Installation & Setup

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- Docker & Docker Compose
- MySQL 8.0 (optional, provided via Docker)

### 1. Clone Repository
```bash
git clone <repository-url>
cd recrutech-backend
```

### 2. Start Database
```bash
docker-compose up -d mysql
```

### 3. Compile Application
```bash
mvn clean compile
```

### 4. Start Authentication Service
```bash
cd recrutech-services/recrutech-auth
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`.

## API Documentation

### Authentication Endpoints

**Base URL:** `/api/auth`

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

#### Registration

**Register Company:**
```http
POST /api/auth/register/company
Content-Type: application/json

{
  "companyName": "Example Corp",
  "email": "admin@example.com",
  "password": "securePassword"
}
```

**Register HR User:**
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

**Register Applicant:**
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

#### Token Management
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

#### Logout
```http
POST /api/auth/logout
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "userId": "user-id",
  "logoutFromAllDevices": false
}
```

#### Health Check
```http
GET /api/auth/health
```

## Development

### Local Development
1. Start the MySQL database via Docker Compose
2. Configure application properties in `application.yml`
3. Run the application in development mode:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Running Tests
```bash
# All tests
mvn test

# Authentication service tests only
cd recrutech-services/recrutech-auth
mvn test
```

### Code Style
The project uses Lombok to reduce boilerplate code. Ensure your IDE has the Lombok plugin installed.

## Docker Deployment

### Start Complete System
```bash
docker-compose up -d
```

### Start Database Only
```bash
docker-compose up -d mysql
```

## Configuration

### Environment Variables
- `MYSQL_ROOT_PASSWORD` - MySQL root password
- `MYSQL_DATABASE` - Database name (default: recrutech)
- `MYSQL_USER` - Database user
- `MYSQL_PASSWORD` - Database password

### Application Configuration
Main configuration is located in:
- `recrutech-services/recrutech-auth/src/main/resources/application.yml`

## Security Features

- **JWT-based Authentication** with access and refresh tokens
- **Password Hashing** with secure algorithms
- **IP Tracking** and user agent logging for security monitoring
- **CORS Configuration** for secure cross-origin requests
- **Input Validation** with Bean Validation
- **GDPR Compliance** with dedicated endpoints

## Monitoring & Logging

The system provides:
- Health check endpoints for system monitoring
- Comprehensive logging for debugging and audit
- Security monitoring for suspicious activities

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the [MIT License](LICENSE).