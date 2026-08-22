# argonaut-core

Shared observable contracts for the Argonaut agentic RAG experiment.

## Purpose

This module contains only what is genuinely common across every framework implementation. All four Argonaut services (Spring AI, LangChain4j, LangGraph4j, Embabel) depend on this module. The Vue experiment console consumes the types defined here via the common HTTP API.

## Package Overview

| Package | Contents |
| --- | --- |
| `experiment` | `ExperimentRequest`, `ExperimentResult`, `ArgonautInfo`, `RunStatus` |
| `evidence` | `Evidence`, `EvidenceKind` |
| `knowledge` | `KnowledgeRepository` (interface), `KnowledgeSearchRequest/Response/Result`, `DocumentReference`, `DocumentContent`, `KnowledgeSourceException` |
| `knowledge.local` | `LocalKnowledgeDocument`, `LocalKnowledgeCorpus` (with `demo()`), `LocalKnowledgeRepository`, `MarkdownCorpusLoader`, `MarkdownParseException` |
| `trace` | `ExecutionTrace`, `ExecutionEvent`, `ExecutionEventType` |
| `observability` | `ExecutionObserver` (interface), `NoopExecutionObserver`, `InMemoryExecutionObserver`, `CompositeExecutionObserver` |
| `metrics` | `ExecutionMetrics`, `ExecutionMetrics.Builder` |
| `error` | `ArgonautError`, `ArgonautErrorCode` |
| `testing` | `ExperimentExecutor`, `ControlledLocalEvidenceContract` (TC-UC-001), `TraceEventMetadata` |

## Key Contracts

**`ExperimentRequest`** — identifies a run (`runId`), carries the mission question, and accepts optional `parameters` for future extension without redesigning the contract.

**`ExperimentResult`** — the complete observable result of one run: framework identity, final answer, evidence list, execution trace, metrics, errors, and status. This is what Vue receives from every service.

**`KnowledgeRepository`** — the conceptual capability contract for the controlled evidence corpus. Each framework implements it differently (tool, node, action) without changing this interface.

**`ExecutionObserver`** — receives normalized events during execution. `InMemoryExecutionObserver` accumulates events and produces an `ExecutionTrace`. `CompositeExecutionObserver` enables future external exporters without changing framework wiring.

## What does not belong here

Orchestration semantics. This module must not become a shared agent framework.

**Never add to this module:**

| Concept | Reason |
| --- | --- |
| `Agent`, `Graph`, `Node`, `Planner`, `Workflow` | Framework-specific — belong in implementation modules |
| Spring AI, LangChain4j, LangGraph4j, Embabel dependencies | Would break framework independence |
| OpenRouter, Langfuse, LangSmith dependencies | Infrastructure adapters, not core contracts |
| Lucene, Qdrant, vector DB dependencies | Knowledge implementation detail, not contract |

> **Rule:** Share contracts, tools, fixtures, and observability structures. Do not share orchestration semantics.

## Tests

107 unit tests across eleven test classes. In addition to the corpus, contract, and scoring tests, `TcUc001Test` (21 tests) covers: reference executor end-to-end pass, each required TC-UC-001 assertion fires on a bad result, `ExperimentResult.completed` rejects blank `finalAnswer`, anti-gaming SC4 check, trace ordering violations, and metrics consistency failures.

Run from the repository root:

```bash
mvn verify
```

## Authoritative corpus and loader

The controlled local evidence corpus is authoritative at:

```text
docs/knowledge/corpus/controlled-local-evidence/   ← human-editable source of truth
argonaut-core/src/main/resources/knowledge/controlled-local-evidence/   ← classpath copy for production and test
```

Five documents: `exp-001`, `rag-001`, `obs-001`, `vt-001`, `sc-001`. The classpath copy must stay identical to the `docs/` source; there is no automated synchronization. Update both locations when editing corpus documents.

**Corpus authority rules:**
- All four framework implementations must use `LocalKnowledgeRepository.withDemoCorpus()`. Substituting different corpus content violates the "same evidence" principle.
- Duplicate document IDs are rejected at construction time by `LocalKnowledgeCorpus.of()`.
- Corpus drift between `demo()` and the Markdown files is treated as a test failure.

**`LocalKnowledgeCorpus.demo()`** loads from the Markdown classpath resources on each call. It is no longer programmatic. It is the required corpus API for all framework implementations.

**Loading the corpus:**

```java
// Framework modules — the only approved way to get the experiment corpus
KnowledgeRepository knowledge = LocalKnowledgeRepository.withDemoCorpus();

// Direct corpus access
LocalKnowledgeCorpus corpus = LocalKnowledgeCorpus.demo();

// Constant for the classpath path
String path = LocalKnowledgeCorpus.CONTROLLED_CORPUS_CLASSPATH; // "knowledge/controlled-local-evidence"
```

**Limitation:** `MarkdownCorpusLoader.fromClasspath` uses filesystem-based resolution (`resource.toURI()`). It works in exploded Maven layouts but not inside JAR files. Use `fromDirectory(Path)` for JAR-packaged deployments.

## TC-UC-001 harness

`ControlledLocalEvidenceContract` is the executable form of the first use case. Framework modules use it in their test suites:

```java
// Plug implementation behind ExperimentExecutor and verify all nine assertions
ControlledLocalEvidenceContract.verify(request -> myFramework.run(request));
```

**Nine assertions (SC1–SC9):** status COMPLETED, non-blank finalAnswer, exp-001 in evidence, evidence traceability (anti-gaming), all required event types, trace ordering, STARTED/COMPLETED pairing, metrics consistency, errors empty.

**Anti-gaming (SC4):** Every `Evidence.sourceId` must appear in the `sourceId` metadata of a `DOCUMENT_READ_COMPLETED` or `EVIDENCE_RETRIEVED` event. Hard-coded evidence without real repository calls fails this check.

**Trace metadata convention** (from `TraceEventMetadata`):

| Event | Required key | Value |
| --- | --- | --- |
| `DOCUMENT_READ_COMPLETED` | `sourceId` | single document ID |
| `EVIDENCE_RETRIEVED` | `sourceId` | single document ID |
| `EVIDENCE_SELECTED` | `sourceId` or `evidenceId` | document or evidence ID |
| `KNOWLEDGE_SEARCH_COMPLETED` | `sourceIds` | comma-separated result IDs |

## Further reading

[docs/knowledge/common-contract.md](../docs/knowledge/common-contract.md) — explains why the contract is designed this way, how Vue consumes it, and how future knowledge implementations and observability exporters fit in.
