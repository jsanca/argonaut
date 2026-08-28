## ARGONAUT-0.2-002 — Evidence-Guided Vector & Embedding Research

## Context

ARGONAUT-0.2-001 performed an initial reconnaissance of candidate embedding and vector-storage technologies.

The purpose of this task is to resolve the most relevant uncertainties identified during that reconnaissance using targeted evidence.

This is still primarily a **research and architecture-learning task**, not an implementation task.

An equally important goal is **knowledge retention**.

Any useful understanding acquired during this investigation must be synthesized into Argonaut's retained documentation so that a future agent does not need to rediscover the same information from source repositories.

---

# Core Principle

Do not treat source inspection as ephemeral agent context.

The intended lifecycle is:

```text
unknown / uncertain
        ↓
targeted source inspection
        ↓
understanding
        ↓
verification
        ↓
synthesized retained knowledge
        ↓
future agent starts from retained knowledge
```

A future agent working on Argonaut should normally be able to consult the retained project knowledge first and only return to upstream source code when deeper verification is necessary.

---

# Available Local Source Repositories

Argonaut is located under the user's common `code` workspace.

The following repositories are siblings of Argonaut.

From the Argonaut repository, go up one directory to find them.

## Integrallis Vectors

```text
../vectors/
```

This is a local checkout of:

```text
integrallis/vectors
```

You are explicitly authorized to inspect this repository.

Study source code, tests, examples, build files, documentation, modules, and public APIs as useful.

Do not modify the repository.

---

## LangChain4j

```text
../langchain4j/
```

Embedding-related implementation is under:

```text
../langchain4j/embeddings/
```

You are explicitly authorized to inspect this repository.

Do not modify it.

Relevant areas are expected to include the implementation around:

```text
langchain4j-embeddings
langchain4j-embeddings-all-minilm-l6-v2
```

and particularly concepts/classes such as:

```text
AllMiniLmL6V2EmbeddingModel
AbstractInProcessEmbeddingModel
OnnxBertBiEncoder
HuggingFaceTokenizer
PoolingMode
```

Follow the implementation where necessary rather than assuming these are the only relevant classes.

---

# Known Evidence

Some uncertainty from ARGONAUT-0.2-001 has already been reduced.

## Local ONNX embeddings

Current evidence indicates that LangChain4j's local MiniLM implementation uses approximately:

```text
String
  ↓
DJL HuggingFaceTokenizer
  ↓
input_ids
attention_mask
token_type_ids (when expected)
  ↓
ONNX Runtime
  ↓
token vectors
  ↓
pooling
  ↓
normalization
  ↓
float[] embedding
```

The current `all-MiniLM-L6-v2` implementation produces embeddings of dimension:

```text
384
```

The implementation uses:

```text
com.microsoft.onnxruntime:onnxruntime
ai.djl.huggingface:tokenizers
```

The model artifact is downloaded during Maven `generate-resources`, verified by SHA-256, and packaged as a classpath resource.

Verify relevant details from the local source rather than treating this summary as authoritative.

---

## sqlite-vec

`sqlite-vec` is **deferred** from the current Argonaut 0.2 investigation.

Do not spend time investigating or integrating it unless new evidence discovered during this task materially changes that decision.

---

## LanceDB

Java support exists, but current evidence suggests an important distinction between its Java client capabilities and local/embedded operation.

Verify current authoritative documentation as necessary and determine how this affects its value as an Argonaut 0.2 candidate.

Do not clone repositories merely for completeness.

---

# Research Areas

## 1. Integrallis Vectors

Resolve the uncertainty from the previous reconnaissance.

Determine from actual source and retained project documentation:

* what `VectorCollection` actually represents;
* whether it is a genuine embedded vector search/storage engine;
* lifecycle of a collection;
* persistence model;
* supported similarity metrics;
* supported index types;
* metadata support;
* filtering;
* add/update/delete behavior;
* commit semantics;
* concurrency considerations where relevant;
* relationship between Vectors and vectors-vcr;
* relationship between `VectorCollection` and VCR functionality;
* LangChain4j integration;
* Spring AI integration;
* Java/JDK requirements;
* use of the JDK Vector API;
* whether native/JNI components are involved;
* operational characteristics relevant to Argonaut.

Pay particular attention to the distinction:

```text
embedded vector engine
        vs
VCR/testing infrastructure
```

Do not conflate them simply because they live in the same ecosystem.

Also identify anything architecturally interesting in this library that could inform Argonaut's design.

---

# 2. Local ONNX Embedding Pipeline

Use the local LangChain4j source as a working reference implementation.

Understand sufficiently how LangChain4j performs:

```text
text
 ↓
tokenization
 ↓
ONNX inference
 ↓
pooling
 ↓
normalization
 ↓
embedding
```

Determine:

* responsibilities of the Hugging Face tokenizer;
* how token IDs and masks are produced;
* ONNX Runtime responsibilities;
* model input/output shape assumptions;
* pooling behavior;
* normalization behavior;
* long-input handling;
* model resource loading;
* Maven model acquisition/packaging;
* runtime dependencies;
* what LangChain4j contributes versus what comes directly from DJL and ONNX Runtime.

Most importantly, answer:

> Can Argonaut reasonably generate local embeddings using ONNX Runtime without depending on LangChain4j's embedding abstraction?

If yes, describe the smallest reasonable implementation surface.

Do **not** implement it yet.

---

# 3. Candidate Vector Systems

Revisit the original candidate assessment using the evidence now available.

Current candidates include:

```text
LangChain4j InMemoryEmbeddingStore
Integrallis Vectors
Qdrant
Pinecone
LanceDB
Weaviate
Milvus
```

`sqlite-vec` is currently deferred.

For each remaining candidate, determine whether it represents a meaningfully different architecture or operational model.

Examples:

```text
in-memory baseline
embedded persistent Java engine
dedicated vector database
managed/serverless vector service
remote/cloud data platform
distributed vector infrastructure
```

We do not need multiple vendors merely because they exist.

We want candidates that teach us something materially different.

---

# 4. Evidence Acquisition

Continue applying the evidence discipline established in ARGONAUT-0.2-001.

Do not indiscriminately download repositories.

For each unresolved question:

1. State what is unknown.
2. Identify the smallest useful evidence source.
3. Inspect it.
4. Record what was learned.
5. State whether confidence changed.

Prefer:

```text
retained project knowledge
        ↓
official docs
        ↓
API/Javadocs
        ↓
examples/tests
        ↓
targeted source inspection
        ↓
additional repository acquisition
```

when practical.

Local source inspection is already authorized for `../vectors/` and `../langchain4j/`.

---

# Knowledge Retention Requirement

This is a required deliverable, not optional reporting.

Inspect the existing Argonaut documentation structure and follow its established OSK conventions.

Useful durable findings should be written into the appropriate retained documentation under:

```text
docs/knowledge/
```

when they describe reusable knowledge about:

* technologies;
* architecture;
* embedding pipelines;
* vector storage/search concepts;
* integration characteristics;
* stable constraints or capabilities.

Use:

```text
docs/engineering/
```

for the investigation history:

* what was inspected;
* decisions made during this task;
* evidence used;
* uncertainties;
* experiment/research results;
* task/report lifecycle.

Do not blindly create duplicate documents.

Inspect the existing structure first and update an appropriate document if one already exists.

The distinction is:

```text
docs/engineering
    = how we learned / investigated / decided

docs/knowledge
    = what the project now knows
```

The retained knowledge should be useful to another agent that has **not** participated in this conversation.

It should not require reading this task or reconstructing Clio's reasoning history to understand the resulting technical knowledge.

---

# Knowledge Compression

Do not copy upstream documentation or source code into Argonaut.

Synthesize it.

For example, avoid retaining:

```text
hundreds of lines from OnnxBertBiEncoder.java
```

Prefer retained knowledge such as:

```text
Argonaut can produce MiniLM embeddings locally without a remote
embedding provider.

Reference implementation:
LangChain4j AllMiniLmL6V2EmbeddingModel.

Runtime:
- ONNX Runtime
- DJL HuggingFaceTokenizer

Output:
- 384-dimensional normalized float vector

Pipeline:
tokenize → ONNX inference → mean pooling → normalization

Important implementation detail:
long inputs are partitioned before inference and the resulting
embeddings are combined using a token-count-weighted average.
```

Include upstream references and relevant source locations so that a future agent can drill down when necessary.

The retained document is a **map of learned knowledge**, not a replacement for upstream source.

---

# Self-Correction Record

Compare the findings from this task against ARGONAUT-0.2-001.

Explicitly identify:

```text
What I believed before
What evidence I inspected
What I learned
Whether my confidence changed
What I would now recommend
```

Pay special attention to cases where the initial assessment was incomplete or incorrect.

This is intentional.

We want evidence of whether targeted source inspection improves the agent's technical model of unfamiliar systems.

---

# Do Not Implement Yet

Do not:

* implement Argonaut adapters;
* introduce `Store` or `Search` interfaces;
* change production dependencies;
* modify application architecture;
* modify upstream repositories;
* implement ONNX embeddings;
* implement a vector backend;
* benchmark vendors;
* choose a final winner.

The next task will use the retained knowledge from this investigation to design Argonaut's common API.

---

# Deliverables

Produce:

## 1. Engineering research report

Under the appropriate:

```text
docs/engineering/
```

location.

Include:

* evidence inspected;
* findings;
* changed assumptions;
* remaining uncertainties;
* candidate assessment;
* recommendations for the next phase.

## 2. Retained technical knowledge

Create or update the appropriate documentation under:

```text
docs/knowledge/
```

Capture durable knowledge about:

* local ONNX embeddings;
* Integrallis Vectors;
* relevant vector backend characteristics;
* constraints that matter to Argonaut.

## 3. Updated candidate matrix

For each candidate:

```text
Technology
Role
Operational model
External vectors supported
Persistence
Java integration
Infrastructure requirement
Confidence
Evidence
Materially distinct for Argonaut? YES / NO / MAYBE
```

## 4. Self-correction summary

Clearly show how the evidence changed or confirmed the initial reconnaissance.

---

# Completion criterion

The task is complete when a future Argonaut agent can enter the repository, read the retained project knowledge, and understand the important conclusions of this investigation **without needing to repeat the same source-code discovery from scratch**.

Source repositories remain available for verification and deeper investigation, but should no longer be the only place where the acquired understanding exists.
