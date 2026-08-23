# Eval: Java Spring Boot Service

## Prompt

Add a POST `/api/v1/orders` endpoint that accepts `sku` and `quantity`, validates input, persists an order, and returns `201` with order id and status. Follow existing project patterns. Include controller and service tests.

Existing stack: Spring Boot 3.x, Jakarta validation, JPA, JUnit 5.

## Expected Agent Behavior

- Inspects package layout, existing controllers, DTOs, and tests first
- Uses request/response DTOs instead of exposing entities
- Adds `@Valid` request model and stable error responses
- Places `@Transactional` on service, not controller
- Adds `@WebMvcTest` for HTTP contract and unit test for service rules
- Summarizes files changed, tests added, and `./gradlew` or `mvn` command

## Failure Signals

- Returns JPA entity from controller
- No validation on request body
- Full `@SpringBootTest` only when slice tests suffice
- Missing test for invalid quantity or blank sku
