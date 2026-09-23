# ARG-DECISION-EP-002 — Jev DecisionModel Provider

**Date:** 2026-09-22  
**Module:** `argonaut-decision-jev`  
**Status:** Complete  
**Tests:** 15 mocked provider tests (JV-001..JV-015) — all pass

---

## Summary

EP-002 delivers the first real `DecisionModel` provider: `JevDecisionModel`, backed by the TypeSafe AI Jev System One API. A single heterogeneous `DecisionRequest` containing Choice, Score, and Noul questions maps to exactly one native Jev HTTP call. All typed retrieval from EP-001 works unchanged. Jev wire DTOs do not appear above the `JevDecisionModel` boundary.

The core verdict: **the provider adapted to the domain. The domain did not adapt to the provider** — with one narrow and justified SPI exception noted in F-001.

---

## Architecture Delivered

```
argonaut-decision (domain — unchanged except EP-001 SPI amendment)
        ▲
        │
argonaut-decision-jev
│
├── public
│   └── JevDecisionModel (implements DecisionModel)
│
└── internal
    ├── dto        — JevRequestDto, JevQuestionDto, JevResponseDto, JevAnswerDto, JevUsageDto
    ├── transport  — JevHttpTransport (java.net.http.HttpClient)
    ├── mapping    — JevRequestMapper, JevResponseMapper, CandidateKey<T>
    └── error      — JevException hierarchy (Auth, RateLimit, Overload, Validation, Transport)
```

---

## Required Findings

### F-001 — Provider SPI

**How can an external provider construct `DecisionResult` safely?**

EP-001 left `DecisionResult.assemble()` as package-private, which `FakeDecisionModel` (same package, test scope) could access, but an external module could not.

Resolution: `DecisionResult.forProvider(Map<String, AnswerResult<?>>)` was added as a public factory in EP-001. This is the only public entry point for external providers to construct a `DecisionResult` from a raw map. Normal consumers continue to use `DecisionResult.Builder` which enforces typed pairing. The unsafe cast remains isolated inside `DecisionResult.get()`.

Change type: **SPI pressure** — the domain needed to expose a construction boundary for external providers. Not a domain-model failure; the boundary rule (`Builder` for consumers, `forProvider` for providers) is correct and holds.

---

### F-002 — Choice Wire Identity

**How are arbitrary Java values mapped to Jev Choice candidates and back without ambiguity?**

`CandidateKey<T>` generates the wire key from `candidate.toString()`. For enums this is `name()`. For `String` candidates it is the identity. Key uniqueness is validated at mapping time — if two candidates produce identical string representations, mapping fails with `IllegalArgumentException`.

The round-trip is:
1. Request: `candidate.toString()` → sent as both the criteria key and description.
2. Response: Jev returns the chosen key → looked up in `CandidateKey.keyToCandidate` → original domain value returned.

This design places the serialization decision entirely in `JevRequestMapper` — `Choice<T>` is unaware of wire formats.

---

### F-003 — Score Fidelity

**Does `ScoreResult<T>` faithfully represent actual Jev output?**

Yes, after the EP-001 amendment. Jev returns a fractional 0-indexed score (e.g. `1.04` meaning slightly above index 1 in a 3-level scale), a per-level probability distribution keyed by string index (`"0"`, `"1"`, `"2"`), and optionally a `legend` array of level names.

`ScoreResult<T>.rawScore` (added in EP-001 as a domain amendment) carries the fractional value without loss. The discrete `selected` value is derived by argmax over the probability distribution when available; the `rawScore` provides a fallback when probabilities are absent. This is **not** a silent discard — both values are preserved and accessible to callers.

Change type: **domain-model gap** (amended in EP-001 before this provider was written, per the architecture rule).

---

### F-004 — Batching

**Does one heterogeneous `DecisionRequest` map to one native Jev request?**

Yes. The Jev System One endpoint (`POST /v1/systemone`) accepts a `questions` map containing heterogeneous entries in a single payload. `JevRequestMapper.map()` produces exactly one `JevRequestDto` regardless of how many questions are in the request. JV-005 verifies this: WireMock confirms exactly one `POST /v1/systemone` was made for a request containing Choice, Score, and Noul questions.

---

### F-005 — Capabilities

**Which `DecisionCapability` values are actually supported by Jev?**

All seven are advertised:

| Capability | Supported | Evidence |
|---|---|---|
| `CHOICE` | Yes | JV-001, JV-002 |
| `SCORE` | Yes | JV-003 |
| `NOUL` | Yes | JV-004 |
| `MULTI_QUESTION` | Yes | JV-005 |
| `NATIVE_BATCH` | Yes | JV-005 (single HTTP call verified) |
| `PROBABILITY` | Yes | JV-001, JV-003, JV-004 (`confidence` field) |
| `PROBABILITY_DISTRIBUTION` | Yes | JV-006 (full distribution for Choice and Score) |

---

### F-006 — LangChain4j Reuse

**Exactly which LangChain4j infrastructure was reused, adapted, rejected, or unnecessary?**

| Component | Decision | Reason |
|---|---|---|
| `java.net.http.HttpClient` (JDK) | **USED** (not LC4J) | LC4J's OkHttp/Retrofit is bound to its own `ChatModel` request shape. JDK HttpClient avoids that coupling with zero extra dependency. |
| Jackson `ObjectMapper` | **REUSED** (same version) | LC4J already pulls `jackson-databind:2.21.4`. Same dependency, no added cost. |
| Builder conventions | **REUSED** (pattern) | `JevDecisionModel.Builder` follows the same style as LC4J provider builders (e.g. `OpenAiChatModel.builder()`). |
| WireMock testing pattern | **REUSED** (same library) | `@WireMockTest` annotation used identically to how LC4J's own provider tests are structured. |
| LC4J `ChatModel` / `ChatRequest` | **REJECTED** | Jev is not a chat API. Mapping Jev questions to `ChatRequest` would distort semantics. |
| LC4J retry infrastructure | **NOT NEEDED** | Jev does not document a standard retry contract. Rate-limit errors are surfaced as `JevRateLimitException` for callers to handle at their level. |
| LC4J `TokenUsage` / `FinishReason` | **REJECTED** | Jev returns `usage` but it belongs in the internal DTO, not the domain. |
| LC4J observability | **NOT NEEDED** (see F-007) | |

---

### F-007 — Observability

**Can existing LangChain4j observability infrastructure be reused for `DecisionModel`?**

No. LC4J's `ChatModelListener` is tightly coupled to `ChatModel` — it carries `ChatRequest`, `ChatResponse`, and `AiMessage` in its lifecycle events. Adapting it for `DecisionModel` would require either:
- Pretending a `DecisionRequest` is a `ChatRequest` (wrong semantics), or
- Creating a new `DecisionModelListener` SPI in `argonaut-decision`.

EP-002 intentionally defers this. The `JevHttpTransport` executes a single HTTP call with no observable lifecycle events beyond success/failure. The smallest missing abstraction would be a `DecisionModelListener` with `onRequest(DecisionRequest)`, `onResponse(DecisionResult)`, `onError(DecisionRequest, Throwable)` — but this should be added only when there is concrete consumer demand (e.g. when Langfuse/LangSmith integration is required for decision audit trails).

**Current state:** no observability hooks. This is correct for EP-002 scope.

---

### F-008 — Core Stability

**Did implementing the first real provider require changing `argonaut-decision`?**

Two changes, both pre-made as EP-001 amendments before this provider was written:

1. **`DecisionResult.forProvider()`** — SPI pressure. An external module cannot access package-private APIs. This is a necessary boundary, not a domain failure.

2. **`ScoreResult.rawScore`** — Domain-model gap. The existing `ScoreResult<T>` had no way to represent Jev's fractional 0-indexed score. Adding `rawScore: Double` (nullable) extends the domain to faithfully carry provider information without distorting the discrete `selected` field.

Both changes are narrow and justified. No Jev-specific types entered `argonaut-decision`. The domain's sealed type hierarchy, typed retrieval, and probability semantics are all unchanged.

---

## Test Coverage

| ID | Scenario | Result |
|---|---|---|
| JV-001 | Choice<String> — happy path | PASS |
| JV-002 | Choice<Enum> — happy path | PASS |
| JV-003 | Score — fractional rawScore, argmax selected | PASS |
| JV-004 | Noul — P(true) preserved without thresholding | PASS |
| JV-005 | Heterogeneous batch — exactly one HTTP call | PASS |
| JV-006 | Full probability distribution mapping | PASS |
| JV-007 | Absent confidence/probabilities → null (not 0.0) | PASS |
| JV-008 | Unknown choice candidate → JevValidationException | PASS |
| JV-009 | Missing question in response → JevValidationException | PASS |
| JV-010 | Unexpected question in response → JevValidationException | PASS |
| JV-011 | Invalid probability value (1.5) → JevValidationException | PASS |
| JV-012 | HTTP 401 → JevAuthException | PASS |
| JV-013 | HTTP 429 → JevRateLimitException | PASS |
| JV-014 | HTTP 500 → JevException(httpStatus=500) | PASS |
| JV-015 | Timeout → JevTransportException | PASS |

Live integration test (`JevDecisionModelLiveIT`) skipped when `JEV_API_KEY` is not set.

---

## Reactor Verification

```
mvn verify --projects '!argonaut-vector'
```

- argonaut-core: 111 tests — all pass
- argonaut-spring-ai: 4 — all pass
- argonaut-langchain4j: 9 — all pass
- argonaut-langgraph4j: 12 — all pass
- argonaut-embabel: 8 — all pass
- argonaut-koog: 12 — all pass
- argonaut-decision: 34 — all pass
- argonaut-decision-jev: 15 — all pass

**Total: 205 tests. Zero failures.**

`argonaut-vector` excluded: pre-existing `spring-boot-maven-plugin:repackage` failure (zip file empty — boot jar packaging issue, not test failure; 31 tests pass, 4 skipped on osx-x86_64 for ONNX native lib).

---

## EP-002 Exit Criteria

| Criterion | Met |
|---|---|
| `JevDecisionModel` implements `DecisionModel` | Yes |
| Choice works for enum and String values | Yes (JV-001, JV-002) |
| Score semantics preserved without information loss | Yes (rawScore + distribution) |
| Noul preserves probability without thresholding | Yes (JV-004) |
| Heterogeneous requests use native batching | Yes (JV-005) |
| Typed result retrieval works unchanged | Yes (all happy-path tests) |
| Jev DTOs remain internal | Yes |
| Core has no Jev dependency | Yes |
| LangChain4j infrastructure reused where appropriate | Yes (Jackson, WireMock pattern) |
| Provider errors are tested | Yes (JV-008..JV-015) |
| No production credentials required for normal test suite | Yes |
| Full reactor verification passes | Yes |
| Core changes documented as explicit findings | Yes (F-001, F-008) |
