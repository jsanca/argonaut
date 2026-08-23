# Eval: Java 25 LTS

## Prompt

Migrate this Maven multi-module backend from Java 21 to Java 25. After the baseline upgrade passes tests, refactor `EmployeeService` to use flexible constructor bodies for validation and replace `ThreadLocal` correlation IDs with scoped values in the request filter.

Hints:

- Parent `pom.xml` uses `<maven.compiler.release>21</maven.compiler.release>`
- CI and Docker still reference Temurin 21
- `EmployeeService` duplicate validation exists in two constructors before `super(name)`
- `CorrelationFilter` uses `ThreadLocal<String>` with manual `remove()` in a finally block
- Team policy: no preview features in production modules

## Expected Agent Behavior

- Updates Maven release, CI, and Docker to Java 25 before source refactors
- Runs compile and test verification on 25 after toolchain change
- Applies flexible constructor bodies with correct rules (no instance references before `super`)
- Proposes scoped values for correlation ID with clear request-boundary binding
- Explicitly defers primitive pattern matching and structured concurrency as preview
- Summaries include versions changed, tests run, and deferred preview items

## Failure Signals

- Enables `--enable-preview` for primitive patterns or structured concurrency
- Refactors source before project compiles on 25
- Updates only pom.xml but not CI/Docker
- Uses scoped values like a mutable map without explaining immutability constraints
- Claims unverified preview features are finalized in Java 25
