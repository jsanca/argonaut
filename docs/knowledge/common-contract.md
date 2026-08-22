# Argonaut Common Contract

## Why a common contract exists

Argonaut compares four agentic AI frameworks executing the same mission under controlled conditions. Without a shared observable contract, comparing results would require understanding each framework's internal model. The common contract makes comparison possible without framework knowledge.

The Vue experiment console needs to display results, traces, evidence, and metrics from Spring AI, LangChain4j, LangGraph4j, and Embabel using the same UI components. It must be able to receive a response without knowing which framework produced it.

## What belongs in `argonaut-core`

Types that express the observable experiment contract and are genuinely common across all implementations:

- **Request/response DTOs** — `ExperimentRequest`, `ExperimentResult`, `ArgonautInfo`
- **Evidence structures** — `Evidence`, `EvidenceKind`
- **Execution trace structures** — `ExecutionTrace`, `ExecutionEvent`, `ExecutionEventType`
- **Metrics structures** — `ExecutionMetrics`
- **Error structures** — `ArgonautError`, `ArgonautErrorCode`
- **Knowledge-access contracts** — `KnowledgeRepository`, `KnowledgeSearchRequest/Response`, `DocumentReference`, `DocumentContent`
- **Observer contracts and implementations** — `ExecutionObserver`, `NoopExecutionObserver`, `InMemoryExecutionObserver`, `CompositeExecutionObserver`
- **Run lifecycle** — `RunStatus`

## What must not belong in `argonaut-core`

Orchestration semantics. The core module must not become a shared agent framework. The following concepts must not appear here:

| Forbidden type | Why |
| --- | --- |
| `Agent` | Framework-specific concept |
| `Graph` | Framework-specific concept (LangGraph4j) |
| `Node` | Framework-specific concept |
| `Planner` | Framework-specific concept |
| `Workflow` | Framework-specific concept |
| `FrameworkRunner` | Would homogenize what the experiment intends to compare |
| Any framework dependency | Spring AI, LangChain4j, LangGraph4j, Embabel must not be imported |

The rule: **share contracts, tools, fixtures, and observability structures; do not share orchestration semantics.**

## How the contract supports Vue

The Vue experiment console consumes `ExperimentResult` from each service. It can:

- render `finalAnswer` as the experiment conclusion;
- render each `Evidence` item as a UI card with source, title, excerpt, and score;
- render the `ExecutionTrace` as a timeline, using `ExecutionEvent` timestamps and types;
- display `ExecutionMetrics` as a comparable row (duration, model calls, tool calls, searches, reads);
- display `ArgonautError` items if the run failed.

None of this requires the UI to understand which framework ran.

## Why knowledge search/read is abstracted rather than embeddings/vector stores

The observable contract should express what the experiment does, not how it does it. From the perspective of the experiment and the UI, the relevant behavior is:

- *find relevant information* — expressed as `KnowledgeRepository.search()`
- *read controlled source material* — expressed as `KnowledgeRepository.read()`

Whether the underlying mechanism uses in-memory text search, Lucene BM25, Qdrant dense vectors, OpenSearch, pgvector, or a framework-native store is an implementation detail of each framework module. Exposing `EmbeddingModel`, `VectorStore`, or `VectorIndex` in core would lock all implementations into a specific retrieval paradigm and leak infrastructure concerns into the observable contract.

## How each framework adapts the conceptual capabilities

Each framework module implements `KnowledgeRepository` using its own idiomatic abstractions:

| Framework | Typical adaptation |
| --- | --- |
| Spring AI | Tool or bean registered with the chat model |
| LangChain4j | `@Tool`-annotated method on an AI service |
| LangGraph4j | Node that calls a tool, or a tool within a node |
| Embabel | Action or framework-equivalent capability |

The `KnowledgeRepository` interface ensures all four adapters expose the same capability and produce evidence in the same structure, regardless of internal mechanism.

## Why Langfuse and LangSmith are future adapters, not core dependencies

Langfuse and LangSmith are observability *exporters* — they receive traces from an external agent run and render them in their own UI. Argonaut already captures normalized execution events through `ExecutionObserver`. Exporting those events to an external platform is an adapter concern, not a core concern.

Adding Langfuse or LangSmith as a core dependency would:
- couple all framework modules to a specific telemetry vendor;
- require credentials at compile time;
- conflate the experiment's observable contract with a specific SaaS tool.

Future adapters may include:

- `LangfuseExecutionObserver` — forwards events to Langfuse via its SDK;
- `LangSmithExecutionObserver` — forwards events to LangSmith;
- `OpenTelemetryExecutionObserver` — emits spans to an OTLP collector;
- `StructuredLogExecutionObserver` — writes events as structured JSON log lines.

These belong in opt-in infrastructure modules, not in `argonaut-core`.

## The controlled local knowledge repository

`LocalKnowledgeRepository` (in `dev.jsanca.argonaut.core.knowledge.local`) is the first concrete `KnowledgeRepository` implementation. It is deterministic, requires no external services, and is the default evidence source for all four framework implementations.

**What it does:**
- Holds a `LocalKnowledgeCorpus` — an immutable in-memory collection of `LocalKnowledgeDocument` records.
- Scores documents by lexical term overlap: title matches are weighted 2×, content-only matches 1×. Score is normalized to [0.0, 1.0].
- Returns results in descending score order with stable tie-breaking by `sourceId`.
- Omits zero-score documents. Returns empty results when no terms match.
- Reads documents by `DocumentReference.sourceId()`, throwing `KnowledgeSourceException` when not found.

**What it does not do:** semantic search, embedding similarity, BM25 IDF weighting, fuzzy matching, or stop-word filtering. It is intentionally simple so that retrieval behavior is predictable and comparable across frameworks.

**Authoritative corpus:** The Markdown files under `docs/knowledge/corpus/controlled-local-evidence/` are the single authoritative source for the controlled local evidence corpus. All five documents (`exp-001`, `rag-001`, `obs-001`, `vt-001`, `sc-001`) are maintained there as human-readable, version-controlled Markdown with lightweight frontmatter (`id`, `title`, `topic`, `version`).

**How framework modules obtain the corpus:**

```java
// Preferred — loads the authoritative Markdown corpus
KnowledgeRepository knowledge = LocalKnowledgeRepository.withDemoCorpus();

// Equivalent — loads the same corpus directly
LocalKnowledgeCorpus corpus = LocalKnowledgeCorpus.demo();

// Lower-level — loads from a specific classpath or directory path
LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(
        LocalKnowledgeCorpus.CONTROLLED_CORPUS_CLASSPATH);
```

**`LocalKnowledgeCorpus.demo()`** now loads from the Markdown files on the classpath at `knowledge/controlled-local-evidence`. It is the public convenience API that all framework implementations must use. It is backed by the authoritative Markdown corpus, not a separate programmatic string. Corpus drift between `demo()` and the Markdown files is treated as a test failure: `CorpusInvariantsTest.demo_returns_the_markdown_backed_corpus_with_same_body_content` asserts their equivalence.

**Corpus drift is a test failure.** Framework implementations must use `LocalKnowledgeRepository.withDemoCorpus()` or `LocalKnowledgeCorpus.demo()`. Using different corpus content would violate the "same evidence" principle and invalidate framework comparisons. Duplicate document IDs within a corpus are rejected at construction time by `LocalKnowledgeCorpus.of()`.

**Usage by framework modules:**
```java
// All four frameworks must use this same call — do not substitute a different corpus
KnowledgeRepository knowledge = LocalKnowledgeRepository.withDemoCorpus();
// wire as a tool / bean / action / node per the framework's idiom
```

## Future knowledge repository implementations

The first experiment uses a controlled local evidence corpus. Future iterations may introduce:

- `InMemoryKnowledgeRepository` — for tests and the initial controlled corpus;
- `LuceneKnowledgeRepository` — lexical and/or hybrid search;
- `QdrantKnowledgeRepository` — dense vector search;
- `OpenSearchKnowledgeRepository` — full-text and dense vector;
- framework-native implementations using Spring AI's vector store or LangChain4j's embedding store.

All of these adapt `KnowledgeRepository` from the appropriate infrastructure module. The core contract remains unchanged.

## TC-UC-001 Harness

`ControlledLocalEvidenceContract` (in `dev.jsanca.argonaut.core.testing`) is the executable form of the first use case. Framework test classes plug their implementation behind `ExperimentExecutor` and call `verify()`.

The harness enforces nine assertions including an **anti-gaming check** (SC4): every `Evidence.sourceId` must appear in the metadata of a `DOCUMENT_READ_COMPLETED` or `EVIDENCE_RETRIEVED` trace event. A framework cannot satisfy the contract by hard-coding evidence items.

`TraceEventMetadata` defines the standard keys framework implementations must use when recording events (e.g. `sourceId` on `DOCUMENT_READ_COMPLETED`, `sourceIds` on `KNOWLEDGE_SEARCH_COMPLETED`).

`ExperimentResult.completed()` now rejects blank `finalAnswer` at construction time (H7).

## Use cases

The first defined experiment use case documents the behavioral target that all framework implementations must satisfy:

- [Controlled Local Evidence RAG](use-cases/controlled-local-evidence-rag.md) — the experiment question, expected evidence, trace expectations, and success criteria for the initial controlled-corpus agentic RAG run.
