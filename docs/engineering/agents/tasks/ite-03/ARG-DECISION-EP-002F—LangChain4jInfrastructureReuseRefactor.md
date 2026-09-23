# ARG-DECISION-EP-002F — LangChain4j Infrastructure Reuse Refactor

## Role

Software Engineer.

## Context

`ARG-DECISION-EP-002` implemented the first Jev provider for the `DecisionModel` abstraction.

The provider's domain mapping is considered sound:

* `JevDecisionModel` implements `DecisionModel`.
* `Choice`, `Score`, and `Noul` are mapped into the provider-independent decision domain.
* heterogeneous multi-question requests preserve native batching;
* probability distributions and raw score semantics are preserved;
* `argonaut-decision` remains independent from Jev and LangChain4j.

A subsequent architectural review, `ARG-DECISION-EP-002R`, found that the provider's transport layer unnecessarily reimplements infrastructure already available in LangChain4j.

The review specifically invalidated the earlier claim that LangChain4j's HTTP infrastructure is coupled to `ChatModel`.

The generative coupling exists above the transport layer, notably in APIs such as `ChatModelListener`. The HTTP client, request/response abstractions, retry infrastructure, and exception machinery are generic.

The review verdict for `JevHttpTransport` is:

**REFACTOR**

Review source:

`docs/engineering/agents/reviews/ARG-DECISION-EP-002R/review.md`

LangChain4j source is available locally under:

`libs-code/langchain4j`

## Objective

Refactor `argonaut-decision-jev` to reuse semantically compatible LangChain4j infrastructure instead of maintaining provider-local equivalents.

The refactor must remain internal to the provider/infrastructure layer.

The existing `DecisionModel` domain and the public semantics of `JevDecisionModel` must remain unchanged unless implementation evidence exposes an actual defect.

## Architectural Principle

Apply this rule throughout the slice:

> Before implementing provider infrastructure, inspect LangChain4j core for an existing semantically compatible abstraction. Prefer reuse over provider-local infrastructure.

But also preserve this boundary:

> Do not reuse LangChain4j abstractions that introduce chat, generative-model, prompt, message, or tool-calling semantics into a non-generative decision model.

The desired architecture is:

```text
argonaut-decision
    │
    │ DecisionModel
    │ DecisionRequest
    │ Choice / Score / Noul
    │ DecisionResult
    │ Probability
    │
    ▼
argonaut-decision-jev
    │
    │ JevDecisionModel
    │ Jev wire DTOs
    │ Jev semantic mapping
    │ Jev-specific validation
    │
    ▼
LangChain4j generic infrastructure
    │
    ├── HTTP
    ├── timeout configuration
    ├── retry
    ├── exception mapping
    ├── JSON where appropriate
    └── generic logging where appropriate
    │
    ▼
System One-compatible endpoint
```

## Required Work

### 1. Replace the provider-local JDK HTTP implementation

Refactor `JevHttpTransport` so it no longer directly constructs and operates `java.net.http.HttpClient` when LangChain4j's generic HTTP abstraction provides the required behavior.

Reuse, as appropriate:

* `dev.langchain4j.http.client.HttpClient`
* `HttpRequest`
* `SuccessfulHttpResponse`
* `JdkHttpClient`
* `JdkHttpClientBuilder`
* related generic HTTP infrastructure.

Do not introduce `ChatModel` or any chat-specific API.

### 2. Separate connect and read timeouts

The current implementation conflates connect and read timeout into one `Duration`.

Use the LangChain4j HTTP infrastructure to represent them independently.

The provider builder should expose or correctly derive:

```text
connectTimeout
readTimeout
```

Do not silently retain the existing conflated semantics merely for implementation convenience.

Preserve reasonable defaults and document them.

### 3. Make HTTP infrastructure injectable

Follow the LangChain4j provider pattern where the provider can receive/configure an `HttpClientBuilder` rather than hardconstructing its transport internally.

The default path should remain convenient:

```java
JevDecisionModel.builder()
    .apiKey(...)
    .baseUrl(...)
    .build();
```

But tests and advanced consumers should be able to supply compatible HTTP infrastructure without modifying the provider.

Do not leak transport concerns into `argonaut-decision`.

### 4. Integrate LangChain4j retry infrastructure

Evaluate and use the existing LangChain4j retry machinery rather than implementing a Jev-specific retry loop.

In particular inspect:

* `RetryUtils`
* `RetryUtils.RetryPolicy`
* `ExceptionMapper`
* `RetriableException`
* `NonRetriableException`

The intended behavior is that transient transport/provider failures can be retried with bounded exponential backoff and jitter while permanent failures fail immediately.

At minimum verify behavior for:

```text
401 / 403    non-retriable
422          non-retriable
429          retriable
5xx          retriable where LC4J semantics classify it so
timeout      retriable where LC4J semantics classify it so
```

Do not assume every failure is retriable.

Do not create a second retry framework.

### 5. Reconcile the Jev exception hierarchy

The current Jev exception hierarchy duplicates concepts already represented by LangChain4j exceptions.

Review the existing provider exceptions against LangChain4j's hierarchy.

For each current Jev exception classify it as:

```text
KEEP — genuinely Jev-specific semantic failure
REPLACE — equivalent LangChain4j infrastructure exception exists
WRAP — provider context adds meaningful information
REMOVE — redundant
```

Do not remove useful Jev-specific semantic information merely to maximize reuse.

The resulting hierarchy should cooperate correctly with LangChain4j retry classification.

### 6. Verify JSON infrastructure reuse

Before retaining direct Jackson usage, inspect LangChain4j's current JSON infrastructure, especially provider-oriented codecs.

Determine whether Jev wire serialization/deserialization can safely reuse it.

Pay particular attention to:

* provider wire DTO serialization;
* unknown provider fields;
* naming strategy;
* enum handling;
* codec SPI/customization;
* error semantics.

Classify the result:

```text
REUSE DIRECTLY
REUSE WITH ADAPTER
KEEP CURRENT JACKSON
```

If direct Jackson remains, document the concrete semantic reason.

Do **not** use tolerant JSON extraction intended for generative LLM output to repair malformed Jev HTTP responses.

A System One endpoint is expected to return valid provider JSON.

### 7. Evaluate generic HTTP logging

Evaluate LangChain4j's `LoggingHttpClient`.

If it can be reused without exposing credentials or introducing generative semantics, integrate it through the normal HTTP configuration path.

Explicitly verify that authorization/API-key material is not inadvertently logged.

If safe integration requires additional work beyond this slice, document and defer it rather than inventing another logging system.

## Explicit Non-Goals

Do not:

* redesign `DecisionModel`;
* modify `Choice`, `Score`, or `Noul` semantics;
* redesign `DecisionRequest` or `DecisionResult`;
* introduce `ChatModel`;
* introduce `ChatModelListener`;
* implement classifier/router/reranker layers;
* implement `DecisionModelListener`;
* introduce a generic Argonaut HTTP framework;
* create a separate OpenRouter provider;
* create a separate Kev provider;
* add application-level policy;
* broaden this into a general LangChain4j refactor.

This is an infrastructure reuse correction to EP-002.

## TDD Requirements

Preserve all existing EP-002 tests.

Add focused tests proving the refactor behavior, including at least:

### HTTP abstraction

* provider executes through injected LangChain4j HTTP infrastructure;
* default builder creates a functional LC4J-backed transport;
* no direct provider dependency on chat-model APIs.

### Timeouts

* connect timeout can be configured independently;
* read timeout can be configured independently.

### Retry

Verify attempts, not merely final exceptions.

Test at minimum:

```text
429 → retry → success
503 → retry → success
timeout → retry where supported
401 → no retry
422 → no retry
```

Also verify exhaustion behavior after the configured maximum retries.

Tests must use deterministic/minimal retry delays where possible.

### Existing semantics

Re-run the existing provider contract tests proving:

* Choice mapping;
* Score mapping including raw score;
* Noul probability;
* probability distributions;
* heterogeneous batching;
* response correlation;
* malformed/invalid provider responses.

The infrastructure refactor must not change these semantics.

## Dependency Guardrail

After the change, verify that:

```text
argonaut-decision
```

still has no dependency on:

```text
argonaut-decision-jev
LangChain4j
Jev
OpenRouter
Kev
```

LangChain4j infrastructure belongs in the provider module, not the decision domain.

## Required Findings

Produce explicit findings for:

### F-001 — HTTP reuse

What LangChain4j HTTP abstractions replaced the previous provider-local implementation?

### F-002 — Timeout semantics

How are connect and read timeout represented after the refactor?

### F-003 — Retry semantics

Which failures are retried, which are not, and why?

### F-004 — Exception hierarchy

Which Jev exceptions were kept, replaced, wrapped, or removed?

### F-005 — JSON reuse

Was LangChain4j provider JSON infrastructure reused?

If not, provide the concrete incompatibility.

### F-006 — Logging

Can generic LC4J HTTP logging safely be used for this provider?

### F-007 — Domain stability

Confirm whether `argonaut-decision` required any modification.

Expected answer:

```text
NO
```

Any required domain modification is an architecture finding and must be reported before implementation.

## Verification

Run the relevant module tests and the full Maven reactor.

Report:

```text
tests run
tests passed
tests failed
tests skipped
```

Distinguish any known unrelated skips/failures.

## Exit Criteria

This slice is complete when:

1. Jev no longer maintains a redundant raw JDK HTTP transport implementation where LC4J provides the equivalent abstraction.
2. Connect/read timeout semantics are correctly separated.
3. Retry behavior uses LangChain4j infrastructure and is covered by tests.
4. Retry classification cooperates with the exception hierarchy.
5. JSON reuse has been explicitly evaluated and either adopted or rejected with evidence.
6. Existing Jev decision semantics remain unchanged.
7. Existing EP-002 tests continue to pass.
8. `argonaut-decision` remains untouched.
9. No chat/generative abstractions have leaked into the provider.
10. Full reactor verification succeeds.

## Deliverable

Implement the refactor and produce a concise engineering report describing:

* changed files;
* LangChain4j components now reused;
* infrastructure deliberately not reused and why;
* exception/retry behavior;
* new tests;
* verification results;
* remaining technical debt;
* any architectural findings.

Do not broaden the scope beyond the findings from `ARG-DECISION-EP-002R`.
