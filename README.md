# Open Source Recruiting Technology Platform (Backend)

**recrutech-backend** is a backend service built using Java and Spring Boot framework to manage recruitment and applicant tracking. It works together with the [recrutech-frontend](https://github.com/ckc-efehan/recrutech-frontend) repository.

## Architecture

The project is structured into multiple modules:

- **recrutech-auth**: A microservice responsible for authentication, user management, JWT token handling, and GDPR compliance.
- **recrutech-common**: A library containing shared implementations used across all microservices.
- **recrutech-notification**: A microservice responsible for all customer notifications and email delivery.
- **recrutech-platform**: The most important microservice, which is responsible for core functionalities such as job postings, application management, company administration, etc.

## Development

This project uses MAVEN to build and test the complete backend microservice architecture. Before you change anything, make sure that your development setup meets the requirements as described in Prerequisites. Afterwards you can build and package the application by running:

```bash
mvn package
```

### Prerequisites

To develop or start this project locally, you need:

- Java 25 or higher
- Maven 3.8.1 or higher
- Docker or Podman to start containers

### How to get started

For ease of use its recommended to run mysql using the provided docker-compose.yml.

```bash
docker compose up -d
```

**Important**: The docker-compose.yml file uses the `include:` directive, which is only supported in newer versions of Docker Compose. Please make sure your Docker Desktop or Docker Compose CLI is up to date.

## CI/CD

This project utilizes Github Actions to check the code quality using SonarCloud therefore its mandatory to pass the specified Quality Gates before a pull request can be merged.

## Start in dev mode

At first you will need to start the db as described in Prerequisites.

Next run the project using the following command:

```bash
.\mvnw clean install
.\mvnw spring-boot:run -pl recrutech-services/recrutech-auth
```

It will automatically recompile when you change something.


## Contributing

Before contributing to RecruTech, please read our contributing guidelines. Participation in the RecruTech project is governed by the CNCF Code of Conduct.

**NOTE**: When contributing to this repository, please first discuss the change you wish to make by creating an issue before making a change. Once you got feedback on your idea, feel free to fork the project and open a pull request.

**NOTE**: Please only make changes in files directly related to your issue!

## License

MIT License
