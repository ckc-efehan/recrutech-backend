# RecruTech Backend

Ein modernes, mikroservice-basiertes Backend-System für Recruitment-Technologie, entwickelt mit Spring Boot und Java 21.

## 📋 Projektübersicht

RecruTech Backend ist eine umfassende Plattform für Personalvermittlung und Bewerbermanagement. Das System bietet sichere Authentifizierung, Benutzerverwaltung und GDPR-konforme Datenverarbeitung für verschiedene Benutzertypen (Unternehmen, HR-Mitarbeiter, Bewerber).

## 🏗️ Architektur

Das Projekt folgt einer modularen Mikroservice-Architektur:

```
recrutech-backend/
├── recrutech-services/
│   ├── recrutech-auth/          # Authentifizierungsservice
│   └── recrutech-common/        # Gemeinsame Utilities und DTOs
├── docker-compose.yml           # Container-Orchestrierung
└── pom.xml                     # Root Maven-Konfiguration
```

## 🚀 Technologie-Stack

### Backend Framework
- **Spring Boot 3.5.5** - Hauptframework
- **Java 21** - Programmiersprache
- **Maven** - Build-Management

### Sicherheit & Authentifizierung
- **Spring Security** - Sicherheitsframework
- **JWT (JSON Web Tokens)** - Token-basierte Authentifizierung
- **JJWT 0.12.3** - JWT-Implementierung

### Datenbank & Persistierung
- **MySQL 8.0** - Primäre Datenbank
- **Spring Data JPA** - ORM-Framework
- **Liquibase** - Datenbank-Migrationen
- **Redis** - Caching und Session-Management

### Entwicklungstools
- **Lombok** - Code-Generierung
- **Spring Boot DevTools** - Entwicklungsunterstützung
- **H2 Database** - In-Memory-Datenbank für Tests

### Testing
- **Spring Boot Test** - Test-Framework
- **Spring Security Test** - Sicherheitstests

## 📦 Installation & Setup

### Voraussetzungen
- Java 21 oder höher
- Maven 3.6+
- Docker & Docker Compose
- MySQL 8.0 (optional, wird über Docker bereitgestellt)

### 1. Repository klonen
```bash
git clone <repository-url>
cd recrutech-backend
```

### 2. Datenbank starten
```bash
docker-compose up -d mysql
```

### 3. Anwendung kompilieren
```bash
mvn clean compile
```

### 4. Authentifizierungsservice starten
```bash
cd recrutech-services/recrutech-auth
mvn spring-boot:run
```

Die Anwendung ist dann unter `http://localhost:8080` verfügbar.

## 🔌 API-Dokumentation

### Authentifizierung Endpoints

**Base URL:** `/api/auth`

#### 🔐 Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

#### 📝 Registrierung

**Unternehmen registrieren:**
```http
POST /api/auth/register/company
Content-Type: application/json

{
  "companyName": "Example Corp",
  "email": "admin@example.com",
  "password": "securePassword"
}
```

**HR-Benutzer registrieren:**
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

**Bewerber registrieren:**
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

#### 🔄 Token-Management
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

#### 🚪 Logout
```http
POST /api/auth/logout
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "userId": "user-id",
  "logoutFromAllDevices": false
}
```

#### 💚 Health Check
```http
GET /api/auth/health
```

## 🛠️ Entwicklung

### Lokale Entwicklung
1. Starten Sie die MySQL-Datenbank über Docker Compose
2. Konfigurieren Sie die Anwendungseigenschaften in `application.yml`
3. Führen Sie die Anwendung im Development-Modus aus:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Tests ausführen
```bash
# Alle Tests
mvn test

# Nur Authentifizierungsservice-Tests
cd recrutech-services/recrutech-auth
mvn test
```

### Code-Stil
Das Projekt verwendet Lombok für die Reduzierung von Boilerplate-Code. Stellen Sie sicher, dass Ihr IDE das Lombok-Plugin installiert hat.

## 🐳 Docker Deployment

### Vollständiges System starten
```bash
docker-compose up -d
```

### Nur Datenbank starten
```bash
docker-compose up -d mysql
```

## 🔧 Konfiguration

### Umgebungsvariablen
- `MYSQL_ROOT_PASSWORD` - MySQL Root-Passwort
- `MYSQL_DATABASE` - Datenbankname (Standard: recrutech)
- `MYSQL_USER` - Datenbankbenutzer
- `MYSQL_PASSWORD` - Datenbankpasswort

### Anwendungskonfiguration
Die Hauptkonfiguration befindet sich in:
- `recrutech-services/recrutech-auth/src/main/resources/application.yml`

## 🔒 Sicherheitsfeatures

- **JWT-basierte Authentifizierung** mit Access- und Refresh-Tokens
- **Passwort-Hashing** mit sicheren Algorithmen
- **IP-Tracking** und User-Agent-Logging für Sicherheitsüberwachung
- **CORS-Konfiguration** für sichere Cross-Origin-Requests
- **Eingabevalidierung** mit Bean Validation
- **GDPR-Compliance** mit dedizierten Endpoints

## 📊 Monitoring & Logging

Das System bietet:
- Health Check Endpoints für Systemüberwachung
- Umfassendes Logging für Debugging und Audit
- Sicherheitsmonitoring für verdächtige Aktivitäten

## 🤝 Beitragen

1. Fork das Repository
2. Erstellen Sie einen Feature-Branch (`git checkout -b feature/AmazingFeature`)
3. Committen Sie Ihre Änderungen (`git commit -m 'Add some AmazingFeature'`)
4. Push zum Branch (`git push origin feature/AmazingFeature`)
5. Öffnen Sie einen Pull Request

## 📄 Lizenz

Dieses Projekt ist unter der [MIT Lizenz](LICENSE) lizenziert.

## 📞 Support

Bei Fragen oder Problemen erstellen Sie bitte ein Issue im Repository oder kontaktieren Sie das Entwicklungsteam.

---

**Entwickelt mit ❤️ für moderne Recruitment-Lösungen**