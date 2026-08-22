# Controlled Local Evidence RAG Use Case

## Purpose

This use case defines the first repeatable Argonaut agentic RAG experiment. It establishes a shared behavioral target that all four framework implementations — Spring AI, LangChain4j, LangGraph4j, and Embabel — must satisfy using the same question, the same controlled local corpus, and the same observable contract.

The use case exists so that:
- the experiment question and its expected evidence are recorded as durable knowledge, separate from any implementation;
- a future task can derive a concrete executable test case directly from this document;
- evaluation of framework results can be grounded in stated expectations rather than post-hoc judgment.

---

## Experiment Question

```
When comparing agentic Java frameworks, why should Argonaut use controlled local evidence
before introducing web search, vector databases, or external observability tools?
```

This question was chosen because:
- it is directly answerable from the controlled local corpus without any internet access or LLM calls;
- it requires multi-step reasoning across more than one document;
- it exercises search, document read, evidence selection, and answer synthesis — the full agentic RAG flow;
- it is stable: the same question produces the same retrievable evidence on every run.

---

## Actors

| Actor | Role |
| --- | --- |
| Human evaluator | Submits the experiment question and evaluates whether the result meets the success criteria |
| Argonaut framework implementation | Spring AI, LangChain4j, LangGraph4j, or Embabel module executing the mission |
| `KnowledgeRepository` | Shared contract for search and document read |
| `LocalKnowledgeRepository` | Concrete in-memory implementation backed by the controlled corpus |
| Controlled local corpus (`LocalKnowledgeCorpus.demo()`) | Five Markdown-backed documents providing all evidence needed to answer the question |
| `ExecutionObserver` / `ExecutionTrace` | Records normalized execution events during the run |
| Future Vue UI consumer | Displays result, evidence cards, trace timeline, and metrics for comparison |

---

## Preconditions

- The controlled local corpus exists and contains the five demo documents with stable source IDs (`vt-001`, `sc-001`, `obs-001`, `rag-001`, `exp-001`).
- A `KnowledgeRepository` instance backed by that corpus is available to the framework implementation.
- An `ExecutionObserver` is wired to record events during execution.
- No internet connection, external model, vector database, or observability exporter is required.
- The framework implementation is configured to use `LocalKnowledgeRepository.withDemoCorpus()`. This is the required corpus source; do not substitute alternative corpus content.

---

## Trigger

A human evaluator or automated test runner submits an `ExperimentRequest` with:

- **`runId`** — a unique identifier for this execution (e.g. `"uc-001-run-1"`)
- **`question`** — the experiment question above
- **`parameters`** — empty or minimal; no framework-specific parameters required for baseline

---

## Main Flow

1. The framework implementation receives the `ExperimentRequest` and records `RUN_STARTED`.
2. The implementation constructs a search query from the question (e.g. "controlled evidence framework comparison").
3. The implementation calls `KnowledgeRepository.search()` and records `KNOWLEDGE_SEARCH_STARTED` / `KNOWLEDGE_SEARCH_COMPLETED`. At least two search queries may be issued to cover different aspects of the question.
4. The implementation identifies the most relevant documents from the search results and calls `KnowledgeRepository.read()` for each one. `DOCUMENT_READ_STARTED` / `DOCUMENT_READ_COMPLETED` are recorded per read.
5. The implementation extracts evidence items from the read documents. `EVIDENCE_RETRIEVED` is recorded for each candidate item; `EVIDENCE_SELECTED` for items that inform the answer.
6. The implementation constructs an answer grounded in the selected evidence and records `ANSWER_SYNTHESIZED`.
7. The implementation records `RUN_COMPLETED` and returns an `ExperimentResult` containing:
   - `status`: `COMPLETED`
   - `finalAnswer`: a prose answer to the question
   - `evidence`: the selected `Evidence` items
   - `trace`: the full `ExecutionTrace`
   - `metrics`: an `ExecutionMetrics` with counts for searches, reads, and evidence items
   - `errors`: empty

---

## Expected Evidence

The following documents from `LocalKnowledgeCorpus.demo()` are expected to contribute evidence. Source IDs are stable.

| Priority | Source ID | Title | Relevance |
| --- | --- | --- | --- |
| Primary | `exp-001` | Controlled Evidence in AI Framework Experiments | Directly addresses why controlled local evidence reduces noise and enables reproducibility |
| Supporting | `rag-001` | Retrieval-Augmented Generation: Core Concepts | Explains what retrieval-augmented generation is and how iterative retrieval works |
| Supporting | `obs-001` | Observability Fundamentals for Agentic Systems | Explains why observability must be comparable across runs |
| Optional | `vt-001` | Virtual Threads and Blocking I/O in Java | May be retrieved for general Java concurrency context |
| Optional | `sc-001` | Structured Concurrency in the JVM | May be retrieved for general concurrent execution context |

A successful baseline run must retrieve and use `exp-001`. Retrieval of `rag-001` and `obs-001` is expected but a framework may satisfy the question with fewer documents if its synthesis is adequate.

---

## Expected Output

The `finalAnswer` must:

- explain that controlled local evidence reduces experimental noise by providing all implementations with identical documents;
- note that a controlled corpus makes experiments reproducible — the same query produces the same retrieval results regardless of when the experiment runs;
- note that without controlled evidence, observed differences in answers may reflect retrieval quality rather than framework orchestration quality;
- not refer to any fact that cannot be sourced from the controlled corpus.

The answer need not be verbatim-matched. The above points define the semantic content that must be present, not a specific phrasing. A future evaluation test may check that the answer addresses these points.

---

## Observable Trace Expectations

The `ExecutionTrace` must contain events from the following types, in logical order. The exact event count depends on the framework's orchestration strategy.

| Event type | Minimum occurrences | Notes |
| --- | --- | --- |
| `RUN_STARTED` | 1 | First event |
| `KNOWLEDGE_SEARCH_STARTED` | 1 | At least one search required |
| `KNOWLEDGE_SEARCH_COMPLETED` | ≥ `KNOWLEDGE_SEARCH_STARTED` count | Paired with started |
| `DOCUMENT_READ_STARTED` | ≥ 1 | At least one document must be read |
| `DOCUMENT_READ_COMPLETED` | ≥ `DOCUMENT_READ_STARTED` count | Paired with started |
| `EVIDENCE_RETRIEVED` | ≥ 1 | At least one evidence item identified |
| `EVIDENCE_SELECTED` | ≥ 1 | At least one evidence item selected for the answer |
| `ANSWER_SYNTHESIZED` | 1 | After evidence selection |
| `RUN_COMPLETED` | 1 | Last event |

A framework that requires model calls to drive reasoning may also emit `MODEL_CALL_STARTED` / `MODEL_CALL_COMPLETED`. These are expected but not required for local-only baseline verification.

Frameworks using step-level orchestration (e.g. LangGraph4j nodes, Embabel actions) may additionally emit `STEP_STARTED` / `STEP_COMPLETED` around major phases. These are permitted and informative but not required for the use case to be considered satisfied.

---

## Success Criteria

The use case is satisfied when all of the following hold:

1. `ExperimentResult.status` is `COMPLETED`.
2. `ExperimentResult.finalAnswer` is non-blank and addresses the expected output content above.
3. `ExperimentResult.evidence` contains at least one item with `sourceId` equal to `exp-001`.
4. `ExperimentResult.trace` contains the minimum required events listed in the trace expectations table.
5. `ExperimentResult.metrics.knowledgeSearches` ≥ 1.
6. `ExperimentResult.metrics.documentReads` ≥ 1.
7. `ExperimentResult.metrics.evidenceCount` ≥ 1.
8. No internet call, vector database query, or external model call was required (for baseline local-only verification).
9. The same experiment can be re-run and produce the same evidence source IDs (determinism requirement).

---

## Out of Scope

This use case explicitly does not cover:

- web search or live internet retrieval;
- calling OpenRouter or any language model (for local baseline verification);
- embedding similarity or vector database retrieval (Lucene, Qdrant, OpenSearch, pgvector);
- Langfuse, LangSmith, or OpenTelemetry integration;
- Vue UI implementation or frontend rendering;
- Docker Compose orchestration;
- LLM answer quality evaluation beyond the semantic content criteria above;
- comparing framework answers to each other (that is the experiment result, not a precondition);
- choosing a winning framework.

---

## Executable Test Case — TC-UC-001

TC-UC-001 is implemented as an executable contract in `argonaut-core`.

**Location:** `dev.jsanca.argonaut.core.testing.ControlledLocalEvidenceContract`

**Usage in framework test classes:**

```java
// Plug the framework implementation behind ExperimentExecutor and verify all assertions
ControlledLocalEvidenceContract.verify(request -> myFramework.run(request));

// Or assert a result already obtained
ControlledLocalEvidenceContract.assertSatisfied(result);
```

**Nine enforced assertions:** SC1 status COMPLETED, SC2 non-blank finalAnswer, SC3 exp-001 in evidence, SC4 evidence traceability (anti-gaming — each Evidence.sourceId must appear in DOCUMENT_READ_COMPLETED or EVIDENCE_RETRIEVED trace event metadata), SC5 all required event types present, SC6 trace ordering (RUN_STARTED first, RUN_COMPLETED last, coarse phase ordering), SC7 STARTED/COMPLETED pairing, SC8 metrics consistency (evidenceCount, knowledgeSearches, documentReads, errors), SC9 errors empty.

**Anti-gaming:** SC4 requires that every `Evidence.sourceId` appear in the `sourceId` metadata of at least one `DOCUMENT_READ_COMPLETED` or `EVIDENCE_RETRIEVED` trace event. A framework that hard-codes evidence without performing real `KnowledgeRepository` calls cannot satisfy this assertion.

**Trace metadata convention:** Framework implementations must populate `ExecutionEvent.metadata()` using the keys defined in `TraceEventMetadata`:
- `DOCUMENT_READ_COMPLETED` — must include `sourceId`
- `EVIDENCE_RETRIEVED` — must include `sourceId`
- `EVIDENCE_SELECTED` — should include `sourceId` or `evidenceId`
- `KNOWLEDGE_SEARCH_COMPLETED` — should include `sourceIds` (comma-separated)

**Corpus:** `LocalKnowledgeCorpus.demo()` loads the authoritative Markdown corpus from classpath; no additional setup required.

**Reference executor:** `ReferenceExecutor` (test-only fixture in `argonaut-core`) performs real search and read calls against the corpus and passes all TC-UC-001 assertions. It is not a framework implementation — it is proof that the contract is passable when behavior is honest.

---

## Related Records

| Record | Location |
| --- | --- |
| Shared knowledge contracts | [`docs/knowledge/common-contract.md`](../common-contract.md) |
| `LocalKnowledgeRepository` implementation report | [`docs/engineering/agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md`](../../engineering/agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md) |
| `argonaut-core` module overview | [`argonaut-core/README.md`](../../../argonaut-core/README.md) |
