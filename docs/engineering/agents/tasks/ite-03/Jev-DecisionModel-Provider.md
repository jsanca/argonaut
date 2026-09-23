# ARG-DECISION-EP-002 — Jev DecisionModel Provider

## Role

Clio — Software Engineer

## Context

`ARG-DECISION-EP-001` established and verified the provider-independent `argonaut-decision` domain.

The module now provides:

```text
DecisionModel
DecisionContext
DecisionRequest
DecisionResult

Question<T,R>
├── Choice<T>
├── Score<T>
└── Noul

AnswerResult<T>
├── ChoiceResult<T>
├── ScoreResult<T>
└── NoulResult

Probability
OutcomeProbability<T>
ProbabilityDistribution<T>

DecisionCapability
```

The domain deliberately contains no Jev, TypeSafe AI, HTTP, `ChatModel`, classifier, router, reranker, or policy concepts.

EP-001 verified:

* heterogeneous multi-question requests;
* typed question handles;
* typed result retrieval without consumer casts;
* ordered scores;
* probabilistic Noul results without implicit boolean thresholds;
* explicit probability absence;
* provider independence.

The architectural rule for this slice is:

> **Jev adapts to `argonaut-decision`. `argonaut-decision` does not adapt itself to Jev unless real provider evidence exposes a genuine missing domain concept.**

Do not silently modify the core domain to make provider implementation easier.

If Jev cannot be represented faithfully by the existing domain, stop and record an architecture finding.

---

# Objective

Create the first real provider implementation of `DecisionModel`:

```text
argonaut-decision-jev
```

with:

```java
JevDecisionModel implements DecisionModel
```

The provider must translate between:

```text
Argonaut Decision Domain
          │
          ▼
    Jev System One
```

while preserving:

* typed questions;
* shared context;
* heterogeneous batching;
* Choice semantics;
* Score semantics;
* Noul probability semantics;
* probability distributions where provided;
* explicit absence where information is unavailable.

Prefer existing LangChain4j infrastructure over custom infrastructure where it can be reused without distorting Jev semantics.

---

# 1. Dependency Direction

The intended dependency graph is:

```text
          argonaut-decision
                  ▲
                  │
                  │
       argonaut-decision-jev
                  │
                  ▼
        LangChain4j infrastructure
                  │
                  ▼
          Jev System One API
```

`argonaut-decision` MUST NOT depend on:

* `argonaut-decision-jev`;
* Jev;
* TypeSafe AI;
* Jev HTTP DTOs;
* LangChain4j `ChatModel`;
* provider-specific infrastructure.

---

# 2. Module

Create:

```text
argonaut-decision-jev
```

Candidate conceptual structure:

```text
argonaut-decision-jev
│
├── public
│   ├── JevDecisionModel
│   └── JevDecisionModelBuilder
│
└── internal
    ├── transport
    ├── dto
    ├── mapping
    └── error
```

Exact package structure should follow existing Argonaut conventions.

Keep the public API narrow.

Do not expose Jev wire DTOs unless concrete evidence proves they are legitimate public-domain concepts.

---

# 3. First Task — Resolve Provider Result Construction

EP-001 exposed a real provider-boundary issue.

Currently:

```java
DecisionResult.assemble(...)
```

is package-private.

That works for the test fake living in the same package but cannot be used naturally by a provider located in another Maven module.

Resolve this before implementing Jev mapping.

Preferred direction:

```text
argonaut-decision
│
└── provider/SPI construction boundary
        │
        ▼
argonaut-decision-jev
```

Investigate the smallest coherent solution.

Candidates include:

```java
DecisionResultFactory
```

or a deliberately exposed provider-oriented builder/factory.

Requirements:

* normal consumers should not need raw result assembly;
* providers must be able to construct heterogeneous `DecisionResult`;
* question/result consistency must remain protected as strongly as practical;
* unsafe casts must remain isolated;
* do not expose the raw internal map as general-purpose API;
* do not introduce a large provider framework.

If this requires modifying `argonaut-decision`, treat it as a narrow EP-001 amendment and document it explicitly.

---

# 4. Jev Native Contract

Verify Jev's current API contract against primary documentation/source before implementing.

Do not implement from memory or previous reports alone.

Establish precisely:

```text
request
├── model
├── state
└── questions
    ├── Choice
    ├── Score
    └── Noul
```

and corresponding response semantics.

Verify specifically:

* endpoint;
* authentication;
* model identifier;
* state representation;
* question representation;
* question IDs/names;
* Choice candidate representation;
* Choice probability distribution;
* Choice confidence;
* Score levels;
* Score ordering;
* Score output semantics;
* Score interpolation/fractional values if applicable;
* Noul probability semantics;
* confidence semantics;
* multi-question behavior;
* API errors;
* malformed/partial responses.

Document any mismatch between previous research and the current API.

---

# 5. JevDecisionModel

Implement:

```java
public final class JevDecisionModel implements DecisionModel
```

with an Argonaut/LangChain4j-style builder.

Conceptual usage:

```java
DecisionModel model = JevDecisionModel.builder()
    .apiKey(...)
    .modelName(...)
    .build();
```

Do not copy this syntax blindly if existing LangChain4j provider conventions suggest a better idiomatic shape.

The model should expose appropriate `DecisionCapability` values based on verified Jev behavior.

Expected candidates include:

```text
CHOICE
SCORE
NOUL
MULTI_QUESTION
NATIVE_BATCH
PROBABILITY
PROBABILITY_DISTRIBUTION
```

Only advertise capabilities actually supported.

---

# 6. Request Mapping

Map:

```text
DecisionRequest
│
├── DecisionContext
│
└── List<Question<?,?>>
```

to a single Jev request whenever Jev natively supports that operation.

The following request:

```java
var request = DecisionRequest.builder()
    .context(context)
    .question(route)
    .question(complexity)
    .question(review)
    .build();
```

should conceptually become:

```text
Jev request
│
├── shared state
│
├── route       → Choice
├── complexity  → Score
└── needsReview → Noul
```

Do not perform three HTTP calls if Jev supports these questions as one native request.

The provider should preserve the batching semantics that motivated the core API.

---

# 7. Choice Mapping

Given:

```java
Choice<Route>
```

the provider must map each bounded candidate to Jev without leaking serialization concerns into `Choice<T>`.

The response must reconstruct:

```java
ChoiceResult<Route>
```

while preserving, where supplied:

* selected candidate;
* probability of outcomes;
* complete distribution;
* confidence.

Enum and String choices must both work.

Do not depend on `Enum.name()` blindly if that would make round-trip mapping ambiguous or unnecessarily couple domain labels to wire representation.

Design and test the round-trip explicitly.

---

# 8. Score Mapping

Map:

```java
Score<T>
```

to Jev's native ordered scoring primitive.

This area requires particular care.

Before implementing the result mapping, verify whether Jev returns:

* a selected level;
* an integer index;
* a continuous/fractional score;
* a distribution over levels;
* confidence;
* some combination of these.

Do not discard information merely because the current `ScoreResult<T>` has a convenient shape.

If verified Jev semantics cannot be represented faithfully by `ScoreResult<T>`, stop and create an architecture finding.

Do not silently alter the domain.

---

# 9. Noul Mapping

Map:

```java
Noul
```

to Jev's native binary proposition evaluation.

The result must preserve:

```text
P(true)
```

as probability evidence.

Example:

```text
Jev: P(true) = 0.73
            │
            ▼
NoulResult
    probabilityTrue = 0.73
```

MUST NOT become:

```text
true
```

through a provider-defined threshold.

The provider must not introduce:

```text
>= 0.5
```

or any other implicit policy.

---

# 10. Probability Semantics

Use the existing:

```text
Probability
OutcomeProbability<T>
ProbabilityDistribution<T>
```

domain types.

Preserve the distinction between:

```text
Probability.of(0.0)
```

and:

```text
probability unavailable
```

Do not introduce sentinel values.

If Jev omits a probability/confidence field, represent absence using the existing domain semantics.

If Jev returns invalid probability values, fail explicitly rather than normalizing silently.

---

# 11. LangChain4j Infrastructure Reuse

A primary goal of this slice is to determine how much infrastructure can be reused from LangChain4j without pretending Jev is a `ChatModel`.

Inspect and reuse where appropriate:

* HTTP client abstraction;
* HTTP client builders/factories;
* timeout configuration;
* retry infrastructure;
* HTTP exception conventions;
* serialization infrastructure;
* builder conventions;
* ServiceLoader/SPI patterns;
* WireMock testing patterns;
* observability infrastructure.

Do NOT reuse:

```text
ChatModel
ChatRequest
ChatResponse
AiMessage
TokenUsage
FinishReason
tool calling
streaming chat abstractions
```

merely because they already exist.

The rule is:

> **Reuse infrastructure, not incorrect semantics.**

Document for every relevant LangChain4j component whether it was:

```text
REUSED
ADAPTED
REJECTED
NOT NEEDED
```

and why.

---

# 12. HTTP Boundary

Do not create a generic Argonaut HTTP framework.

Prefer the existing LangChain4j HTTP abstraction if it supports the Jev request/response shape cleanly.

The Jev-specific transport layer should only know enough to perform:

```text
POST System One request
       │
       ▼
deserialize response
```

HTTP concerns must remain below `JevDecisionModel`.

Domain types must not know:

* URLs;
* headers;
* Bearer tokens;
* JSON field names;
* HTTP status codes.

---

# 13. Error Mapping

Investigate and define behavior for:

```text
authentication failure
rate limiting
timeout
connection failure
HTTP 4xx
HTTP 5xx
malformed response
missing question result
unknown question result
invalid probability
unknown Choice candidate
invalid Score result
```

Reuse LangChain4j exception conventions where semantically appropriate.

Do not create a deep Jev-specific exception hierarchy without demonstrated need.

Domain validation errors and provider/transport errors should remain distinguishable.

---

# 14. Observability

EP-001 intentionally deferred `DecisionModelListener`.

This provider gives us the first real implementation pressure to decide whether that abstraction is needed.

Investigate whether LangChain4j's existing observability infrastructure can be adapted to expose a lifecycle equivalent to:

```text
Decision request
      │
      ▼
Jev provider invocation
      │
   ┌──┴──┐
   ▼     ▼
response error
```

Desired eventual lifecycle:

```text
onRequest
onResponse
onError
```

However:

* do not introduce `DecisionModelListener` merely because it was previously proposed;
* do not build custom LangSmith integration;
* do not build custom Langfuse integration;
* do not claim those systems work with DecisionModel unless verified.

If existing LangChain4j observability cannot be reused cleanly, document the gap and defer implementation rather than inventing a large subsystem.

---

# 15. TDD Strategy

Implementation must be TDD-first.

The main provider contract must be testable without real Jev credentials.

Use the existing LangChain4j provider testing patterns and WireMock or the equivalent infrastructure already used by the project.

Do not begin with live Jev calls.

---

# 16. Required Mocked Provider Tests

At minimum implement the following scenarios.

## JV-001 — Choice<Enum>

Given:

```text
Choice<Route>
FAST
NORMAL
DEEP
```

and a mocked Jev response selecting `DEEP`, produce:

```text
ChoiceResult<Route>
```

with correct selected value and probabilities.

---

## JV-002 — Choice<String>

Round-trip arbitrary String candidates correctly.

---

## JV-003 — Score

Given an ordered scale:

```text
LOW
MEDIUM
HIGH
```

preserve verified Jev Score semantics and probability information.

---

## JV-004 — Noul

Given:

```text
P(true) = 0.73
```

produce:

```text
NoulResult
```

preserving `0.73`.

No boolean threshold.

---

## JV-005 — Heterogeneous Native Batch

Send one:

```text
DecisionRequest
```

containing:

```text
Choice
Score
Noul
```

and verify that exactly one Jev HTTP request is made when native batching is supported.

Recover all three results through their original typed question handles.

---

## JV-006 — Probability Distribution

Verify complete distribution mapping.

---

## JV-007 — Probability Absence

Verify unavailable information remains unavailable rather than becoming `0.0`.

---

## JV-008 — Unknown Choice Candidate

A Jev response containing an outcome that cannot be mapped to the original `Choice<T>` must fail explicitly.

Do not silently manufacture a value.

---

## JV-009 — Missing Question

If Jev omits a requested question result, fail with a useful provider error.

---

## JV-010 — Unexpected Question

Define and test behavior when Jev returns a question not present in the request.

Prefer strictness unless API evidence supports otherwise.

---

## JV-011 — Invalid Probability

Invalid probability data must fail domain validation.

---

## JV-012 — Authentication Error

Map the provider error coherently.

---

## JV-013 — Rate Limit

Verify retry/error behavior according to the reused infrastructure.

---

## JV-014 — Server Error

Verify provider/HTTP error mapping.

---

## JV-015 — Timeout

Verify timeout behavior.

---

# 17. Live Integration Test

Only after all mocked provider tests pass may a live integration test be added.

The live test must:

* require an externally supplied Jev API key;
* never contain credentials in source;
* be disabled/skipped when credentials are absent;
* issue a minimal request;
* verify actual current API compatibility.

Suggested live request:

```text
state:
    "The production API is returning HTTP 500 for most requests."

questions:

    severity:
        Score(LOW, MEDIUM, HIGH)

    requires_attention:
        Noul("This incident requires immediate attention")
```

Do not assert exact probabilities.

Assert only stable structural properties:

* expected questions are returned;
* probabilities are valid;
* result types are correct;
* selected values belong to their defined domain.

---

# 18. No Higher-Level Abstractions

This slice MUST NOT implement:

```text
Classifier
Router
Reranker
Judge
DecisionPolicy
```

Those remain separate experiments.

Likewise do not implement:

```text
argonaut-decision-classifier
argonaut-decision-router
argonaut-decision-rerank
```

during EP-002.

The purpose of this slice is to prove the provider boundary.

---

# 19. Core Modification Rule

Changes to `argonaut-decision` are allowed only when one of these conditions holds:

### A. Provider SPI necessity

Example:

```text
JevDecisionModel cannot construct DecisionResult
because the current construction API is module-private.
```

A narrow SPI amendment is acceptable.

### B. Semantic mismatch

Verified Jev behavior contains information that the current provider-independent domain genuinely should represent.

This requires an explicit finding before changing the core.

### C. Bug

The EP-001 implementation violates its own documented contract.

Normal implementation convenience is **not** sufficient reason to change the core.

---

# 20. Expected Public Usage

The final provider should make code conceptually equivalent to this possible:

```java
DecisionModel model = JevDecisionModel.builder()
    .apiKey(System.getenv("JEV_API_KEY"))
    .modelName("...")
    .build();

var route = Choice.ofEnum(
    "route",
    "Select execution route",
    Route.class
);

var complexity = Score.of(
    "complexity",
    "Evaluate task complexity",
    List.of(
        Complexity.LOW,
        Complexity.MEDIUM,
        Complexity.HIGH
    )
);

var review = Noul.of(
    "review",
    "This task requires architectural review"
);

var request = DecisionRequest.builder()
    .context(DecisionContext.of(context))
    .question(route)
    .question(complexity)
    .question(review)
    .build();

DecisionResult result = model.decide(request);

ChoiceResult<Route> routeResult =
    result.get(route);

ScoreResult<Complexity> complexityResult =
    result.get(complexity);

NoulResult reviewResult =
    result.get(review);
```

No Jev DTOs should appear above `JevDecisionModel`.

---

# 21. Required Deliverables

Produce:

1. `argonaut-decision-jev` Maven module;
2. `JevDecisionModel`;
3. narrow builder/configuration API;
4. provider request mapper;
5. provider response mapper;
6. Jev internal DTOs;
7. HTTP transport using reusable LangChain4j infrastructure where appropriate;
8. provider error mapping;
9. mocked provider tests;
10. capability declaration;
11. any required narrow provider SPI amendment to `argonaut-decision`;
12. LangChain4j infrastructure reuse report;
13. observability finding;
14. live integration test only if credentials/environment permit;
15. implementation report.

---

# 22. Required Findings

The implementation report must explicitly answer:

### F-001 — Provider SPI

How can an external provider construct `DecisionResult` safely?

What changed from EP-001?

---

### F-002 — Choice Wire Identity

How are arbitrary Java values mapped to Jev Choice candidates and back without ambiguity?

---

### F-003 — Score Fidelity

Does the current `ScoreResult<T>` faithfully represent actual Jev output?

If not, stop and document the mismatch before changing core.

---

### F-004 — Batching

Does one heterogeneous `DecisionRequest` map to one native Jev request?

---

### F-005 — Capabilities

Which `DecisionCapability` values are actually supported by Jev?

---

### F-006 — LangChain4j Reuse

Exactly which LangChain4j infrastructure was reused, adapted, rejected, or unnecessary?

---

### F-007 — Observability

Can existing LangChain4j observability infrastructure be reused for `DecisionModel` without pretending it is a `ChatModel`?

If not, what is the smallest missing abstraction?

---

### F-008 — Core Stability

Did implementing the first real provider require changing the `argonaut-decision` domain?

If yes, distinguish:

```text
SPI pressure
```

from:

```text
domain-model failure
```

This distinction is important.

---

# 23. Architecture Guardrails

Clio MUST NOT:

* implement Jev as `ChatModel`;
* introduce `ChatRequest`/`AiMessage` into the decision API;
* expose Jev DTOs through `argonaut-decision`;
* add implicit probability thresholds;
* convert Noul into boolean policy;
* split a native heterogeneous Jev batch into multiple calls without evidence;
* reimplement generic HTTP infrastructure unnecessarily;
* create custom LangSmith/Langfuse integrations;
* implement classifier/router/reranker;
* introduce RAG;
* introduce tool calling;
* introduce agent middleware;
* redesign EP-001 merely for provider convenience.

If implementation evidence contradicts the architecture:

**record the finding before changing the contract.**

---

# 24. Verification

Run:

```text
mvn verify
```

for the complete Argonaut reactor.

EP-002 succeeds when:

```text
argonaut-decision
        ▲
        │
argonaut-decision-jev
        │
        ▼
       Jev
```

works while preserving all EP-001 contracts.

The most important verification is not merely that Jev responds.

It is that the first real provider can inhabit the `DecisionModel` abstraction without forcing provider-specific semantics back into the core.

---

# 25. Exit Criteria

EP-002 is complete when:

* `JevDecisionModel` implements `DecisionModel`;
* Choice works for enum and String values;
* Score semantics are preserved without information loss;
* Noul preserves probability without thresholding;
* heterogeneous requests use native batching where supported;
* typed result retrieval still works unchanged;
* Jev DTOs remain internal;
* core has no Jev dependency;
* LangChain4j infrastructure is reused where appropriate;
* provider errors are tested;
* no production credentials are required for the normal test suite;
* full reactor verification passes;
* any changes to `argonaut-decision` are documented as explicit findings.

At completion we should be able to answer:

> **Did the provider adapt to the domain, or did the domain have to adapt to the provider?**

A successful EP-002 should overwhelmingly demonstrate the former.
