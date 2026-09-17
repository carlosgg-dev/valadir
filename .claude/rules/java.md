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
- **Explicit `Locale` whenever case or format depends on one:**
  - `Locale.ROOT` — the result is consumed by code: cache and Redis keys, identifiers, protocol
    values, and any normalization that precedes a comparison. It must not depend on where the JVM runs.
  - The user's locale — only for text a person reads, and only where that locale arrives as an
    explicit input rather than being picked up from the environment.
  - Never call the no-arg `toLowerCase()` / `toUpperCase()`, `String.format`, or a `DateTimeFormatter`
    built without one: they all read `Locale.getDefault()`, which the deployment sets, so the same
    input produces different output depending on where the JVM runs. Case folding is the sharpest
    edge — in some locales an uppercase `I` does not fold to `i` — which is enough to key a rate
    limit under a different Redis key, and enough to make a case-insensitive check accept input it
    was written to reject.
  - A regression test for this sets the default locale and restores it in a `finally`. Put the
    uppercase letter on one side of the comparison only: when both sides carry it they fold alike
    and the test passes against the very bug it was written for.
- **`var`** for local variables when the type is unambiguous without navigation: instantiation with `new` where variable and constructor type are identical, or when the type is immediately obvious from the right-hand side. Never use `var` when the type requires navigating to another file to be understood.
- **Factory method naming conventions:**
  - `from` — construction from parameters or a specific source (`User.from(id, name)`, `User.fromSafetyData(...)`).
  - `reconstitute` — rebuilding a domain object from raw persisted data (`User.reconstitute(id, email, hashedPassword, role)`).
  - `new` + context — construction with a clear domain purpose (`User.newProfile(...)`, `User.newAnonymous(...)`).
  - `create` — belongs in **services only**, signals orchestration and side effects.
  - `build` — reserved for test helper methods (`buildValidUser()`, `buildExpiredAccount()`).
  - `of` / `xxxOf(x)` — `x` is the concrete source transformed or derived into the result (hash,
    decode, field extraction). The result is computed *from* `x` (`TokenFingerprint.of(token)`,
    `fingerprintOf(token)`, `jtiOf(accessToken)`).
  - `xxxFor(key)` — `key` is an opaque identifier used to look up or compute a value that
    conceptually belongs to it (a DB/Redis lookup, or a rule mapping the key to a domain value).
    The method name states the domain concept returned, not necessarily the literal Java type
    (`lockoutFor(failureCount)`, `passwordResetOtpFor(email)`). Never use `of` when the argument is
    a lookup key rather than the source being transformed, and never use `for` when the argument is
    the source itself.

## Transactions
- **The transaction boundary is the driven adapter here, not the service.** The usual rule is the
  service layer, and it cannot apply to this project: the application layer is framework-free by
  construction (`domain_and_application_are_framework_free`), so the persistence adapter is the
  innermost place `@Transactional` can live. It goes on a method that writes more than once, or on
  one whose `@Modifying` query needs a transaction Spring Data does not open for it. Never on a
  controller. An adapter IT runs with `propagation = NOT_SUPPORTED` so a missing annotation fails
  there rather than passing on a transaction the test supplied.
- Use `@Transactional(readOnly = true)` for a read-only operation the adapter composes itself. A
  single query through a Spring Data repository already runs in one — `SimpleJpaRepository` is
  annotated `@Transactional(readOnly = true)` at class level — so annotating every lookup restates
  what the framework already guarantees.
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
- **One validation rule across request, domain and schema, applied in degrees:**
  - **The value object is the guarantee.** It is at least as strict as request validation and never
    leans on it, so a value reaching the domain by any other path meets the same rules.
  - **Bean Validation on a DTO is a fast-fail, and it is what names the field.** A use case stops at
    the first value object that refuses and names nothing, so the annotation is what lets a form mark
    the input that was wrong. Where it validates, it uses the domain's numbers and never states a rule
    the domain does not have. A rule the domain owns that no built-in constraint can express (password
    composition, invisible characters, unpaired surrogates) stays in the value object alone: an
    annotation restating it is a second copy that drifts.
  - **The DTO sees the raw value, so it is stricter on surrounding whitespace.** `@Email` rejects
    `"a@b.cd "`, which `Email` trims and accepts. That is the one accepted exception to the rule above,
    and it is deliberate: the domain normalizes as a convenience for any caller, not as a promise the
    API makes to a client that pads its input.
  - **The schema accepts every value the domain accepts** (length, `NOT NULL`, encoding) and mirrors
    structure, not business rules: no `CHECK` repeating the domain, no `DEFAULT` deciding a value the
    domain decides. Hibernate writes every mapped column, so such a `DEFAULT` never applies from the
    application and only decides in silence for a manual `INSERT`. `ddl-auto: validate` checks
    tables, columns and types against the entities; nullability and lengths are mirrored by hand.
  - **Shape before policy.** Shape (presence, maximum length, format) may be answered before
    authentication. Policy (password composition, personal data) applies only where the value is
    chosen, and after re-authentication in a flow that has one: a presented password is checked by
    shape only, so a wrong one answers 401 and counts as a failed attempt.
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
- **The error body is our own envelope, not `ProblemDetail`.** Every failure answers a `code`, plus an
  `errors` array when Bean Validation named the fields, and nothing else. RFC 9457 would add four fields
  that restate the status line and one, `detail`, that carries prose — the framework's own sentence, or a
  message bundle interpolated for the request's locale — which is the leak the `code` exists to avoid;
  `code` itself is an extension property there either way. Four of the error paths are written outside MVC
  (authentication, access denied, rate limiting, infrastructure failure) and no framework format reaches
  them, so one envelope every path can produce is worth more than a standard one only some can.
