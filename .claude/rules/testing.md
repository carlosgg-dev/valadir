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

## Coverage

- A coverage target is only meaningful if it is measured: prefer wiring measurement
  and threshold enforcement into the standard test build over manual, ad-hoc checks.
- Branch coverage is the primary signal — 100% line or instruction coverage can still
  hide untested decision outcomes (`if`, `switch`, `&&`, `||`).
- Never write a test whose only purpose is to raise the coverage number.
- Exempt from the target: DI wiring and configuration classes, the application entry
  point, framework-managed accessors, and compiler-generated synthetic branches
  (e.g. the implicit default of a switch over a sealed type).
- A mutation threshold, once green, is never lowered to make a build pass. A surviving mutant is
  killed by a new test or discussed on its merits, never accommodated (`mutationThreshold`, `pom.xml`).
- Adapters earn their coverage from integration tests against the real dependency, never from
  mock-based unit tests that only assert the interaction — a mocked adapter test stays green when
  the query, the key or the script is wrong.

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
- Run full integration tests in the `verify` phase, not on every local compile.
- An IT class asserts only what belongs to its flow. Before adding a case, ask which regression
  turns it red while every other test stays green; if another class already owns that answer, the
  case does not belong here (the functional suite disables per-IP rate limiting because one
  enforcement class owns it).
- Share the HTTP vocabulary through a base class split into three families, never blended:

| Family | Rule |
|---|---|
| Steps | one call to the system, no assertions — the same step drives the success and the failure case |
| Readers | pull one value out of a response or a test double; when it is absent, throw with the status in the message |
| Preconditions | composed from steps, and they do assert — a broken precondition must fail on the spot, not surface as a null further down |

- Pull a helper up to the base class on its first reuse, not before, and never nest one helper call
  inside another.

## Test data
- Use builder methods or Object Mother factories for complex fixtures. Centralize them in dedicated classes under `src/test/` (e.g. `UserTestData`, `OrderBuilder`).
- Never duplicate fixture construction inline across multiple tests — a single change should only require one update.
- An invalid fixture must be invalid for the reason under test, not for an earlier one: a value that
  trips input validation never reaches the branch it was meant to exercise. A wrong one-time code is
  a well-formed code with one digit substituted, not a malformed string. A body carrying a null
  field needs a mutable map — `Map.of` rejects the null before the request is ever sent. A null goes
  through the type the code takes (`GivenName.from(null)`), not as a bare `null`, so the test
  exercises that type's validation instead of an ambiguous call.

## General rules

- Tests are production code: apply the same Clean Code standards.
- Every test must be able to fail due to a plausible regression in our own code.
  Never assert language or framework behavior: exception message/cause storage,
  record accessors, trivial field assignment. Custom logic in constructors
  (validation, defensive copies, defaults) is our code and must be tested.
- Assert that something is single-use by replaying it, never by claiming it "expires eventually":
  a second call with the same code must be rejected.
- Put two entities in play whenever a regression could resolve the wrong one — a flow scoped to an
  account is tested with two accounts, and the second must come out untouched.
- Extract repeated primitive values to local variables when the same data appears multiple
  times in a test. Two objects with the same primitive value are not the same data
  (e.g. `new Id("5")` and `new Year("5")` are unrelated despite sharing `"5"`).
  Extracting to a variable makes the intent clear and reduces the cost of changing the value.
- No commented-out tests. If a test is skipped, document why explicitly.
- Avoid `Thread.sleep` for async assertions — use the project's waiting mechanism.
- Each test must be independent and idempotent — no shared mutable state between tests.
- If a bug is fixed, write a regression test that would have caught it before merging.