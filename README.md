<div align="center">
    <img src="assets/logo.svg" alt="Valadir Auth System logo" width="600">
</div>

A Spring Boot authentication service built on Hexagonal Architecture: the auth flows are complete, their failure
policy is documented, and the test suite enforces both on every build.

## Features

Every endpoint lives under `/api/auth`.

- **Registration with email activation** — a one-time code sent by email, a resend route, and a scheduled purge of the
  accounts that never activate.
- **Login** — JWT access token plus a rotating refresh token, account lockout after repeated failures, and a CAPTCHA
  step-up challenge (Cloudflare Turnstile).
- **Session control** — refresh, logout of the current session, and logout of every session of the account.
- **Password reset** by email one-time code, in three steps: initiate, verify, complete.
- **Account management** (authenticated, password re-authentication where it matters) — read and update profile, change
  password, change email proving the new mailbox, delete account.
- **Rate limiting** per IP, per email or per authenticated user, declared as rules per endpoint and enforced in Redis.
- **Transactional email** rendered in the account's language and sent asynchronously.
- **Documented failure policy** — fail-closed for security operations, degradation for secondary ones, asserted by the
  resilience tests.

What each flow guarantees — and what it deliberately does not — is
in [Security Architecture](docs/security-architecture.md).

## Architecture

This project follows Hexagonal Architecture (Ports and Adapters) to ensure business logic remains isolated from
infrastructure concerns.

- valadir-domain: The core. Pure business logic and domain models. Zero dependencies.
- valadir-application: Application use cases and input/output ports. Defines what the application does.
- valadir-infrastructure-persistence: Persistence adapter for data storage and retrieval.
- valadir-infrastructure-web: Web adapter for external communication and API endpoints.
- valadir-infrastructure-security: Security and identity adapter for authentication and authorization logic.
- valadir-infrastructure-notifications: Notification adapter for transactional email.
- valadir-common: Shared exceptions, error DTOs, and logging utilities.
- valadir-boot: Application entry point, configuration, and dependency injection glue.

The domain is modelled with DDD tactical patterns: value objects that validate on construction (`Email`, `RawPassword`,
`PlainOtp`), the `Account` and `User` aggregates, domain services (`PasswordSecurityService`) and policies
(`LoginLockoutPolicy`). The application layer owns the ports and never exposes a domain type at its boundary, so a
driving adapter speaks primitives to a use case. `HexagonalArchitectureTest` enforces the dependency rule on every
build.

## Documentation

- [Security Architecture](docs/security-architecture.md) — token strategy, Redis usage, session ownership, account
  enumeration, and how the system behaves when Postgres, Redis or SMTP are unavailable.

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose

Maven is not required: use the wrapper (`./mvnw`) checked into the repository.

### Infrastructure Setup

The project uses Docker to manage its external dependencies for local development.

1. Configure environment: Create a .env file in the root (use .env.example as a template).
2. Start services:

```bash
docker compose -f docker/docker-compose.yml --env-file .env up -d
```

This brings up Postgres (5432), Redis (6379) and Mailpit (SMTP on 1025, web UI on
[localhost:8025](http://localhost:8025), where the outgoing mail lands).

The schema comes from `docker/postgres/init.sql`, which Postgres runs only when it creates its volume — the application
starts with `ddl-auto: validate` and never creates a table. After changing the script, recreate the volume:

```bash
docker compose -f docker/docker-compose.yml --env-file .env down -v
```

### Running the Application

The `--env-file` above feeds Docker Compose, not the JVM. The application takes the same variables from its own
environment, so whatever launches it has to carry them — an IDE run configuration reading `.env`, an exported shell, or
any equivalent.

`spring-boot:run` resolves the sibling modules from the local repository rather than from the reactor, so they have to
be installed first — again after a version bump, or after touching any module the boot depends on:

```bash
./mvnw install -DskipTests -Djacoco.skip=true
./mvnw -pl valadir-boot spring-boot:run
```

The coverage gates are skipped on purpose in that install: a build with no tests has no coverage to measure, and a
stale exec file from an earlier run would fail it.

The API listens on [localhost:8080](http://localhost:8080).

To carry the variables from a shell, export the file before running: `set -a; source .env; set +a`. On that route
`JWT_PRIVATE_KEY` must stay single-quoted in `.env` — it is a JSON document, and unquoted the shell strips its quotes
and the key fails to parse at startup. `.env.example` shows the form.

## Testing

Tests are split by Maven phase, so the inner loop never needs Docker:

| Suite             | Runner                   | Naming  | Docker | Command         |
|-------------------|--------------------------|---------|--------|-----------------|
| Unit              | Surefire, `test` phase   | `*Test` | no     | `./mvnw test`   |
| Integration & E2E | Failsafe, `verify` phase | `*IT`   | yes    | `./mvnw verify` |

`./mvnw verify` runs everything: the unit tests, the persistence/security/notifications slices and the `valadir-boot`
E2E flows against Testcontainers Postgres, Redis and Mailpit, and the coverage gates.

Two subsets are worth knowing by name:

- **Architecture** — `HexagonalArchitectureTest` (ArchUnit) enforces the dependency rule between layers. It is a unit
  test, so `./mvnw test` already runs it.
- **Resilience** — `valadir-boot/src/test/java/com/valadir/resilience` pauses Postgres, Redis or SMTP mid-request and
  asserts the [Failure Policy](docs/security-architecture.md#failure-policy): which operations fail closed, which
  degrade, and what the client is told in each case.

### Running a subset

```bash
# One unit test class (or a path pattern) plus the modules it depends on.
./mvnw -am -pl valadir-domain test -Dtest=EmailTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true

# Only the resilience ITs.
./mvnw -am -pl valadir-boot verify -Dit.test='com/valadir/resilience/*IT' \
  -Dfailsafe.failIfNoSpecifiedTests=false -Djacoco.skip=true
```

`failIfNoSpecifiedTests=false` is what lets the upstream modules build without matching the filter, and
`-Djacoco.skip=true` is part of the recipe: the gates below are written for the whole suite, so a filtered run would
fail them for a reason that is not a regression.

### Coverage (JaCoCo)

Coverage is attributed by test type via two JaCoCo exec files and two gates: a strict **unit gate**
(`domain`/`application`/`common`, at the `test` phase, unit coverage only) and a **union gate**
(`infrastructure-*`/`boot`, at the `verify` phase). The unit gate cannot be satisfied by integration
tests, so a business-logic gap can never be masked by an E2E.

Both gates demand 100% of branches and instructions; only the composition roots (`*Wiring`) and the
entry point are exempt, because every method in them is a `new` with no decision of its own.
`infrastructure-security` is the single exception at 99% of instructions, for a `NoSuchAlgorithmException`
catch that no JVM reaches.

Per module, the HTML reports land in `target/site/jacoco` (unit) and `target/site/jacoco-it` (integration).

### Mutation (PIT)

Mutation testing is opt-in — PIT and JaCoCo cannot instrument the same run — and covers the business logic plus the pure
logic of the adapters. The threshold is 95%, and no Docker is involved:

```bash
./mvnw -Pmutation -am -pl valadir-common,valadir-domain,valadir-application,valadir-infrastructure-security,valadir-infrastructure-notifications,valadir-infrastructure-web test
```

The report of each module lands in `target/pit-reports/index.html`. `infrastructure-persistence` and `boot` are out:
their code earns its coverage from integration tests against the real dependency, which PIT does not run.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
