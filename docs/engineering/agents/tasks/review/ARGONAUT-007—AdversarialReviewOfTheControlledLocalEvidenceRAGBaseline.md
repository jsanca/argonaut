# ARGONAUT-007 — Adversarial Review of the Controlled Local Evidence RAG Baseline

## Assignee

Deep

## Mission

Perform an adversarial review of Argonaut's controlled local evidence RAG baseline before the first framework implementation is added.

The goal is to challenge assumptions, find counterexamples, identify weak invariants, expose validation gaps, and recommend concrete hardening actions.

Do not implement code.

Do not rewrite the design.

Do not start Spring AI yet.

This is a review task.

---

## Context

Argonaut currently has:

```text
ARGONAUT-003 → shared observable contracts in argonaut-core
ARGONAUT-004 → LocalKnowledgeRepository deterministic lexical baseline
ARGONAUT-005 → Controlled Local Evidence RAG use case
ARGONAUT-006 → Markdown-controlled local corpus and loader, if already completed
```

The intended experiment principle is:

```text
Same mission.
Same model.
Same evidence.
Different agentic frameworks.
```

Before adding Spring AI, LangChain4j, LangGraph4j, or Embabel, review whether the current baseline is strong enough to serve as the shared foundation.

---

## Required Skills

Use the installed OSK skills:

```text
osk-adversarial-analysis
osk-boundary-review
osk-verification-engineering
osk-engineering-reporting
```

Primary skill:

```text
osk-adversarial-analysis
```

The review should follow the adversarial flow:

```text
assumptions
→ counterexamples
→ failure modes
→ validation gaps
→ hardening/tests
```

Core principle:

```text
Do not criticize the design. Try to falsify its assumptions.
```

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
docs/engineering/agents/reports/
argonaut-core/README.md
argonaut-core/src/main/java/dev/jsanca/argonaut/core/
argonaut-core/src/test/java/dev/jsanca/argonaut/core/
```

Also inspect installed skill references:

```text
.osk/skills/osk-adversarial-analysis/
.osk/skills/osk-boundary-review/
.osk/skills/osk-verification-engineering/
.osk/skills/osk-engineering-reporting/
```

If `ARGONAUT-006` is not present yet, review only the programmatic corpus and clearly report that the Markdown corpus was unavailable.

---

## Review Target

Review the current controlled local evidence RAG baseline.

Focus on whether the baseline is good enough to support fair comparison across future framework implementations.

Review these areas:

```text
1. Experiment question and use case
2. Expected evidence IDs and corpus content
3. LocalKnowledgeRepository search behavior
4. Markdown loader / corpus materialization, if present
5. KnowledgeRepository contract boundaries
6. ExperimentResult / Evidence / Trace / Metrics contract
7. Observable trace expectations
8. Determinism and reproducibility
9. Validation and test coverage
10. Future framework adaptation risks
```

---

## Specific Questions to Challenge

Answer adversarially:

```text
What assumption could make the framework comparison unfair?

Can a framework satisfy the use case without really doing meaningful retrieval?

Can the lexical scorer retrieve the wrong documents because of common words?

Can the expected evidence criteria be gamed?

Is exp-001 too dominant or too easy to retrieve?

Are success criteria specific enough to become assertions?

Can the trace contain the required events while hiding important behavior?

Are metrics strong enough to compare implementations?

Can local corpus loading behave differently across OS/filesystems?

Can document ordering, tokenization, casing, encoding, or line endings affect determinism?

Does the core contract accidentally constrain framework orchestration?

Does the baseline leak implementation-specific assumptions into all frameworks?

Are missing document, empty query, malformed corpus, duplicate ID, and zero-result cases covered?

What failure would appear only after adding Spring AI or LangChain4j?
```

---

## Required Output

Create an adversarial review report.

Preferred path:

```text
docs/engineering/agents/reviews/ARGONAUT-007-controlled-local-evidence-rag-adversarial-review.md
```

If the repository convention uses reports instead of reviews for this type, use the closest OSK-compliant location and explain why.

The review must include:

```markdown
# ARGONAUT-007 — Controlled Local Evidence RAG Baseline — Adversarial Review

## Status

Complete | Partial | Blocked

## Reviewed Target

## Executive Summary

## Assumptions Identified

| Assumption | Evidence | Why It Matters | Confidence |
| --- | --- | --- | --- |

## Counterexamples

| Counterexample | Target Assumption | Expected Failure | Verification Status |
| --- | --- | --- | --- |

## Failure Modes

| Failure Mode | Trigger | Impact | Existing Coverage | Gap |
| --- | --- | --- | --- | --- |

## Boundary Conditions

## Invariants Challenged

## Validation Gaps

## Recommended Hardening

## Recommended Tests

## Findings by Severity

## Limitations

## Related Records
```

---

## Severity Model

Classify findings as:

```text
Critical
High
Medium
Low
Observation
```

Use judgment, but do not inflate severity.

A finding should be High or Critical only if it could invalidate the experiment, corrupt comparison fairness, hide failures, or cause misleading conclusions.

---

## Verification Rules

For each finding, mark verification status:

```text
Verified
Plausible but unverified
Not reproducible
Out of scope
```

Do not claim a bug is verified unless repository evidence or a command supports it.

If a counterexample can be tested cheaply, inspect the relevant tests or suggest an exact future test.

Do not modify production code.

---

## Expected Areas of Attention

Pay special attention to these likely weak spots:

```text
- lexical scoring and common-word noise
- source ID stability
- duplicate corpus document IDs
- deterministic ordering
- empty or malformed documents
- missing frontmatter
- trace event ordering
- event count assertions
- evidence selection semantics
- finalAnswer semantic assertions
- no-external-call enforcement
- metrics consistency
- framework adapters possibly bypassing the repository
```

---

## Validation

Run lightweight validation.

Required:

```bash
git diff --check
```

Preferred if cheap:

```bash
mvn verify
```

If validation is skipped, state why.

Do not claim validation that was not run.

---

## Engineering Reporting

Use the OSK engineering-reporting convention.

Create/update:

```text
docs/engineering/agents/reviews/ARGONAUT-007-controlled-local-evidence-rag-adversarial-review.md
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

Do not put the full review body in `ENGINEERING_LOG.md`.

---

## Non-Goals

Do not:

```text
- implement fixes;
- modify LocalKnowledgeRepository behavior;
- change the corpus;
- rewrite the use case;
- add Spring AI;
- add LangChain4j;
- add LangGraph4j;
- add Embabel;
- add OpenRouter;
- add LLM calls;
- add vector search;
- add Lucene/Qdrant/OpenSearch;
- create Vue UI;
- create Docker Compose;
- choose a winning framework.
```

This task reviews the baseline only.

---

## Final Response

Report:

```text
- review path created;
- number of assumptions identified;
- number of counterexamples;
- findings by severity;
- most important validation gaps;
- recommended next hardening task;
- validation commands/results;
- engineering log row verified;
- limitations.
```

Do not commit changes.
