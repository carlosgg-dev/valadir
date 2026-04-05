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

## Security Architecture

### Token Strategy

Authentication is based on two tokens with different roles:

- **Access token** — JWT, short-lived (15 min), stateless, sent on every request. Validated by signature and expiry.
- **Refresh token** — opaque UUID, long-lived (7 days), stored server-side in Redis. Carries no claims.

### Redis Usage

| Store                  | Type      | Purpose                                                                                                                                                               |
|------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RefreshTokenStore`    | Whitelist | Tracks active refresh tokens. Long-lived tokens require explicit server-side revocation on logout or reuse detection. Each entry has a TTL matching the token expiry. |
| `AccessTokenBlacklist` | Blacklist | Tracks revoked access tokens. Consulted on every request to reject tokens invalidated before expiry. Each entry has a TTL equal to the remaining token lifetime.      |

The access token uses a blacklist (not a whitelist) because it is used on every request — querying a whitelist on each
call would add unnecessary latency. The refresh token uses a whitelist because its long lifetime would make a blacklist
grow indefinitely without TTL-based cleanup.

### Refresh Token Rotation

Every refresh operation consumes the current refresh token and issues a new pair. A token not found in Redis
is treated as invalid — the user must log in again.

On login, all existing refresh tokens for the account are revoked before issuing a new one. This ensures that
any previously stolen token is invalidated the moment the legitimate user authenticates again.

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
