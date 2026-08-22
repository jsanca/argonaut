# ARGONAUT-008B — TC-UC-001 Baseline Harness and Anti-Gaming Assertions

## Assignee

Clio

## Mission

Implement the first executable use-case validation harness for Argonaut: `TC-UC-001`.

This task converts the documented Controlled Local Evidence RAG use case into a reusable, framework-agnostic test contract that future framework implementations must satisfy.

The goal is to prevent hollow or gamed success before adding Spring AI, LangChain4j, LangGraph4j, or Embabel.

Do not start Spring AI yet.

Do not implement framework modules.

---

## Context

Argonaut's experiment principle is:

```text id="p6yavc"
Same mission.
Same model.
Same evidence.
Different agentic frameworks.
```

`ARGONAUT-007` identified the remaining High risks after the controlled corpus baseline:

```text id="q4fxv5"
- Success criteria are gameable.
- TC-UC-001 is documented but not implemented.
- Trace events can be emitted without real repository usage.
- Metrics can disagree with trace/evidence.
```

The adversarial review recommended:

```text id="1g8739"
H3 — Implement TC-UC-001 harness.
H4 — Add assertions beyond the minimum success criteria.
```

This task implements that slice.

`ARGONAUT-008A` should already have established the single authoritative corpus and corpus invariants. If not, stop and report that this task is blocked by corpus authority ambiguity.

---

## Read First

Inspect:

```text id="ta6i8t"
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
docs/engineering/agents/reports/ARGONAUT-008A-single-authoritative-corpus-and-invariants.md

argonaut-core/README.md
argonaut-core/src/main/java/dev/jsanca/argonaut/core/
argonaut-core/src/test/java/dev/jsanca/argonaut/core/
argonaut-core/src/test/resources/knowledge/controlled-local-evidence/
```

Also inspect installed OSK skills:

```text id="n9rg7q"
.osk/skills/osk-verification-engineering/
.osk/skills/osk-engineering-reporting/
.osk/skills/osk-boundary-review/
```

Use the ARGONAUT-007 adversarial review as the main input.

---

## Scope

Implement reusable validation support in `argonaut-core` for the first use case.

Preferred package:

```text id="9ev6f2"
dev.jsanca.argonaut.core.testing
```

or:

```text id="djc7d7"
dev.jsanca.argonaut.core.validation
```

Choose a name that does not imply framework orchestration.

Avoid names such as:

```text id="bqpx61"
Agent
Graph
Node
Planner
Workflow
UniversalOrchestrator
FrameworkRunner
```

The harness should validate `ExperimentResult` produced by a function, adapter, or future framework module without forcing a shared orchestration model.

---

## Required Design

Create a small test harness around this conceptual interface:

```java id="bhyk9r"
@FunctionalInterface
public interface ExperimentExecutor {
    ExperimentResult execute(ExperimentRequest request);
}
```

Name may differ.

This interface must represent only:

```text id="pxz0pp"
given request → result
```

It must not define orchestration semantics.

Do not call it `FrameworkRunner` unless there is already a repo convention for that term. The adversarial review warned that `FrameworkRunner` would homogenize the experiment.

---

## TC-UC-001 Definition

Create a reusable validator for the Controlled Local Evidence RAG use case.

Possible name:

```text id="nkws3v"
ControlledLocalEvidenceUseCaseContract
```

or:

```text id="592uud"
ControlledLocalEvidenceAssertions
```

It should expose something like:

```java id="igb4b9"
public static ExperimentRequest request();

public static void assertSatisfied(ExperimentResult result);
```

or:

```java id="z7mzeo"
public static void verify(ExperimentExecutor executor);
```

Exact API may differ, but future framework modules should be able to use it in tests.

---

## Required Assertions

The test contract must assert at least:

### 1. Result status

```text id="sm0qoc"
ExperimentResult.status == COMPLETED
```

### 2. Final answer present

```text id="n88wpv"
finalAnswer is non-blank
```

If `ExperimentResult.completed(...)` currently permits null or blank final answers, harden it in this task or add validator-level rejection.

Preferred: harden the factory/record constructor if it does not break legitimate failed/partial results.

### 3. Evidence includes exp-001

```text id="i0ociz"
At least one Evidence item has sourceId == exp-001.
```

### 4. Evidence is not arbitrary

Evidence source IDs must be traceable to real search/read behavior.

At minimum:

```text id="lyz8zb"
Every selected Evidence.sourceId must appear in either:
- a DOCUMENT_READ_COMPLETED event metadata field; or
- an EVIDENCE_RETRIEVED event metadata field; or
- another documented trace metadata convention created by this task.
```

If current `ExecutionEvent` metadata is too free-form, define a small metadata key convention.

Example:

```text id="q41llo"
sourceId
sourceIds
query
resultSourceIds
```

Keep it simple and document it.

### 5. Required trace events exist

The trace must contain:

```text id="6l1akp"
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

### 6. Trace order

Assert:

```text id="rwt4n9"
RUN_STARTED is first.
RUN_COMPLETED is last.
ANSWER_SYNTHESIZED occurs after EVIDENCE_SELECTED.
EVIDENCE_SELECTED occurs after EVIDENCE_RETRIEVED.
DOCUMENT_READ_COMPLETED occurs after DOCUMENT_READ_STARTED.
KNOWLEDGE_SEARCH_COMPLETED occurs after KNOWLEDGE_SEARCH_STARTED.
```

Do not require an overly rigid trace sequence that would prevent frameworks from being idiomatic.

Assert only coarse ordering invariants.

### 7. STARTED / COMPLETED pairing

For search and document read:

```text id="liut81"
KNOWLEDGE_SEARCH_COMPLETED count >= KNOWLEDGE_SEARCH_STARTED count
DOCUMENT_READ_COMPLETED count >= DOCUMENT_READ_STARTED count
```

If event IDs or correlation metadata exist, use them.

If not, use count/order as a baseline and report that richer correlation is deferred.

### 8. Metrics consistency

Assert metrics match observable result data:

```text id="hdkfra"
metrics.knowledgeSearches >= 1
metrics.documentReads >= 1
metrics.evidenceCount == result.evidence().size()
metrics.errors == result.errors().size()
```

If model/tool metrics exist:

```text id="uwkmjw"
modelCalls >= 0
toolCalls >= 0
durationMs >= 0
```

Do not over-constrain model calls yet. The local baseline may not require LLM calls.

### 9. Errors empty

For a successful baseline run:

```text id="hho25i"
errors is empty
```

### 10. No hollow result

Add at least one negative test proving the validator rejects a constructed hollow result that has:

```text id="m5zuub"
- COMPLETED status;
- non-blank finalAnswer;
- Evidence(sourceId=exp-001);
- minimal trace event types;
- but missing real search/read/evidence linkage or inconsistent metrics.
```

This is the core anti-gaming assertion.

---

## Reference Executor

Add a simple reference executor in tests only.

It should use:

```text id="cwzeco"
LocalKnowledgeRepository
authoritative corpus from ARGONAUT-008A
InMemoryExecutionObserver
```

It may be a test class or private fixture.

It should:

```text id="d5wlvg"
1. build the TC-UC-001 request;
2. record RUN_STARTED;
3. perform at least one real KnowledgeRepository.search();
4. perform at least one real KnowledgeRepository.read();
5. create Evidence from the read document(s);
6. record EVIDENCE_RETRIEVED and EVIDENCE_SELECTED with sourceId metadata;
7. synthesize a simple deterministic finalAnswer string from local evidence;
8. record ANSWER_SYNTHESIZED and RUN_COMPLETED;
9. return ExperimentResult with metrics consistent with trace/evidence.
```

This reference executor is not a framework implementation.

It is only a test fixture proving the harness can pass when behavior is honest.

Do not put framework-specific abstractions in it.

---

## Negative Test Fixtures

Add tests that fail the validator for:

```text id="fkg0jn"
- missing exp-001 evidence;
- blank finalAnswer;
- missing RUN_STARTED;
- RUN_COMPLETED not last;
- ANSWER_SYNTHESIZED before evidence selection;
- metrics evidenceCount mismatch;
- metrics knowledgeSearches == 0;
- evidence sourceId not represented in trace metadata;
- hollow hard-coded result.
```

Use clear assertion messages.

---

## Metadata Convention

Define and document minimal metadata keys for trace events.

Suggested keys:

```text id="lup2t0"
query
sourceId
sourceIds
evidenceId
```

Use these in the reference executor and validator.

Do not require every future event to use every key.

Required minimum for TC-UC-001:

```text id="s02z20"
DOCUMENT_READ_COMPLETED or EVIDENCE_RETRIEVED must expose sourceId.
EVIDENCE_SELECTED must expose sourceId or evidenceId.
KNOWLEDGE_SEARCH_COMPLETED should expose sourceIds when practical.
```

Update documentation accordingly.

---

## Documentation Updates

Update:

```text id="y04psq"
docs/knowledge/use-cases/controlled-local-evidence-rag.md
docs/knowledge/common-contract.md
argonaut-core/README.md
```

Document:

```text id="r6yhxe"
- TC-UC-001 now exists as executable validation;
- how future framework modules should reuse the harness;
- trace metadata conventions used by the validator;
- what anti-gaming checks exist;
- what remains deferred.
```

Do not rewrite the whole use case.

Add concise links/notes.

---

## Boundary Rules

Do not add dependencies on:

```text id="qaa42a"
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

```text id="n69ft7"
Agent
Graph
Node
Planner
Workflow
UniversalOrchestrator
FrameworkRunner
```

inside `argonaut-core`.

Do not create framework modules.

Do not create HTTP controllers.

Do not create Vue UI.

Do not call any LLM.

Do not fetch internet content.

---

## Interaction with ARGONAUT-008A

If `ARGONAUT-008A` changed corpus APIs, adapt to the new authoritative corpus access pattern.

The harness must use the authoritative corpus source.

Do not reintroduce a second corpus source.

If corpus authority is still ambiguous, stop and report the blocker.

---

## Tests

Add tests for:

```text id="or6fnd"
- reference executor satisfies TC-UC-001;
- each required assertion rejects a bad result;
- hollow hard-coded result fails;
- metrics mismatch fails;
- trace order failure fails;
- missing sourceId linkage fails;
- finalAnswer blank fails;
- missing exp-001 fails.
```

Run:

```bash id="8qi14x"
mvn test -pl argonaut-core
mvn verify
git diff --check
```

Report exact results.

Do not claim validation that was not run.

---

## Engineering Reporting

Use the OSK engineering-reporting convention.

Create a detailed report under:

```text id="ib0cbe"
docs/engineering/agents/reports/ARGONAUT-008B-tc-uc-001-baseline-harness-and-anti-gaming.md
```

Update:

```text id="oo7pm0"
docs/engineering/ENGINEERING_LOG.md
```

as a compact index entry.

The log entry must include:

```text id="1fx82d"
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

## Non-Goals

Do not:

```text id="zaw78o"
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
- choose a winning framework;
- implement full semantic answer grading;
- enforce no-external-call through network sandboxing;
- implement CI.
```

This task only creates the executable use-case contract and anti-gaming assertions.

---

## Final Report

Report:

```text id="zieumf"
- harness package/classes added;
- request factory / validator API;
- reference executor behavior;
- anti-gaming checks added;
- trace metadata convention;
- tests added;
- validation commands/results;
- docs updated;
- engineering report path;
- ENGINEERING_LOG.md row added and verified;
- limitations;
- recommended next task.
```

Do not commit changes.
