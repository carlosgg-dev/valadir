# Java Engineering Standards

## Tooling & Build
- Always use the project wrapper: `./mvnw` or `./gradlew`. Never call `mvn` or `gradle` directly.
- Verify structural or logic changes with `./mvnw clean compile` (or `./gradlew build`) before closing a task.
- Before adding a new library, check `pom.xml` / `build.gradle` for an existing equivalent to avoid classpath conflicts.

## LSP — jdtls
- Before proposing a fix, check if jdtls already surfaces the diagnostic.
- After structural changes (new class, moved package, renamed symbol), remind me to run
  `./mvnw clean compile` to resync the workspace.
- Never suppress a jdtls warning without explaining the trade-off.

## Language Idioms
- **Constructor Injection only.** Never `@Autowired` on fields — it breaks immutability and testability.
- **`final` by default** on fields, local variables, and parameters to minimize side effects.
- **Java Records** for DTOs, events, and value objects (immutable data carriers).
- **`Optional<T>`** for values that may be absent. Never return `null` for optional results.
- **Streams and Lambdas** for collection processing. Prefer readability over clever one-liners.
- **Factory method naming conventions:**
  - `from` for construction from parameters or a specific source/context. Use `from` alone
    when no context name adds meaning (e.g. `User.from(id, name, givenName)`), or append
    the context when it does (e.g. `User.fromSafetyData(...)`, `Money.fromSpain(...)`).
  - `reconstitute` for rebuilding a domain object from raw persisted data — signals the
    boundary crossing from infrastructure data into a valid domain object
    (e.g. `User.reconstitute(id, email, hashedPassword, role)`).
  - `new` + semantic context for construction with a clear domain purpose
    (e.g. `User.newProfile(...)`, `Account.newAnonymous(...)`).
  - `create` belongs in **services**, never in domain objects — it signals orchestration
    and side effects (e.g. `accountService.createWithProfileSafety(...)`).
  - `build` is reserved for test helper methods that construct objects for test setup
    (e.g. `buildValidUser()`, `buildExpiredAccount()`).
- **try-with-resources** for every `AutoCloseable`. No exceptions.

## Architecture
- Adapt the structure (Layered, Hexagonal, Feature-based) to the project's scale and complexity.
  Discuss and agree on the approach **before** scaffolding — see `rules/architecture.md`.
- **Composition over Inheritance** for extensibility.
- **DTOs at system boundaries**: never expose persistence entities or internal domain state to external APIs.
  Use mappers (manual or generated) at the boundary layer.
- Protect the domain. External concerns (HTTP, persistence, messaging) must not leak inward.

## Validation & Error Handling
- **Fail-fast at the entry point**: apply JSR-303 / Bean Validation on all external inputs.
- Use a centralized exception handling mechanism (e.g. `@ControllerAdvice`).