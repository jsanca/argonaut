# ARGONAUT-003 — Define Observable Agentic RAG Contracts in argonaut-core

## Mission

Define the first version of the shared observable Agentic RAG contract in `argonaut-core`.

This task must establish the common request/response model, evidence model, execution trace model, metrics model, error model, and conceptual knowledge-access contracts used by all Argonaut implementations.

Do not implement any framework-specific runtime yet.

Do not implement Spring AI, LangChain4j, LangGraph4j, Embabel, OpenRouter, Lucene, Qdrant, Langfuse, LangSmith, Docker Compose, Vue.js, or HTTP controllers in this task.

The goal is to define what every Argonaut must expose and what the Vue.js experiment console will eventually be able to observe.

---

## Context

Argonaut compares multiple agentic AI frameworks while keeping the mission, model, controlled evidence, conceptual tools, observable contract, and evaluation expectations as consistent as practical.

Initial Argonaut implementations:

```text id="40c8pt"
Spring AI      → localhost:8081
LangChain4j    → localhost:8082
LangGraph4j    → localhost:8083
Embabel        → localhost:8084
```

All implementations will eventually expose the same HTTP API, but this task only defines the shared Java contracts in `argonaut-core`.

The frontend must be able to compare different runtimes without knowing whether the backend is Spring AI, LangChain4j, LangGraph4j, or Embabel.

---

## Read First

Before implementing, inspect:

```text id="f486ka"
README.md
docs/PROJECT.md
docs/OSK.md
docs/knowledge/
docs/engineering/
docs/adr/
docs/roadmap/
```

Also inspect relevant OSK skills under:

```text id="fdxsq4"
.osk/skills/
```

especially:

```text id="o274mu"
osk-architecture-review
osk-boundary-review
osk-execution-observability
osk-verification-engineering
osk-engineering-reporting
```

Respect the current OSK documentation conventions.

Do not remove or reorganize existing OSK files.

---

## Core Design Principle

`argonaut-core` must define shared observable contracts and conceptual capabilities.

It must not define a shared agent framework.

Allowed:

```text id="ohqm2u"
request/response DTOs
evidence structures
execution trace structures
metrics structures
error structures
knowledge search/read contracts
observer contracts
small test utilities
```

Not allowed:

```text id="mxc5ch"
Agent
Graph
Node
Planner
Workflow
FrameworkRunner
UniversalOrchestrator
```

The core rule is:

> Share contracts, tools, fixtures, and observability structures. Do not share orchestration semantics.

Each framework must remain free to express the same mission using its own idiomatic abstractions.

---

## Agentic RAG Contract Level

Define contracts at the level of observable Agentic RAG behavior.

Do not define the contract around embeddings directly.

For this task, avoid abstractions such as:

```text id="1uvgy6"
EmbeddingModel
VectorStore
VectorIndex
EmbeddingSearch
```

unless a minimal type is absolutely required for compile-time clarity.

Instead, prefer stable knowledge-access capabilities such as:

```text id="c51i00"
KnowledgeRepository
KnowledgeSearch
DocumentReader
```

The shared contract should express:

```text id="7qceos"
find relevant information
read controlled source material
return evidence
show execution trace
report metrics/errors
produce final answer
```

not:

```text id="omw1q4"
how embeddings are generated
where vectors are stored
which vector database is used
how a framework internally wires retrieval
```

Later implementations may use:

```text id="2vkxpx"
in-memory search
Lucene
Qdrant
OpenSearch
pgvector
framework-native vector stores
```

without changing the observable experiment contract.

---

## Required Package Structure

Use clear package names under the existing module.

Suggested packages:

```text id="towjj8"
dev.jsanca.argonaut.core
dev.jsanca.argonaut.core.experiment
dev.jsanca.argonaut.core.evidence
dev.jsanca.argonaut.core.trace
dev.jsanca.argonaut.core.metrics
dev.jsanca.argonaut.core.error
dev.jsanca.argonaut.core.knowledge
dev.jsanca.argonaut.core.observability
```

Adjust if the existing project naming convention differs, but keep it simple.

---

# Required Model

## Experiment Request

Create a request model representing a single experiment run.

Suggested shape:

```java id="g6m7h3"
record ExperimentRequest(
    String runId,
    String question,
    Map<String, String> parameters
) {}
```

Requirements:

* `runId` identifies the execution.
* `question` is the user-facing mission/question.
* `parameters` allows simple future extension without redesigning the contract.

Do not include framework-specific fields.

---

## Experiment Result

Create a result model representing the complete observable result of one Argonaut run.

Suggested shape:

```java id="jyxacy"
record ExperimentResult(
    String runId,
    String frameworkId,
    RunStatus status,
    String finalAnswer,
    List<Evidence> evidence,
    ExecutionTrace trace,
    ExecutionMetrics metrics,
    List<ArgonautError> errors
) {}
```

Requirements:

* must support completed runs;
* must support failed runs;
* must expose evidence;
* must expose execution trace;
* must expose basic metrics;
* must expose structured errors;
* must not require framework-specific interpretation.

---

## Argonaut Info

Create a model for future `GET /api/about` responses.

Suggested shape:

```java id="v4jayh"
record ArgonautInfo(
    String frameworkId,
    String frameworkName,
    String implementationVersion,
    String modelProvider,
    String modelName,
    List<String> capabilities
) {}
```

The model provider/name may be unknown at construction time for some implementations, so use a representation that can tolerate absent values if needed.

Do not add OpenRouter-specific dependencies.

---

## Run Status

Create an enum such as:

```text id="gl94ft"
PENDING
RUNNING
COMPLETED
FAILED
PARTIAL
CANCELLED
```

Keep the status set small.

Do not over-model lifecycle complexity yet.

---

# Evidence Model

Create an evidence model suitable for showing retrieved/selected knowledge in Vue.

Suggested shape:

```java id="fhwvw1"
record Evidence(
    String id,
    EvidenceKind kind,
    String sourceId,
    String title,
    String excerpt,
    double score,
    String usedFor
) {}
```

`EvidenceKind` may include:

```text id="r62p83"
SUPPORTING
COUNTER
BACKGROUND
NEUTRAL
UNKNOWN
```

Requirements:

* evidence should be displayable as UI cards;
* evidence must include a source reference;
* evidence may include a relevance or confidence score;
* evidence should explain how it was used when available.

Do not require citation formatting yet.

---

# Knowledge Contracts

Define conceptual knowledge-access contracts.

Suggested interfaces:

```java id="2egko5"
interface KnowledgeRepository {
    KnowledgeSearchResponse search(KnowledgeSearchRequest request);
    DocumentContent read(DocumentReference reference);
}
```

Supporting models may include:

```text id="m65vrh"
KnowledgeSearchRequest
KnowledgeSearchResponse
KnowledgeSearchResult
DocumentReference
DocumentContent
```

Requirements:

* search should support a query string and `topK`;
* read should retrieve document content by reference;
* results should include source id/title/excerpt/score where available;
* no concrete vector DB, embedding model, or search engine dependency;
* no HTTP/client implementation.

The repository represents the conceptual capability:

```text id="64heav"
search/read controlled knowledge
```

Each framework may expose it differently:

```text id="g1gm9o"
Spring AI      → tool/bean
LangChain4j    → tool
LangGraph4j    → node/tool interaction
Embabel        → action/capability equivalent
```

---

# Execution Trace Model

Create a trace model that Vue can render as a timeline.

Suggested shape:

```java id="bqyvlr"
record ExecutionTrace(
    List<ExecutionEvent> events
) {}
```

Suggested event shape:

```java id="3lbejj"
record ExecutionEvent(
    String id,
    Instant timestamp,
    ExecutionEventType type,
    String actor,
    String name,
    String summary,
    Map<String, String> metadata,
    Long durationMs
) {}
```

Event types should include enough to observe Agentic RAG execution:

```text id="3kahjg"
RUN_STARTED
RUN_COMPLETED
RUN_FAILED

STEP_STARTED
STEP_COMPLETED
STEP_FAILED

TOOL_CALL_STARTED
TOOL_CALL_COMPLETED
TOOL_CALL_FAILED

MODEL_CALL_STARTED
MODEL_CALL_COMPLETED
MODEL_CALL_FAILED

KNOWLEDGE_SEARCH_STARTED
KNOWLEDGE_SEARCH_COMPLETED
DOCUMENT_READ_STARTED
DOCUMENT_READ_COMPLETED

EVIDENCE_RETRIEVED
EVIDENCE_SELECTED
ANSWER_SYNTHESIZED
```

Keep the event type list practical.

Do not model every possible framework-native event.

If needed later, framework-specific information can be stored in metadata.

---

# Observability Contract

Create a minimal observer interface.

Suggested shape:

```java id="4ow3wc"
interface ExecutionObserver {
    void record(ExecutionEvent event);
}
```

Add simple implementations:

```text id="dzxl1p"
NoopExecutionObserver
InMemoryExecutionObserver
CompositeExecutionObserver
```

Requirements:

* `NoopExecutionObserver` ignores events;
* `InMemoryExecutionObserver` stores events in order;
* `CompositeExecutionObserver` forwards events to multiple observers;
* implementations should be small and deterministic;
* no Langfuse;
* no LangSmith;
* no OpenTelemetry dependency yet.

This observer is for Argonaut's normalized trace.

External observability exporters may be added later.

---

# Metrics Model

Create basic comparable metrics.

Suggested shape:

```java id="xyt4vl"
record ExecutionMetrics(
    long durationMs,
    int modelCalls,
    int toolCalls,
    int knowledgeSearches,
    int documentReads,
    int evidenceCount,
    int errors
) {}
```

Keep metrics simple.

The goal is to give Vue enough to compare runs visually, not to build production telemetry.

---

# Error Model

Create structured errors for failed or partial runs.

Suggested shape:

```java id="dkw1w2"
record ArgonautError(
    ArgonautErrorCode code,
    String message,
    String component,
    boolean retryable
) {}
```

Suggested error codes:

```text id="n63k1s"
CONFIGURATION_ERROR
MODEL_PROVIDER_ERROR
MODEL_TIMEOUT
TOOL_ERROR
KNOWLEDGE_SOURCE_ERROR
VALIDATION_ERROR
UNSUPPORTED_OPERATION
INTERNAL_ERROR
```

Requirements:

* errors must be displayable by Vue;
* failed framework runs should not collapse into unstructured exceptions;
* do not expose secrets in error messages;
* keep the enum small and understandable.

---

# Serialization

The contracts should be easy to serialize as JSON later.

Do not add a heavy serialization framework if the parent project does not already include one.

If Jackson annotations are useful and lightweight, use them only if already appropriate for the project.

Otherwise prefer plain Java records and enums.

Avoid framework-specific annotations in core.

---

# Validation / Utility Behavior

Add simple validation helpers if useful, but do not overbuild.

Examples:

```text id="m90t2j"
ExperimentRequest requires non-blank runId and question
KnowledgeSearchRequest requires non-blank query and positive topK
Evidence score should be finite
```

If validation logic is added, keep it explicit and tested.

Do not introduce Bean Validation dependencies unless already justified.

---

# Tests

Add unit tests for the core model behavior.

At minimum test:

* creating an `ExperimentRequest`;
* creating a successful `ExperimentResult`;
* creating a failed `ExperimentResult` with structured error;
* `InMemoryExecutionObserver` stores events in order;
* `CompositeExecutionObserver` forwards events;
* `NoopExecutionObserver` does not fail;
* knowledge request validation if implemented;
* metrics can represent a basic run;
* records/enums are framework-neutral.

Do not add fake tests that only assert `true`.

---

# Documentation

Create or update:

```text id="8eeas2"
docs/knowledge/common-contract.md
```

This document should explain:

* why Argonaut has a common contract;
* what belongs in `argonaut-core`;
* what must not belong in `argonaut-core`;
* how the contract supports Vue;
* why knowledge search/read is abstracted instead of embeddings/vector stores;
* how each framework is expected to adapt the same conceptual capabilities;
* why Langfuse/LangSmith are deferred as exporters/adapters rather than core dependencies.

Also update:

```text id="gapj9t"
argonaut-core/README.md
```

with the implemented package/contract overview.

Update:

```text id="aqabxh"
docs/engineering/ENGINEERING_LOG.md
```

with a concise ARGONAUT-003 entry.

---

# Important Boundary Notes

The following must be true after this task:

```text id="qcd2uz"
argonaut-core does not depend on Spring AI
argonaut-core does not depend on LangChain4j
argonaut-core does not depend on LangGraph4j
argonaut-core does not depend on Embabel
argonaut-core does not depend on OpenRouter
argonaut-core does not depend on Langfuse
argonaut-core does not depend on LangSmith
argonaut-core does not depend on Lucene
argonaut-core does not depend on Qdrant
```

Also:

```text id="c1v4d6"
argonaut-core does not define Agent
argonaut-core does not define Graph
argonaut-core does not define Node
argonaut-core does not define Planner
argonaut-core does not define Workflow
```

These may be native concepts inside framework-specific modules later, but not in core.

---

# Future Direction

Document but do not implement that future knowledge implementations may include:

```text id="490ok3"
controlled in-memory repository
Lucene lexical/vector/hybrid search
Qdrant vector database
OpenSearch/Elasticsearch
framework-native vector stores
```

Also document that future observability exporters may include:

```text id="k2ojxy"
Langfuse
LangSmith
OpenTelemetry
structured logs
```

These are future adapters, not current core contracts.

---

# Build Verification

Run:

```bash id="h9rr4t"
mvn verify
```

from the repository root.

All modules must still compile.

---

# Non-Goals

Do not:

* implement Spring AI runtime;
* implement LangChain4j runtime;
* implement LangGraph4j runtime;
* implement Embabel runtime;
* implement OpenRouter;
* implement HTTP controllers;
* implement Vue UI;
* implement Docker Compose;
* implement embeddings;
* implement vector search;
* implement Lucene;
* implement Qdrant;
* implement Langfuse;
* implement LangSmith;
* implement the experiment mission;
* create the Hybrid Argonaut;
* choose a winning framework.

This task defines the shared observable core contract only.

---

# Final Report

Report:

* OSK docs reviewed;
* files created/modified;
* package structure created;
* records/enums/interfaces added;
* observer implementations added;
* validation behavior added, if any;
* tests added;
* boundary checks;
* documentation created/updated;
* `mvn verify` result;
* deferred decisions.

Do not commit changes.
