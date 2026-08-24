# Engineering Log

This is the stable active engineering log. Add concise, dated entries for material completed work, decisions, evidence, and open follow-up.

## 2026-08-23 — ARGONAUT-UI-001: Experiment Console

**Type:** Frontend implementation
**Input:** ARGONAUT-MILESTONE-UC001-001 (comparative synthesis), all five framework HTTP contracts
**Output:** Vue 3 + Vite + TypeScript experiment console in `argonaut-ui/`; CORS configuration added to all five framework Spring Boot modules; engineering report.
**Key decisions:** Framework endpoints sourced from runtime `public/config/frameworks.json`; one shared `ArgonautClient`; metric accuracy caveat displayed in comparison table; `runAll()` uses `Promise.allSettled()` for fault isolation.
**Validation:** Vitest — 19 tests, 0 failures; `git diff --check` clean.
**Status:** Complete

## 2026-08-23 — UC-ARTICLE-001: Five Ways to Build the Same AI Capability

**Type:** Knowledge article / publication artifact
**Input:** UC-001 comparative synthesis, five repository-local implementations and tests, and the shared experiment, knowledge, evidence, trace, metric, and verification contracts.
**Output:** Canonical [Markdown article](../knowledge/articles/UC-ARTICLE-001-five-ways-to-build-the-same-ai-capability.md) and shareable [PDF publication](../knowledge/articles/UC-ARTICLE-001-five-ways-to-build-the-same-ai-capability.pdf). The article reframes engineering evidence for senior engineers and architects: five control models, deliberate deterministic/stochastic boundaries, non-universal tool semantics, observability/testability implications, bounded contextual selection guidance, and UC-002 capability needs.
**Evidence boundary:** Claims are confined to repository-local UC-001 evidence. Framework selection guidance is explicitly architectural inference; unexercised framework capabilities are documented as limitations rather than findings.
**Validation:** Code snippets checked against adapter sources; PDF generated with title, abstract, code samples, diagrams, comparison table, attribution, and page numbering. Eight rendered page images visually inspected; no clipping or table/code overflow observed. PDF text extraction confirmed non-empty text on every page. `git diff --check` clean.
**Status:** Complete

## 2026-08-23 — ARGONAUT-MILESTONE-UC001-001: Five-framework comparative synthesis

**Type:** Comparative architecture review / engineering milestone
**Input:** UC-001 implementations and tests for Spring AI, LangChain4j, LangGraph4j, Koog, and Embabel; `ControlledLocalEvidenceContract`; shared experiment, knowledge, evidence, trace, and metric contracts; local engineering reports and log entries only.
**Output:** [Five-framework comparative synthesis](agents/reviews/ARGONAUT-MILESTONE-UC001-001-five-framework-comparative-synthesis.md), including control/retrieval/state/tool/observability/testability comparisons, bounded selection guidance, neutral-versus-premature abstraction findings, and UC-002 capability implications.
**Finding:** UC-001's contract is portable, but orchestration is not. Stable Argonaut concerns are controlled knowledge access, observable experiment results, evidence provenance, and normalized lifecycle semantics. `Tool`, `Agent`, `Graph`, planner, model-call, and agent-loop abstractions remain premature because the five implementations assign control differently; Embabel does not use an LLM tool at all in UC-001.
**Validation:** Source/report inspection completed; `git diff --check` clean; `mvn verify` — BUILD SUCCESS, 156 tests (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j + 8 Embabel + 12 Koog), 0 failures.
**Status:** Complete

## 2026-08-23 — ARGONAUT-EMBABEL-001: Embabel Controlled Local Evidence Implementation

**Type:** Implementation / Framework module
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), local Embabel 1.5.0 framework evidence (`libs-code/embabel-agent-examples/`)
**Output:** `argonaut-embabel` module (Embabel 1.5.0 + Spring Boot 4.1.0): `ControlledLocalEvidenceAgent` (`@Agent` with single `@AchievesGoal @Action`; planner-driven retrieval calling `KnowledgeRepository` directly; `context.ai().withDefaultLlm().createObject()` for synthesis); typed domain records (`EvidenceQuestion`, `AnswerText`, `EvidenceAnswer`); `EmbabelConfig` (`KnowledgeRepository` bean); REST controllers (`/api/health`, `/api/about`, `/api/experiment/run`, port 8084); `TcUc001EmbabelTest` (8 tests: 4 TC-UC-001 contract tests + 4 action shape tests using `FakeOperationContext`)
**Key architectural finding — Embabel's natural model for RAG:**
- Embabel separates retrieval (planner/Java, deterministic) from synthesis (LLM). The `@AchievesGoal @Action` calls `KnowledgeRepository` directly and uses `context.ai()` only for answer synthesis. There is no LLM-driven tool-call loop — the planner controls retrieval.
- This is the primary architectural observation Argonaut sought: Embabel's GOAP planner replaces the ReAct tool loop used by Spring AI, LangChain4j, LangGraph4j, and Koog.
**Testing strategy:** `FakeOperationContext` (from `embabel-agent-test`) is passed directly to the `@Action` method (actions are plain Java methods). Real `KnowledgeRepository` handles retrieval; LLM synthesis is mocked via `expectResponse()`. No Spring context or `AgentPlatform` required in tests.
**Surefire reporting note:** JUnit 5 + Surefire 3.5.2 rolls the outer class tests under `$ActionShapeTests` in the console summary (shows "Tests run: 8" nested, "Tests run: 0" outer). Surefire XML confirms all 8 tests ran correctly.
**Report:** [ARGONAUT-EMBABEL-001-embabel-controlled-local-evidence.md](agents/reports/ARGONAUT-EMBABEL-001-embabel-controlled-local-evidence.md)
**Validation:** `git diff --check` clean. `mvn verify` — BUILD SUCCESS, 156 tests (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j + 12 Koog + 8 Embabel), 0 failures.
**Status:** Complete

## 2026-08-13 — ARGONAUT-001: Define Project Scope and Create Root README

**Status:** Complete

**Summary:** Established project scope and documentation. Inspected the full OSK workspace (AGENTS.md, OSK.md, PROJECT.md, all installed skills). Wrote the root README covering: project intent, frameworks under comparison, what stays constant vs. what varies, intended module layout and ports, common HTTP contract, experiment console, controlled evidence, observability model, Docker Compose and OpenRouter configuration approach, documentation map, and results/article path. Updated docs/PROJECT.md from OSK placeholder template to project-specific context.

**Evidence:** `README.md`, `docs/PROJECT.md` — both reviewed for link validity, credential absence, and OSK documentation convention compliance.

**Open direction:** Mission definition (docs/knowledge/), Maven module structure, framework implementations, Vue UI, Docker Compose, and OpenRouter integration all deferred to follow-up tasks.

## 2026-08-22 — ARGONAUT-KOOG-001: Koog Controlled Local Evidence Implementation

**Type:** Implementation / Framework module
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), LangGraph4j reference implementation
**Output:** `argonaut-koog` module (Koog 1.1.1 + Spring Boot 4.1.0, Kotlin 2.3.21): `KnowledgeTools` (Kotlin `ToolSet` with `@Tool`/`@LLMDescription`-annotated methods, per-run constructor injection); `ControlledLocalEvidenceAgent` (`AIAgent` factory + implicit `singleRunStrategy` ReAct loop; `handleEvents` block with `onLLMCallStarting`/`onLLMCallCompleted`; `runBlocking` bridge from Spring MVC); REST controllers (`/api/health`, `/api/about`, `/api/experiment/run`, port 8085); `MockPromptExecutor` (extends `MultiLLMPromptExecutor(emptyMap())`, overrides `resolveModel` + `ResolvedModel`-based `execute`; 3-stage state machine: search → read → answer); `TcUc001KoogTest` (12 tests all passing against shared `ControlledLocalEvidenceContract`)
**Key differences from prior frameworks:**
- Kotlin-native (all other services are Java); requires `allopen/spring` plugin and `-java-parameters` Kotlin compiler arg
- `AIAgent.run()` and `AIAgent.close()` are Kotlin suspend functions — `runBlocking` bridge required from synchronous Spring MVC handlers
- `handleEvents { onLLMCallStarting { } onLLMCallCompleted { } }` provides accurate model-call observation (same as LangGraph4j)
- `ContextualPromptExecutor` (agent-internal) calls `executor.resolveModel()` before `execute()` — mock must override BOTH the `ResolvedModel`-based `execute` AND `resolveModel`, not just the `LLModel`-based `execute`
**Dependency fix:** `koog-agents-jvm:1.1.1` pulls `kotlin-reflect:2.3.21` transitively; `kotlin-reflect:2.3.21` references `KotlinGenericDeclaration` from `kotlin-stdlib:2.3.21`. Pinning `kotlin.version=2.3.21` (not 2.3.10) in the koog module eliminates `NoClassDefFoundError` at test runtime.
**Report:** [ARGONAUT-KOOG-001-koog-controlled-local-evidence.md](agents/reports/ARGONAUT-KOOG-001-koog-controlled-local-evidence.md)
**Validation:** `mvn verify` — BUILD SUCCESS, 148 tests (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j + 12 Koog), 0 failures.
**Status:** Complete

## 2026-08-22 — ARGONAUT-010: LangGraph4j Controlled Local Evidence Implementation

**Type:** Implementation / Framework module
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), Spring AI and LangChain4j reference implementations, LangGraph4j 1.8.24 source (cloned locally at `libs-code/langgraph4j`)
**Output:** `argonaut-langgraph4j` module (LangGraph4j 1.8.24 + LangChain4j 1.18.1 + Spring Boot 4.1.0): `KnowledgeTools` (per-run `@Tool`-annotated POJO, same tool descriptions/metadata as prior frameworks; constructed per run with injected observer and reads accumulator captured in tools-node closure); `ControlledLocalEvidenceAgent` (explicit ReAct graph: `START → agent → tools → agent → ... → END`; `ChatModel.chat(ChatRequest)` called directly in agent node; `AtomicInteger` model-call counter in node closure; `MODEL_CALL_STARTED/COMPLETED` events emitted — first framework to observe these); REST controllers (`/api/health`, `/api/about`, `/api/experiment/run`); `MockChatModel` (4-stage state machine: search → search+read → read×2 → answer, implementing `doChat(ChatRequest)` not `chat()`); `TcUc001LangGraph4jTest` (12 tests, all passing against shared `ControlledLocalEvidenceContract`)
**Key differences from prior frameworks:**
- Graph loop is explicit (`addEdge`, `addConditionalEdges`) vs. hidden inside `ChatClient` (Spring AI) and `AiServices` (LangChain4j)
- `MODEL_CALL_STARTED/COMPLETED` events are now observable; `modelCalls` metric is accurate (Spring AI hardcoded 1; LangChain4j reported 0)
- Per-run state injected via node closure capture rather than `ToolContext` side-channel (Spring AI) or per-run `AiServices.build()` (LangChain4j)
- `LC4jStateSerializer<>(MessagesState::new)` required — LangChain4j message types are not Java-Serializable; default `ObjectStreamStateSerializer` throws `NotSerializableException` at state-clone time
**Dependency fix:** `langchain4j-open-ai:1.0.0` pulled `langchain4j-core:1.0.0` transitively (nearest-wins), shadowing the 1.18.1 needed by `langgraph4j-langchain4j`. Fixed by adding explicit `langchain4j-core:1.18.1` direct dependency; `InvocationContext` (used by `LC4jToolService`) only exists in ≥ 1.18.x.
**Pre-existing fix:** `argonaut-embabel/pom.xml` was missing Spring Boot 4.1.0 BOM in `dependencyManagement`, leaving `spring-boot-starter-test` and `opentelemetry-exporter-zipkin` without versions and blocking all `mvn verify` runs. Fixed in this task.
**Report:** [ARGONAUT-010-langgraph4j-controlled-local-evidence.md](agents/reports/ARGONAUT-010-langgraph4j-controlled-local-evidence.md)
**Validation:** `git diff --check` clean. `mvn verify` — BUILD SUCCESS, 136 tests (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j), 0 failures.
**Status:** Complete

## 2026-08-21 — ARGONAUT-009: LangChain4j Controlled Local Evidence Implementation

**Type:** Implementation / Framework module
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), Spring AI reference implementation
**Output:** `ControlledLocalEvidencePrompt` in `argonaut-core` (common system prompt extracted from Spring AI; Spring AI agent updated to reference it; 4 prompt tests added); `argonaut-langchain4j` module (LangChain4j 0.36.2 + Spring Boot 4.1.0): `KnowledgeTools` (per-run `@Tool`-annotated POJO with constructor-injected observer; mirrors Spring AI capability names and trace metadata); `ControlledLocalEvidenceAgent` (`AiServices`-based orchestration, per-run tool instantiation, same 9 trace events, `modelCalls` reported as 0 due to `AiServices` loop opacity); REST controllers (`/api/health`, `/api/about`, `/api/experiment/run`); `MockChatLanguageModel` (3-stage state machine: 2 searches → 3 reads → answer); `TcUc001LangChain4jTest` (9 tests all passing against shared `ControlledLocalEvidenceContract`)
**Key difference from Spring AI:** LangChain4j `@Tool` methods receive no side-channel `ToolContext`; per-run state (observer, reads accumulator) is injected at `KnowledgeTools` construction. `AiServices` is rebuilt per run to carry the per-run tool instance.
**Metric ambiguity:** `modelCalls` = 0 (not reliably measurable through `AiServices`) vs Spring AI's hard-coded 1. Documented in engineering report; not resolved in this task.
**Report:** [ARGONAUT-009-langchain4j-controlled-local-evidence.md](agents/reports/ARGONAUT-009-langchain4j-controlled-local-evidence.md)
**Validation:** `git diff --check` clean. `mvn verify` — BUILD SUCCESS, 124 tests (111 core + 4 Spring AI + 9 LangChain4j), 0 failures.
**Status:** Complete

## 2026-08-16 — ARGONAUT-009: Spring AI Module Skeleton

**Type:** Implementation / Framework module
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`, `ExperimentExecutor`, `TraceEventMetadata`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`)
**Output:** `argonaut-spring-ai` module (Spring AI 2.0.0 + Spring Boot 4.1.0): `KnowledgeTool` (`@Tool`-annotated `searchKnowledge`/`readDocument` with `ToolContext` injection for per-run observer and reads); `ControlledLocalEvidenceAgent` (ChatClient orchestration loop, full trace event emission, evidence assembly); REST controllers (`/api/health`, `/api/about`, `/api/experiment/run`); `MockRagChatModel` (3-stage state machine simulating search→read→answer); `TcUc001SpringAiTest` (4 tests, all passing); `MarkdownCorpusLoader.fromClasspath()` hardened to support jar-protocol URIs (zip FileSystem) in addition to exploded-directory URIs
**Fixes:** `-parameters` compiler flag added to `argonaut-spring-ai/pom.xml` so `MethodToolCallback` binds JSON arguments to named parameters; `MarkdownCorpusLoader.fromClasspath()` fixed to open a zip `FileSystem` for `jar:` URIs so `mvn verify` (where `argonaut-core` is already packaged) succeeds
**Validation:** `mvn verify` — BUILD SUCCESS, 111 tests (107 core + 4 Spring AI), 0 failures.
**Status:** Complete

## 2026-08-14 — ARGONAUT-008B: TC-UC-001 Baseline Harness and Anti-Gaming Assertions

**Type:** Implementation / Verification harness
**Input:** ARGONAUT-008A (authoritative Markdown corpus, `LocalKnowledgeRepository.withDemoCorpus()`), ARGONAUT-007 adversarial review (H3 TC-UC-001 harness, H4 anti-gaming assertions, H7 non-blank finalAnswer)
**Output:** `ExperimentExecutor` (`@FunctionalInterface`), `TraceEventMetadata` (metadata key constants), `ControlledLocalEvidenceContract` (TC-UC-001: 9 assertions SC1–SC9 including SC4 anti-gaming); `ExperimentResult.completed()` hardened to reject blank `finalAnswer`; `ReferenceExecutor` (test fixture proving contract is passable); `TcUc001Test` (21 tests: 5 positive, 16 negative)
**Report:** [ARGONAUT-008B-tc-uc-001-baseline-harness-and-anti-gaming.md](agents/reports/ARGONAUT-008B-tc-uc-001-baseline-harness-and-anti-gaming.md)
**Validation:** `git diff --check` clean. `mvn verify` — BUILD SUCCESS, 107 tests, 0 failures.
**Status:** Complete

## 2026-08-14 — ARGONAUT-008A: Single Authoritative Corpus and Corpus Invariants

**Type:** Hardening / Invariant enforcement
**Input:** ARGONAUT-007 adversarial review (dual corpus drift C1, golden query gaps H5, duplicate-ID gap H6, blank-body gap), ARGONAUT-006 Markdown corpus and loader
**Output:** `LocalKnowledgeCorpus.demo()` now loads from Markdown (Option A); corpus files moved from `src/test/resources` to `src/main/resources`; duplicate-ID rejection in `LocalKnowledgeCorpus.of()`; blank-body rejection in `MarkdownCorpusLoader`; `CorpusInvariantsTest` (17 tests: stable IDs/titles, demo() parity, duplicate-ID, blank-body, 4 golden queries, noisy-query characterization)
**Report:** [ARGONAUT-008A-single-authoritative-corpus-and-invariants.md](agents/reports/ARGONAUT-008A-single-authoritative-corpus-and-invariants.md)
**Validation:** `git diff --check` clean. `mvn verify` — BUILD SUCCESS, 86 tests, 0 failures.
**Status:** Complete

## 2026-08-13 — ARGONAUT-006: Controlled Markdown Knowledge Corpus

**Type:** Implementation / Knowledge materialization
**Input:** ARGONAUT-004 `LocalKnowledgeCorpus`/`LocalKnowledgeDocument`, ARGONAUT-005 use case document (5 stable source IDs), `osk-knowledge-curator` durability criteria
**Output:** 5 Markdown corpus documents under `docs/knowledge/corpus/controlled-local-evidence/`; test fixture copies under `argonaut-core/src/test/resources/knowledge/controlled-local-evidence/`; `MarkdownCorpusLoader` + `MarkdownParseException` in `dev.jsanca.argonaut.core.knowledge.local`; 13 new tests
**Report:** [ARGONAUT-006-controlled-markdown-knowledge-corpus.md](agents/reports/ARGONAUT-006-controlled-markdown-knowledge-corpus.md)
**Validation:** `git diff --check` clean. `mvn verify` — BUILD SUCCESS, 69 tests, 0 failures. `LocalKnowledgeCorpus.demo()` unchanged.
**Status:** Complete

## 2026-08-13 — ARGONAUT-005: Controlled Local Evidence RAG Use Case

**Type:** Knowledge curation / Documentation
**Input:** ARGONAUT-003 trace contract, ARGONAUT-004 corpus (5 demo documents, stable source IDs), `docs/knowledge/common-contract.md`, `docs/OSK.md` information model, `osk-knowledge-curator` skill
**Output:** `docs/knowledge/use-cases/controlled-local-evidence-rag.md` — experiment question, actors, preconditions, main flow, expected evidence (exp-001 primary), trace expectations, 9 success criteria, out-of-scope list, future test case note (TC-UC-001). Link added to `common-contract.md`.
**Report:** [ARGONAUT-005-controlled-local-evidence-rag-use-case.md](agents/reports/ARGONAUT-005-controlled-local-evidence-rag-use-case.md)
**Validation:** `git diff --check` clean. `mvn verify` — BUILD SUCCESS, 56 tests, 0 failures.
**Status:** Complete

## 2026-08-13 — ARGONAUT-004: Controlled Local Knowledge Repository

**Type:** Implementation
**Input:** ARGONAUT-003 knowledge contracts (`KnowledgeRepository`, `KnowledgeSearchRequest/Response`, `DocumentReference`, `DocumentContent`)
**Output:** `LocalKnowledgeDocument`, `LocalKnowledgeCorpus` (with `demo()` corpus of 5 synthetic documents), `LocalKnowledgeRepository` (lexical search + read), `KnowledgeSourceException`
**Report:** [ARGONAUT-004-controlled-local-knowledge-repository.md](agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md)
**Validation:** `mvn verify` — BUILD SUCCESS, 56 tests, 0 failures. Zero runtime dependencies added to `argonaut-core`.
**Status:** Complete

## 2026-08-13 — ARGONAUT-003: Observable Agentic RAG Contracts in argonaut-core

**Status:** Complete

**Summary:** Defined the first version of the shared observable contract in `argonaut-core`. Added 23 source files across 7 packages and 8 test files containing 35 unit tests. All types are plain Java records, enums, and interfaces — no framework dependencies introduced. Boundary verified: argonaut-core depends only on JUnit 5 (test scope). JUnit 5.11.4 BOM added to parent pom; JUnit Jupiter added as test-scoped dependency to argonaut-core only.

**Evidence:** `mvn verify` — BUILD SUCCESS, 35 tests run, 0 failures, 0 errors. `argonaut-core` has no runtime dependencies and no orchestration concepts.

**Contracts delivered:**
- `experiment`: ExperimentRequest (validation), ExperimentResult (completed/failed factories), ArgonautInfo, RunStatus
- `evidence`: Evidence (finite score validation), EvidenceKind
- `knowledge`: KnowledgeRepository interface, KnowledgeSearchRequest (validation), KnowledgeSearchResponse, KnowledgeSearchResult, DocumentReference, DocumentContent
- `trace`: ExecutionTrace, ExecutionEvent, ExecutionEventType (19 event types)
- `observability`: ExecutionObserver, NoopExecutionObserver (singleton), InMemoryExecutionObserver (thread-safe), CompositeExecutionObserver
- `metrics`: ExecutionMetrics with Builder for framework-side accumulation
- `error`: ArgonautError, ArgonautErrorCode

**Documentation:** `docs/knowledge/common-contract.md` created (explains design rationale, Vue consumption model, knowledge abstraction, and deferred exporters). `argonaut-core/README.md` updated with package overview and boundary table.

**Open direction:** Framework-specific implementations (Spring Boot wiring, HTTP controllers, KnowledgeRepository adapters) deferred to follow-up tasks per implementation module.

## 2026-08-13 — ARGONAUT-002: Maven Multi-Module Skeleton

**Status:** Complete

**Summary:** Created the Java 25 Maven multi-module project skeleton. Parent pom (`argonaut-parent`, `dev.jsanca.argonaut`, `0.1.0-SNAPSHOT`) configured with `release 25`, maven-compiler-plugin 3.13.0, maven-surefire-plugin 3.5.2, maven-failsafe-plugin 3.5.2, and reproducible-build timestamp. Five Java modules created: `argonaut-core`, `argonaut-spring-ai`, `argonaut-langchain4j`, `argonaut-langgraph4j`, `argonaut-embabel`. Each framework module declares a dependency on `argonaut-core`. `argonaut-ui` created as a sibling directory (not in the Maven reactor) with a README placeholder. `.gitignore` augmented with `.env`, `target/`, IDE, macOS, and frontend entries. `.env.example` created with OpenRouter and port placeholders.

**Evidence:** `mvn verify` — BUILD SUCCESS, all 6 modules (parent + 5 Java), Java 25 release, 2.3 seconds total. Zero tests (no test code yet — skeleton only). No framework dependencies introduced.

**Open direction:** Framework dependencies (Spring AI, LangChain4j, LangGraph4j, Embabel), common HTTP contract in argonaut-core, Spring Boot scaffolding per module, Vue UI, Docker Compose, and OpenRouter integration all deferred to follow-up tasks.

## YYYY-MM-DD — <Work Item>

**Status:** <Complete | In progress | Blocked>

**Summary:** <What happened.>

**Evidence:** <Links to reports, reviews, commands, or artifacts.>

**Open direction:** <Remaining uncertainty or next action; use None when none.>
## 2026-08-23 — ARGONAUT-INFRA-001: Containerized UC-001 Laboratory

**Type:** Infrastructure / deployment implementation
**Input:** UC-001 framework services, Vue experiment console, common HTTP contract
**Output:** Docker Compose laboratory for Spring AI, LangChain4j, LangGraph4j, Embabel, Koog, and nginx-hosted UI; reusable Java 25 backend Dockerfile; UI production image; same-origin framework routing; runtime registry; secret-free environment template; concise local-running documentation
**Report:** [ARGONAUT-INFRA-001-containerized-uc001-lab.md](agents/reports/ARGONAUT-INFRA-001-containerized-uc001-lab.md)
**Validation:** `mvn verify -q` passed (156 tests); `npm test` passed (19 tests); `npm run build` passed; `docker compose config --quiet` passed. Full Docker image build retry in progress after an interrupted Maven Central Kotlin compiler download.
**Status:** In progress — awaiting Docker image and stack verification
