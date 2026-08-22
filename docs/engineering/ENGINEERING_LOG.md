# Engineering Log

This is the stable active engineering log. Add concise, dated entries for material completed work, decisions, evidence, and open follow-up.

## 2026-08-13 — ARGONAUT-001: Define Project Scope and Create Root README

**Status:** Complete

**Summary:** Established project scope and documentation. Inspected the full OSK workspace (AGENTS.md, OSK.md, PROJECT.md, all installed skills). Wrote the root README covering: project intent, frameworks under comparison, what stays constant vs. what varies, intended module layout and ports, common HTTP contract, experiment console, controlled evidence, observability model, Docker Compose and OpenRouter configuration approach, documentation map, and results/article path. Updated docs/PROJECT.md from OSK placeholder template to project-specific context.

**Evidence:** `README.md`, `docs/PROJECT.md` — both reviewed for link validity, credential absence, and OSK documentation convention compliance.

**Open direction:** Mission definition (docs/knowledge/), Maven module structure, framework implementations, Vue UI, Docker Compose, and OpenRouter integration all deferred to follow-up tasks.

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
