# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Start with [docs/PROJECT.md](docs/PROJECT.md) to understand the project, then read [docs/OSK.md](docs/OSK.md) before creating or changing workspace documentation.

Use canonical project documentation as the authority. Do not place shared project knowledge exclusively in this file.

<!-- OSK:BEGIN -->

## OSK Workspace

Read:

- `docs/PROJECT.md`
- `docs/OSK.md`

<!-- OSK:END -->

## Tool-Specific Instructions

Add only instructions required by Claude here. Keep shared project guidance in `docs/`.

---

## Build and Test

```bash
# Full build and test (run from repo root)
mvn verify

# Compile only — fast syntax check
mvn compile

# Run tests for one module
mvn test -pl argonaut-core

# Run a single test class
mvn test -pl argonaut-core -Dtest=ExperimentRequestTest

# Skip tests during a build
mvn verify -DskipTests
```

Requires Java 25 (Temurin 25 recommended via SDKMAN). The build will fail explicitly on JDK < 25 — do not attempt to downgrade the `maven.compiler.release` property.

`argonaut-ui` is **not** part of the Maven reactor. It is a sibling directory for the future Vue.js frontend.

Unit tests in framework modules use mock chat models — no real LLM call or network is required. To run a framework service locally against a real model, set the env vars from `.env.example` (`OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, optionally `OPENROUTER_BASE_URL`).

---

## Architecture

Argonaut is a Maven multi-module project (`dev.jsanca.argonaut`, version `0.1.0-SNAPSHOT`). Each framework runs as an independent service. All services will eventually expose the same HTTP contract.

### Module layout

```
argonaut-core         — shared contracts only (no framework deps)
argonaut-spring-ai    — Spring AI service, port 8081
argonaut-langchain4j  — LangChain4j service, port 8082
argonaut-langgraph4j  — LangGraph4j service, port 8083
argonaut-embabel      — Embabel service, port 8084
argonaut-ui/          — Vue.js experiment console (not a Maven module)
```

### The non-negotiable boundary rule

`argonaut-core` must never contain:
- Framework dependencies (Spring AI, LangChain4j, LangGraph4j, Embabel)
- Infrastructure dependencies (Lucene, Qdrant, Langfuse, LangSmith, OpenRouter)
- Orchestration concepts: `Agent`, `Graph`, `Node`, `Planner`, `Workflow`

The rule in one line: **share contracts, tools, fixtures, and observability structures; do not share orchestration semantics.**

### `argonaut-core` package map

| Package | Key types |
| --- | --- |
| `experiment` | `ExperimentRequest`, `ExperimentResult`, `ArgonautInfo`, `RunStatus` |
| `evidence` | `Evidence`, `EvidenceKind` |
| `knowledge` | `KnowledgeRepository` (interface), request/response/result records, `DocumentReference/Content` |
| `trace` | `ExecutionTrace`, `ExecutionEvent`, `ExecutionEventType` (19 event types) |
| `observability` | `ExecutionObserver`, `NoopExecutionObserver`, `InMemoryExecutionObserver`, `CompositeExecutionObserver` |
| `metrics` | `ExecutionMetrics`, `ExecutionMetrics.Builder` |
| `error` | `ArgonautError`, `ArgonautErrorCode` |

### Common HTTP contract (intended, not yet implemented)

All framework services will implement:
```
GET  /api/health
GET  /api/about       → ArgonautInfo
POST /api/experiment/run  → ExperimentResult
```

### Execution observability pattern

Framework modules use `InMemoryExecutionObserver` to collect `ExecutionEvent` objects during a run, then call `.toTrace()` at the end to produce the `ExecutionTrace` included in `ExperimentResult`. `CompositeExecutionObserver` allows future external exporters (Langfuse, LangSmith, OpenTelemetry) to be added without changing framework wiring.

### Controlled knowledge corpus

Corpus documents live at `argonaut-core/src/main/resources/knowledge/controlled-local-evidence/`. Each file is a Markdown document with a required YAML frontmatter block:

```
---
id: exp-001
title: Some Title
topic: optional-topic
version: 1
---

Body content here.
```

`id` and `title` are required. `LocalKnowledgeCorpus.demo()` / `LocalKnowledgeRepository.withDemoCorpus()` loads all `.md` files from this classpath directory. Framework config classes use `LocalKnowledgeRepository.withDemoCorpus()` to expose a `KnowledgeRepository` bean. The primary required evidence document is `exp-001`.

### Framework implementation pattern

Each new framework module follows the Spring AI module as its reference implementation:

1. **`pom.xml`**: Must set `<parameters>true</parameters>` in `maven-compiler-plugin`. Without this flag, `@Tool`-annotated methods silently receive `null` for all arguments. Run `mvn clean` after adding the flag.

2. **Config class**: Provides a `KnowledgeRepository` bean via `LocalKnowledgeRepository.withDemoCorpus()` and wires the framework's chat client.

3. **Tool wrapper**: Wraps `KnowledgeRepository` and emits required `ExecutionEvent` objects. Events for `KNOWLEDGE_SEARCH_COMPLETED` and `DOCUMENT_READ_COMPLETED`/`EVIDENCE_RETRIEVED` must populate `TraceEventMetadata` keys (`SOURCE_ID`, `SOURCE_IDS`, `QUERY`, `EVIDENCE_ID`). These keys are checked by the SC4 anti-gaming assertion in `ControlledLocalEvidenceContract`.

4. **Agent/Service**: Creates a fresh `InMemoryExecutionObserver` per run, emits `RUN_STARTED` first and `RUN_COMPLETED` last, calls `observer.toTrace()`, and returns `ExperimentResult.completed(...)` or `ExperimentResult.failed(...)`.

5. **REST controllers**: Implement the three shared endpoints (`/api/health`, `/api/about`, `/api/experiment/run`).

6. **TC-UC-001 test**: Framework tests must verify against `ControlledLocalEvidenceContract` using a mock chat model (no live model call):
   ```java
   ControlledLocalEvidenceContract.verify(request -> myAgent.run(request));
   ```
   The contract enforces nine success criteria (SC1–SC9) covering status, final answer, trace event presence and ordering, STARTED/COMPLETED pairing, metrics consistency, absence of errors, evidence including `exp-001`, and evidence traceability.

---

## OSK Installed Skills

The following skills are available and activated by invocation — use them for their specific domains:

- [Architecture Review](.osk/skills/osk-architecture-review/SKILL.md) — structural/boundary assessment
- [Architecture Boundary Review](.osk/skills/osk-boundary-review/SKILL.md) — dependency direction and placement
- [Engineering Reporting](.osk/skills/osk-engineering-reporting/SKILL.md) — implementation/review/checkpoint reports
- [Execution Observability](.osk/skills/osk-execution-observability/SKILL.md) — progress checkpoints and mode selection
- [Verification Engineering](.osk/skills/osk-verification-engineering/SKILL.md) — traceable test cases and verification evidence

Additional skills in `.claude/skills/` cover Java 25, Spring Boot 4.1, Spring Boot service patterns, AI backend engineering, concurrency, quality gates, security hardening, testing, and the full Vue ecosystem.

---

## Documentation Placement

| Content type | Location |
| --- | --- |
| Durable project concepts, contract rationale | `docs/knowledge/` |
| Task reports, experiment runs, validation evidence | `docs/engineering/` |
| Architectural decisions and rationale | `docs/adr/` |
| Committed direction / future ideas | `docs/roadmap/` |
| Active engineering record | `docs/engineering/ENGINEERING_LOG.md` |

Update `ENGINEERING_LOG.md` with a dated entry after completing any non-trivial task.
