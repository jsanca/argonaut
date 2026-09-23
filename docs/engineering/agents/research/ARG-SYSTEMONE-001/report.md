# ARG-JEV-002 — LangChain4j Jev Integration Design

**Date:** 2026-09-22  
**Role:** Clio — Software Engineer / Code Investigator  
**Codebase under investigation:** `libs-code/langchain4j` (local checkout)

---

## Investigation 1 — The LangChain4j Way

LangChain4j separates public API from provider SPI using a Template Method pattern on `ChatModel`. Every new provider follows the same extension path.

### The ChatModel SPI contract

`dev.langchain4j.model.chat.ChatModel` is the synchronous interface. Public entry points are default methods; providers override the SPI hook:

- `chat(ChatRequest)` — public default, delegates to `doChat()` after notifying listeners
- `doChat(ChatRequest)` — **the SPI hook** — abstract; providers implement this
- `chatAsync(ChatRequest)` → `CompletableFuture<ChatResponse>` — experimental async path; SPI hook is `doChatAsync()`
- `defaultRequestParameters()` — providers declare their parameter defaults here
- `supportedCapabilities()` → `Set<Capability>` — providers declare opt-in capabilities (currently only `RESPONSE_FORMAT_JSON_SCHEMA`)
- `listeners()` — observer hooks for request/response/error events
- `provider()` → `ModelProvider` — enum; new providers return `OTHER` until accepted upstream

`dev.langchain4j.model.chat.StreamingChatModel` is the parallel interface with `chat(ChatRequest, StreamingChatResponseHandler)` for token-stream responses.

### Request and response

`ChatRequest` (`dev.langchain4j.model.chat.request.ChatRequest`) carries:
- `List<ChatMessage> messages` — system, user, assistant, tool messages
- `ChatRequestParameters parameters` — all tuning knobs

`ChatRequestParameters` fields: `modelName`, `temperature`, `topP`, `topK`, `frequencyPenalty`, `presencePenalty`, `maxOutputTokens`, `stopSequences`, `toolSpecifications`, `toolChoice`, `responseFormat`.

`ResponseFormat` (`dev.langchain4j.model.chat.request.ResponseFormat`) is the structured-output gate:
- `ResponseFormatType type` — `TEXT` or `JSON`
- `JsonSchema jsonSchema` — optional; present when `type = JSON` for schema-constrained output
- Constants: `ResponseFormat.TEXT`, `ResponseFormat.JSON`

`ChatResponse` (`dev.langchain4j.model.chat.response.ChatResponse`) carries:
- `AiMessage aiMessage` — the text (or JSON-as-text) response
- `ChatResponseMetadata metadata` — `id`, `modelName`, `TokenUsage` (input/output/total token counts), `FinishReason` (STOP, LENGTH, TOOL_EXECUTION, CONTENT_FILTER, OTHER)

### Parameters hierarchy

```
ChatRequestParameters (interface)
  └─ DefaultChatRequestParameters (base record)
       ├─ OpenAiChatRequestParameters     (adds seed, logitBias, reasoningEffort, …)
       └─ AnthropicChatRequestParameters  (adds thinkingBudgetTokens, cacheSystemMessages, …)
```

Providers subclass `DefaultChatRequestParameters` to add provider-specific fields without polluting the core API.

### HTTP infrastructure

`langchain4j-http-client` module defines the shared abstraction:

```java
// dev.langchain4j.http.client.HttpClientBuilder
public interface HttpClientBuilder {
    Duration connectTimeout(); HttpClientBuilder connectTimeout(Duration);
    Duration readTimeout();    HttpClientBuilder readTimeout(Duration);
    HttpClient build();
}
```

Each provider constructs an internal client class (e.g. `OpenAiClient`, `AnthropicClient`) that receives an `HttpClientBuilder`. Concrete implementations (`langchain4j-http-client-jdk`, `langchain4j-http-client-apache`) are injected at runtime, not hardwired into the provider.

### Provider pattern — two analogues

**OpenAI** (`langchain4j-open-ai`, `dev.langchain4j.model.openai.OpenAiChatModel`):
- `doChat(ChatRequest)` calls `OpenAiUtils.toOpenAiChatRequest()` to produce `ChatCompletionRequest`, sends via `OpenAiClient.chatCompletion().executeRaw()`, maps back with `toChatResponse()`.
- Extended metadata: `OpenAiChatResponseMetadata` adds `created`, `serviceTier`, `systemFingerprint`.
- Builder uses SPI factory: `loadFactories(OpenAiChatModelBuilderFactory.class)` allows downstream override.

**Anthropic** (`langchain4j-anthropic`, `dev.langchain4j.model.anthropic.AnthropicChatModel`):
- `doChat(ChatRequest)` calls `InternalAnthropicHelper.createAnthropicRequest()`, sends via `AnthropicClient.createMessageWithRawResponse()`, maps back through `AnthropicMapper.toFinishReason()`.
- Extended metadata: `AnthropicChatResponseMetadata` adds `cacheDiagnostics`.
- Builder instantiated directly (no SPI factory).

Both patterns: provider-specific DTO ↔ LangChain4j types conversion happens in helper classes; the `doChat` body stays thin.

### SPI/extension mechanism

`dev.langchain4j.spi.ServiceHelper.loadFactories(Class<T>)` wraps `ServiceLoader.load()` with priority ordering via `PrioritizedFactory`. OpenAI uses this for `OpenAiChatModelBuilderFactory`; new providers can do the same or skip it and instantiate their builder directly as Anthropic does.

### Tests

Providers use **WireMock** to stub the provider HTTP endpoint:
- `OpenAiChatModelAsyncTest` — stubs `/v1/chat/completions` returning a canned JSON payload; asserts `chatAsync()` completes with expected `AiMessage` or completes exceptionally on HTTP 500.
- `OpenAiChatModelAsyncRetryTest` — verifies retry logic on transient failures.
- `AnthropicChatModelTest` — asserts that model-level defaults are applied when request parameters are absent, and that per-request parameters override them; verifies non-Anthropic parameter types are handled gracefully.

### Error handling

`dev.langchain4j.exception.LangChain4jException` (extends `RuntimeException`) is the root. HTTP-level errors are mapped at the provider boundary:
- `AuthenticationException` (401/403), `RateLimitException` (429), `ModelNotFoundException` (404), `InternalServerException` (500+), `TimeoutException`, `InvalidRequestException`.
- Errors are delivered via `ChatModelListener.onError()` before being (re)thrown.
- Async errors complete the `CompletableFuture` exceptionally.

---

## Investigation 2 — Jev as ChatModel

Jev's System One API exposes three typed operations — `Choice`, `Score`, `Noul` — each returning a probability distribution over a bounded outcome space. This section tests whether that contract can be expressed through `ChatModel`.

### What would the adapter look like

A minimal `JevChatModel` would implement `doChat(ChatRequest)` by:
1. Extracting state context from system+user messages.
2. Inspecting the last user message to detect which operation is being requested (Choice, Score, Noul).
3. Calling the Jev HTTP API.
4. Encoding the result as a JSON string inside `AiMessage`.

This is technically possible. But each step reveals a semantic mismatch.

### Mapping analysis

| LangChain4j concept | Jev equivalent | Assessment |
| --- | --- | --- |
| `apiKey` + `modelName` | Jev API key + model | Direct |
| `builder()` pattern | Identical shape | Direct |
| `provider()` enum | New `JEV` entry or `OTHER` | Minor |
| System+user messages as state context | Plausible — state maps to system message, question to user message | Adapter, fragile |
| `temperature` | No equivalent — Jev's randomness is probabilistic classification, not sampling temperature | Incompatible |
| `maxOutputTokens` | No tokens generated | Incompatible |
| `stopSequences`, `frequencyPenalty`, `presencePenalty` | No equivalents | Incompatible |
| `AiMessage` text content | Jev returns structured decision, not natural language | Misleading |
| `TokenUsage` | Jev does not consume tokens in the transformer sense | Inapplicable |
| `FinishReason` (STOP, LENGTH, TOOL_EXECUTION) | None apply to a classification decision | Misleading |
| `ResponseFormat.JSON` + `JsonSchema` | Closest mapping: force Jev result into JSON schema | Artificial encoding |
| Streaming / `StreamingChatModel` | Jev returns a complete distribution atomically | Inapplicable |
| Tool calling / `ToolSpecification` | Jev has no tool concept | Inapplicable |
| `supportedCapabilities()` | Only `RESPONSE_FORMAT_JSON_SCHEMA` exists — none fit | No match |
| `ChatModelListener` (observability) | Reusable: request/response events still meaningful | Reusable |

### The encoding problem

The most serious issue is what goes in `AiMessage`. LangChain4j's entire downstream pipeline — `AiServices`, prompt templates, memory, tool execution — assumes `AiMessage` contains either natural language or structured JSON that the framework requested via `ResponseFormat.JSON`. Stuffing a Jev `ChoiceResult` with its probability distribution into this field would be:

- **Lossy**: `AiMessage` is text. Probability distributions are floating-point vectors. Encoding them as JSON strings and then re-parsing them in the caller is an unnecessary roundtrip.
- **Fragile**: Operation type detection (is this a Choice or a Noul?) from message text is not reliable.
- **Invisible**: The `FinishReason`, `TokenUsage`, and `ChatResponseMetadata` fields that carry Jev's most valuable data (confidence, distribution) have no natural home in `ChatResponse`.

### The state+questions mapping

Jev's contract is `(state, question_type, alternatives?) → distribution`. The closest message encoding would be:

```
system: <the state/context being evaluated>
user:   {"operation": "Choice", "alternatives": ["A", "B", "C"]}
```

This encoding works for a single call but discards typing. The caller must pre-agree on a JSON protocol for the user message, which is not what `ChatMessage` is designed for.

### Verdict

**Jev is not genuinely a `ChatModel`. Implementing `ChatModel` would be semantically misleading.**

Jev does not generate text. It classifies. The concepts that make `ChatModel` coherent — token generation, temperature, finish reasons, AiMessage content — are category errors when applied to Jev. A `JevChatModel` would compile and run, but every downstream LangChain4j component that consumes a `ChatModel` would either ignore Jev's most important outputs (the probability distribution) or fail when it tries to parse `AiMessage` as text.

The one legitimate use of `ChatModel` semantics is through `ChatDecisionModel`: a wrapper that takes a conventional LangChain4j `ChatModel` (OpenAI, Anthropic, etc.) and drives it with `ResponseFormat.JSON` + a `JsonSchema` to produce structured decision outputs. That is not Jev — that is the LangChain4j path to the same decision abstraction.

Jev should be exposed through a purpose-built `DecisionModel` interface where its probability distribution is a first-class return value, not an encoded side-channel.

---

## Investigation 3 — Higher-Level Decision Abstraction

### Are Choice/Score/Noul Jev concepts or domain concepts?

The key question is whether `Choice`, `Score`, and `Noul` are concepts that happen to appear in Jev, or whether they are decision-domain concepts that Jev provides an especially natural implementation for.

Evidence that they are **domain concepts**:
- A LangChain4j `ChatModel` (OpenAI, Anthropic, Gemini) can answer "which of A, B, C?" or "score LOW/MEDIUM/HIGH" via `ResponseFormat.JSON` with a `JsonSchema`. The answer comes back as structured JSON. This is already possible with `AiServices`.
- The decision semantics (bounded alternatives, ordered scale, boolean proposition) are not invented by Jev — they predate it.
- A `DecisionModel` abstraction makes Jev substitutable with any generative model, which is exactly the testability and portability goal.

**Conclusion: `Choice`, `Score`, and `Noul` are domain concepts. Jev provides a native (and probabilistically richer) implementation; a conventional `ChatModel` provides a fallback implementation via structured output.**

### Proposed architecture

```
DecisionModel (interface — domain layer)
    ├─ JevDecisionModel          (Jev HTTP API — native probability)
    └─ ChatDecisionModel         (LangChain4j ChatModel + ResponseFormat.JSON)
             └─ ChatModel         (any provider: OpenAI, Anthropic, …)
```

This separates concerns cleanly:
- `DecisionModel` owns the typed decision contract and probability representation.
- `JevDecisionModel` maps to Jev's native HTTP API; no LangChain4j dependency required.
- `ChatDecisionModel` wraps any `ChatModel` and uses JSON schema-constrained output to approximate the same contract.
- Callers are written against `DecisionModel`; the provider is injected.

### Probability semantics

Jev's distributions are **first-class outputs**: P(A)=0.60, P(B)=0.30, P(C)=0.10 is the result, not a side-effect. A generative model using structured output can only return a single chosen value; getting a distribution requires either multiple sampling passes or a model-specific logprob API (OpenAI supports logprobs; Anthropic does not).

The abstraction must preserve this asymmetry without hiding it:

```java
public record ChoiceResult(
    String value,                     // the selected alternative
    List<ChoiceDistribution> distribution, // full P over all alternatives (empty from ChatDecisionModel unless supported)
    double confidence                  // point estimate: P(selected value)
) {}
```

`distribution` being empty for a `ChatDecisionModel` is **correct and honest**, not a defect. Callers that need full distributions should use `JevDecisionModel`; callers that only need the selected value are provider-agnostic.

### Uncertainty states: DECIDED / AMBIGUOUS / INSUFFICIENT_INFORMATION

These do **not** belong in the domain model.

Reasoning:
- Whether a decision with `confidence=0.52` is "ambiguous" depends on the application's threshold, not on the model contract.
- Jev's API returns distributions; it does not classify its own confidence as DECIDED or AMBIGUOUS.
- These labels are **policy** that callers apply to raw probabilities.
- Adding them to the domain model would either hard-code thresholds (wrong) or require the caller to pass thresholds in (which is just another way to say "the caller decides").

The abstraction should return `confidence` (a `double`) and let callers apply their own threshold policy.

### Noul and probabilistic booleans

`Noul` is semantically important: it is not `boolean`, it is `P(true)`. The abstraction must not reduce it prematurely:

```java
public record NoulResult(
    double probabilityTrue,   // e.g. 0.73
    double probabilityFalse,  // 1 - probabilityTrue
    double confidence         // same as probabilityTrue when binary
) {
    // Convenience only — callers that need a boolean apply their own threshold
    public boolean asBoolean() { return probabilityTrue > 0.5; }
}
```

P(true)=0.73 must be representable as `NoulResult(0.73, 0.27, 0.73)` without forcing a `true` answer.

### Score and ordered scale

`Score` is not a numeric value; it is a position in an ordered categorical scale. The abstraction should preserve the scale order:

```java
public record ScoreRequest(DecisionContext context, List<String> scale) {} // order matters: ["LOW", "MEDIUM", "HIGH"]
public record ScoreResult(
    String level,                      // the selected scale point
    int levelIndex,                    // position in the scale (0 = lowest)
    List<ScoreDistribution> distribution,
    double confidence
) {}
```

### Dependency direction conclusion

```
[Domain layer]         DecisionModel interface
                           /             \
[Jev adapter]     JevDecisionModel    ChatDecisionModel   [LangChain4j adapter]
                       |                    |
[Provider]       Jev HTTP API          ChatModel (LangChain4j)
                                            |
                                   LangChain4j infrastructure
```

Jev and LangChain4j sit at the same level as interchangeable providers. Neither is required by the domain layer. The domain layer (`DecisionModel`, `ChoiceRequest`, `ChoiceResult`, etc.) has zero provider dependencies.

---

## Deliverable 1 — LangChain4j Integration Study

### LangChain4j infrastructure reuse matrix

| LangChain4j component | Reuse verdict | Rationale |
| --- | --- | --- |
| `ChatModel` SPI (`doChat` hook) | **Incompatible for Jev** — **Reusable for `ChatDecisionModel`** | Jev is not generative; `ChatDecisionModel` wraps any `ChatModel` via this hook |
| `ChatRequest` / `ChatMessage` | Reusable with adaptation for `ChatDecisionModel`; unnecessary for Jev | Messages are the input vehicle for generative models |
| `ChatResponse` / `AiMessage` | Unnecessary for Jev; reusable for `ChatDecisionModel` output | Jev's probability distribution has no home in `AiMessage` |
| `ChatRequestParameters` (temperature, tokens…) | Incompatible for Jev | All fields are generative |
| `ResponseFormat.JSON` + `JsonSchema` | Reusable — `ChatDecisionModel` uses this to request structured output | |
| `HttpClientBuilder` / `langchain4j-http-client` | **Directly reusable** for `JevDecisionModel` HTTP transport | Same interface; Jev HTTP client can use JDK or Apache impl |
| `ServiceHelper.loadFactories()` (SPI) | **Directly reusable** for `JevDecisionModelBuilderFactory` | Same pattern as `OpenAiChatModelBuilderFactory` |
| Builder pattern (`builder()` / fluent setters) | **Directly reusable** | Identical shape; `apiKey`, `baseUrl`, `modelName`, timeout |
| `ChatModelListener` / observability | Reusable with adaptation | Rename to `DecisionModelListener`; event types differ |
| Exception hierarchy (`LangChain4jException` subclasses) | **Directly reusable** | `AuthenticationException`, `RateLimitException`, etc. apply to Jev HTTP errors |
| Retry logic (`withRetryMappingExceptions`) | **Directly reusable** | Jev HTTP calls are idempotent; same retry strategy applies |
| WireMock test patterns | **Directly reusable** | Same HTTP mock pattern; stub Jev endpoints instead of OpenAI |
| Token usage (`TokenUsage`) | **Unnecessary for Jev** | Jev does not bill by token |
| `FinishReason` enum | **Incompatible** | STOP, LENGTH, TOOL_EXECUTION have no Jev equivalent |
| Tool calling / `ToolSpecification` | **Unnecessary for Jev** | |
| Streaming / `StreamingChatModel` | **Unnecessary for Jev** | Jev decisions are atomic |
| `Capability` enum | Reusable with adaptation | New values: `PROBABILITY_DISTRIBUTION`, `CHOICE`, `SCORE`, `NOUL` |

### Proposed integration architecture

```
Caller / Argonaut
        │
        ▼
DecisionModel interface (domain layer)
       / \
      /   \
     ▼     ▼
JevDecisionModel          ChatDecisionModel
(Jev HTTP adapter)        (LangChain4j adapter)
     │                          │
     ▼                          ▼
Jev System One API          ChatModel (LangChain4j SPI)
(HTTP)                      / \
     │                     /   \
     │            OpenAiChatModel  AnthropicChatModel
     │                     \   /
     └──────────────────────▼──▼
                    HttpClientBuilder
                  (langchain4j-http-client)
```

### Module placement

| Module | Contents | LangChain4j dependency |
| --- | --- | --- |
| `langchain4j-decision` | `DecisionModel`, `ChoiceRequest/Result`, `ScoreRequest/Result`, `NoulRequest/Result`, `DecisionContext` | `langchain4j-core` (for `HttpClientBuilder`, SPI, exceptions) |
| `langchain4j-jev` | `JevDecisionModel`, Jev HTTP DTOs, `JevDecisionModelBuilder` | `langchain4j-decision`, `langchain4j-http-client` |
| `langchain4j-decision-chat` | `ChatDecisionModel` | `langchain4j-decision`, `langchain4j-core` (for `ChatModel`, `ResponseFormat`) |

Callers that only use `DecisionModel` depend on `langchain4j-decision` alone. The Jev HTTP dependency is isolated in `langchain4j-jev`.

### Extension path (minimum steps for a new Jev provider)

1. Add `langchain4j-decision` module with domain interfaces.
2. Add `langchain4j-jev` module:
   - Define Jev HTTP DTOs (`JevChoiceRequest`, `JevChoiceResponse`, etc.).
   - Implement `JevClient` using `HttpClientBuilder` from `langchain4j-http-client`.
   - Implement `JevDecisionModel` with `choose()`, `score()`, `evaluate()` mapping domain requests → Jev DTOs → domain results.
   - Provide `JevDecisionModelBuilder` following the OpenAI builder pattern.
   - Register `JevDecisionModelBuilderFactory` via `META-INF/services`.
3. Add `langchain4j-decision-chat` module:
   - Implement `ChatDecisionModel` wrapping `ChatModel`; use `ResponseFormat.JSON` + `JsonSchema` per operation type.
4. Add `FakeJevDecisionModel` and `FakeChatDecisionModel` in test scope for the TDD suite.

---

## Deliverable 2 — Proposed Domain Model and Interfaces

### PUBLIC DOMAIN API

These types have no provider dependency. They live in `langchain4j-decision`.

```java
// Context passed to every decision operation
public record DecisionContext(
    String state,               // the situation being evaluated
    List<String> background     // optional supporting evidence
) {
    public static DecisionContext of(String state) {
        return new DecisionContext(state, List.of());
    }
}

// --- CHOICE ---

public record ChoiceRequest(
    DecisionContext context,
    List<String> alternatives   // non-empty, no duplicates
) {}

public record ChoiceDistribution(
    String alternative,
    double probability          // in [0.0, 1.0]
) {}

public record ChoiceResult(
    String value,                          // the selected alternative
    List<ChoiceDistribution> distribution, // full P over all alternatives; empty when provider cannot supply
    double confidence                      // P(selected value); 0.0 when unknown
) {}

// --- SCORE ---

public record ScoreRequest(
    DecisionContext context,
    List<String> scale    // ordered ascending: ["LOW", "MEDIUM", "HIGH"]; non-empty
) {}

public record ScoreDistribution(
    String level,
    double probability
) {}

public record ScoreResult(
    String level,                          // the selected scale point
    int levelIndex,                        // position in the scale, 0 = lowest
    List<ScoreDistribution> distribution,
    double confidence
) {}

// --- NOUL (probabilistic boolean) ---

public record NoulRequest(
    DecisionContext context,
    String proposition          // the statement being evaluated
) {}

public record NoulResult(
    double probabilityTrue,     // in [0.0, 1.0]; e.g. 0.73
    double probabilityFalse,    // 1.0 - probabilityTrue
    double confidence           // == probabilityTrue for a binary decision
) {
    /** Convenience threshold at 0.5. Callers apply their own policy. */
    public boolean asBoolean() { return probabilityTrue > 0.5; }
}
```

### PROVIDER SPI

The interface that both `JevDecisionModel` and `ChatDecisionModel` implement, also in `langchain4j-decision`.

```java
public interface DecisionModel {

    ChoiceResult choose(ChoiceRequest request);

    ScoreResult score(ScoreRequest request);

    NoulResult evaluate(NoulRequest request);

    /** Providers declare which operations they support natively. */
    default Set<DecisionCapability> supportedCapabilities() {
        return Set.of();
    }
}

public enum DecisionCapability {
    PROBABILITY_DISTRIBUTION,   // full distribution available, not just the winner
    CHOICE,
    SCORE,
    NOUL
}
```

Providers implement `DecisionModel` directly. No abstract base class is required for the initial slice.

### JEV-SPECIFIC ADAPTER TYPES

These live in `langchain4j-jev` and are **never exposed through the `DecisionModel` public API**.

```java
// Internal Jev HTTP DTOs (package-private or internal package)
record JevChoiceRequest(
    String model,
    String state,
    List<String> choices
) {}

record JevChoiceResponse(
    String selected,
    Map<String, Double> probabilities
) {}

// Similar: JevScoreRequest/Response, JevNoulRequest/Response

// The public-facing model
public final class JevDecisionModel implements DecisionModel {

    public static JevDecisionModelBuilder builder() { … }

    @Override
    public ChoiceResult choose(ChoiceRequest request) {
        // validate → map to JevChoiceRequest → HTTP call → map JevChoiceResponse → ChoiceResult
    }
    // … score(), evaluate()

    @Override
    public Set<DecisionCapability> supportedCapabilities() {
        return Set.of(PROBABILITY_DISTRIBUTION, CHOICE, SCORE, NOUL);
    }
}

public final class JevDecisionModelBuilder {
    public JevDecisionModelBuilder apiKey(String apiKey) { … }
    public JevDecisionModelBuilder baseUrl(String baseUrl) { … }
    public JevDecisionModelBuilder modelName(String modelName) { … }
    public JevDecisionModelBuilder timeout(Duration timeout) { … }
    public JevDecisionModelBuilder maxRetries(int maxRetries) { … }
    public JevDecisionModel build() { … }
}
```

### LangChain4j adapter (in `langchain4j-decision-chat`)

```java
public final class ChatDecisionModel implements DecisionModel {

    private final ChatModel chatModel;

    public ChatDecisionModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public ChoiceResult choose(ChoiceRequest request) {
        // Build ChatRequest with ResponseFormat.JSON + JsonSchema for {"selected": string}
        // Send via chatModel.chat()
        // Parse AiMessage text as JSON, return ChoiceResult with empty distribution
    }
    // … score(), evaluate()

    @Override
    public Set<DecisionCapability> supportedCapabilities() {
        // No PROBABILITY_DISTRIBUTION unless the underlying ChatModel exposes logprobs
        return Set.of(CHOICE, SCORE, NOUL);
    }
}
```

### Boundary summary

| Boundary | Types |
| --- | --- |
| **PUBLIC DOMAIN API** | `DecisionModel`, `DecisionContext`, `ChoiceRequest`, `ChoiceResult`, `ChoiceDistribution`, `ScoreRequest`, `ScoreResult`, `ScoreDistribution`, `NoulRequest`, `NoulResult`, `DecisionCapability` |
| **PROVIDER SPI** | `DecisionModel` (interface — same type; providers implement it) |
| **JEV-SPECIFIC ADAPTER** | `JevChoiceRequest/Response`, `JevScoreRequest/Response`, `JevNoulRequest/Response`, `JevClient` (internal) |

---

## Deliverable 3 — Minimum TDD Specification

No live network access is required. All tests use `FakeJevDecisionModel` (hard-coded outputs, simulates Jev-native distribution) and `FakeChatDecisionModel` (hard-coded outputs, simulates LangChain4j structured-output path with empty distribution).

### Fake implementations

```java
/** Simulates JevDecisionModel: returns full probability distributions. */
class FakeJevDecisionModel implements DecisionModel {
    // Configured via constructor with pre-baked responses
    private final Map<String, ChoiceResult> choiceResults;
    // … similar for score, noul
}

/** Simulates ChatDecisionModel: returns only the winner, no distribution. */
class FakeChatDecisionModel implements DecisionModel {
    // Returns ChoiceResult with empty distribution and confidence=0.0
}
```

### ChoiceTest

```java
class ChoiceTest {

    @Test
    void jev_returns_selected_value_and_full_distribution() {
        var model = new FakeJevDecisionModel(
            new ChoiceResult("B",
                List.of(new ChoiceDistribution("A", 0.20),
                        new ChoiceDistribution("B", 0.65),
                        new ChoiceDistribution("OTHER", 0.15)),
                0.65)
        );
        var result = model.choose(new ChoiceRequest(
            DecisionContext.of("some state"),
            List.of("A", "B", "OTHER")
        ));
        assertThat(result.value()).isEqualTo("B");
        assertThat(result.confidence()).isEqualTo(0.65);
        assertThat(result.distribution()).hasSize(3);
        assertThat(result.distribution())
            .extracting(ChoiceDistribution::alternative)
            .containsExactly("A", "B", "OTHER");
    }

    @Test
    void chat_returns_selected_value_without_distribution() {
        var model = new FakeChatDecisionModel("B");
        var result = model.choose(new ChoiceRequest(
            DecisionContext.of("some state"),
            List.of("A", "B", "OTHER")
        ));
        assertThat(result.value()).isEqualTo("B");
        assertThat(result.distribution()).isEmpty();
    }

    @Test
    void same_choice_request_works_with_both_providers() {
        var request = new ChoiceRequest(
            DecisionContext.of("same state"),
            List.of("A", "B", "OTHER")
        );
        DecisionModel jev = new FakeJevDecisionModel(new ChoiceResult("A", …, 0.70));
        DecisionModel chat = new FakeChatDecisionModel("A");
        assertThat(pickWinner(jev, request)).isEqualTo("A");
        assertThat(pickWinner(chat, request)).isEqualTo("A");
    }

    private String pickWinner(DecisionModel model, ChoiceRequest req) {
        return model.choose(req).value();
    }
}
```

### NoulTest

```java
class NoulTest {

    @Test
    void probabilistic_true_is_not_reduced_to_boolean() {
        var model = new FakeJevDecisionModel(
            new NoulResult(0.73, 0.27, 0.73)
        );
        var result = model.evaluate(new NoulRequest(
            DecisionContext.of("some state"),
            "the proposition is valid"
        ));
        assertThat(result.probabilityTrue()).isEqualTo(0.73);
        assertThat(result.probabilityFalse()).isEqualTo(0.27);
        assertThat(result.probabilityTrue() + result.probabilityFalse()).isCloseTo(1.0, offset(0.0001));
    }

    @Test
    void low_confidence_noul_does_not_force_a_decisive_boolean() {
        var result = new NoulResult(0.51, 0.49, 0.51);
        assertThat(result.probabilityTrue()).isLessThan(0.6);
        assertThat(result.asBoolean()).isTrue(); // passes threshold but is clearly weak
    }
}
```

### ScoreTest

```java
class ScoreTest {

    @Test
    void score_result_preserves_ordered_scale_position() {
        var scale = List.of("LOW", "MEDIUM", "HIGH");
        var model = new FakeJevDecisionModel(
            new ScoreResult("MEDIUM", 1,
                List.of(new ScoreDistribution("LOW", 0.15),
                        new ScoreDistribution("MEDIUM", 0.60),
                        new ScoreDistribution("HIGH", 0.25)),
                0.60)
        );
        var result = model.score(new ScoreRequest(
            DecisionContext.of("some state"), scale
        ));
        assertThat(result.level()).isEqualTo("MEDIUM");
        assertThat(result.levelIndex()).isEqualTo(1);
        assertThat(result.distribution()).hasSize(3);
    }
}
```

### InvalidDomainTest

```java
class InvalidDomainTest {

    @Test
    void choice_with_empty_alternatives_is_rejected() {
        assertThatThrownBy(() ->
            new ChoiceRequest(DecisionContext.of("state"), List.of())
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void choice_with_duplicate_alternatives_is_rejected() {
        assertThatThrownBy(() ->
            new ChoiceRequest(DecisionContext.of("state"), List.of("A", "A", "B"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noul_with_probability_outside_unit_interval_is_rejected() {
        assertThatThrownBy(() -> new NoulResult(1.5, -0.5, 1.5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void choice_distribution_with_probability_outside_unit_interval_is_rejected() {
        assertThatThrownBy(() -> new ChoiceDistribution("A", -0.1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void score_with_empty_scale_is_rejected() {
        assertThatThrownBy(() ->
            new ScoreRequest(DecisionContext.of("state"), List.of())
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
```

### Test count summary

| Suite | Tests | What it proves |
| --- | --- | --- |
| `ChoiceTest` | 3 | Value + distribution + provider independence |
| `NoulTest` | 2 | Probabilistic boolean preservation |
| `ScoreTest` | 1 | Ordered scale + distribution |
| `InvalidDomainTest` | 5 | Invariants: empty/duplicate alternatives, probability range, empty scale |
| **Total** | **11** | Minimum useful suite before real HTTP integration |

---

## Decision Record

**1. Can Jev reasonably implement LangChain4j `ChatModel`? Why or why not?**

No. `ChatModel.doChat()` returns a `ChatResponse` containing an `AiMessage` (text) plus `TokenUsage` and `FinishReason`. None of these concepts have a Jev equivalent: Jev does not generate text, does not consume tokens in the transformer sense, and its decision outcomes do not map to STOP/LENGTH/TOOL_EXECUTION finish reasons. A `JevChatModel` could compile, but it would be a misleading adapter that hides Jev's most valuable outputs (the probability distribution) inside a text-formatted JSON string with no natural place in `ChatResponse`. Semantic correctness takes priority over compatibility.

**2. How much LangChain4j infrastructure can the Jev integration reuse?**

Directly reusable: `HttpClientBuilder` (transport), `ServiceHelper.loadFactories()` (SPI), builder pattern, exception hierarchy (`AuthenticationException`, `RateLimitException`, etc.), retry logic, WireMock test patterns.

Reusable with adaptation: `ChatModelListener` → `DecisionModelListener`; `Capability` enum → add `PROBABILITY_DISTRIBUTION`, `CHOICE`, `SCORE`, `NOUL`; `ResponseFormat.JSON` + `JsonSchema` used by `ChatDecisionModel` only.

**3. What cannot be reused because Jev is not generative?**

`ChatRequest`, `ChatResponse`, `AiMessage`, `TokenUsage`, `FinishReason`, `ChatRequestParameters` (temperature, maxOutputTokens, stopSequences, tool calling), `StreamingChatModel`, and `AiServices`. These are generative concepts with no Jev equivalent.

**4. Should `Choice`, `Score`, and `Noul` exist above LangChain4j as provider-independent domain concepts?**

Yes. The investigation confirms that a conventional LangChain4j `ChatModel` can answer the same decision requests via `ResponseFormat.JSON` + `JsonSchema`. The semantics of "choose among alternatives" and "score on a scale" are not Jev inventions — they are decision-domain concepts. Jev provides a native probabilistically-rich implementation; `ChatDecisionModel` provides a generative fallback. Both implement `DecisionModel`.

**5. Can an ordinary LangChain4j `ChatModel` implement the same decision abstraction using structured output?**

Yes, with a documented limitation: `ChatDecisionModel` returns only the selected value; `distribution` is empty because a single-pass generative model does not expose a probability distribution over alternatives. `confidence` is 0.0 unless the model is prompted to self-report a score. Callers that need true probability distributions should use `JevDecisionModel`.

**6. Does the proposed abstraction preserve Jev's probability semantics without leaking Jev-specific DTOs?**

Yes. `ChoiceResult.distribution`, `NoulResult.probabilityTrue/probabilityFalse`, and `ScoreResult.distribution` are typed in terms of domain records. Jev HTTP DTOs (`JevChoiceResponse`, etc.) are internal to `langchain4j-jev` and never exposed through `DecisionModel`. The domain layer has zero Jev imports.

**7. What is the smallest implementation slice we should build next?**

Implement `langchain4j-decision` with the domain records and `DecisionModel` interface, plus `FakeJevDecisionModel` and `FakeChatDecisionModel`. Run the 11-test TDD suite to prove the domain model compiles and the invariants hold. This slice requires no HTTP calls, no Jev credentials, and no model dependency beyond `langchain4j-core` exception types. Once green, the next slice is `JevDecisionModel` with a WireMock-stubbed HTTP integration test.
