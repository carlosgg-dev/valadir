# Valadir Auth System - Project Context

This document serves as the **technical and operational memory** for the project. It must be respected in every
interaction to maintain "Technical Paper" quality standards.

## Architecture & Core Stack

- **Paradigm:** Domain-Driven Design (DDD) & Hexagonal Architecture (Ports & Adapters).
- **Language:** Java 21 (leveraging Records, Sealed Classes, and Pattern Matching).
- **Build System:** Maven (Multi-module structure).
- **Framework:** Spring Boot 3.5.11 (Restricted strictly to `boot` and `infrastructure` layers).
- **Database:** PostgreSQL (Identities via **UUID v4** generated in the Domain).
- **Quality Goal:** Total decoupling, semantic clarity, and pure business logic.

## Features & Key Requirements

- **Multi-module Maven Project:** Enforcing strict separation of concerns.
- **Decoupled Identity:** `Account` (Auth) separated from `User` (Profile).
- **Security:**
    - **JWT with Asymmetric Signing:** ECDSA P-256.
    - **Token Revocation:** Blacklist stored in Redis.
    - **Refresh Token Strategy:** Secure rotation.
- **Observability:** MDC for distributed logging.
- **Resilience:** Rate Limiting on critical endpoints.
- **Error Handling:** Centralized Controller Advice with unified error responses.
- **Core Actions:** Login, Logout, Register, Refresh Token.

## Module Structure

1. **`valadir-domain` (Core):** Pure business logic. Entities, Value Objects, Domain Services. **NO frameworks.**
2. **`valadir-application`:** Orchestration. Defines Ports (Interfaces) and implements Use Cases.
3. **`valadir-infrastructure-persistence`:** PostgreSQL adapters.
4. **`valadir-infrastructure-security`:** Security mechanisms (Hashing, Tokens).
5. **`valadir-infrastructure-web`:** REST Controllers.
6. **`valadir-boot`:** Application entry point and configuration.
7. **`valadir-common`:** Shared utilities and semantic error definitions.

## Domain Design Rules (ADRs)

> [!IMPORTANT]
> **ADR-001 (Immutability):** All **Value Objects** must be implemented as Java `Records`.
> **ADR-002 (Valid State):** Entities (`Account`, `User`) must use **Static Factory Methods** (`create` for new,
`reconstitute` for loading). Public constructors are prohibited to ensure entities are never born in an invalid state.
> **ADR-003 (Separation):** `Account` (credentials/auth) and `User` (profile/personal data) are distinct entities.
> **ADR-004 (Validation):** Password security logic resides in **Domain Services** (`PasswordSecurityService`), not in
> the entity itself if it requires external data context.

## Constraints & Restrictions

- **No Spring Dependencies:** Strictly forbidden in `valadir-domain` and `valadir-application`.
- **Security Agnosticism:** JWT, ECDSA, and specific hashing algorithms are infrastructure details. The domain only
  defines contracts (e.g., `PasswordHasher`).
- **Error Handling:** Use the `ErrorCode` system (e.g., `INVALID_EMAIL`) defined in `valadir-common`. Do not use generic
  exceptions or HTTP status codes in core layers.

## Testing Standards

- **Framework:** JUnit 5, AssertJ.
- **Integration:** Testcontainers (for persistence/infra tests).
- **Style:** Heavy use of `@ParameterizedTest` for edge cases.
- **Naming Convention:** `[Class]Test`.
