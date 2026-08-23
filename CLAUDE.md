# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Read [docs/OSK.md](docs/OSK.md) before creating or changing workspace documentation. `docs/PROJECT.md` is stale (pre-implementation era) — use `docs/engineering/ENGINEERING_LOG.md` for current state.

Use canonical project documentation as the authority. Do not place shared project knowledge exclusively in this file.

<!-- OSK:BEGIN -->
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
argonaut-koog         — Koog service, port 8085 (Kotlin)
argonaut-ui/          — Vue.js experiment console (not a Maven module)
libs-code/            — locally-cloned framework sources (e.g. langgraph4j); not built by Maven
```

### Implementation status

| Module | Framework version | Status | Tests |
| --- | --- | --- | --- |
| `argonaut-spring-ai` | Spring AI 2.0.0 + Spring Boot 4.1.0 | Complete | 4 (TC-UC-001) |
| `argonaut-langchain4j` | LangChain4j 0.36.2 + Spring Boot 4.1.0 | Complete | 9 (TC-UC-001) |
| `argonaut-langgraph4j` | LangGraph4j 1.8.24 + LangChain4j 1.18.1 + Spring Boot 4.1.0 | Complete | 12 (TC-UC-001) |
| `argonaut-koog` | Koog 1.1.1 + Spring Boot 4.1.0 (Kotlin 2.3.21) | Complete | 12 (TC-UC-001) |
| `argonaut-embabel` | Embabel 1.5.0 + Spring Boot 4.1.0 | Complete | 8 (TC-UC-001) |

A clean `mvn verify` produces **156 tests** (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j + 12 Koog + 8 Embabel).

### The non-negotiable boundary rule

`argonaut-core` must never contain:
- Framework dependencies (Spring AI, LangChain4j, LangGraph4j, Embabel)
- Infrastructure dependencies (Lucene, Qdrant, Langfuse, LangSmith, OpenRouter)
- Orchestration concepts: `Agent`, `Graph`, `Node`, `Planner`, `Workflow`

The rule in one line: **share contracts, tools, fixtures, and observability structures; do not share orchestration semantics.**

### `argonaut-core` package map

| Package | Key types |
| --- | --- |
| `experiment` | `ExperimentRequest`, `ExperimentResult`, `ArgonautInfo`, `RunStatus`, `ControlledLocalEvidencePrompt` |
| `evidence` | `Evidence`, `EvidenceKind` |
| `knowledge` | `KnowledgeRepository` (interface), request/response/result records, `DocumentReference/Content`, `KnowledgeSourceException` |
| `knowledge.local` | `LocalKnowledgeRepository`, `LocalKnowledgeCorpus`, `LocalKnowledgeDocument`, `MarkdownCorpusLoader` |
| `trace` | `ExecutionTrace`, `ExecutionEvent`, `ExecutionEventType` (19 event types) |
| `observability` | `ExecutionObserver`, `NoopExecutionObserver`, `InMemoryExecutionObserver`, `CompositeExecutionObserver` |
| `metrics` | `ExecutionMetrics`, `ExecutionMetrics.Builder` |
| `error` | `ArgonautError`, `ArgonautErrorCode` |
| `testing` | `ControlledLocalEvidenceContract`, `ExperimentExecutor`, `TraceEventMetadata` |

### Common HTTP contract

Implemented in `argonaut-spring-ai`, `argonaut-langchain4j`, `argonaut-langgraph4j`, and `argonaut-koog`; pending for `argonaut-embabel`.

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

### Per-framework implementation differences

These are key differences discovered during implementation — not in the general pattern below.

| Concern | Spring AI | LangChain4j | LangGraph4j | Koog | Embabel |
| --- | --- | --- | --- | --- | --- |
| Tool loop | Hidden in `ChatClient` | Hidden in `AiServices` | Explicit graph edges | Hidden in `singleRunStrategy` | **No LLM tool loop** — planner drives retrieval via Java |
| Per-run state injection | `ToolContext` side-channel | Constructor + per-run `AiServices.build()` | Node closure capture | Constructor + per-run `AIAgent` | Local vars in `@Action` method |
| `modelCalls` accuracy | Hard-coded `1` | `0` (loop opaque) | Accurate (`AtomicInteger`) | Accurate (`AtomicInteger` via `handleEvents`) | Hard-coded `1` (synthesis only) |
| `MODEL_CALL_STARTED/COMPLETED` | Not observable | Not observable | First to emit | Also emits via `handleEvents` | Not emitted (synthesis only, no loop) |
| Mock interface | `ChatModel` | `ChatLanguageModel` | `ChatModel` (LangChain4j 1.18.x) | `PromptExecutor` (Koog, via `MultiLLMPromptExecutor`) | `FakeOperationContext` (Embabel test) |
| Language | Java | Java | Java | Kotlin | Java |

**LangGraph4j-specific gotchas:**
- `LC4jStateSerializer<>(MessagesState::new)` is required. The default `ObjectStreamStateSerializer` throws `NotSerializableException` at state-clone time because LangChain4j message types are not Java-Serializable.
- `langchain4j-open-ai:1.0.0` (pulled transitively) downgrades `langchain4j-core` to `1.0.0`, breaking `InvocationContext` (only in ≥ 1.18.x). Fix: add explicit `langchain4j-core:1.18.1` direct dependency.

**Koog-specific gotchas:**
- `AIAgent.run()` and `AIAgent.close()` are Kotlin suspend functions — use `runBlocking { }` from synchronous Spring MVC handlers.
- `koog-agents-jvm:1.1.1` pulls `kotlin-reflect:2.3.21` transitively. Pin `kotlin.version=2.3.21` (not 2.3.10) to avoid `NoClassDefFoundError: kotlin/jvm/internal/KotlinGenericDeclaration`.
- `MockPromptExecutor` must override BOTH `resolveModel()` AND the `ResolvedModel`-based `execute()` overload. `ContextualPromptExecutor` (agent-internal) calls `resolveModel()` before `execute()` — overriding only the `LLModel`-based `execute()` won't intercept actual calls.
- `OpenRouterLLMClient(apiKey)` is a Kotlin top-level factory function with `@file:JvmName("OpenRouterClientFactory")`. In Kotlin, call it directly: `OpenRouterLLMClient(apiKey = apiKey)`.
- Kotlin `allopen/spring` plugin and `-java-parameters` compiler arg are both required.

**Embabel-specific implementation findings:**
- Uses a custom Maven repository: `https://repo.embabel.com/artifactory/libs-release`.
- Minimal dependency: `embabel-agent-starter-openai` (provides platform + OpenAI-compatible LLM). `spring-boot-starter-web` added for REST. No `@EnableAgents` annotation needed — discovered via Spring Boot autoconfiguration.
- **Planner-driven RAG**: Unlike other frameworks where the LLM drives tool calls (ReAct), Embabel's natural pattern separates retrieval (planner/Java code, deterministic) from synthesis (LLM). The `@AchievesGoal @Action` calls `KnowledgeRepository` directly and uses `context.ai().createObject()` only for synthesis.
- **Testing without AgentPlatform**: `FakeOperationContext` (from `embabel-agent-test`) is passed directly to `@Action` methods — actions are plain Java methods callable without the full Embabel platform. Real `KnowledgeRepository` runs during tests; only the LLM synthesis step is mocked.
- **Surefire reporting quirk**: JUnit 5 + Surefire 3.5.2 reports outer class `@Test` methods under the `$ActionShapeTests` nested class label (totaling 8 instead of the expected split of 4+4). The surefire XML confirms all 8 tests ran correctly.
- Production invocation: `AgentInvocation.builder(agentPlatform).build(EvidenceAnswer.class).invoke(question)` drives the planner from a REST controller.

---

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
