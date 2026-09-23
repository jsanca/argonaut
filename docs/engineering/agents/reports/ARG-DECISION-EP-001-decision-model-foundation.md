# ARG-DECISION-EP-001 — Decision Model Foundation: Implementation Report

**Date:** 2026-09-22
**Plan:** ARG-DECISION-EP-001
**Module:** `argonaut-decision`
**Status:** VERIFIED

---

## Summary

The `argonaut-decision` domain skeleton has been implemented and verified. All 10 TDD scenarios from §24 of ARG-DECISION-EP-001 pass. The exit criteria from §27 compile and run without consumer casts, Jev dependencies, ChatModel dependencies, or implicit Noul thresholds.

---

## Deliverables

### 1. Module and package skeleton

New Maven module `argonaut-decision` added to the reactor. Base package `dev.jsanca.argonaut.decision`.

```
argonaut-decision/src/main/java/dev/jsanca/argonaut/decision/
    probability/
        Probability.java
        OutcomeProbability.java
        ProbabilityDistribution.java
    question/
        Question.java          (sealed interface)
        Choice.java            (record)
        Score.java             (record)
        Noul.java              (record)
    result/
        AnswerResult.java      (sealed interface)
        ChoiceResult.java      (record)
        ScoreResult.java       (record)
        NoulResult.java        (record)
    DecisionCapability.java    (enum)
    DecisionContext.java       (record)
    DecisionModel.java         (interface)
    DecisionRequest.java       (final class + builder)
    DecisionResult.java        (final class + builder)
```

### 2. Domain types

All types from the §23 "first slice" list are implemented. `AnswerResult<T>` is included (it is required to express the type bound `R extends AnswerResult<T>` in `DecisionResult.get()` without which no-consumer-cast retrieval is not achievable).

`DecisionCapability` and `DecisionModelListener` status:
- `DecisionCapability` — included. The capability vocabulary is useful and its inclusion surfaced the MULTI_QUESTION vs NATIVE_BATCH distinction, which is documented in the enum Javadoc.
- `DecisionModelListener` — not included. Not required by the first slice and no implementation pressure arose.

### 3. Relationships

The type hierarchy is:

```
Question<T, R extends AnswerResult<T>>  (sealed: Choice, Score, Noul)
    Choice<T>   → ChoiceResult<T>
    Score<T>    → ScoreResult<T>
    Noul        → NoulResult            (T = Void)

AnswerResult<T> (sealed: ChoiceResult, ScoreResult, NoulResult)
```

`DecisionResult.get(Question<T,R>)` returns `R` with no consumer cast. The single unchecked cast is isolated inside `DecisionResult.get()`, documented, and safe by construction via the builder's typed `answer()` method.

### 4. TDD suite

**Test file:** `DecisionModelContractTest.java` (11 tests) + `ProbabilityTest.java` (8) + `QuestionValidationTest.java` (9) + `DecisionRequestTest.java` (6) = **34 tests total**

| TC ID | Scenario | Result |
|-------|----------|--------|
| TC-DM-001 | Typed Choice (`Choice<Route>` → `ChoiceResult<Route>`) | PASS |
| TC-DM-002 | String Choice | PASS |
| TC-DM-003 | Ordered Score — scale preserved and indexable | PASS |
| TC-DM-004 | Probabilistic Noul — `P(true)=0.73` not converted to boolean | PASS |
| TC-DM-005 | Heterogeneous batch — all three results recoverable | PASS |
| TC-DM-006 | Typed retrieval — no consumer cast, compiles without `@SuppressWarnings` | PASS |
| TC-DM-007 | Probability absence — `null` ≠ `Probability.of(0.0)` | PASS |
| TC-DM-008 | Duplicate question ID rejected at build time | PASS |
| TC-DM-009 | Invalid probability (NaN, ±∞, <0, >1) rejected | PASS |
| TC-DM-010 | Provider independence — `FakeDecisionModel` has no Jev/LangChain4j dep | PASS |

### 5. FakeDecisionModel

`FakeDecisionModel` lives in `src/test/java` in the same package as `DecisionResult` (to access the package-private `DecisionResult.assemble()` factory). It is a fully typed builder-configured fake that satisfies TC-DM-010.

### 6. Module and API visibility

The public API surface is:
- All types in `dev.jsanca.argonaut.decision.*` — public
- `DecisionResult.assemble()` — package-private (for provider implementations only)
- No Jev, LangChain4j, or Spring types in any public surface

### 7. Exit criteria

The code from §27 of ARG-DECISION-EP-001 compiles and runs exactly as written:

```java
var route = Choice.ofEnum("route", "Select route", Route.class);
var complexity = Score.of("complexity", "Evaluate complexity",
    List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH));
var review = Noul.of("needs-review", "Requires architectural review");

var request = DecisionRequest.builder()
    .context(DecisionContext.of(context))
    .question(route)
    .question(complexity)
    .question(review)
    .build();

DecisionResult result = model.decide(request);

ChoiceResult<Route> routeResult = result.get(route);
ScoreResult<Complexity> complexityResult = result.get(complexity);
NoulResult reviewResult = result.get(review);
```

✓ No consumer casts  
✓ No Jev dependency  
✓ No ChatModel dependency in the public decision contract  
✓ No hidden 0.5 Noul policy  
✓ No provider-specific result types

---

## Findings

### F-001: Noul type parameter (T = Void)

**Observation:** `Noul implements Question<Void, NoulResult>` and `NoulResult implements AnswerResult<Void>`. The `Void` type parameter is a Java formalism required to satisfy the sealed hierarchy; it has no semantic meaning. `NoulResult` carries no `selected()` and no threshold.

**Assessment:** Architecturally sound. `Void` is the correct Java idiom for "this position has no domain type". No amendment required.

### F-002: AnswerResult<T> included (was "candidate")

**Observation:** Including `AnswerResult<T>` as the bound in `Question<T, R extends AnswerResult<T>>` is what makes `DecisionResult.get()` return `R` without a consumer cast. Without it, the return type would be `Object`.

**Assessment:** The "candidate" status in §22 is resolved: it is required for typed retrieval. No amendment required; the architecture's concern about `selected()` is satisfied by the absence of that method.

### F-003: DecisionResult.assemble() package-private factory

**Observation:** Provider implementations (including `FakeDecisionModel`) need to construct a `DecisionResult` from a raw `Map<String, AnswerResult<?>>`. The public typed builder works for callers who have compile-time question references; providers iterating over `List<Question<?,?>>` need a lower-level path.

**Decision:** Added `static DecisionResult assemble(Map<String, AnswerResult<?>> rawAnswers)` as a package-private factory, documented with a warning that callers must ensure type consistency. This boundary is documented in the class Javadoc alongside the unchecked-cast explanation.

**Assessment:** Architecturally acceptable for the first slice. If a future provider needs this from a different module, a `DecisionResultFactory` in a `spi` or `provider` subpackage would be the right migration path.

### F-004: Probability.of(Double.NaN) requires explicit isFinite check

**Observation:** `NaN < 0.0` and `NaN > 1.0` both return `false` in Java, so the naive range check would silently accept NaN as a valid probability.

**Decision:** Guard uses `!Double.isFinite(value) || value < 0.0 || value > 1.0`, rejecting NaN and both infinities before the range test.

---

## Verification Evidence

```
mvn verify (full reactor)

Argonaut Core ............ 111 tests  0 failures  0 skip
Argonaut Spring AI .......   4 tests  0 failures  0 skip
Argonaut LangChain4j .....   9 tests  0 failures  0 skip
Argonaut LangGraph4j .....  12 tests  0 failures  0 skip
Argonaut Embabel .........   8 tests  0 failures  0 skip
Argonaut Koog ............  12 tests  0 failures  0 skip
Argonaut Vector ..........  27 tests  0 failures  4 skip (platform ONNX, known)
Argonaut Decision ........  34 tests  0 failures  0 skip
─────────────────────────────────────────────────────────
Total ...................  217 pass   0 failures  4 skip

BUILD SUCCESS
```

---

## Architecture Guardrail Compliance

| Guardrail (§25) | Status |
|----------------|--------|
| No Jev as ChatModel | ✓ No ChatModel anywhere |
| No string-cast result retrieval as primary API | ✓ Typed `get(Question<T,R>)` |
| No implicit Noul threshold | ✓ `NoulResult` has no `selected()` |
| No classifier/router/reranker in core | ✓ None present |
| No provider DTOs | ✓ Pure domain types only |
| No production HTTP | ✓ No HTTP |
| No RAG | ✓ None |
| No tool calling | ✓ None |
| No agent middleware | ✓ None |
| No provider routing | ✓ None |

---

## Verification Result

**VERIFIED**

All 10 TDD scenarios pass. The exit-criteria code from §27 compiles and executes correctly. No architectural guardrails violated. No findings require an architecture amendment.
