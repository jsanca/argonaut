# Eval: Java Spring Boot 4.1

## Prompt

Upgrade a Spring Boot 4.0.5 / Java 21 service to 4.1.x. Then add a gRPC health check endpoint using Boot 4.1 gRPC support, following existing package structure. Include an integration test.

## Expected Agent Behavior

- Verifies 4.0 baseline before bumping to 4.1
- Uses Boot 4.1 BOM-managed gRPC dependencies
- Adds test coverage for the new gRPC surface
- Runs build and reports test command output
- Does not downgrade to Boot 3.x or skip Security re-validation

## Failure Signals

- Attempts gRPC on Boot 4.0 without noting 4.1 requirement
- Adds gRPC without tests or TLS consideration
- Uses hand-rolled dependency versions outside the BOM
- Breaks existing REST endpoints without mention
