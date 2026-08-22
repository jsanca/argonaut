# ARGONAUT-005 — Document the First Controlled Knowledge Use Case

## Mission

Create the first durable Argonaut use case document under `docs/knowledge/`.

This use case will describe how Argonaut should use local controlled documents as evidence for a repeatable agentic RAG experiment.

The goal is not to implement the test yet.

The goal is to define the use case clearly enough that a later task can create a test case, local Markdown corpus, and framework runs based on it.

---

## Context

Argonaut is a comparative lab:

```text
Same mission.
Same model.
Same evidence.
Different agentic frameworks.
```

`ARGONAUT-003` defined the shared observable contracts.

`ARGONAUT-004` implemented the first deterministic `LocalKnowledgeRepository` in `argonaut-core`.

The repository currently supports:

```text
- controlled local documents;
- lexical search;
- document read by sourceId;
- stable deterministic ordering;
- demo corpus;
- no external services;
- no LLM calls;
- no vector database.
```

Now we need to define the first actual use case in `docs/knowledge/` so the experiment has a durable knowledge target and not only Java classes.

---

## Read First

Before writing, inspect:

```text
README.md
docs/OSK.md
docs/PROJECT.md
docs/knowledge/README.md
docs/knowledge/common-contract.md
docs/engineering/ENGINEERING_LOG.md
docs/engineering/agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md
argonaut-core/README.md
argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/
argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/local/
```

Also inspect the installed skill:

```text
.osk/skills/osk-knowledge-curator/
```

Use the knowledge-curator skill to decide what belongs in `docs/knowledge/`.

Use the engineering-reporting skill for the final report.

---

## Use Case to Document

Document the first use case around this mission:

```text
Given a local controlled corpus about Java virtual threads, structured concurrency, agent observability, RAG, and controlled evidence, Argonaut should answer a technical question by searching the corpus, reading relevant documents, selecting evidence, and producing an evidence-grounded answer with an observable execution trace.
```

The exact wording may be improved, but preserve the core idea:

```text
local documents
→ search
→ read
→ evidence selection
→ answer
→ trace
```

---

## Suggested User Question

Use this as the first candidate experiment question:

```text
When comparing agentic Java frameworks, why should Argonaut use controlled local evidence before introducing web search, vector databases, or external observability tools?
```

If you find a better question while reading the current docs, you may refine it.

The question must remain answerable from the controlled documents we plan to create.

Do not require internet access.

Do not require OpenRouter or any LLM yet.

---

## Document Location

Create:

```text
docs/knowledge/use-cases/controlled-local-evidence-rag.md
```

If `docs/knowledge/use-cases/` does not exist, create it.

Do not place this under `docs/engineering/`.

This is durable project/domain knowledge, not execution history.

---

## Required Document Structure

The document should include at least:

```markdown
# Controlled Local Evidence RAG Use Case

## Purpose

Why this use case exists.

## Actors

Who or what participates.

## Preconditions

What must exist before the use case can run.

## Trigger

What starts the use case.

## Main Flow

Step-by-step happy path.

## Expected Evidence

What documents or evidence should be retrieved/read.

## Expected Output

What the final answer/result should contain.

## Observable Trace Expectations

What execution events should be visible.

## Success Criteria

How we know the use case worked.

## Out of Scope

What this use case intentionally does not cover.

## Future Test Case Notes

How this document should later become an executable test case.
```

You may add sections if helpful, but keep the document readable.

---

## Actors to Consider

Include actors such as:

```text
Human evaluator
Argonaut framework implementation
KnowledgeRepository
LocalKnowledgeRepository
Controlled local corpus
ExecutionObserver / ExecutionTrace
Future UI consumer
```

Do not invent business actors unrelated to Argonaut.

---

## Preconditions

Include preconditions such as:

```text
- local controlled documents exist;
- each document has a stable id and title;
- KnowledgeRepository can search and read documents;
- execution events can be recorded;
- no external search, LLM, vector store, or observability exporter is required for the baseline.
```

---

## Main Flow

The main flow should describe something like:

```text
1. A human evaluator submits the experiment question.
2. A framework implementation receives the request.
3. The implementation searches the controlled corpus.
4. The implementation reads one or more relevant documents.
5. The implementation extracts/selects evidence.
6. The implementation produces an answer grounded in retrieved evidence.
7. The implementation records normalized execution events.
8. The result includes answer, evidence, metrics, and trace.
9. The UI or human evaluator can inspect what happened.
```

Keep this at use-case level.

Do not implement code.

---

## Expected Evidence

Base this on the current conceptual corpus topics from `ARGONAUT-004`:

```text
- Virtual Threads and Blocking I/O in Java
- Structured Concurrency in the JVM
- Observability Fundamentals for Agentic Systems
- Retrieval-Augmented Generation: Core Concepts
- Controlled Evidence in AI Framework Experiments
```

Do not copy implementation details unnecessarily.

Do not require exact document text yet.

This task defines the use case; a future task may create Markdown documents for the corpus.

---

## Observable Trace Expectations

Describe expected event types conceptually.

Examples:

```text
RUN_STARTED
KNOWLEDGE_SEARCH_STARTED
KNOWLEDGE_SEARCH_COMPLETED
DOCUMENT_READ_STARTED
DOCUMENT_READ_COMPLETED
EVIDENCE_RETRIEVED
EVIDENCE_SELECTED
ANSWER_SYNTHESIZED
RUN_COMPLETED
```

Use names that already exist in the core contracts when possible.

Do not invent a new trace model.

---

## Success Criteria

The use case succeeds when:

```text
- the same question can be run repeatedly against the same local corpus;
- the same evidence source ids are available;
- the answer is grounded in local documents;
- the trace shows search/read/evidence/answer steps;
- no internet or external model/tool is required for the baseline;
- future framework implementations can run the same use case with their own idiomatic orchestration.
```

---

## Out of Scope

Explicitly exclude:

```text
- web search;
- live internet research;
- OpenRouter calls;
- LLM answer quality evaluation;
- vector database setup;
- Lucene/Qdrant/OpenSearch;
- Langfuse/LangSmith/OpenTelemetry exporters;
- Vue UI implementation;
- Docker Compose;
- choosing a winning framework.
```

---

## Documentation Updates

Update `docs/knowledge/common-contract.md` only if needed to link to the new use case.

Do not over-document.

Do not rewrite the ARGONAUT-004 report.

---

## Engineering Reporting

Use the OSK engineering-reporting convention.

Create a detailed report under:

```text
docs/engineering/agents/reports/ARGONAUT-005-controlled-local-evidence-rag-use-case.md
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

Run lightweight validation:

```bash
git diff --check
```

If available and cheap, also run:

```bash
mvn verify
```

Since this is documentation-only, `mvn verify` is optional but preferred if the repo is already in a clean/fast state.

Report exactly what was run.

Do not claim tests passed if they were not run.

---

## Boundary Rules

Do not:

```text
- write Java implementation;
- modify LocalKnowledgeRepository behavior;
- add dependencies;
- add framework modules;
- create HTTP controllers;
- create Vue UI;
- create Docker Compose;
- create ADRs unless a real decision is made;
- move existing OSK files;
- rewrite prior reports.
```

This is a documentation and knowledge-curation task.

---

## Final Report

Report:

```text
- use case document created;
- any knowledge docs updated;
- engineering report path;
- ENGINEERING_LOG.md row added and verified;
- validation commands/results;
- limitations;
- proposed next task.
```

Do not commit changes.
