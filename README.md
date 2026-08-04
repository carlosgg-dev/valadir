<div style="text-align: center;">
    <img src="assets/logo.svg" alt="Valadir Auth System logo" width="600">
</div>

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

## Documentation

- [Security Architecture](docs/security-architecture.md) — token strategy, Redis usage, session ownership, and how the
  system behaves when Postgres, Redis or SMTP are unavailable.

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

Tests are split by Maven phase, so the inner loop never needs Docker:

```bash
# Fast inner loop — unit tests only (Surefire, *Test), no Docker.
./mvnw test

# Full build — also runs the integration/E2E slices (Failsafe, *IT) against
# Testcontainers Postgres + Redis. Requires Docker to be running.
./mvnw verify
```

Coverage is attributed by test type via two JaCoCo exec files and two gates: a strict **unit gate**
(`domain`/`application`/`common`, 100% at the `test` phase, unit coverage only) and a **union gate**
(`infrastructure-*`/`boot`, 90% at the `verify` phase). The unit gate cannot be satisfied by integration
tests, so a business-logic gap can never be masked by an E2E.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
