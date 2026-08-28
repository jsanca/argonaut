# ARGONAUT-0.2-003 — Common Vector Ports and First Backend Wiring

## Context

ARGONAUT-0.2-001 identified candidate vector technologies and knowledge gaps.

ARGONAUT-0.2-002 resolved the most important uncertainties and retained the resulting knowledge under `docs/knowledge/vector-backends/`.

The current recommended experimental shortlist is:

* LangChain4j `InMemoryEmbeddingStore`
* Integrallis Vectors
* Qdrant

Local embeddings will use an ONNX-based pipeline, with LangChain4j's MiniLM implementation serving as the verified reference.

This task begins the **implementation phase** of Argonaut 0.2.

The architecture has already been discussed and should be treated as the intended starting point rather than redesigned from scratch.

---

# Architectural Direction

The desired shape is:

```text
Controller
    ↓
Service
    ↓
SemanticRepository
    ↓
 ┌──────────────┬─────────────┐
 ↓              ↓             ↓
EmbeddingPort  StorePort    SearchPort
 ↓              ↓             ↓
ONNX        Integrallis     Integrallis
            Qdrant          Qdrant
            InMemory        InMemory
```

The intent is to separate three concerns:

1. generating a semantic representation;
2. storing vectorized documents;
3. searching over vectors.

The vector backend must **not** be responsible for generating embeddings.

---

# Module Placement

Prefer implementing the experiment inside a dedicated Argonaut vector-oriented module rather than immediately promoting experimental abstractions into the general `argonaut-core`.

A reasonable starting point is a module such as:

```text
argonaut-vector
```

or the closest equivalent consistent with the current repository structure.

Do not move these abstractions into `argonaut-core` merely because they are generic-looking.

Promote them later only if another Argonaut capability genuinely needs to share them.

Inspect the current project/module structure first and choose the smallest consistent placement.

---

# Domain / Value Objects

Use simple records with minimal semantics.

The intended conceptual shape is:

```java
record Document(
    String id,
    String content,
    Map<String, Object> metadata
) {}
```

```java
record Embedding(
    float[] values
) {}
```

```java
record VectorDocument(
    Document document,
    Embedding embedding
) {}
```

These names may be adjusted only if there is a concrete conflict with an existing Argonaut type.

## Identity

Keep `id` on `Document`.

Even if the first use cases do not heavily depend on identity, update/delete/persistence behavior will likely require it.

`Embedding` should remain a value object and does not need its own id.

`VectorDocument` inherits identity through its `Document`.

Avoid adding speculative fields.

---

# Ports

Define three small ports.

## EmbeddingPort

Conceptually:

```java
interface EmbeddingPort {

    Embedding embed(String text);
}
```

Batch support may be added if it simplifies the ONNX implementation and has a clear use case:

```java
List<Embedding> embedAll(Collection<String> texts);
```

Do not expose ONNX, tokenizers, tensors, LangChain4j types, or vendor-specific concepts through this port.

---

## StorePort

Conceptually:

```java
interface StorePort {

    void store(VectorDocument document);
}
```

Batch/collection storage is desirable if it remains simple.

For example:

```java
void store(Iterable<VectorDocument> documents);
```

Choose an idiomatic shape after examining the actual adapters.

Do not force `Stream` as the primary contract unless there is a compelling lifecycle reason.

The store receives already-created embeddings.

---

## SearchPort

Conceptually:

```java
interface SearchPort {

    List<SearchResult> search(
        Embedding query,
        SearchOptions options
    );
}
```

Create the smallest useful `SearchResult` and `SearchOptions` models.

Do not attempt to normalize every advanced feature of Qdrant or Integrallis into the common API.

The first common surface should represent only capabilities required by all initial backends.

Likely concepts include:

* query embedding;
* top K / limit;
* returned document;
* similarity score.

Metadata filtering may be deferred unless it fits naturally without complicating the first contract.

---

# SemanticRepository

`SemanticRepository` is responsible for orchestrating embedding generation and vector persistence/search.

Conceptually:

```java
class SemanticRepository {

    private final EmbeddingPort embeddings;
    private final StorePort store;
    private final SearchPort search;

    void store(Document document) {
        Embedding embedding = embeddings.embed(document.content());
        store.store(new VectorDocument(document, embedding));
    }

    List<SearchResult> search(String query, SearchOptions options) {
        Embedding embedding = embeddings.embed(query);
        return search.search(embedding, options);
    }
}
```

The exact API can be refined, but preserve this responsibility boundary.

The caller should deal in:

```text
Document
String query
```

The vector adapters should deal in:

```text
Embedding
VectorDocument
```

This is the boundary where human/domain content becomes vector-oriented infrastructure.

---

# ONNX Embedding Adapter

Implement a local ONNX-backed `EmbeddingPort`.

Use the retained knowledge first:

```text
docs/knowledge/vector-backends/onnx-embedding-pipeline.md
```

Only return to upstream LangChain4j source when details need verification.

The verified reference implementation is available locally under:

```text
../langchain4j/embeddings/
```

The implementation should use the relevant independent runtime dependencies rather than depending on LangChain4j's embedding abstraction unless there is a strong engineering reason not to.

Expected ingredients include:

```text
ONNX Runtime
DJL HuggingFaceTokenizer
all-MiniLM-L6-v2
384-dimensional normalized embedding
```

Preserve the important behavior documented during ARGONAUT-0.2-002, including correct long-input handling if required for compatibility.

Do not expose ONNX-specific types outside the adapter.

---

# Backend Selection

The set of vector providers is known at compile time.

Do not use arbitrary string keys as the primary internal selector.

Use an enum such as:

```java
enum VectorProvider {
    IN_MEMORY,
    INTEGRALLIS,
    QDRANT
}
```

The exact names may follow project conventions.

The desired idea is:

```java
Map<VectorProvider, StorePort>
Map<VectorProvider, SearchPort>
```

An `EnumMap` is preferred internally where appropriate.

`EnumSet` is not appropriate for the primary mapping because the application needs to associate a provider with an implementation.

---

# Spring Strategy Wiring

Use Spring DI as the strategy registry rather than introducing a custom factory prematurely.

Spring may inject multiple implementations and a configuration/component may build the provider mappings.

Conceptually:

```java
Map<VectorProvider, StorePort> stores;
Map<VectorProvider, SearchPort> searches;
```

or an equivalent type-safe registration.

The selected provider should be configurable, for example conceptually:

```yaml
argonaut:
  embedding:
    provider: onnx

  vector:
    provider: integrallis
```

The exact property hierarchy should follow existing Argonaut configuration conventions.

Avoid a custom `VectorProviderFactory` unless runtime/dynamic provider creation is genuinely needed.

Backend selection at application startup should preferably remain a Spring configuration concern.

---

# Initial Backend Adapters

Implement the common ports for:

## 1. LangChain4j InMemoryEmbeddingStore

Purpose:

```text
zero-infrastructure baseline
```

It is acceptable for this adapter to internally bridge Argonaut's `Embedding`/`Document` types to LangChain4j types.

Do not leak LangChain4j types through the Argonaut ports.

---

## 2. Integrallis Vectors

Purpose:

```text
embedded persistent/indexed Java backend
```

Read retained knowledge first:

```text
docs/knowledge/vector-backends/integrallis-vectors.md
```

The upstream source is available at:

```text
../vectors/
```

Use upstream source only when deeper verification is needed.

Prefer direct `VectorCollection` integration rather than the framework-specific LangChain4j adapter because Argonaut is intentionally testing its own common abstraction.

Account for required JDK/JVM settings documented in retained knowledge.

Argonaut currently targets Java 25.

---

## 3. Qdrant

Purpose:

```text
dedicated external vector database
```

Qdrant will require external infrastructure.

Provide the smallest reproducible local setup, preferably Docker/Docker Compose consistent with the repository's existing conventions.

Do not introduce unnecessary production orchestration.

The goal is simply:

```text
developer starts Qdrant locally
Argonaut connects
same common StorePort/SearchPort works
```

Use the official Java client unless evidence strongly favors another route.

---

# Keep the Experiment Controlled

All three backends should consume vectors generated by the same embedding implementation.

The desired experimental variable is:

```text
vector storage/search backend
```

not:

```text
embedding model + vector backend simultaneously
```

The flow should remain:

```text
same text
   ↓
same ONNX MiniLM embedding
   ↓
same VectorDocument
   ↓
different backend
```

---

# First Common Capability Surface

Do not prematurely generalize advanced vendor features.

For the first implementation, focus on the common denominator required to demonstrate:

```text
store document
search by semantic query
return top matching documents + score
```

Features such as:

* quantization;
* HNSW tuning;
* hybrid search;
* named vectors;
* sparse vectors;
* sophisticated metadata filters;
* namespaces;
* replication;
* distributed operation

should remain vendor capabilities unless the common use case proves they belong in the port.

Prefer a small honest abstraction over a broad artificial one.

---

# Controller and Service

Expose a small demonstration path through the normal Argonaut application layering.

Conceptually:

```text
Controller
    ↓
Service
    ↓
SemanticRepository
```

The controller should not know:

* ONNX;
* vector dimensions;
* Qdrant;
* Integrallis;
* LangChain4j storage types.

The service should coordinate the use case but should not contain vendor-specific logic.

Follow the existing Argonaut controller/service conventions.

---

# Testing

Add tests at multiple levels where useful.

At minimum:

## Port/semantic behavior

Verify that:

```text
Document
  ↓
embedding generated
  ↓
stored as VectorDocument
```

and:

```text
String query
  ↓
embedding generated
  ↓
SearchPort invoked
```

## Backend contract behavior

Run equivalent behavior against:

* InMemory;
* Integrallis;
* Qdrant where practical.

Do not build an elaborate generic conformance framework unless it emerges naturally.

The important part is demonstrating that the same semantic use case works through each backend.

## ONNX

Include a small deterministic sanity test such as semantically related vs unrelated texts.

Do not attempt to benchmark model quality in this task.

---

# Qdrant Infrastructure

Add the minimum local development setup necessary to run Qdrant.

Document:

* how to start it;
* ports used;
* required environment/configuration;
* how to run the relevant tests.

Do not make the whole Argonaut test suite depend on Qdrant being available.

Use an integration-test boundary/profile/tag consistent with existing project conventions.

---

# Knowledge Retention

Continue the pattern established in ARGONAUT-0.2-002.

If implementation reveals durable knowledge not already captured, update:

```text
docs/knowledge/
```

If implementation reveals:

* design tradeoffs;
* adapter limitations;
* deviations from the proposed architecture;
* backend incompatibilities;
* important decisions;

capture them in the appropriate engineering report and retained knowledge where relevant.

Do not force future agents to rediscover implementation constraints from code alone.

---

# Architectural Feedback Requirement

The proposed architecture is intentionally concrete, but it is not sacred.

If implementation evidence shows that one of these assumptions is wrong or unnecessarily awkward, do not silently work around it.

Explicitly report:

```text
Proposed abstraction
Observed friction
Vendor/API causing the friction
Why the abstraction does not fit cleanly
Recommended adjustment
```

Examples:

```text
StorePort batch shape does not map cleanly to backend X
SearchResult lacks an essential common concept
StorePort/SearchPort separation creates unnecessary duplication
Enum-based provider selection conflicts with Spring wiring
SemanticRepository owns too much or too little
```

We want to learn whether the abstraction survives contact with three materially different implementations.

---

# Non-goals

Do not:

* implement Pinecone yet;
* implement Weaviate;
* implement Milvus;
* implement LanceDB;
* revive sqlite-vec;
* benchmark backend performance;
* optimize ANN parameters;
* redesign Argonaut core;
* introduce a generic plugin framework;
* create dynamic provider discovery;
* build a custom factory unless required by evidence;
* expose vendor APIs through controllers.

---

# Deliverables

Produce:

1. Common records/value objects.
2. `EmbeddingPort`.
3. `StorePort`.
4. `SearchPort`.
5. `SemanticRepository`.
6. ONNX-based embedding adapter.
7. InMemory backend adapter.
8. Integrallis backend adapter.
9. Qdrant backend adapter.
10. Type-safe provider selection using Spring DI.
11. Minimal Qdrant local infrastructure.
12. Focused tests demonstrating the same semantic use case across backends.
13. Normal engineering report.
14. Retained knowledge updates for durable findings.
15. Explicit architectural feedback on where the proposed ports fit well or poorly.

---

# Completion Criterion

The same application-level operation should be able to run conceptually as:

```text
Document / String query
        ↓
SemanticRepository
        ↓
ONNX embedding
        ↓
StorePort / SearchPort
        ↓
selected backend
```

with the vector backend changed through configuration rather than application code.

At minimum, demonstrate this behavior with:

```text
IN_MEMORY
INTEGRALLIS
QDRANT
```

The task is successful not only if all three work, but also if the resulting implementation gives us reliable evidence about whether the proposed `EmbeddingPort` / `StorePort` / `SearchPort` abstraction is genuinely useful.
