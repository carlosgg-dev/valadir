# Java & Spring Standards

## Language Idioms
- **Constructor injection only.** Never `@Autowired` on fields — it breaks immutability and testability.
- **`final` on fields**: always — guarantees immutability after construction and correct visibility under the Java Memory Model.
- **`final` on local variables and parameters**: avoid by default. Use only when it prevents a real ambiguity: a parameter that must not be reassigned in a complex method, or a local variable whose immutability is non-obvious from context.
- **Java Records** for DTOs, events, and value objects.
- **`Optional<T>`** for absent values. Never return `null` for optional results. Never use `Optional` as a method parameter.
- **`java.time` for all time values**:
  - `Duration` — intervals without an anchor: TTLs, timeouts, lockout durations, rate-limit windows.
  - `Instant` — absolute points in time (UTC): `createdAt`, `expiresAt`, event timestamps.
  - `LocalDate` / `LocalDateTime` — dates or date-times without a time zone (business domain dates).
  - `ZonedDateTime` / `OffsetDateTime` — date-times with a time zone (user-facing or API serialization).
  - `long` (epoch seconds/millis) — **only** at infrastructure boundaries where the external protocol requires it: Redis Lua scripts, HTTP headers (`Retry-After`, `X-RateLimit-Reset`), Kafka timestamps.
  - Never use `java.util.Date`, `java.sql.Timestamp`, or raw `long`/`int` fields to represent durations or timestamps anywhere else.
- **`var`** for local variables when the type is unambiguous without navigation: instantiation with `new` where variable and constructor type are identical, or when the type is immediately obvious from the right-hand side. Never use `var` when the type requires navigating to another file to be understood.
- **Factory method naming conventions:**
  - `from` — construction from parameters or a specific source (`User.from(id, name)`, `User.fromSafetyData(...)`).
  - `reconstitute` — rebuilding a domain object from raw persisted data (`User.reconstitute(id, email, hashedPassword, role)`).
  - `new` + context — construction with a clear domain purpose (`User.newProfile(...)`, `User.newAnonymous(...)`).
  - `create` — belongs in **services only**, signals orchestration and side effects.
  - `build` — reserved for test helper methods (`buildValidUser()`, `buildExpiredAccount()`).

## Transactions
- Apply `@Transactional` at the **service layer** only — never on controllers or repository methods.
- Use `@Transactional(readOnly = true)` for read-only operations.
- Never call a `@Transactional` method from within the same class — Spring proxies will not intercept self-invocation.
- Keep transactions short. Never perform external I/O (HTTP calls, file operations) inside a transaction.

## Architecture
- **DTOs at system boundaries**: never expose persistence entities or domain objects to external APIs. Use mappers at the boundary layer.
- Protect the domain. External concerns (HTTP, persistence, messaging) must not leak inward.

## Naming Conventions
- **`Config` suffix** for infrastructure configuration classes (e.g. `SecurityConfig`, `RedisConfig`).
- **`Wiring` suffix** for composition root classes that bind interfaces to implementations (e.g. `UserWiring`, `PaymentWiring`).

Never mix both responsibilities in the same class.

## Validation & Error Handling
- Apply JSR-303 / Bean Validation on all external inputs.
- Use `@ControllerAdvice` for centralized exception handling.
