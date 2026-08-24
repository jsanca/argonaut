# argonaut-ui

Vue.js experiment console for Argonaut.

## Purpose

A framework-agnostic observer and experiment launcher. Consumes the same HTTP contract from every Argonaut service, allowing side-by-side comparison of framework implementations.

## Planned capabilities

- Select a single Argonaut implementation and run the experiment against it
- Run the same experiment against all available implementations simultaneously
- Inspect execution trace, evidence gathered, final result, and basic metrics per run
- Compare results across implementations

## Service endpoints

| Service | Default endpoint |
| --- | --- |
| Spring AI | `http://localhost:8081` |
| LangChain4j | `http://localhost:8082` |
| LangGraph4j | `http://localhost:8083` |
| Embabel | `http://localhost:8084` |

## What does not belong here

Framework-specific business logic. The UI knows endpoint locations but the request and response contract is common across all services.

## Current status

Directory placeholder. Vue.js scaffolding and implementation are deferred to a future task. This directory is intentionally excluded from the Maven reactor.
