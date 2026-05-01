# Testing Standards — Java / Spring Boot

## Pyramid and scope

| Layer | Scope | Target coverage |
|---|---|---|
| Unit | Single class, no Spring context | All business logic, >90% |
| Integration (slice) | One layer in isolation with partial Spring context | Controllers, repositories |
| Integration (full) | Full context with real infrastructure | Critical user flows |
| Contract | API producer-consumer compatibility | External API boundaries |

Read `pom.xml` or `build.gradle` to identify the testing stack available before proposing any test.
Never introduce a new testing library without discussing it first.
Do not default to full integration tests for everything — use the narrowest slice
that gives meaningful coverage.

## Unit tests

- One test class per production class. Mirror the package structure under `src/test/`.
- Test class naming: `[Subject]Test` (e.g., `AccountTest`, `EmailServiceTest`).
- Test method naming: `methodName_stateUnderTest_expectedBehavior`
  (e.g. `save_duplicateEmail_throwsConflict`).
- Mock dependencies at the unit boundary. Never load a Spring context in a unit test.
- Assert behavior and outcomes, not internal implementation details.
- Prefer `assertThat` (AssertJ) over `assertEquals` / `assertTrue` — it produces more readable
  failure messages and allows fluent chaining (e.g. `assertThat(result).isNotNull().hasSize(3)`).
- Cover: happy path, boundary values, and all failure branches.

## Integration tests — slice

- Test each layer in isolation using the project's available slice annotations or equivalent.
- Prefer constructor-injected test doubles over `@MockBean` — `@MockBean` forces a Spring context reload and slows the suite.
- Never load the full application context in a slice test.

## Integration tests — full stack

- Use Testcontainers for real infrastructure (DB, queues, caches). Never rely on a shared external environment.
- Keep full integration tests separated from unit tests in the build lifecycle.
- Run full integration tests in CI, not on every local compile.

## Test data
- Use builder methods or Object Mother factories for complex fixtures. Centralize them in dedicated classes under `src/test/` (e.g. `UserTestData`, `OrderBuilder`).
- Never duplicate fixture construction inline across multiple tests — a single change should only require one update.

## General rules

- Tests are production code: apply the same Clean Code standards.
- Extract repeated primitive values to local variables when the same data appears multiple
  times in a test. Two objects with the same primitive value are not the same data
  (e.g. `new Id("5")` and `new Year("5")` are unrelated despite sharing `"5"`).
  Extracting to a variable makes the intent clear and reduces the cost of changing the value.
- No commented-out tests. If a test is skipped, document why explicitly.
- Avoid `Thread.sleep` for async assertions — use the project's waiting mechanism.
- Each test must be independent and idempotent — no shared mutable state between tests.
- If a bug is fixed, write a regression test that would have caught it before merging.