# ARGONAUT-006 — Materialize the Controlled Local Knowledge Corpus as Markdown

## Mission

Move Argonaut's first controlled knowledge corpus from programmatic demo documents into local Markdown documents, then add a deterministic loader that converts those Markdown files into `LocalKnowledgeDocument` instances.

The goal is to make the corpus visible, editable, reviewable, and versioned as durable project knowledge while keeping the experiment deterministic and dependency-light.

This task prepares the ground for future executable use-case tests and framework implementations.

---

## Context

Argonaut now has:

```text
ARGONAUT-003 → shared observable contracts
ARGONAUT-004 → LocalKnowledgeRepository with programmatic demo corpus
ARGONAUT-005 → Controlled Local Evidence RAG use case
```

The current use case expects a controlled corpus with stable source IDs:

```text
exp-001
rag-001
obs-001
vt-001
sc-001
```

The corpus currently exists as synthetic documents in code through `LocalKnowledgeCorpus.demo()`.

That was acceptable for the first baseline, but the next step is to make the corpus real local evidence.

---

## Read First

Before implementing, inspect:

```text
README.md
docs/OSK.md
docs/PROJECT.md
docs/knowledge/README.md
docs/knowledge/common-contract.md
docs/knowledge/use-cases/controlled-local-evidence-rag.md
docs/engineering/ENGINEERING_LOG.md
docs/engineering/agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md
docs/engineering/agents/reports/ARGONAUT-005-controlled-local-evidence-rag-use-case.md
argonaut-core/README.md
argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/
argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/local/
argonaut-core/src/test/java/dev/jsanca/argonaut/core/knowledge/local/
```

Also inspect installed OSK skills:

```text
.osk/skills/osk-knowledge-curator/
.osk/skills/osk-engineering-reporting/
.osk/skills/osk-verification-engineering/
```

Use `osk-knowledge-curator` to decide what belongs in durable knowledge.

Use `osk-engineering-reporting` for the final report.

---

## Design Goal

Make the controlled corpus available as Markdown documents.

Preferred canonical source:

```text
docs/knowledge/corpus/controlled-local-evidence/
```

Expected files:

```text
docs/knowledge/corpus/controlled-local-evidence/
├── exp-001-controlled-evidence.md
├── rag-001-retrieval-augmented-generation.md
├── obs-001-agentic-observability.md
├── vt-001-virtual-threads-blocking-io.md
└── sc-001-structured-concurrency.md
```

These documents are durable knowledge.

They should be understandable by humans and consumable by the local repository loader.

---

## Required Corpus Documents

Create five Markdown documents matching the IDs expected by the use case.

### `exp-001`

Title:

```text
Controlled Evidence in AI Framework Experiments
```

Purpose:

Explain why Argonaut uses controlled local evidence before adding web search, vector databases, external observability tools, or live LLM-based research.

This is the primary evidence source for the first use case.

It should support claims such as:

```text
- controlled evidence reduces experimental noise;
- all frameworks should receive the same source material;
- reproducible evidence helps distinguish framework orchestration behavior from retrieval variability;
- external retrieval systems should be introduced only after the baseline is stable.
```

---

### `rag-001`

Title:

```text
Retrieval-Augmented Generation: Core Concepts
```

Purpose:

Explain RAG at a conceptual level for the Argonaut experiment.

It should support claims such as:

```text
- retrieval provides source material before answer synthesis;
- search and document reading are separate observable steps;
- evidence selection should happen before final answer generation;
- RAG systems should expose which sources influenced the answer.
```

Do not make this a general internet-sourced RAG article.

Keep it specific to Argonaut's controlled experiment.

---

### `obs-001`

Title:

```text
Observability Fundamentals for Agentic Systems
```

Purpose:

Explain why Argonaut records normalized execution traces and metrics.

It should support claims such as:

```text
- humans need to inspect what happened during a run;
- trace events make search, read, evidence selection, and answer synthesis visible;
- comparable traces allow different frameworks to be evaluated without knowing their internals;
- external exporters are optional adapters, not the source of truth.
```

---

### `vt-001`

Title:

```text
Virtual Threads and Blocking I/O in Java
```

Purpose:

Provide Java concurrency context for future framework/runtime experiments.

It may be optional evidence for the first use case.

It should support claims such as:

```text
- virtual threads are useful for workloads that spend time waiting on blocking I/O;
- framework implementations may need to consider concurrency behavior;
- Java runtime behavior can affect experiment execution but is not the main focus of the first use case.
```

Keep it short and local.

Do not turn it into a full Java tutorial.

---

### `sc-001`

Title:

```text
Structured Concurrency in the JVM
```

Purpose:

Provide Java structured-concurrency context for future framework/runtime experiments.

It may be optional evidence for the first use case.

It should support claims such as:

```text
- structured concurrency groups related concurrent work;
- cancellation and failure propagation matter when a run performs multiple steps;
- this may become relevant when framework implementations perform parallel searches, reads, or model calls.
```

Keep it short and local.

Do not turn it into a full Java tutorial.

---

## Markdown Format

Use lightweight frontmatter.

Preferred format:

```markdown
---
id: exp-001
title: Controlled Evidence in AI Framework Experiments
topic: experiment-design
version: 1
---

# Controlled Evidence in AI Framework Experiments

Body text...
```

Required frontmatter fields:

```text
id
title
topic
version
```

Optional fields are allowed if useful, but keep the parser simple.

Do not require YAML libraries.

---

## Loader Implementation

Add a deterministic Markdown corpus loader in `argonaut-core`.

Suggested package:

```text
dev.jsanca.argonaut.core.knowledge.local
```

Possible class names:

```text
MarkdownKnowledgeCorpusLoader
LocalKnowledgeCorpusLoader
```

The exact naming is up to the implementation, but keep it local and boring.

The loader should:

```text
- load all `.md` files from a given directory;
- parse minimal frontmatter;
- require `id` and `title`;
- preserve extra frontmatter as metadata;
- treat the Markdown body as document content;
- return `LocalKnowledgeCorpus`;
- produce deterministic ordering, preferably by source ID or filename;
- fail with clear exceptions for malformed documents.
```

Avoid adding Jackson, SnakeYAML, commonmark, or other parsing dependencies.

Implement a tiny parser sufficient for the controlled format.

---

## Source Location and Runtime Choice

The canonical human-maintained corpus should live under:

```text
docs/knowledge/corpus/controlled-local-evidence/
```

For tests, either:

1. load directly from that docs path; or
2. copy a small fixture into:

```text
argonaut-core/src/test/resources/knowledge/controlled-local-evidence/
```

Preferred approach:

```text
docs/knowledge/corpus/controlled-local-evidence/
→ canonical corpus

argonaut-core/src/test/resources/knowledge/controlled-local-evidence/
→ test fixture copy only if needed for stable module-local tests
```

If duplicating fixtures, document the duplication and keep it minimal.

Do not overbuild synchronization between docs and test resources in this task.

---

## LocalKnowledgeCorpus.demo()

Update `LocalKnowledgeCorpus.demo()` carefully.

Preferred behavior:

```text
LocalKnowledgeCorpus.demo()
→ returns the same five documents and stable IDs as before
```

Implementation options:

```text
Option A:
Keep demo() programmatic for now, but add a separate loader and tests.

Option B:
Make demo() load the Markdown corpus from classpath resources.

Option C:
Add a new factory such as fromMarkdownDirectory(Path) and leave demo() unchanged.
```

Recommended for this task:

```text
Add the loader and tests first.
Keep demo() stable.
Do not break existing tests or use case expectations.
```

If making `demo()` file-backed adds classpath/resource complexity, defer that change and report it.

---

## Tests

Add tests for the Markdown loader.

Minimum test coverage:

```text
- loads all five controlled corpus documents;
- IDs match exp-001, rag-001, obs-001, vt-001, sc-001;
- titles match the use case expectations;
- metadata includes topic and version;
- body content is not blank;
- loaded corpus can be searched through LocalKnowledgeRepository;
- query related to controlled evidence ranks or retrieves exp-001;
- query related to observability retrieves obs-001;
- malformed markdown missing id fails clearly;
- malformed markdown missing title fails clearly;
- deterministic ordering is preserved.
```

Run:

```bash
mvn verify
```

Expected: build success.

---

## Documentation Updates

Update:

```text
docs/knowledge/common-contract.md
docs/knowledge/use-cases/controlled-local-evidence-rag.md
argonaut-core/README.md
```

Document:

```text
- where the controlled corpus lives;
- that Markdown files are the human-readable source of evidence;
- how the loader maps Markdown frontmatter/body into LocalKnowledgeDocument;
- that LocalKnowledgeCorpus.demo() remains stable or how it changed;
- that vector search, Lucene, Qdrant, OpenSearch, and external search remain deferred.
```

Keep updates concise.

Do not duplicate the full corpus inside `common-contract.md`.

Link to the corpus directory instead.

---

## Boundary Rules

Do not add dependencies on:

```text
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
Jackson
SnakeYAML
CommonMark
```

Do not introduce concepts named:

```text
Agent
Graph
Node
Planner
Workflow
UniversalOrchestrator
```

inside `argonaut-core`.

Do not implement framework modules.

Do not create HTTP controllers.

Do not create Vue UI.

Do not call any LLM.

Do not fetch internet content.

---

## Engineering Reporting

Use the updated OSK reporting convention.

Create a detailed report under:

```text
docs/engineering/agents/reports/ARGONAUT-006-controlled-markdown-knowledge-corpus.md
```

Update:

```text
docs/engineering/ENGINEERING_LOG.md
```

as a compact index entry.

The log entry must include:

```text
Date
Task
Type
Input
Output
Report
Validation
Status
```

Before final response, verify that the `ENGINEERING_LOG.md` diff actually contains the new row.

Do not put the full report body in `ENGINEERING_LOG.md`.

---

## Validation

Run:

```bash
git diff --check
mvn verify
```

Report exact results.

Do not claim validation that was not run.

---

## Non-Goals

Do not:

```text
- implement Spring AI;
- implement LangChain4j;
- implement LangGraph4j;
- implement Embabel;
- add OpenRouter;
- add LLM calls;
- add embeddings;
- add vector search;
- add Lucene/Qdrant/OpenSearch;
- add Docker Compose;
- add UI;
- create an evaluation scoring framework;
- compare frameworks;
- choose a winning framework.
```

This task only materializes the controlled local corpus as Markdown and adds deterministic loading support.

---

## Final Report

Report:

```text
- corpus directory created;
- Markdown documents created;
- loader classes added;
- parser behavior;
- tests added;
- search validation from loaded corpus;
- docs updated;
- engineering report path;
- ENGINEERING_LOG.md row added and verified;
- validation commands/results;
- limitations;
- deferred work.
```

Do not commit changes.
