# Valadir - Modular Auth System

A production-ready base for modern applications built with Spring Boot, following Hexagonal Architecture principles.
This project is designed to be highly decoupled, maintainable, and easily pluggable into different environments.

## Architecture

This project follows Hexagonal Architecture (Ports and Adapters) to ensure business logic remains isolated from
infrastructure concerns.

- valadir-domain: The core. Pure business logic and domain models. Zero dependencies.
- valadir-application: Application use cases and input/output ports. Defines what the application does.
- valadir-infrastructure-persistence: Persistence adapter for data storage and retrieval.
- valadir-infrastructure-web: Web adapter for external communication and API endpoints.
- valadir-infrastructure-security: Security and identity adapter for authentication and authorization logic.
- valadir-common: Shared exceptions, error DTOs, and logging utilities.
- valadir-boot: Application entry point, configuration, and dependency injection glue.

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.6.3+

### Infrastructure Setup

The project uses Docker to manage its external dependencies for local development.

1. Configure environment: Create a .env file in the root (use .env.example as a template).
2. Start services:
   ```bash
   docker compose -f docker/docker-compose.yml --env-file .env up -d
   ```

### Build and Test

Run the full build including infrastructure validation:

```bash
mvn clean install
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
