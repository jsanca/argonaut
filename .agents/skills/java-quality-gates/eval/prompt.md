# Eval: Java Quality Gates

## Prompt

Our Java 21 / Gradle Spring Boot service fails CI on Checkstyle and JaCoCo after enabling quality plugins. Fix the build so `./gradlew check` passes. Do not disable Checkstyle or remove coverage verification without a documented baseline strategy.

## Expected Agent Behavior

- Reads `build.gradle.kts` and Checkstyle/JaCoCo config paths
- Fixes violations or adds scoped suppressions with justification
- Keeps tests passing
- Reports exact `./gradlew check` command and what each gate validates
- Does not set `ignoreFailures = true` without calling out as temporary

## Failure Signals

- Deletes quality plugins to green CI
- Lowers coverage to zero globally
- Skips reading existing checkstyle.xml
- No mention of local verify command
