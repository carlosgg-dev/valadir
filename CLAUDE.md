# Valadir

Maven multi-module, Java 21, Spring Boot. No Gradle build exists.

## Build & verify

- Always `./mvnw`, never `mvn`.
- `./mvnw test` is the gate for a change, not `clean compile`. It runs the unit tests, the ArchUnit
  dependency rule and the JaCoCo unit gate — a layering violation compiles cleanly and fails here.
- `./mvnw verify` adds the Testcontainers ITs (Postgres, Redis, Mailpit). Needs Docker.
- Coverage is gated at 100% branch and instruction, so new code lands with the tests that cover
  every branch or the build goes red.
- A filtered run needs `-Djacoco.skip=true`: the gates are written for the whole suite and a subset
  fails them for a reason that is not a regression.
- Before adding a library, check the parent `pom.xml` for an equivalent.

## Layout

- Package roots do not mirror module names: `com.valadir.web`, `com.valadir.persistence`,
  `com.valadir.security`, `com.valadir.notifications` — no `infrastructure.` prefix.
- Wiring is manual. `domain` and `application` carry no Spring annotation at all; a new use case is
  a plain class plus a `@Bean` method in `ApplicationWiring`, never a `@Service`.
- Tests: `*Test` is Surefire, unit, no Docker. `*IT` is Failsafe, `verify` phase, Testcontainers.

## LSP — jdtls

- Before proposing a fix, check if jdtls already surfaces the diagnostic.
- After structural changes (new class, moved package, renamed symbol), remind me to run
  `./mvnw clean compile` to resync the workspace.
- Never suppress a jdtls warning without explaining the trade-off.

## docs/

`security-architecture.md` is committed and changes when a guarantee changes. Any other file there
is an untracked working plan — never stage it.
