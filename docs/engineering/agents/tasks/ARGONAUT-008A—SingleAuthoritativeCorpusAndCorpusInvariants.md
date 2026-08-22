# ARGONAUT-008A — Single Authoritative Corpus and Corpus Invariants

## Assignee

Clio

## Mission

Harden Argonaut's controlled local evidence baseline by establishing a single authoritative corpus source and adding corpus-level invariants.

This task addresses the highest-priority finding from `ARGONAUT-007`: `LocalKnowledgeCorpus.demo()` and the Markdown corpus currently share IDs/titles but do not share the same body content, which can break the experiment principle:

```text
Same mission.
Same model.
Same evidence.
Different agentic frameworks.
```

The goal is to ensure future framework implementations cannot accidentally compare against different evidence surfaces.

Do not start Spring AI yet.

Do not implement framework modules.

---

## Context

`ARGONAUT-007` found that the baseline is a credible contract/fixture skeleton, but not yet a comparison-ready experiment harness.

The most important verified issue was:

```text
Dual corpus drift:
LocalKnowledgeCorpus.demo() and the Markdown corpus share IDs/titles but not body text.
Frameworks using withDemoCorpus() vs MarkdownCorpusLoader do not share the same evidence surface.
```

This can invalidate the fairness of future framework comparisons.

The review recommended:

```text
H1 — Declare one authoritative corpus.
H2 — Add parity test or make demo() load the same bytes.
H5 — Add golden query tests.
H6 — Reject duplicate IDs.
```

This task implements that hardening slice only.

TC-UC-001 and anti-gaming runner assertions belong to `ARGONAUT-008B`, not this task.

---

## Read First

Inspect:

```text
README.md
docs/OSK.md
docs/PROJECT.md

docs/knowledge/common-contract.md
docs/knowledge/use-cases/controlled-local-evidence-rag.md
docs/knowledge/corpus/controlled-local-evidence/

docs/engineering/ENGINEERING_LOG.md
docs/engineering/agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md
docs/engineering/agents/reports/ARGONAUT-005-controlled-local-evidence-rag-use-case.md
docs/engineering/agents/reports/ARGONAUT-006-controlled-markdown-knowledge-corpus.md
docs/engineering/agents/reviews/ARGONAUT-007-controlled-local-evidence-rag-adversarial-review.md

argonaut-core/README.md
argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/
argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/local/
argonaut-core/src/test/java/dev/jsanca/argonaut/core/knowledge/local/
argonaut-core/src/test/resources/knowledge/controlled-local-evidence/
```

Also inspect installed OSK skills:

```text
.osk/skills/osk-verification-engineering/
.osk/skills/osk-engineering-reporting/
.osk/skills/osk-boundary-review/
```

Use the ARGONAUT-007 adversarial review as the main input for this task.

---

## Primary Design Decision

Make the Markdown corpus the authoritative source of controlled evidence.

Preferred authoritative location:

```text
docs/knowledge/corpus/controlled-local-evidence/
```

The Java programmatic corpus must not silently diverge from the Markdown corpus.

Choose one of these approaches:

### Preferred Option A

Make `LocalKnowledgeCorpus.demo()` load from the same Markdown-backed content used by tests/runtime.

This makes the public convenience API return the authoritative corpus.

### Acceptable Option B

Keep `LocalKnowledgeCorpus.demo()` programmatic, but add a hard parity test proving it has the same IDs, titles, and normalized body content as the Markdown corpus.

If Option B is chosen, clearly document that `demo()` is a mirror and must stay in sync.

### Avoid

Do not allow both corpus sources to evolve independently.

Do not merely document the drift.

Do not leave future framework modules free to choose either source without an invariant.

---

## Required Outcomes

By the end of this task:

```text
1. There is one declared authoritative corpus.
2. demo() either uses the authoritative corpus or is proven equivalent by tests.
3. Duplicate document IDs are rejected.
4. Golden query tests lock expected retrieval behavior for exp-001.
5. Documentation no longer creates ambiguity between programmatic demo corpus and Markdown corpus.
6. The engineering report records exactly what was chosen and why.
```

---

## Corpus Authority

Update documentation so it is clear that:

```text
docs/knowledge/corpus/controlled-local-evidence/
→ human-readable authoritative corpus
```

If runtime/tests use a copy under:

```text
argonaut-core/src/test/resources/knowledge/controlled-local-evidence/
```

then document whether it is:

```text
- a test fixture copy;
- a classpath runtime fixture;
- or intentionally equivalent to docs/knowledge/corpus.
```

If both locations exist, add a test or validation that catches drift.

---

## Implementation Scope

Work only in `argonaut-core` and documentation.

Allowed code changes:

```text
- LocalKnowledgeCorpus
- LocalKnowledgeRepository
- MarkdownKnowledgeCorpusLoader or equivalent loader
- corpus-related tests
- ExperimentResult validation only if strictly necessary for corpus invariant work
```

Do not implement the full TC-UC-001 harness in this task.

Do not implement anti-gaming assertions yet, except where directly needed for corpus/retrieval invariants.

---

## Required Corpus Invariants

Add or update tests enforcing:

### 1. Stable IDs

The authoritative corpus must contain exactly these source IDs:

```text
exp-001
rag-001
obs-001
vt-001
sc-001
```

No missing IDs.

No extra IDs unless explicitly approved in documentation.

---

### 2. Stable titles

The authoritative titles must match the use case expectations:

```text
exp-001 → Controlled Evidence in AI Framework Experiments
rag-001 → Retrieval-Augmented Generation: Core Concepts
obs-001 → Observability Fundamentals for Agentic Systems
vt-001  → Virtual Threads and Blocking I/O in Java
sc-001  → Structured Concurrency in the JVM
```

---

### 3. Duplicate IDs rejected

`LocalKnowledgeCorpus.of(...)` and Markdown loading must reject duplicate source IDs.

A duplicate ID must fail fast with a clear exception message.

Do not allow `findById()` to silently return the first duplicate.

---

### 4. Non-blank content

Each controlled document must have non-blank content.

If a blank-body document is loaded, fail clearly.

---

### 5. Deterministic ordering

Loaded corpus order must be deterministic.

Preferred ordering:

```text
ascending source ID
```

or another clearly documented stable order.

---

### 6. demo() and Markdown equivalence

If `demo()` remains public, test one of:

```text
demo() loads exactly the Markdown corpus
```

or:

```text
demo() has the same normalized IDs, titles, and body content as the Markdown corpus
```

Normalize only harmless formatting differences such as line endings and trailing whitespace.

Do not hide real content drift.

---

## Golden Query Tests

Add tests against the authoritative corpus using `LocalKnowledgeRepository`.

Minimum golden queries:

```text
controlled local evidence
controlled evidence framework comparison
why controlled evidence before web search
same evidence different frameworks
```

Expected behavior:

```text
exp-001 appears in results for every golden query.
exp-001 ranks first for the strongest mission queries.
```

Suggested exact expectations:

```text
Query: "controlled local evidence"
→ exp-001 first

Query: "controlled evidence framework comparison"
→ exp-001 first

Query: "why controlled evidence before web search"
→ exp-001 present, preferably first

Query: "same evidence different frameworks"
→ exp-001 present, preferably first
```

If current lexical scoring cannot satisfy a reasonable query, do not blindly overfit.

Instead:

```text
- document the limitation;
- add the test that reflects the intended baseline behavior;
- minimally adjust corpus wording or tokenizer only if justified.
```

Keep the scorer simple.

Do not add embeddings, BM25, Lucene, Qdrant, OpenSearch, or stop-word libraries.

---

## Noisy Query Characterization

Add one characterization test for noisy/common-word behavior.

Example:

```text
the a and or of
```

The test should not necessarily require good ranking.

It should document expected behavior:

```text
- returns empty results
```

or:

```text
- returns weak/non-discriminative results
```

depending on current behavior.

This is a characterization test, not a search-quality project.

---

## Documentation Updates

Update:

```text
docs/knowledge/common-contract.md
docs/knowledge/use-cases/controlled-local-evidence-rag.md
argonaut-core/README.md
```

Clarify:

```text
- which corpus source is authoritative;
- whether demo() is authoritative, mirror, or compatibility API;
- how framework modules should obtain the controlled corpus;
- that framework implementations must use the same corpus content;
- that corpus drift is treated as a test failure;
- vector/hybrid search remains deferred.
```

Do not duplicate full document bodies inside `common-contract.md`.

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

Do not create framework modules.

Do not create HTTP controllers.

Do not create Vue UI.

Do not call any LLM.

Do not fetch internet content.

---

## Engineering Reporting

Use the OSK engineering-reporting convention.

Create a detailed report under:

```text
docs/engineering/agents/reports/ARGONAUT-008A-single-authoritative-corpus-and-invariants.md
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

Also run the relevant module tests explicitly if useful:

```bash
mvn test -pl argonaut-core
```

Report exact results.

Do not claim validation that was not run.

---

## Non-Goals

Do not:

```text
- implement TC-UC-001 harness;
- implement anti-gaming framework assertions;
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
- compare frameworks;
- choose a winning framework.
```

This task only hardens the corpus authority and corpus/search invariants.

---

## Final Report

Report:

```text
- authoritative corpus decision;
- files changed;
- whether demo() now loads Markdown or remains parity-tested;
- duplicate-ID behavior;
- non-blank-content behavior;
- deterministic-ordering behavior;
- golden query tests added;
- noisy query characterization;
- docs updated;
- engineering report path;
- ENGINEERING_LOG.md row added and verified;
- validation commands/results;
- limitations;
- recommended next task.
```

Do not commit changes.
