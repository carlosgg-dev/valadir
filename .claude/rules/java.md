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
- **The exception type is decided by who supplies the value, not by the layer that rejects it:**
  - **Request value, rejected at runtime** — `DomainException` (or `ApplicationException`) carrying
    an `ErrorCode`. The failure is an expected business outcome and the caller needs a code it can
    act on: `Email`, `PlainOtp` and every other value object built from a request work this way.
  - **Configuration value, rejected at startup** — `IllegalArgumentException`. There is no caller to
    answer; a misconfigured deployment must fail to start rather than serve traffic. `LoginLockoutPolicy`,
    `RateLimitProperties`, `CaptchaProperties` and `AsyncProperties` work this way.
  - **Value produced by our own code** — `IllegalArgumentException` / `IllegalStateException` as a
    programming guard, unreachable by definition unless the code is wrong (`TokenFingerprint` accepts
    only the hex its own factory produces). Guards like these belong behind a factory that makes the
    invalid value impossible, never on a constructor a request can reach directly.
- **Never let a configuration-style guard sit on a request path.** The central handler has no
  `IllegalArgumentException` branch, so one reaching a request falls through to the `Exception`
  catch-all and answers `internal_server_error` — a bad input reported as a 500 instead of a 400.
  Moving such a class onto a request path means converting its guards to an `ErrorCode` first.
