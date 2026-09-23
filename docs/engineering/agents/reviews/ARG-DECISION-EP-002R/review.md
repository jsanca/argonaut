# ARG-DECISION-EP-002R — LangChain4j Infrastructure Reuse Review

**Date:** 2026-09-22  
**Scope:** `argonaut-decision-jev` — transport, timeout, retry, exception handling  
**Source verified against:** `libs-code/langchain4j` (checked-out source)

---

## Executive Summary

The primary justification given in EP-002 for not reusing LangChain4j HTTP infrastructure was:

> "LC4J's OkHttp/Retrofit is bound to its own ChatModel request shape."

**This statement is false about the abstraction layer.** The current LangChain4j source contains a fully generic, ChatModel-free HTTP abstraction in `langchain4j-http-client`. The EP-002 statement appears to have been written from memory of older LC4J versions that used Retrofit internally. As of the version in `libs-code/langchain4j`, Retrofit does not appear anywhere in the HTTP stack.

This finding changes the reuse calculus for several components.

---

## Component-by-Component Classification

### 1. HTTP Client Abstraction (`dev.langchain4j.http.client.HttpClient`)

**Classification: REUSE DIRECTLY**

`langchain4j-http-client` contains:

```
HttpClient           — interface: execute(HttpRequest), executeAsync, SSE/streaming variants
HttpRequest          — generic: method, url, headers, query params, body, form data
HttpClientBuilder    — interface: connectTimeout, readTimeout, build()
SuccessfulHttpResponse — 2xx response: statusCode, headers, body
HttpClientBuilderLoader — ServiceLoader-based factory discovery
```

**None of these carry any reference to `ChatModel`, `ChatRequest`, `AiMessage`, tool calling, or any generative semantics.** The interface is `execute(HttpRequest) → SuccessfulHttpResponse throws HttpException`. That is exactly what `JevHttpTransport.send()` does, reimplemented from scratch using raw JDK APIs.

The claim that OkHttp/Retrofit coupling prevented reuse applies to **Retrofit-era LC4J** (pre-1.0), not to the current source. In the current architecture OkHttp is one backend behind the `HttpClient` interface, not the interface itself.

---

### 2. JdkHttpClient / JdkHttpClientBuilder

**Classification: REUSE DIRECTLY**

`langchain4j-http-client-jdk` provides `JdkHttpClient`, which wraps `java.net.http.HttpClient` and implements `dev.langchain4j.http.client.HttpClient`. Compared to `JevHttpTransport`:

| Concern | JevHttpTransport | JdkHttpClient |
|---|---|---|
| Connect timeout | `HttpClient.newBuilder().connectTimeout(t)` | `JdkHttpClientBuilder.connectTimeout(t)` |
| Read timeout | Same `t` applied per-request via `.timeout(t)` | Separate `readTimeout` applied per-request |
| `HttpTimeoutException` | → `JevTransportException` | → `TimeoutException` (retriable) |
| `IOException` | → `JevTransportException` | → `RuntimeException` |
| `InterruptedException` | `Thread.currentThread().interrupt()` + `JevTransportException` | Same pattern |
| 4xx/5xx response | Manual if/else dispatch | → `HttpException(statusCode, body)` |

The critical deficiency in `JevHttpTransport`: **connect timeout and read timeout are conflated.** The builder accepts a single `Duration timeout` and sets it on the `HttpClient` builder (connect timeout) AND on each `HttpRequest` (read/request timeout). For a decision model call, a 30-second connect timeout is correct for slow DNS; a 30-second read timeout is correct for inference latency. These should be independently configurable. `JdkHttpClientBuilder` separates them correctly.

---

### 3. Timeout Configuration

**Classification: REUSE WITH ADAPTER**

`HttpClientBuilder` separates `connectTimeout()` and `readTimeout()`. `JevDecisionModel.Builder` exposes a single `timeout(Duration)` that applies to both paths identically. This is a functional deficiency — not just a style issue. A user who needs a short connect timeout (fail fast on DNS/network failure) but a long read timeout (inference takes time) cannot express that with the current API.

Adopting `HttpClientBuilder` as the injection point (as `DefaultOpenAiClient` does) would fix this and remove `JevHttpTransport` as a non-testable internal construction site.

---

### 4. Retry Infrastructure (`RetryUtils`, `RetryPolicy`)

**Classification: REUSE DIRECTLY (with caveat on `@Internal`)**

`RetryUtils` in `langchain4j-core` is:
- `@Internal` annotated — not a guaranteed public API
- Semantically neutral — wraps `Callable<T>` with exponential backoff + jitter
- `withRetryMappingExceptions(Callable<T>, int)` calls `ExceptionMapper.DEFAULT.withExceptionMapper()` to map exceptions before deciding to retry

The retry decision turns on `NonRetriableException`. When `ExceptionMapper.DEFAULT` maps an `HttpException(429)` → `RateLimitException extends RetriableException` and an `HttpException(401)` → `AuthenticationException extends NonRetriableException`, the retry loop automatically does the right thing: retries 429/503/timeout, stops immediately on 401/422.

**Is "no documented Jev retry contract" sufficient reason to not retry 429 and 503?**  
**No.** HTTP 429 (Too Many Requests) and 503 (Service Unavailable) have universally understood semantics independent of any provider's documentation. These are transient errors that warrant an exponential-backoff retry regardless of what Jev's API docs say or don't say. Deferring retry responsibility to callers of `JevDecisionModel` creates a leaky abstraction — every caller must independently implement the same backoff logic for a class of errors that the provider layer is best positioned to handle.

The current `JevHttpTransport` performs zero retries on any error. A 429 from Jev causes an immediate `JevRateLimitException` to propagate out. This is a correctness gap, not merely a missing convenience.

---

### 5. Exception Hierarchy (`HttpException`, `RetriableException`, `NonRetriableException`, etc.)

**Classification: REUSE DIRECTLY**

LC4J exception hierarchy in `langchain4j-core`:

```
LangChain4jException
├── RetriableException
│   ├── RateLimitException       — HTTP 429
│   ├── InternalServerException  — HTTP 5xx
│   └── TimeoutException         — connect/read timeout, HTTP 408
└── NonRetriableException
    ├── AuthenticationException  — HTTP 401/403
    ├── InvalidRequestException  — HTTP 4xx (other)
    └── ModelNotFoundException   — HTTP 404
```

`HttpException(statusCode, message)` is the intermediate type produced by `JdkHttpClient` on non-2xx responses. `ExceptionMapper.DefaultExceptionMapper` maps it to the appropriate typed exception.

The current `JevException` hierarchy has the same shape but without the `Retriable`/`NonRetriable` classification:

```
JevException (int httpStatus)
├── JevAuthException        — HTTP 401
├── JevRateLimitException   — HTTP 429
├── JevOverloadException    — HTTP 503
├── JevValidationException  — HTTP 422
└── JevTransportException   — network/IO
```

The Jev-specific types carry `httpStatus()` and nothing else. `AuthenticationException` and `RateLimitException` already carry the same information and add the `Retriable`/`NonRetriable` marker that makes retry decisions correct automatically. The Jev hierarchy is a parallel reimplementation that is strictly weaker.

One genuine distinction: `JevValidationException` covers domain-level mapping failures (unknown choice candidate, missing question ID) with `httpStatus=0`, not just HTTP 422. This use case should be kept as a domain-level `JevValidationException` — it is not an HTTP exception and does not belong in the LC4J hierarchy. All HTTP-originated errors can use LC4J types.

---

### 6. Serialization (Jackson)

**Classification: REUSED** — correctly identified in EP-002.

LC4J's `Json` utility class in `langchain4j-open-ai:internal` is OpenAI-internal, not a general-purpose LC4J facility. Jackson is the correct reuse point: same transitive dependency, no additional coupling.

---

### 7. Builder/Configuration Conventions

**Classification: REUSE WITH ADAPTER**

`DefaultOpenAiClient.Builder` accepts an `HttpClientBuilder` as an injection point:

```java
HttpClientBuilder httpClientBuilder =
    getOrDefault(builder.httpClientBuilder, HttpClientBuilderLoader::loadHttpClientBuilder);
```

This lets callers inject a custom `HttpClient` (e.g. for testing, or to set a custom SSL context) without subclassing. `JevDecisionModel.Builder` does not do this — `JevHttpTransport` is hard-constructed internally. The `baseUrl(String)` override for testing works, but an injected `HttpClient` would be more composable and is a standard LC4J convention.

---

### 8. Observability (`ChatModelListener`)

**Classification: SEMANTICALLY COUPLED — NOT APPLICABLE**

`ChatModelListener` and its context objects (`ChatModelRequestContext`, `ChatModelErrorContext`) carry `ChatRequest`, `ChatResponse`, and `ModelProvider`. These are generative-model concepts. They cannot be adapted for `DecisionModel` without distorting semantics.

`LoggingHttpClient` (HTTP-layer logging decorator) is **NOT** coupled to `ChatModel`:

```java
public class LoggingHttpClient implements HttpClient {
    SuccessfulHttpResponse execute(HttpRequest request) throws HttpException { ... }
}
```

It logs `HttpRequest` and `SuccessfulHttpResponse` — purely generic. This could be wrapped around a `JdkHttpClient` in `JevDecisionModel` with no semantic distortion.

The finding from F-007 stands: a `DecisionModelListener` SPI has not been implemented. Nothing in LC4J provides one. `LoggingHttpClient` fills the transport-level logging gap independently of that future SPI.

---

## Validation of Clio's Statement

> "LC4J's OkHttp/Retrofit is bound to its own ChatModel request shape."

**This statement does not apply to the HTTP abstraction layer.** It applies, at best, to a prior architecture.

Current source evidence:
- `dev.langchain4j.http.client.HttpClient` — zero references to `ChatModel`, `ChatRequest`, `AiMessage`, or tool calling
- `JdkHttpClient` — implements `HttpClient` over JDK `java.net.http.HttpClient`; no generative types
- `OkHttpClient` — same interface, OkHttp backend; no generative types
- Retrofit — does not appear anywhere in `libs-code/langchain4j`
- `DefaultOpenAiClient` uses `dev.langchain4j.http.client.HttpClient`, not OkHttp/Retrofit directly

The generative-model coupling exists **above** the HTTP layer, in `ChatModelListener` context objects and in OpenAI's `ChatCompletionRequest`/`ChatCompletionResponse` DTOs. These were correctly not reused. But that coupling does not extend downward into `HttpClient`, `HttpRequest`, `RetryUtils`, or the exception hierarchy.

---

## JevHttpTransport Classification

**REFACTOR**

The implementation is functionally correct as written — all 15 tests pass. The classification is REFACTOR rather than REPLACE because the public API of `JevDecisionModel` is correct and should not change; only the internal transport layer needs reworking.

**Specific deficiencies requiring correction:**

| Deficiency | Evidence | Fix |
|---|---|---|
| Connect and read timeout conflated | Single `Duration timeout` used for both `connectTimeout` and per-request `timeout` | Separate `connectTimeout` / `readTimeout` in builder; use `JdkHttpClientBuilder` |
| No retry on 429 or 503 | `JevHttpTransport.send()` throws immediately on any non-2xx | Wrap with `RetryUtils.withRetryMappingExceptions()` using `ExceptionMapper` |
| Duplicate exception hierarchy without `Retriable` classification | `JevException` hierarchy duplicates LC4J exceptions without the retry-decision marker | Use LC4J exception types for HTTP errors; keep `JevValidationException(httpStatus=0)` for domain-mapping failures only |
| `HttpClient` not injectable | `JevHttpTransport` is constructed inside `JevDecisionModel` | Expose `httpClientBuilder(HttpClientBuilder)` in `JevDecisionModel.Builder`; default to `HttpClientBuilderLoader.loadHttpClientBuilder()` |

**What to keep:**
- `JevValidationException` for domain-level mapping failures (these are not HTTP exceptions)
- `JevException` as a module-level base if callers need to catch all Jev-originated errors with a single type — but it should extend or wrap LC4J types rather than duplicate them
- Jackson `ObjectMapper` for serialization
- The `baseUrl` override pattern for test isolation

**Dependency implication:**  
Adopting `JdkHttpClient` + `RetryUtils` + `HttpException` requires adding `langchain4j-core` and `langchain4j-http-client-jdk` to `argonaut-decision-jev`'s compile-scope dependencies. This is a real cost. The benefit — correct timeout separation, automatic retry on 429/503/timeout, `Retriable`/`NonRetriable` classification — is proportionate. The alternative is reimplementing all of this correctly, which reproduces LC4J's work without the test coverage it already carries.

---

## Summary Table

| Component | Classification | EP-002 Claim | Finding |
|---|---|---|---|
| `dev.langchain4j.http.client.HttpClient` interface | REUSE DIRECTLY | Not considered (attributed to OkHttp/Retrofit coupling) | Claim invalidated; interface is generic |
| `JdkHttpClient` / `JdkHttpClientBuilder` | REUSE DIRECTLY | Not considered | Correct connect/read timeout separation; already wraps JDK HttpClient |
| `HttpClientBuilder` injection pattern | REUSE WITH ADAPTER | Not considered | Enables testability without `baseUrl` hack |
| `LoggingHttpClient` | REUSE WITH ADAPTER | NOT NEEDED | Generic HTTP-level logging; no ChatModel coupling |
| `RetryUtils.RetryPolicy` | REUSE DIRECTLY (`@Internal` caveat) | NOT NEEDED (no documented retry contract) | Insufficient reason; 429/503 retry semantics are universal |
| `ExceptionMapper.DEFAULT` | REUSE DIRECTLY | NOT NEEDED | Correctly classifies retriable vs. non-retriable without generative semantics |
| `HttpException`, `RateLimitException`, `AuthenticationException`, `TimeoutException`, `InternalServerException` | REUSE DIRECTLY | NOT NEEDED | Parallel hierarchy reimplemented without `Retriable` classification |
| `ChatModelListener` | NOT APPLICABLE | NOT NEEDED — correct | Carries ChatRequest/ChatResponse — correct rejection |
| Jackson `ObjectMapper` | REUSED | REUSED — correct | Correctly identified |
| OkHttp / Retrofit coupling claim | **INVALIDATED** | "LC4J OkHttp/Retrofit bound to ChatModel" | Retrofit absent from codebase; HttpClient interface is ChatModel-free |
