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
