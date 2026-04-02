# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Full build (all modules)
./mvnw clean install

# Run all tests
./mvnw test

# Run domain unit tests only (no Docker required)
./mvnw -pl valadir-domain test

# Run a specific test class
./mvnw -pl valadir-domain test -Dtest=PasswordSecurityServiceTest

# Run a specific test method
./mvnw -pl valadir-domain test -Dtest=EmailTest#shouldCreateEmail_WhenFormatIsValid

# Run infrastructure tests (requires Docker running)
./mvnw -pl valadir-infrastructure-persistence test
./mvnw -pl valadir-infrastructure-security test

# Run the application
./mvnw -pl valadir-boot spring-boot:run
```

## Infrastructure Setup

```bash
cp .env.example .env
docker compose -f docker/docker-compose.yml --env-file .env up -d
```

Services: PostgreSQL 17 (port 5432) and Redis 7.4 (port 6379). Infrastructure tests use Testcontainers and manage their
own containers automatically.

## Architecture

- This project implements **Hexagonal Architecture (Ports & Adapters)** with **DDD** using Java 21 and Spring Boot
  3.5.11.
- Quality goal: **total decoupling, semantic clarity, and pure business logic**. Organized in 7 Maven modules:

```
valadir-domain                        → Core business logic. Zero framework dependencies.
valadir-application                   → Orchestration. Use cases and port interfaces. Depends only on domain.
valadir-infrastructure-persistence    → Database adapters.
valadir-infrastructure-security       → Security mechanisms (JWT, ECDSA P-256 signing, token blacklist).
valadir-infrastructure-web            → REST controllers.
valadir-boot                          → Application entry point, Spring Boot configuration.
valadir-common                        → Shared utilities, cross-cutting types, and semantic error definitions. No dependencies.
```

**Dependency rule:** `domain ← application ← infrastructure ← boot`. Spring Framework is **strictly forbidden** in
`valadir-domain` and `valadir-application`.

## System Features

Core actions: **Register, Login, Logout, Refresh Token**.

- **JWT signing:** ECDSA P-256 (asymmetric). The specific algorithm and all cryptographic details are infrastructure
  concerns — the domain only defines contracts (e.g., `PasswordHasher`).
- **Token revocation:** Blacklist stored in Redis with secure refresh token rotation.
- **Observability:** MDC (Mapped Diagnostic Context) for distributed logging.
- **Resilience:** Rate limiting on critical endpoints.
- **Error responses:** Centralized `@ControllerAdvice` with unified error response format using `ErrorCode`.

## Domain Design Decisions

These ADRs are enforced throughout the codebase:

1. **Value Objects as Records** — All value objects (e.g., `Email`, `RawPassword`, `HashedPassword`, `FullName`,
   `AccountId`) are Java Records with self-validating constructors. Violations throw `DomainException(ErrorCode)`.

2. **Static Factory Methods only** — Entities (`Account`, `User`) expose `create*()` for new instances and
   `reconstitute()` for loading from persistence. No public constructors.

3. **`Account` ≠ `User`** — `Account` owns authentication credentials (email, hashed password, role). `User` owns
   profile data (full name, given name). They are separate entities linked by `accountId`.

4. **Password security in domain service** — `PasswordSecurityService` validates that passwords don't contain personal
   data using a sliding window algorithm. `PasswordHasher` is a domain port (interface); its implementation lives in
   `valadir-infrastructure-security`.

## Error Handling

- Domain violations throw `DomainException(ErrorCode)`.
- Application rule violations throw `ApplicationException(ErrorCode)`.

Error codes are namespaced:

- `VAL-xxx` — Validation errors (`INVALID_FIELD`, `INVALID_PASSWORD`, `REQUIRED_FIELD_MISSING`)
- `BIZ-xxx` — Business rule violations (`INSECURE_PASSWORD`, `EMAIL_ALREADY_EXISTS`)
- `SEC-xxx` — Security violations (`CREDENTIAL_INTEGRITY_ERROR`, `TOKEN_REUSE_DETECTED`)
- `SYS-xxx` — System errors (`INTERNAL_SERVER_ERROR`)

**Do not use generic exceptions or HTTP status codes in `valadir-domain` or `valadir-application`.** HTTP semantics
belong exclusively to the web adapter.

## Test Conventions

General testing and Mockito rules are in `.claude/rules/testing.md` and `.claude/rules/mockito.md`.

Project-specific conventions:

- Domain tests are pure unit tests with no Spring context.
- Infrastructure tests use `@Testcontainers` + `@Container` annotations for automatic container lifecycle.
- Parameterized tests use `@CsvSource`, `@ValueSource`, or `@MethodSource` for edge case coverage.
- The `ValadirApplicationTest` in `valadir-boot` is an integration test that loads the full Spring context.
