# ARGONAUT-005 — Controlled Local Evidence RAG Use Case

**Date:** 2026-08-13
**Task:** ARGONAUT-005 — Document the First Controlled Knowledge Use Case
**Type:** Knowledge curation / Documentation
**Status:** Complete

---

## Objective

Create the first durable Argonaut use case document in `docs/knowledge/` defining the behavioral target for the initial controlled-corpus agentic RAG experiment. The document must be specific enough that a future task can derive an executable test case from it without further design work.

---

## Mode

Authorized knowledge creation. Scope: new use case document and a link addition to `common-contract.md`. No existing records were modified or superseded.

---

## Authorities and Inputs Consulted

| Input | Purpose |
| --- | --- |
| `docs/OSK.md` — information model | Confirmed use case belongs in `docs/knowledge/`, not `docs/engineering/` |
| `.osk/skills/osk-knowledge-curator/SKILL.md` | Durability test: retain knowledge useful across implementation changes; do not elevate transient delivery detail |
| `docs/knowledge/common-contract.md` | Existing canonical knowledge — used to link the new use case, not duplicated |
| `docs/engineering/agents/reports/ARGONAUT-004-*` | Evidence for corpus contents, document IDs, and search/read capabilities |
| ARGONAUT-003 trace contract (`ExecutionEventType`) | Source of truth for permitted event type names in the trace expectations table |

---

## Durability Test Applied

Per the knowledge-curator skill, the durability test asks whether the content will remain useful across implementation changes.

**Verdict: passes.** The use case document describes the experiment question, expected evidence source IDs, trace event expectations, and success criteria at the behavioral level. It does not describe how any specific framework orchestrates the flow, which dependencies are on the classpath, or any transient delivery detail. It will remain valid as implementations are built, changed, and compared.

---

## Files Changed

### Created

| File | Type | Purpose |
| --- | --- | --- |
| `docs/knowledge/use-cases/controlled-local-evidence-rag.md` | Durable knowledge | Full use case: purpose, actors, preconditions, trigger, main flow, expected evidence, expected output, trace expectations, success criteria, out of scope, future test case notes |

### Updated

| File | Change |
| --- | --- |
| `docs/knowledge/common-contract.md` | Added "Use cases" section at end with link to the new document |
| `docs/engineering/ENGINEERING_LOG.md` | Compact ARGONAUT-005 index entry |

---

## Document Summary

**Location:** `docs/knowledge/use-cases/controlled-local-evidence-rag.md`

**Experiment question:**
> When comparing agentic Java frameworks, why should Argonaut use controlled local evidence before introducing web search, vector databases, or external observability tools?

**Expected evidence (from `LocalKnowledgeCorpus.demo()`):**

| Priority | Source ID | Title |
| --- | --- | --- |
| Primary | `exp-001` | Controlled Evidence in AI Framework Experiments |
| Supporting | `rag-001` | Retrieval-Augmented Generation: Core Concepts |
| Supporting | `obs-001` | Observability Fundamentals for Agentic Systems |

**Minimum required trace events:** `RUN_STARTED` → `KNOWLEDGE_SEARCH_*` → `DOCUMENT_READ_*` → `EVIDENCE_RETRIEVED` → `EVIDENCE_SELECTED` → `ANSWER_SYNTHESIZED` → `RUN_COMPLETED`

**Nine success criteria** defined, covering status, answer content, evidence coverage, trace completeness, metrics counts, determinism, and no-external-call requirement for baseline verification.

**Future test case note:** maps to `TC-UC-001`; each success criterion maps to an assertion; corpus fixture is `LocalKnowledgeCorpus.demo()`.

---

## Validation

**`git diff --check`:** clean (no trailing whitespace errors)

**`mvn verify`:** BUILD SUCCESS — 56 tests, 0 failures, 0 errors. No Java source files were modified; the build result confirms no regressions from documentation changes.

---

## Limitations

- The use case document references the current five demo documents by ID. If the demo corpus changes in a future task, the Expected Evidence table must be updated.
- The expected output section describes semantic content that the answer must address; it does not define exact wording or a scoring rubric. A future evaluation task may need to formalize this into assertions or a grading checklist.
- The use case does not yet specify a concrete timeout or metric thresholds (e.g. `durationMs < 5000`). These may be added once framework runtimes are known.

---

## Proposed Next Task

**ARGONAUT-006 — Implement Spring AI Module Skeleton with Common HTTP API**

The use case and contracts are now defined. The next natural step is the first framework implementation — wiring Spring Boot, Spring AI, the `LocalKnowledgeRepository`, the `ExecutionObserver`, and the `POST /api/experiment/run` endpoint in `argonaut-spring-ai`. The use case document provides the acceptance target; the core contracts provide the types.
