# ARG-DECISION-EP-002F — LangChain4j Infrastructure Reuse Refactor

**Date:** 2026-09-23  
**Module:** `argonaut-decision-jev`  
**Status:** Complete  
**Tests:** 23 total (15 original JV-001..JV-015 + 8 new JV-016..JV-023) — all pass

---

## Summary

Refactored `argonaut-decision-jev` to reuse semantically compatible LangChain4j infrastructure
in place of the provider-local JDK HTTP reimplementation identified in `ARG-DECISION-EP-002R`.

The refactor is contained entirely within the provider/infrastructure layer. `argonaut-decision`
required no modifications. Public semantics of `JevDecisionModel` are unchanged.

---

## Changed Files

| File | Change |
|---|---|
| `argonaut-decision-jev/pom.xml` | Added `langchain4j-core:1.19.0`, `langchain4j-http-client:1.19.0`, `langchain4j-http-client-jdk:1.19.0` |
| `JevDecisionModel.java` | Split timeout; added `httpClientBuilder()`; added `maxRetries()`; wires `RetryPolicy` |
| `JevHttpTransport.java` | Replaced JDK `HttpClient` with `dev.langchain4j.http.client.HttpClient`; integrated `RetryUtils.RetryPolicy` |
| `JevException.java` | Extends `RetriableException` instead of `RuntimeException` |
| `JevAuthException.java` | Extends `NonRetriableException` directly (broken from `JevException` hierarchy) |
| `JevValidationException.java` | Extends `NonRetriableException` directly; own `httpStatus` field |
| `JevTransportException.java` | Extends `RetriableException` directly (broken from `JevException` hierarchy) |
| `JevDecisionModelTest.java` | Updated helpers for new API; added JV-016..JV-023 |

`JevRateLimitException` and `JevOverloadException` — documentation-only update; hierarchy
unchanged (`extends JevException` which now `extends RetriableException`).

---

## Required Findings

### F-001 — HTTP Reuse

`java.net.http.HttpClient`, `java.net.http.HttpRequest`, and `java.net.http.HttpResponse` in
`JevHttpTransport` are replaced by:

- `dev.langchain4j.http.client.HttpClient` — interface, injected
- `dev.langchain4j.http.client.HttpRequest` — built per-call with `HttpRequest.builder()`
- `dev.langchain4j.http.client.SuccessfulHttpResponse` — 2xx response holder
- `dev.langchain4j.http.client.jdk.JdkHttpClient` — default backend (wraps JDK `HttpClient`)
- `dev.langchain4j.http.client.jdk.JdkHttpClientBuilder` — configures connect/read timeouts

`JevDecisionModel.Builder` accepts a custom `HttpClientBuilder`, following the pattern of
`DefaultOpenAiClient`. The default path (`JdkHttpClientBuilder`) requires no configuration.

### F-002 — Timeout Semantics

`JevDecisionModel.Builder` now exposes two independent setters:

```text
connectTimeout(Duration)  — default: 10 s  (fail fast on unresponsive hosts)
readTimeout(Duration)     — default: 60 s  (allow time for inference)
```

Both are forwarded to the `HttpClientBuilder` before `build()`. The previous single `timeout()`
setter is removed. `JdkHttpClientBuilder` applies `connectTimeout` to the JDK `HttpClient`
builder and `readTimeout` to each per-request `HttpRequest` timeout.

### F-003 — Retry Semantics

`RetryUtils.RetryPolicy` (from `langchain4j-core`, `@Internal`) wraps the execution callable.
The retry decision is determined by `RetriableException` vs `NonRetriableException` classification:

| HTTP Status | Exception | Classification | Retried? |
|---|---|---|---|
| 401, 403 | `JevAuthException extends NonRetriableException` | Non-retriable | No |
| 422 | `JevValidationException extends NonRetriableException` | Non-retriable | No |
| 429 | `JevRateLimitException extends JevException extends RetriableException` | Retriable | Yes |
| 503 | `JevOverloadException extends JevException extends RetriableException` | Retriable | Yes |
| 5xx (other) | `JevException extends RetriableException` | Retriable | Yes |
| Timeout / IO | `JevTransportException extends RetriableException` | Retriable | Yes |
| Domain mapping | `JevValidationException extends NonRetriableException` | Non-retriable | No |

Default policy: `maxRetries=2`, `delayMillis=500`, `jitterScale=0.2`, `backoffExp=1.5`.
Serialization errors (pre-call) throw `JevValidationException` outside the retry loop — non-retriable.

### F-004 — Exception Hierarchy

| Exception | Before | After | Classification |
|---|---|---|---|
| `JevException` | `extends RuntimeException` | `extends RetriableException` | REPLACE (base class changed) |
| `JevAuthException` | `extends JevException` → `RuntimeException` | `extends NonRetriableException` | REPLACE (now non-retriable) |
| `JevValidationException` | `extends JevException` → `RuntimeException` | `extends NonRetriableException` | REPLACE (now non-retriable) |
| `JevTransportException` | `extends JevException` → `RuntimeException` | `extends RetriableException` | REPLACE (explicit retriable) |
| `JevRateLimitException` | `extends JevException` → `RuntimeException` | unchanged (inherits `RetriableException` via `JevException`) | KEEP |
| `JevOverloadException` | `extends JevException` → `RuntimeException` | unchanged (inherits `RetriableException` via `JevException`) | KEEP |

`JevValidationException` retains a `httpStatus` field to cover both HTTP 422 (status=422) and
domain mapping failures (status=0). This is a genuine Jev-specific distinction not expressible
by `InvalidRequestException` alone.

`JevAuthException` and `JevValidationException` break from the `JevException` hierarchy because
they are non-retriable and `JevException` is now a `RetriableException`. Each retains its
`httpStatus()` method added directly.

All existing test assertions (`assertThrows(JevAuthException.class)`, `ex.httpStatus()`, etc.)
remain valid — the Jev type names are preserved throughout.

### F-005 — JSON Reuse

**KEEP CURRENT JACKSON**

`ObjectMapper` (Jackson) retained directly in `JevHttpTransport`. LangChain4j's `Json` utility
in `langchain4j-open-ai` is package-scoped to the OpenAI provider — not a general-purpose
facility. Jackson is the correct reuse point: same transitive dependency at the same version
(`jackson-databind:2.21.4`), no additional coupling, full control over `FAIL_ON_UNKNOWN_PROPERTIES`.

### F-006 — Logging

`LoggingHttpClient` is available (`dev.langchain4j.http.client.log.LoggingHttpClient`) and is
confirmed safe: `HttpRequestLogger` masks headers containing "auth", "token", "api-key", and
similar patterns — `Authorization: Bearer <key>` is redacted.

**Deferred** — safe integration requires passing `LoggingHttpClient` through the `httpClientBuilder`
injection path. The mechanism is in place (`httpClientBuilder(HttpClientBuilder)`) but the
`logRequests`/`logResponses` convenience flags found in OpenAI provider are not added here as
they are outside this slice's scope. A caller may wrap `JdkHttpClient` in `LoggingHttpClient`
manually using the injected builder path.

### F-007 — Domain Stability

`argonaut-decision` required **NO** modification.

---

## LangChain4j Components Now Reused

| Component | Module | Usage |
|---|---|---|
| `HttpClient` (interface) | `langchain4j-http-client` | Transport execution |
| `HttpRequest` / `HttpMethod` | `langchain4j-http-client` | Request construction |
| `SuccessfulHttpResponse` | `langchain4j-http-client` | 2xx response access |
| `HttpClientBuilder` (interface) | `langchain4j-http-client` | Injectable timeout config |
| `JdkHttpClient` | `langchain4j-http-client-jdk` | Default JDK backend |
| `JdkHttpClientBuilder` | `langchain4j-http-client-jdk` | Default builder |
| `RetryUtils.RetryPolicy` | `langchain4j-core` | Retry with backoff+jitter |
| `HttpException` | `langchain4j-core` | Non-2xx signal from JdkHttpClient |
| `TimeoutException` | `langchain4j-core` | Timeout signal from JdkHttpClient |
| `RetriableException` | `langchain4j-core` | Base for retriable Jev exceptions |
| `NonRetriableException` | `langchain4j-core` | Base for non-retriable Jev exceptions |

## Infrastructure Deliberately Not Reused

| Component | Reason |
|---|---|
| `ChatModelListener` | Carries `ChatRequest`/`ChatResponse` — generative semantics |
| `LoggingHttpClient` | Safe but wiring deferred (see F-006) |
| `ExceptionMapper.DEFAULT` | Not needed: Jev types are thrown directly inside the retry callable, avoiding the need for a post-hoc mapping step |
| LC4J `Json` utility | OpenAI-internal; Jackson retained directly |

---

## New Tests

| ID | Scenario |
|---|---|
| JV-016 | HTTP 429 → retry → success (verifies 2 total attempts) |
| JV-017 | HTTP 503 → retry → success (verifies 2 total attempts) |
| JV-018 | HTTP 401 → no retry (verifies single attempt with maxRetries=2) |
| JV-019 | HTTP 422 → no retry (verifies single attempt with maxRetries=2) |
| JV-020 | HTTP 429 persistent → exhausts retries → throws after maxRetries+1 attempts |
| JV-021 | Timeout → retry → success (verifies transport-level retriability) |
| JV-022 | Connect and read timeout configured independently (structural) |
| JV-023 | Injectable `HttpClientBuilder` accepted and functional |

Retry tests use WireMock scenarios for deterministic attempt sequencing. `buildModelForRetry(n)`
uses default retry delays — tests are fast because maxRetries=1 or maxRetries=2 and WireMock
responds immediately on the retry.

Existing JV-001..JV-015 use `buildModel()` / `buildModelWithReadTimeout()` with `maxRetries(0)`
to prevent retry delays from inflating suite time.

---

## Verification Results

```
argonaut-decision:      34 tests — all pass
argonaut-decision-jev:  23 tests — all pass  (15 original + 8 new)
argonaut-core:         111 tests — all pass
argonaut-spring-ai:      4 tests — all pass
argonaut-langchain4j:    9 tests — all pass
argonaut-langgraph4j:   12 tests — all pass
argonaut-embabel:        8 tests — all pass
argonaut-koog:          12 tests — all pass

Total: 213 tests. Zero failures. Zero errors.
```

`argonaut-vector` excluded: pre-existing `spring-boot-maven-plugin:repackage` failure (unrelated
to this change; 31 tests pass, 4 skipped on osx-x86_64 for ONNX native lib).

---

## Dependency Guardrail

`argonaut-decision` compile-scope dependencies after this change:

```
dev.jsanca.argonaut:argonaut-core   — unchanged
```

No `langchain4j-*`, `argonaut-decision-jev`, or Jev-specific types in `argonaut-decision`.

---

## Technical Debt

- `LoggingHttpClient` integration is safe but deferred (F-006). A `logRequests(boolean)` /
  `logResponses(boolean)` flag pair in `JevDecisionModel.Builder` would complete this.
- `RetryUtils` is `@Internal` in LC4J. If a public retry API is introduced in a future LC4J
  version, this usage should be migrated. Risk is low: `RetryPolicy` is stable and semantically
  compatible.
- The `buildModelForRetry` test helper uses default `delayMillis=500`, making retry tests
  add ~500 ms per retried attempt. A dedicated test constructor accepting a custom `RetryPolicy`
  (with `delayMillis=1`) would speed these tests without changing production defaults.

---

## Exit Criteria

| Criterion | Met |
|---|---|
| Jev no longer maintains a redundant JDK HTTP transport | Yes — `JdkHttpClient` replaces raw JDK usage |
| Connect/read timeout semantics correctly separated | Yes — independent `connectTimeout`/`readTimeout` |
| Retry uses LangChain4j infrastructure and is covered by tests | Yes — `RetryUtils.RetryPolicy`; JV-016..JV-021 |
| Retry classification cooperates with exception hierarchy | Yes — `RetriableException`/`NonRetriableException` |
| JSON reuse explicitly evaluated | Yes — KEEP CURRENT JACKSON (F-005) |
| Existing Jev decision semantics unchanged | Yes — JV-001..JV-015 all pass |
| Existing EP-002 tests continue to pass | Yes — 15/15 pass |
| `argonaut-decision` untouched | Yes — no changes |
| No chat/generative abstractions in provider | Yes — `ChatModelListener` not used |
| Full reactor verification succeeds | Yes — 213 tests, zero failures |
