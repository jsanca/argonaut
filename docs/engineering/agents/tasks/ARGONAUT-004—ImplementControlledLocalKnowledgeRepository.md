# ARGONAUT-004 — Implement Controlled Local Knowledge Repository

## Mission

Implement the first concrete `KnowledgeRepository` in `argonaut-core` using controlled local documents.

This repository will provide deterministic search and document-read behavior for the first Argonaut experiments before adding Spring AI, LangChain4j, LangGraph4j, Embabel, OpenRouter, or vector databases.

The goal is to create a small, testable knowledge source that every framework implementation can later use through the same core contract.

---

## Context

Argonaut is a comparative lab:

```text id="nj5gck"
Same mission.
Same model.
Same evidence.
Different agentic frameworks.
```

`ARGONAUT-003` defined observable core contracts using records, enums, and simple observer interfaces.

Now we need a first concrete local knowledge implementation so future agentic runs can retrieve evidence from the same controlled corpus.

This task must remain inside `argonaut-core`.

Do not introduce framework-specific dependencies.

---

## Read First

Before implementing, read:

```text id="z1z6sf"
README.md
docs/OSK.md
docs/PROJECT.md
docs/knowledge/common-contract.md
docs/engineering/ENGINEERING_LOG.md
argonaut-core/README.md
argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/
argonaut-core/src/main/java/dev/jsanca/argonaut/core/evidence/
argonaut-core/src/main/java/dev/jsanca/argonaut/core/trace/
argonaut-core/src/main/java/dev/jsanca/argonaut/core/observability/
```

Also inspect installed OSK skills before reporting:

```text id="o6qbgf"
.osk/skills/osk-engineering-reporting/
.osk/skills/osk-verification-engineering/
.osk/skills/osk-boundary-review/
```

Use the current OSK reporting convention:

```text id="1084c2"
docs/engineering/agents/reports/
docs/engineering/ENGINEERING_LOG.md
```

---

## Scope

Create a deterministic local document repository implementation.

Suggested package:

```text id="b81lr1"
dev.jsanca.argonaut.core.knowledge.local
```

The exact package may differ if it better matches the existing core layout.

---

## Required Behavior

Implement a local repository that can:

1. load a small set of controlled documents;
2. search documents by simple lexical scoring;
3. return `KnowledgeSearchResponse`;
4. read a document by `DocumentReference`;
5. preserve source id, title, and content;
6. support deterministic tests;
7. avoid external services.

This is intentionally simple.

Do not implement embeddings, Lucene, Qdrant, OpenSearch, LangChain4j retrievers, Spring AI advisors, or OpenRouter calls.

---

## Suggested Model

If useful, add simple local types such as:

```text id="7asbk3"
LocalKnowledgeDocument
LocalKnowledgeRepository
LocalKnowledgeCorpus
```

Possible fields:

```text id="4bz11i"
id
title
content
metadata
```

Keep the public contract aligned with the existing core knowledge records.

Avoid exposing implementation details through the core interface unnecessarily.

---

## Search Semantics

Use a simple deterministic lexical search.

Acceptable alpha behavior:

```text id="nxbdcf"
- tokenize query;
- tokenize title/content;
- score documents by term overlap;
- title matches may score higher than body matches;
- return top N based on the existing request limit;
- omit zero-score results unless the current contract expects otherwise.
```

Keep it boring and explainable.

The point is not search quality yet.

The point is a stable shared evidence source for framework comparison.

---

## Controlled Corpus

Add a small controlled corpus for tests and future demos.

Prefer a location under `argonaut-core` test resources for now, for example:

```text id="y30jfg"
argonaut-core/src/test/resources/knowledge/
```

If a main-resource corpus is useful for future runtime demos, propose it in the report but do not overbuild it.

Suggested demo topic:

```text id="kicflk"
virtual threads and blocking I/O
structured concurrency
agent observability
retrieval evidence
```

Keep the corpus small, artificial, and clearly controlled.

Do not fetch web content.

Do not include copyrighted articles.

---

## Document Reading

`read` should retrieve a document by reference/id.

If the document does not exist, return the existing contract's appropriate empty/error behavior.

If the contract does not define this clearly, choose a simple behavior and document it.

Do not throw obscure exceptions for normal missing-document cases unless the existing contract expects that.

---

## Tests

Add unit tests covering at least:

```text id="g04xeo"
- repository can load documents;
- search finds a document by title term;
- search finds a document by body term;
- search respects limit/top N;
- search returns deterministic ordering;
- read returns document content by reference;
- read handles missing document predictably;
- empty query is handled predictably;
- no external dependency is required.
```

Run:

```bash id="qi86x1"
mvn verify
```

Expected: build success.

---

## Documentation

Update:

```text id="bb16ok"
argonaut-core/README.md
docs/knowledge/common-contract.md
```

Document:

```text id="n0fco6"
- why the first repository is deterministic and local;
- what it does;
- what it intentionally does not do;
- how future frameworks should use it;
- why embeddings/vector databases are deferred.
```

Do not claim this is production-grade search.

---

## Boundary Rules

Do not add dependencies on:

```text id="q2vt4t"
Spring AI
LangChain4j
LangGraph4j
Embabel
OpenRouter
Langfuse
LangSmith
OpenTelemetry
Lucene
Qdrant
OpenSearch
Elasticsearch
```

Do not introduce concepts named:

```text id="iayfl5"
Agent
Graph
Node
Planner
Workflow
UniversalOrchestrator
```

inside `argonaut-core`.

The core may expose capabilities and evidence contracts, not orchestration semantics.

---

## Engineering Reporting

Use the updated OSK reporting convention.

Create a detailed report under:

```text id="vgoqod"
docs/engineering/agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md
```

Update the engineering log as a compact index entry:

```text id="e7a3l8"
docs/engineering/ENGINEERING_LOG.md
```

The log entry must include:

```text id="t6nv64"
Date
Task
Type
Input
Output
Report
Validation
Status
```

Do not put the full report body in `ENGINEERING_LOG.md`.

The full evidence belongs in the report file.

---

## Non-Goals

Do not:

```text id="atxnul"
- implement any framework module;
- create Spring Boot applications;
- create HTTP controllers;
- create Vue UI;
- call OpenRouter;
- call any LLM;
- add embeddings;
- add vector search;
- add Lucene/Qdrant/OpenSearch;
- add Docker Compose;
- add Langfuse/LangSmith exporters;
- implement a general RAG pipeline;
- choose a winning framework.
```

This task only creates the first deterministic local knowledge repository.

---

## Final Report

Report:

```text id="w9vbzm"
- files changed;
- classes/records/interfaces added;
- search behavior implemented;
- read behavior implemented;
- test corpus location;
- tests added;
- mvn verify result;
- documentation updated;
- boundary validation;
- report path created;
- engineering log entry added;
- limitations;
- deferred work.
```

Do not commit changes.
