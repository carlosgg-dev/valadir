# Mockito Standards

## Setup
- Always use `@ExtendWith(MockitoExtension.class)` at the class level.
- Never use manual `mock()` calls in `@BeforeEach` — use annotations instead.

## Annotations
- `@Mock` for dependencies that need to be fully controlled.
- `@Spy` for real instances where only specific methods need to be stubbed.
- `@InjectMocks` for the class under test — Mockito will inject `@Mock` and `@Spy` fields automatically.
- `@Captor` for `ArgumentCaptor` fields — never instantiate them manually inside test methods.

## Stubbing
- Use BDDMockito: `given(...).willReturn(...)` over `when(...).thenReturn(...)` —
  it reads naturally with the Given/When/Then structure of the test.
- Use `willDoNothing().given(...)` and `willThrow(...).given(...)` for void methods or spies.
- Never stub methods that are not exercised by the test — it signals the test is doing too much.
- Never mix `given()` and `when()` styles within the same project.

## Verification
- Use BDDMockito: `then(...).should(...)` over `verify(...)` for consistency with the stubbing style.
- Verify interactions only when the interaction itself is the behavior under test.
- A failure test verifies `never` on every side effect after the failure point whose occurrence
  would itself be the bug: writes, revocations, counter resets, notifications. Queries and pure
  computations (a lookup, a hash, a validation) are left out — calling them does no harm, and
  asserting it only pins the implementation.
- Calls before the failure point are never verified in a failure test: strict stubbing fails on an
  unused stub, and the asserted exception already proves the flow got there. A void collaborator
  whose removal nothing else would detect is verified once, in the happy path.
- Skip the `never` when the failure assertion already makes the call unreachable: a malformed input
  rejected with its own `ErrorCode` cannot have reached a lookup that would have answered a different one.
- Avoid verifying every mock call — it couples tests to implementation details.
- Use `then(...).shouldHaveNoMoreInteractions()` sparingly and only when strict interaction control is required.

## Static mocking
- Use `MockedStatic` only as a last resort — static dependencies are a design smell.
- Always open `MockedStatic` in a try-with-resources block to avoid leaking the mock across tests.

## Argument matching
- Prefer exact argument matching over `any()` when the argument value is meaningful to the test.
- Use `ArgumentCaptor` to assert on complex objects passed to a mock instead of overusing `any()`.
- Reserve `any()` for cases where the argument is genuinely irrelevant to what is being tested:
  - Verifying a service was never called: `then(service).should(never()).save(any())`.
  - Stubbing an exception regardless of input: `given(repo.save(any())).willThrow(RuntimeException.class)`.
- Never use `any()` in stubs or verifications where the argument value is part of the behavior under test —
  it hides regressions and makes the test meaningless as a specification.