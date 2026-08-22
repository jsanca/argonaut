# ARGONAUT-008B — TC-UC-001 Baseline Harness and Anti-Gaming Assertions

**Date:** 2026-08-14
**Task:** ARGONAUT-008B — TC-UC-001 Baseline Harness and Anti-Gaming Assertions
**Type:** Implementation / Verification harness
**Status:** Complete

---

## Objective

Convert the documented Controlled Local Evidence RAG use case into an executable, framework-agnostic test contract (`TC-UC-001`) that:

- future framework implementations can plug into to prove they satisfy the use case;
- rejects hollow or gamed results that satisfy structural criteria without performing real retrieval;
- is located in `argonaut-core` and depends on no external libraries.

This task implements H3 and H4 from the ARGONAUT-007 adversarial review.

---

## Prerequisite Verified

ARGONAUT-008A is complete: the Markdown corpus is authoritative, `LocalKnowledgeCorpus.demo()` loads from it, and corpus drift is a test failure. The harness uses `LocalKnowledgeRepository.withDemoCorpus()` — no second corpus source was introduced.

---

## Files Created

### Main sources — `dev.jsanca.argonaut.core.testing`

| Class | Purpose |
| --- | --- |
| `ExperimentExecutor` | `@FunctionalInterface` — `given ExperimentRequest → ExperimentResult`. Carries no orchestration semantics. Framework modules implement this to plug into the contract. |
| `TraceEventMetadata` | String constants for `ExecutionEvent.metadata()` keys: `SOURCE_ID`, `SOURCE_IDS`, `QUERY`, `EVIDENCE_ID`. Documents which events must carry which keys. |
| `ControlledLocalEvidenceContract` | TC-UC-001 assertions. Provides `request()`, `assertSatisfied(result)`, and `verify(executor)`. Throws `AssertionError` with SC-prefixed messages for each failure. |

### Test sources

| Class | Purpose |
| --- | --- |
| `ReferenceExecutor` | Test-only fixture performing real `KnowledgeRepository.search()` and `.read()` calls, recording proper trace events with metadata, synthesizing a deterministic finalAnswer. Proves the contract is passable when behavior is honest. |
| `TcUc001Test` | 21 tests: 5 positive (reference executor passes, result fields verified), 16 negative (each SC assertion fires correctly on bad input). |

### Modified — `ExperimentResult`

`ExperimentResult.completed()` now rejects blank `finalAnswer`:

```java
if (finalAnswer == null || finalAnswer.isBlank()) {
    throw new IllegalArgumentException("finalAnswer must not be blank for a COMPLETED result");
}
```

This addresses H7 from ARGONAUT-007. Existing tests are unaffected (all used non-blank answers). `failed()` still accepts null finalAnswer — that is correct behavior.

---

## TC-UC-001 Contract Design

### Request factory

```java
ExperimentRequest request = ControlledLocalEvidenceContract.request();
// runId = "tc-uc-001"
// question = "When comparing agentic Java frameworks, why should Argonaut use controlled
//             local evidence before introducing web search, vector databases, or external
//             observability tools?"
```

### Nine assertions

| SC | What is asserted | Anti-gaming relevance |
| --- | --- | --- |
| SC1 | `result.status == COMPLETED` | Structural |
| SC2 | `finalAnswer` not blank | Prevents empty-answer pass |
| SC3 | `evidence` contains `exp-001` sourceId | Requires primary evidence present |
| SC4 | Every `Evidence.sourceId` appears in a `DOCUMENT_READ_COMPLETED` or `EVIDENCE_RETRIEVED` event's `sourceId` metadata | **Core anti-gaming check** |
| SC5 | All required event types present (9 types) | Structural completeness |
| SC6 | Trace ordering: RUN_STARTED first, RUN_COMPLETED last, coarse phase order | Ordering sanity |
| SC7 | SEARCH_COMPLETED count ≥ SEARCH_STARTED count; READ_COMPLETED count ≥ READ_STARTED count | Pairing |
| SC8 | metrics.knowledgeSearches ≥ 1; documentReads ≥ 1; evidenceCount == evidence.size(); errors == errors.size() | Metrics consistency |
| SC9 | `errors` empty | No error for a good run |

### SC4 — Anti-gaming mechanism

The key anti-gaming check verifies that evidence items were not hand-assembled without real repository usage. The validator:

1. Scans all `DOCUMENT_READ_COMPLETED` and `EVIDENCE_RETRIEVED` events in the trace.
2. Extracts `sourceId` (single) or `sourceIds` (comma-separated) from each event's metadata.
3. For every `Evidence` in the result, asserts its `sourceId` appears in the collected set.

A framework that constructs `Evidence(sourceId="exp-001")` without calling `KnowledgeRepository.read()` and recording a `DOCUMENT_READ_COMPLETED` event with `metadata["sourceId"]="exp-001"` will fail SC4.

---

## Trace Metadata Convention

Framework implementations must record the following metadata in specified events:

| Event | Required key | Value format |
| --- | --- | --- |
| `DOCUMENT_READ_COMPLETED` | `TraceEventMetadata.SOURCE_ID` | Single document source ID |
| `EVIDENCE_RETRIEVED` | `TraceEventMetadata.SOURCE_ID` | Single document source ID |
| `EVIDENCE_SELECTED` | `TraceEventMetadata.SOURCE_ID` or `TraceEventMetadata.EVIDENCE_ID` | Document or evidence item ID |
| `KNOWLEDGE_SEARCH_COMPLETED` | `TraceEventMetadata.SOURCE_IDS` | Comma-separated source IDs |

Other events may include additional metadata freely. These four requirements are the minimum for TC-UC-001 validation.

---

## Reference Executor Behavior

`ReferenceExecutor` is a test-only fixture that proves the harness passes when retrieval is honest:

1. Records `RUN_STARTED`.
2. Issues two `KnowledgeRepository.search()` calls (query 1: "controlled evidence framework comparison"; query 2: "controlled local evidence reproducible deterministic"). Records `KNOWLEDGE_SEARCH_STARTED/COMPLETED` with `sourceIds` metadata.
3. Reads top-3 deduplicated, score-ranked documents. Records `DOCUMENT_READ_STARTED/COMPLETED` with `sourceId` metadata.
4. Records `EVIDENCE_RETRIEVED` and `EVIDENCE_SELECTED` with `sourceId` and `evidenceId` metadata for each document.
5. Synthesizes a deterministic finalAnswer from evidence excerpts (no LLM call — template-based).
6. Records `ANSWER_SYNTHESIZED` and `RUN_COMPLETED`.
7. Returns `ExperimentResult.completed(...)` with consistent metrics.

Because both search queries rank `exp-001` first on the authoritative Markdown corpus, the reference executor reliably includes `exp-001` in the evidence list.

---

## Negative Tests (16 cases)

| Test | SC triggered |
| --- | --- |
| `validator_rejects_failed_status` | SC1 |
| `completed_factory_rejects_blank_final_answer` | factory guard |
| `completed_factory_rejects_null_final_answer` | factory guard |
| `validator_rejects_blank_final_answer_via_record_constructor` | SC2 |
| `validator_rejects_missing_exp001_evidence` | SC3 |
| `validator_rejects_hollow_hard_coded_result` | SC4 |
| `validator_rejects_evidence_sourceId_not_in_any_trace_event` | SC4 |
| `validator_rejects_missing_run_started` | SC5 |
| `validator_rejects_missing_knowledge_search_started` | SC5 |
| `validator_rejects_missing_document_read_completed` | SC5 |
| `validator_rejects_missing_evidence_selected` | SC5 |
| `validator_rejects_missing_answer_synthesized` | SC5 |
| `validator_rejects_run_completed_not_last` | SC6 |
| `validator_rejects_answer_synthesized_before_evidence_selected` | SC6 |
| `validator_rejects_metrics_evidence_count_mismatch` | SC8 |
| `validator_rejects_zero_knowledge_searches` | SC8 |

---

## Validation

**`git diff --check`:** clean — no trailing whitespace errors.

**`mvn verify`:** BUILD SUCCESS — 107 tests, 0 failures, 0 errors. (21 new tests in `TcUc001Test`; prior count was 86.)

```
Tests run: 107, Failures: 0, Errors: 0, Skipped: 0
```

---

## Limitations

- **SC4 traceability is event-metadata-only.** If a framework records the correct metadata without actually calling `KnowledgeRepository`, SC4 passes. This is a structural check, not a byte-code intercept. The reference executor is evidence that honest implementations naturally emit the right metadata.
- **SC6 ordering is coarse.** The check uses first-occurrence index comparison. A framework that emits multiple search and read phases interleaved (e.g. iterative retrieval) may need both phases completed before `EVIDENCE_SELECTED` — the current check only requires `indexOf(first EVIDENCE_RETRIEVED) < indexOf(first EVIDENCE_SELECTED)`. This is intentionally permissive to avoid over-constraining framework idioms.
- **SC7 pairing uses counts, not correlation IDs.** `KNOWLEDGE_SEARCH_COMPLETED ≥ KNOWLEDGE_SEARCH_STARTED` catches gross imbalances. Richer per-call correlation (matching event IDs) is deferred.
- **SC8 metrics are self-reported.** The contract asserts internal consistency (evidenceCount == evidence.size()) but cannot verify that `knowledgeSearches` matches the actual number of `search()` calls without inspecting the implementation.
- **SC9 (no-external-call) is behavioral, not enforced.** The contract asserts `errors` is empty; it cannot prevent a framework from making external network calls.
- **`finalAnswer` semantic grounding** is not checked. SC2 requires non-blank text; it does not require the text to be derived from the evidence. Full semantic grading is deferred (H4/A11 from ARGONAUT-007).
- **The reference executor's finalAnswer is template-based.** It is deterministic but not a natural-language answer. It proves the harness structure, not answer quality.

---

## Recommended Next Task

**ARGONAUT-009 — Spring AI Module Skeleton**

With TC-UC-001 in place as an executable contract and the authoritative corpus established, the first framework implementation can begin. Spring AI is the recommended first framework: wire `LocalKnowledgeRepository.withDemoCorpus()` as a Spring AI tool, emit the required trace events with `TraceEventMetadata` keys, and pass `ControlledLocalEvidenceContract.verify(...)` in the module's test suite.
