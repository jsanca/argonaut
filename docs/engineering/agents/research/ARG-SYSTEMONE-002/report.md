# ARG-SYSTEMONE-002 — System One Protocol Investigation

**Date:** 2026-09-23
**Scope:** TypeSafe System One, Jev, Kev, OpenRouter Decisions API, and existing Argonaut decision model
**Investigator:** Clio (Software Engineer / Code Investigator)
**Question answered:** Are we implementing Jev specifically, or are we implementing System One with Jev as one model/provider?

---

## 1. Executive Conclusion

**We are implementing System One with Jev as one model (provider).**

System One is a wire protocol and API contract. Jev is TypeSafe's hosted model that speaks it. Kev is an independent open-source implementation that speaks the same contract. OpenRouter proxies Jev under a different path with a compatible superset response. The shared wire contract is `POST /v1/systemone` with `{model, state, questions}` -> `{model, answers, usage}`. Any endpoint that honors this contract can be targeted by a single Argonaut client.

The smallest coherent Argonaut API that can exploit the common capabilities of TypeSafe Jev, OpenRouter Jev, and local Kev without becoming a God Request is the current `DecisionModel.decide(DecisionRequest)` interface -- with the transport layer generalized from `JevHttpTransport` to a `SystemOneTransport` (or equivalently a `SystemOneHttpTransport`) configured with `baseUrl`, `apiKey`, and the endpoint path. The domain types -- `Choice`, `Score`, `Noul`, `ChoiceResult`, `ScoreResult`, `NoulResult` -- are already named in protocol-neutral terms and need no renaming. The module `argonaut-decision-jev` and class `JevDecisionModel` carry naming debt but not structural debt; they can remain as-is until there is concrete pressure to add a second provider.

---

## 2. Terminology

| Term | Definition (as used in this report) |
|---|---|
| **System One** | The wire protocol and API contract: `POST /v1/systemone`, `{model, state, questions}` -> `{model, answers, usage}`. Also the name TypeSafe gives to the model architecture family. Both usages are legitimate; this report uses it to mean the contract unless stated otherwise. |
| **Jev** | TypeSafe AI's hosted model that implements the System One contract. Current version: `jev-1.13`, aliased as `jev-latest`. |
| **Kev** | An independent open-source System One implementation by Jared Palmer, built on Qwen3.5. Speaks the same `POST /v1/systemone` contract as Jev. |
| **SystemOneRequest** | The wire request body: `{model: string, state: JSONContent, questions: map<id, Question>}`. |
| **SystemOneResponse** | The wire response body: `{model: string, answers: map<id, Answer>, usage: {input_tokens, output_tokens}}`. |
| **Question** | A discriminated union of `Noul | Choice | Score`, each with `type`, `instructions`, and `criteria`. |
| **Answer** | A discriminated union of `NoulAnswer | ChoiceAnswer | ScoreAnswer`. |
| **Usage** | Token count: `{input_tokens: int, output_tokens: int}`. |
| **state** | The content to evaluate: `string | object | array`. Shared by all questions in a request. |
| **TypeSafe SDK** | Official Python (`typesafe_sdk`) and JavaScript (`@typesafe-ai/sdk`) SDKs. Model-aware; hardcodes `/v1/systemone` as the path. |
| **provider** | An entity that hosts a System One-compatible endpoint (TypeSafe, OpenRouter, a Kev instance, a fine-tuned local model). |
| **transport** | The HTTP layer responsible for sending a serialized request and receiving a serialized response. |

---

## 3. Primary Evidence

### 3.1 Local sources

| Source | Key facts |
|---|---|
| `argonaut-decision-jev/.../JevHttpTransport.java` | Posts to `{baseUrl}/v1/systemone`. Bearer auth. LangChain4j HTTP client. Retry on 429/503/timeout. |
| `argonaut-decision-jev/.../JevRequestDto.java` | Wire fields: `model`, `state`, `questions: map<String, JevQuestionDto>`. |
| `argonaut-decision-jev/.../JevResponseDto.java` | Wire fields: `answers: map<String, JevAnswerDto>`, `usage`. |
| `argonaut-decision-jev/.../JevAnswerDto.java` | Fields: `type`, `choice`, `score`, `noul`, `confidence`, `probabilities`, `legend`. |
| `argonaut-decision-jev/.../JevDecisionModel.java` | Default base URL: `https://api.typesafe.ai`. Default model: `jev-latest`. `baseUrl` is overridable via builder. |
| `libs-code/kev/kev/api.py` | Defines `SystemOneRequest(state, model, questions)`. Field `model` defaults to `"kev-latest"`. Outputs match TypeSafe shape. |
| `libs-code/kev/kev/serve.py` | Serves `POST /v1/systemone`. Accepts `"jev-latest"` as a model name (both `"kev-latest"` and `"jev-latest"` serve the loaded checkpoint). Returns `x-typesafe-request-id` header. Responds to `GET /v1/models`. |
| `libs-code/kev/tests/test_api.py` | Sends requests with `"model": "jev-latest"` to a Kev server. Uses the TypeSafe SDK (`TypeSafeClient`) pointed at `BASE` (a Kev URL). Both prove the contract is shared. |
| `libs-code/kev/README.md` | States explicitly: "The API matches TypeSafe's System One, so you can point their Python SDK at your local server." |

### 3.2 Web sources

| Source | Key facts |
|---|---|
| `docs.typesafe.ai/concepts/system-one` | "System One is a class of AI models built to make fast, structured decisions." Jev is "the first System One model." |
| `typesafe.ai/blog/introducing-system-one-models-and-jev` | System One is the architectural paradigm; Jev is the product. Trained with RLCD. |
| `openrouter.ai/docs/guides/community/typesafe-sdk` | TypeSafe SDK can target OpenRouter by setting `base_url="https://openrouter.ai/api"`. |
| `docs.typesafe.ai/api` | Full wire contract for `POST /v1/systemone` documented. Response: `{model, answers, usage}`. |
| OpenRouter pydantic-ai issue #8552 | "The wire format (`state` + `questions` -> `answers`) is the same -- OpenRouter just proxies the TypeSafe Decisions API." TypeSafe SDK hardcodes `/v1/systemone`; OpenRouter uses `/api/alpha/decisions`. |
| OpenRouter blog: "What Is Jev?" | TypeSafe SDK points to OpenRouter by changing base URL. Response additionally includes `id`, `provider`, `usage.cost`. |

---

## 4. System One Analysis

### 4.1 What is System One?

System One has two simultaneous meanings that are both legitimate and mutually reinforcing:

1. **Model architecture**: A design philosophy for "fast, structured decision models" (the Kahneman System 1 analogy). Inference is a single forward pass (prefill-only for Kev). No text generation. Calibrated probability output.

2. **API/wire contract**: `POST /v1/systemone` with `{model, state, questions: map<id, Question>}` -> `{model, answers: map<id, Answer>, usage}`. This is the contract that both Jev (TypeSafe's hosted model) and Kev (open-source) implement identically, and that OpenRouter proxies.

The existence of Kev as a fully independent reimplementation that explicitly targets this same contract is definitive evidence that System One has crystallized into a wire protocol, not merely a brand for TypeSafe's product. The TypeSafe SDK can target Kev unchanged (proven by `tests/test_api.py`). This is the strongest single piece of evidence: a protocol is defined by its independent implementations.

### 4.2 Canonical field vocabulary

| Wire term | Description | Kev name | Argonaut domain name |
|---|---|---|---|
| `state` | Content to evaluate | `state` (`JSONContent`) | `DecisionContext.state()` |
| `model` | Model identifier string | `model` (string) | `JevDecisionModel.modelName` (builder config) |
| `questions` | Map of question objects | `questions: dict[str, Question]` | `DecisionRequest.questions()` |
| Question `type` | `"noul" | "choice" | "score"` | `q.type` | `Noul`, `Choice`, `Score` (sealed hierarchy) |
| Question `instructions` | Text describing the question | `q.instructions: JSONContent` | `Question.instructions()` |
| Question `criteria` | Options/scale/labels | `q.criteria` (type-specific) | `Choice.candidates()`, `Score.scale()`, absent for `Noul` |
| Answer `type` | Discriminates answer shape | `m["type"]` | Java discriminated by `instanceof` in mapper |
| Choice `choice` | Selected option name | `m["keys"][argmax]` | `ChoiceResult.selected()` |
| Choice `confidence` | Normalized confidence in [0,1] | `choice_confidence(p)` | `ChoiceResult.confidence()` |
| Choice `probabilities` | `{option: p}` | `dist` dict | `ChoiceResult.distribution()` |
| Score `score` | Fractional level index | `score = sum(i * pi ...)` | `ScoreResult.rawScore()` |
| Score `legend` | `{"0": text, "1": text, ...}` | `m["legend"]` | Not in `ScoreResult` directly; reconstructable from `scale()` |
| Score `probabilities` | `{"0": p, "1": p, ...}` | `{str(i): round_prob(v) ...}` | `ScoreResult.distribution()` (after mapping) |
| Score `confidence` | Distance from modal level | `score_confidence(p)` | `ScoreResult.confidence()` |
| Noul `noul` | P(true) in [0,1] | `round_prob(p[1])` | `NoulResult.probabilityTrue().value()` |
| `usage` | `{input_tokens, output_tokens}` | `{input_tokens, output_tokens}` | `JevUsageDto` (internal; not surfaced to domain) |
| `id` | Request ID (OpenRouter only) | `x-typesafe-request-id` header | Not present in domain |
| `provider` | "TypeSafe" (OpenRouter only) | -- | Not present in domain |
| `usage.cost` | Billing cost (OpenRouter only) | -- | Not present in domain |
| `latency_ms` | Inference time (Kev only) | `round(dt * 1000, 1)` | Not present in domain |

---

## 5. Jev Analysis

### 5.1 What is Jev?

Jev is TypeSafe AI's hosted model that implements the System One contract. Specifically:

- It is a trained model (current version: `jev-1.13`, aliased `jev-latest`).
- It is accessible at `https://api.typesafe.ai/v1/systemone`.
- It is also accessible through OpenRouter at `POST /api/alpha/decisions` with an OpenRouter API key, model name `typesafe/jev-1.13`.
- It is not the protocol. Another model (Kev) implements the same protocol.

Jev is the first and currently the most capable public System One model. The TypeSafe SDK's default model is `jev-latest`. The Kev server also accepts `jev-latest` as a model name so that unconfigured TypeSafe SDK clients work against it.

### 5.2 Concept classification

| Current Argonaut concept | Classification | Justification |
|---|---|---|
| `JevDecisionModel` | SYSTEM-ONE-SPECIFIC (with JEV-SPECIFIC naming) | The class implements the System One protocol; its name says "Jev" but nothing in the implementation is Jev-specific. `baseUrl` is overridable; `modelName` is configurable. It already can target Kev. |
| `JevHttpTransport` | TRANSPORT-SPECIFIC | Posts to `{baseUrl}/v1/systemone`. Not Jev-specific beyond the error message strings. Would work against any System One endpoint at that path. |
| `JevRequestDto` / `JevResponseDto` | SYSTEM-ONE-SPECIFIC (with JEV-SPECIFIC naming) | Fields `model`, `state`, `questions`, `answers`, `usage` are the System One wire contract. No Jev-proprietary fields. |
| `JevQuestionDto` | SYSTEM-ONE-SPECIFIC | Fields `type`, `instructions`, `criteria` are protocol fields. |
| `JevAnswerDto` | SYSTEM-ONE-SPECIFIC | Fields `type`, `choice`, `score`, `noul`, `confidence`, `probabilities`, `legend` are protocol fields. None Jev-proprietary. |
| `JevUsageDto` | SYSTEM-ONE-SPECIFIC | `input_tokens`/`output_tokens` are protocol fields. |
| `JevRequestMapper` | SYSTEM-ONE-SPECIFIC | Maps Argonaut domain types to the System One wire format. Not Jev-specific. |
| `JevResponseMapper` | SYSTEM-ONE-SPECIFIC | Maps System One wire response to Argonaut domain types. Not Jev-specific. |
| `CandidateKey<T>` | GENERIC DECISION DOMAIN | A bidirectional string-key mapping for Choice candidates. Protocol-independent. |
| `JevException` / subclasses | JEV-SPECIFIC (HTTP error mapping) | HTTP status code mapping to named errors is Jev-specific in naming. However, the HTTP status codes (401, 422, 429, 503) are standard and would apply to any REST System One provider. |
| `JevDecisionModel.Builder` | JEV-SPECIFIC (in naming; SYSTEM-ONE-SPECIFIC in capability) | `baseUrl`, `modelName`, timeouts, retries. Entirely general; naming is the only Jev coupling. |
| `DEFAULT_BASE_URL = "https://api.typesafe.ai"` | JEV-SPECIFIC | The TypeSafe production endpoint. |
| `DEFAULT_MODEL = "jev-latest"` | JEV-SPECIFIC | The Jev model name. |
| `argonaut-decision-jev` (module) | JEV-SPECIFIC (naming) | The module name implies a Jev-specific provider. The implementation is a System One provider. |

**Key finding:** The only genuinely Jev-specific elements in the existing implementation are the two default values (`https://api.typesafe.ai`, `jev-latest`), the error message strings, and the names. The entire structure is a System One protocol implementation.

---

## 6. Kev Analysis

### 6.1 Kev's /v1/systemone endpoint: what it accepts and returns

From `libs-code/kev/kev/api.py` and `libs-code/kev/kev/serve.py`:

**Request model (`SystemOneRequest`):**
```python
class SystemOneRequest(BaseModel):
    state: JSONContent          # string | dict | list | int | float | bool | None
    model: str = "kev-latest"   # also accepts "jev-latest"
    questions: dict[str, Question]  # min 1 question
```

Question types:
- `Noul`: `{type: "noul", instructions: JSONContent = None, criteria: dict[str, JSONContent] | None = None}`
- `Choice`: `{type: "choice", instructions: JSONContent = None, criteria: dict[str, JSONContent]}` (1-255 options)
- `Score`: `{type: "score", instructions: JSONContent = None, criteria: list[JSONContent]}` (1-255 levels)

**Response (from `to_answers()` and `answer()` in serve.py):**
```json
{
  "model": "kev-latest",
  "answers": {
    "<id>": { "type": "noul", "noul": 0.93 },
    "<id>": { "type": "choice", "choice": "returns", "confidence": 0.21, "probabilities": {"returns": 0.47, "shipping": 0.28, "billing": 0.25} },
    "<id>": { "type": "score", "score": 1.44, "legend": {"0": "Calm", "1": "Frustrated", "2": "Very angry"}, "probabilities": {"0": 0.00, "1": 0.56, "2": 0.44}, "confidence": 0.78 }
  },
  "usage": {"input_tokens": 101, "output_tokens": 161},
  "latency_ms": 495
}
```

Also: `x-typesafe-request-id` response header on every response.

### 6.2 Field-by-field comparison: Kev vs Argonaut JevDTOs

| Wire field | Kev | Argonaut JevDto | Status |
|---|---|---|---|
| Request `model` | `str`, default `"kev-latest"` | `String` | IDENTICAL |
| Request `state` | `JSONContent` (str/dict/list/...) | `String` | COMPATIBLE -- Kev allows richer types; Argonaut sends only string. Kev renders non-strings to text. No breakage. |
| Request `questions` | `dict[str, Question]` | `Map<String, JevQuestionDto>` | IDENTICAL structure |
| Question `type` | `"noul" | "choice" | "score"` | `String` (`"noul"`, `"choice"`, `"score"`) | IDENTICAL |
| Question `instructions` | `JSONContent` (optional) | `String` (non-null in Argonaut mapper) | COMPATIBLE -- Kev makes it optional; Argonaut always sends it. Valid. |
| Choice `criteria` | `dict[str, JSONContent]` | `Map<String,String>` (key-to-key mapping) | COMPATIBLE -- Argonaut uses identical key-key; Kev accepts richer values. No information lost. |
| Score `criteria` | `list[JSONContent]` | `List<String>` | COMPATIBLE -- Argonaut sends strings; Kev accepts richer types. |
| Noul `criteria` | `dict[str, JSONContent] | None` | absent (null via `@JsonInclude(NON_NULL)`) | COMPATIBLE -- Kev makes Noul criteria optional; Argonaut omits it. Kev defaults to `{false: "no", true: "yes"}`. |
| Response `model` | Present | Not deserialized in `JevResponseDto` | MINOR GAP -- field is part of the protocol; not captured. Non-blocking. |
| Response `answers` | `dict[str, Answer]` | `Map<String, JevAnswerDto>` | IDENTICAL |
| Answer `type` | `"noul" | "choice" | "score"` | `String` | IDENTICAL |
| Choice `choice` | option name string | `String` | IDENTICAL |
| Choice `confidence` | float | `Double` | IDENTICAL |
| Choice `probabilities` | `{name: float}` | `Map<String, Double>` | IDENTICAL |
| Score `score` | fractional index float | `Double` | IDENTICAL |
| Score `legend` | `{"0": text, ...}` | `List<String>` (not used in mapper) | COMPATIBLE -- Kev returns legend as a map; Argonaut reconstructs from scale. Scale is preserved. |
| Score `probabilities` | `{"0": float, ...}` | `Map<String, Double>` | IDENTICAL |
| Score `confidence` | float | `Double` | IDENTICAL |
| Noul `noul` | float P(true) | `Double` | IDENTICAL |
| Response `usage` | `{input_tokens, output_tokens}` | `JevUsageDto` | IDENTICAL |
| Response `latency_ms` | float (Kev only) | not present | OPTIONAL EXTENSION -- `FAIL_ON_UNKNOWN_PROPERTIES = false` means silently ignored. |

### 6.3 Can the current Argonaut JevDecisionModel talk to Kev unchanged?

**Yes, with only a builder configuration change:**

```java
JevDecisionModel model = JevDecisionModel.builder()
    .apiKey("local")                          // KEV_API_KEY if set, otherwise any string
    .baseUrl("http://127.0.0.1:8009")         // Kev's server URL
    .modelName("kev-latest")                  // or "jev-latest" -- Kev accepts both
    .build();
```

The `JevHttpTransport.send()` method posts to `{baseUrl}/v1/systemone`, which is exactly the path Kev exposes. `ObjectMapper` is configured with `FAIL_ON_UNKNOWN_PROPERTIES = false`, so Kev's `latency_ms` extension field is silently ignored. All wire fields Argonaut sends are valid System One fields that Kev accepts.

### 6.4 Difference classification

| Difference | Category |
|---|---|
| `latency_ms` in Kev response | OPTIONAL EXTENSION -- Argonaut silently ignores it (correct). |
| `model` field absent in `JevResponseDto` | ARGONAUT BUG (minor) -- field is part of the protocol; not deserialized. No behavioral impact. |
| Kev allows `state` as object/array; Argonaut sends string only | OPTIONAL EXTENSION -- no incompatibility. |
| Kev allows Noul `criteria` with descriptions; Argonaut omits it | OPTIONAL EXTENSION -- Kev defaults gracefully. |
| Kev limits Score to 1-255 levels; TypeSafe limits Score to 2-10 | IMPLEMENTATION METADATA -- different constraint enforced server-side only. |
| Score `legend` as map vs list | COMPATIBLE -- Argonaut reconstructs from `scale`; semantically equivalent. |
| Kev `x-typesafe-request-id` response header | OPTIONAL EXTENSION -- not captured by Argonaut currently. |

No PROTOCOL INCOMPATIBILITIES were found.

---

## 7. OpenRouter Analysis

### 7.1 OpenRouter Decisions API

OpenRouter exposes Jev at `POST https://openrouter.ai/api/alpha/decisions` with authorization `Bearer <OPENROUTER_API_KEY>`. The model name is `typesafe/jev-1.13` (or `~typesafe/jev-latest`).

The wire request body is identical to the TypeSafe System One contract: `{model, state, questions}`.

The response body is a superset: `{model, answers, usage}` plus OpenRouter-specific fields `id`, `provider`, and `usage.cost`.

### 7.2 TypeSafe SDK and OpenRouter

The TypeSafe SDK can target OpenRouter by setting `base_url = "https://openrouter.ai/api"`. When configured this way, the SDK appends `/v1/systemone` to form `https://openrouter.ai/api/v1/systemone` -- and OpenRouter handles this path for TypeSafe SDK compatibility.

For Argonaut's `JevDecisionModel`:

```java
JevDecisionModel model = JevDecisionModel.builder()
    .apiKey(System.getenv("OPENROUTER_API_KEY"))
    .baseUrl("https://openrouter.ai/api")
    .modelName("typesafe/jev-1.13")
    .build();
```

`FAIL_ON_UNKNOWN_PROPERTIES = false` in `JevHttpTransport` means `id`, `provider`, and `usage.cost` in the OpenRouter response are silently ignored.

### 7.3 Path note

OpenRouter's native path (`/api/alpha/decisions`) differs from the TypeSafe SDK-compatible path (`/api/v1/systemone`). The `JevHttpTransport.send(request, endpoint)` overload already supports targeting an arbitrary path suffix if the native Decisions path is ever preferred. For current Argonaut use, the SDK-compatible path via `baseUrl("https://openrouter.ai/api")` is simpler.

---

## 8. Compatibility Matrix

Status values: IDENTICAL / COMPATIBLE / COMPATIBLE SUPERSET / SEMANTICALLY EQUIVALENT / DIFFERENT / UNKNOWN.

| Capability | TypeSafe /v1/systemone | Kev /v1/systemone | OpenRouter /api/alpha/decisions | OpenRouter via SDK base_url |
|---|---|---|---|---|
| Endpoint path | `/v1/systemone` | `/v1/systemone` | `/api/alpha/decisions` | `/api/v1/systemone` |
| `model` field in request | Required | Required (`kev-latest` / `jev-latest`) | Required (`typesafe/jev-1.13`) | Required (`typesafe/jev-1.13`) |
| `state` type | string / object / array | string / object / array | string / object / array | string / object / array |
| Multiple questions | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Heterogeneous questions | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Choice question | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Score question | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Noul question | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Choice `probabilities` | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Choice `confidence` | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Score `legend` | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Score fractional `score` | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Score `probabilities` | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Score `confidence` | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Noul `noul` probability | IDENTICAL | IDENTICAL | IDENTICAL | IDENTICAL |
| Response `model` field | Yes | Yes | Yes | Yes |
| Response `usage` | `{input_tokens, output_tokens}` | `{input_tokens, output_tokens}` + `latency_ms` | COMPATIBLE SUPERSET (+ `cost`) | COMPATIBLE SUPERSET (+ `cost`) |
| Response `id` | Not present | `x-typesafe-request-id` header only | Present in body | Present in body |
| Response `provider` | Not present | Not present | Present in body | Present in body |
| Authentication | `Bearer <typesafe_key>` | `Bearer <kev_key>` or open | `Bearer <openrouter_key>` | `Bearer <openrouter_key>` |
| Error HTTP 401 | Yes | Yes | Yes | Yes |
| Error HTTP 422 | Yes | Yes | Yes | Yes |
| Error HTTP 429 | Yes | Not documented | Yes | Yes |
| Error HTTP 503/529 | 529 | Not documented | Yes | Yes |
| Native batching (one call, N questions) | Yes | Yes | Yes | Yes |
| Noul `criteria` descriptions | Optional | Optional | Optional | Optional |
| `state` as structured object | Yes | Yes | Yes | Yes |
| Permute endpoint | No | `/v1/systemone/permute` | No | No |
| Separate endpoint | No | `/v1/systemone/separate` | No | No |
| GET /v1/models | No | Yes | Via OpenRouter models API | Via OpenRouter models API |

**Summary:** The core `{model, state, questions}` -> `{model, answers, usage}` contract is IDENTICAL across all three. Differences are either OPTIONAL EXTENSIONS (extra response fields, extra endpoints) or DIFFERENT paths (OpenRouter's `/api/alpha/decisions` vs `/api/v1/systemone`).

---

## 9. Smoke-Test Evidence

**Live tests could not be executed in this investigation.**

No live Kev server was running during this investigation, and no TypeSafe or OpenRouter API keys were available. The compatibility analysis in section 6 is derived from source code inspection (`libs-code/kev/kev/api.py`, `kev/serve.py`, `tests/test_api.py`) and documentation, not from executed requests.

The strongest indirect evidence:

1. `tests/test_api.py` sends `TypeSafeClient(base_url=BASE, api_key="local", model="kev-latest")` to a running Kev server and asserts the same response contract. This is a contract test the Kev maintainer runs against their own server.

2. The Kev README states: "The API matches TypeSafe's System One, so you can point their Python SDK at your local server." This is a first-party compatibility claim by the Kev author.

3. `JevDecisionModel.Builder.baseUrl()` already exists explicitly for "tests and self-hosted deployments" -- the Argonaut implementer anticipated this use case.

**Recommended live test:** Point `JevDecisionModel` at a running Kev server with `baseUrl("http://127.0.0.1:8009")`, `modelName("kev-latest")`, `apiKey("local")`, run one heterogeneous request (Choice + Score + Noul), and assert the response schema.

---

## 10. Current Argonaut Mapping

| Argonaut concept | Layer | Notes |
|---|---|---|
| `DecisionModel` | Domain contract | Protocol-neutral. Correct. |
| `DecisionRequest` | Domain | Carries `DecisionContext.state` (maps to wire `state`) and questions. No wire dependency. |
| `DecisionResult` | Domain | Typed result container. No wire dependency. |
| `DecisionContext` | Domain | `state: String`, `metadata: Map`. `state` -> wire `state`. `metadata` is not sent. |
| `Choice`, `Score`, `Noul` | Domain | Named for System One question types, not Jev-specific. |
| `ChoiceResult`, `ScoreResult`, `NoulResult` | Domain | Domain result types with richer semantics than wire types. |
| `JevDecisionModel` | Provider entry point | Implements `DecisionModel`. Configures transport. Only Jev-specific in name and defaults. |
| `JevHttpTransport` | Transport | Posts to `{baseUrl}/v1/systemone`. System One-specific, not Jev-specific. |
| `JevRequestDto` | Wire (request) | Mirrors `SystemOneRequest`: `{model, state, questions}`. |
| `JevQuestionDto` | Wire (request) | Mirrors System One Question: `{type, instructions, criteria}`. |
| `JevResponseDto` | Wire (response) | Mirrors `SystemOneResponse`: `{answers, usage}`. Missing `model` field. |
| `JevAnswerDto` | Wire (response) | Mirrors System One Answer: discriminated union. |
| `JevUsageDto` | Wire (response) | `{input_tokens, output_tokens}`. |
| `JevRequestMapper` | Mapping | Domain -> wire. Contains no Jev-specific logic. |
| `JevResponseMapper` | Mapping | Wire -> domain. Contains no Jev-specific logic. |
| `CandidateKey<T>` | Mapping utility | Bidirectional string<->candidate map. Generic. |
| `JevException` hierarchy | Error handling | HTTP status -> exception class. Standard HTTP codes. |

---

## 11. Invocation API Analysis

### 11.1 Current API

```java
DecisionResult result = model.decide(request);
```

Where `request` is built with:
```java
DecisionRequest.builder()
    .context(DecisionContext.of(stateText))
    .question(route)         // Choice<Route>
    .question(complexity)    // Score<Complexity>
    .question(review)        // Noul
    .build();
```

### 11.2 Alternatives evaluated

**Option A: Per-type convenience methods**
```java
systemOne.choose(state, route);
systemOne.score(state, complexity);
systemOne.noul(state, review);
```
Drawback: destroys the packed-batch semantics. Three method calls = three HTTP requests (or manual batching hidden behind a session). The System One value proposition is heterogeneous evaluation in one call.

**Option B: Fluent packed API**
```java
decisionModel.decide(context).ask(route).ask(complexity).ask(review).evaluate();
```
This is essentially what the builder API already does with different syntax. No semantic improvement.

**Option C: Current API (recommended)**
`decide(DecisionRequest)` is the natural name for "evaluate this state against these questions." `DecisionRequest.builder()` makes the packed structure explicit without obscuring it.

**Assessment:** The current `DecisionModel.decide(DecisionRequest)` API is the correct shape. One method = one System One call, always, by design. No API change is recommended.

---

## 12. Packed Evaluation Analysis

### 12.1 Is packed evaluation merely convenience?

No. Packed evaluation is **semantically meaningful** for three reasons:

1. **Question isolation by design**: The System One architecture ensures questions "share the input text but can't read each other" (Kev README). Packed is not less isolated than separate; it is identically isolated. Kev's `test_packed_equals_separate` verifies probabilities match within 0.011. TypeSafe's Jev makes the same guarantee.

2. **Performance characteristic**: One HTTP call vs N HTTP calls. On a self-hosted Kev, the state is also prefix-cached across questions in a packed call.

3. **Atomicity**: All answers come from the same model invocation, with the same state snapshot.

### 12.2 Should Argonaut expose both primitive and packed operations?

Argonaut should expose **packed operations only** through `DecisionModel.decide()`. Single-question requests are a degenerate case of the packed form and work today. Exposing separate methods (`choose`, `score`, `noul`) would invite callers to issue three sequential HTTP calls when one would suffice, create an artificial impedance mismatch with the wire protocol, and hide the packed capability.

---

## 13. Provider / Protocol / Model / Transport Boundaries

### 13.1 Architecture A: `DecisionModel -> JevDecisionModel -> JevHttpTransport` (current)

The hierarchy works but conflates the "Jev provider" name with a System One implementation. Adding a second provider would require duplicating or awkwardly naming the existing System One infrastructure.

| Criterion | Assessment |
|---|---|
| Semantic accuracy | Low -- `JevHttpTransport` is a System One transport, not a Jev transport |
| Wire compatibility | High -- works for Jev, Kev, and OpenRouter via baseUrl config |
| Provider independence | Low in naming; high in capability |
| Model independence | Low -- model name is configurable but name implies Jev only |
| Extensibility | Low -- adding Kev would require naming gymnastics or duplication |
| Testability | High -- builder accepts `httpClientBuilder`; all existing tests mock correctly |
| Naming accuracy | Low -- naming problem, not a structural problem |
| Dependency direction | Correct -- provider depends on domain, not vice versa |

### 13.2 Architecture B: `DecisionModel -> SystemOneDecisionModel -> SystemOneTransport -> {TypeSafe, Kev, OpenRouter}` (recommended)

Rename all wire, transport, and provider types to reflect the System One protocol. Configuration (baseUrl, apiKey, modelName) selects the provider.

| Criterion | Assessment |
|---|---|
| Semantic accuracy | High -- names match what they actually are |
| Wire compatibility | High |
| Provider independence | High -- configuration drives provider selection |
| Model independence | High -- model name is configuration |
| Transport independence | High -- transport is the protocol, not the provider |
| Extensibility | High -- adding a provider is a configuration change |
| Testability | High |
| Naming accuracy | High |
| Dependency direction | Correct |

### 13.3 Recommendation

The current codebase already implements Architecture B structurally. It has B's names wrong. No structural refactoring is needed -- only renaming. The rename is deferred until a second provider creates concrete pressure (see section 17).

---

## 14. Naming Audit

### 14.1 Each current name analyzed

| Name | What it describes | Is name accurate? | Recommendation | Migration impact |
|---|---|---|---|---|
| `argonaut-decision-jev` | A System One protocol provider with TypeSafe Jev as default target | No | Rename to `argonaut-decision-systemone` when adding a second provider | Maven module rename; all POM dependency declarations referencing this artifact need updating |
| `JevDecisionModel` | A `DecisionModel` backed by any System One endpoint | Partially | Rename to `SystemOneDecisionModel` when adding a second provider | All caller imports change |
| `JevHttpTransport` | An HTTP transport for the System One wire protocol | No | Rename to `SystemOneHttpTransport` | Internal only; no public API impact |
| `JevRequestDto` | Wire serialization of a System One request body | No | Rename to `SystemOneRequestDto` | Internal only |
| `JevResponseDto` | Wire deserialization of a System One response body | No | Rename to `SystemOneResponseDto` | Internal only |
| `JevQuestionDto` | Wire representation of a System One question | No | Rename to `SystemOneQuestionDto` | Internal only |
| `JevAnswerDto` | Wire representation of a System One answer | No | Rename to `SystemOneAnswerDto` | Internal only |
| `JevUsageDto` | Wire representation of System One usage stats | No | Rename to `SystemOneUsageDto` | Internal only |
| `JevRequestMapper` | Maps domain types to System One wire request | No | Rename to `SystemOneRequestMapper` | Internal only |
| `JevResponseMapper` | Maps System One wire response to domain types | No | Rename to `SystemOneResponseMapper` | Internal only |
| `JevException` | HTTP error for any REST System One endpoint | Partially | Rename to `SystemOneException` | Public API -- exception catches in callers change |
| `JevAuthException` | 401/403 from any System One endpoint | No | Rename to `SystemOneAuthException` | Public API |
| `JevValidationException` | 422 / domain mapping error | No | Rename to `SystemOneValidationException` | Public API |
| `JevRateLimitException` | 429 from any System One endpoint | No | Rename to `SystemOneRateLimitException` | Public API |
| `JevOverloadException` | 503 from any System One endpoint | No | Rename to `SystemOneOverloadException` | Public API |
| `JevTransportException` | Connect/read timeout, IO error | No | Rename to `SystemOneTransportException` | Public API |

**All internal types** (DTOs, mappers, transport) carry Jev-specific names but are not in the public API. Renaming them is a pure refactor with no consumer impact.

**Exception types** are the most impactful: callers catching `JevAuthException` would need to update. Currently no other Argonaut module catches Jev exceptions. Impact is low.

**`JevDecisionModel`** is the public entry point. Currently no other Argonaut module constructs it.

**`argonaut-decision-jev` module** rename would affect all POM files referencing the artifact ID. Currently no other Argonaut module depends on it.

**Guardrail:** This audit does not rename anything. It documents the migration path only.

---

## 15. Extension Strategy

### 15.1 Kev-specific extensions

Kev exposes `/v1/systemone/permute` and `/v1/systemone/separate`. These are diagnostics/research tools. The current `JevHttpTransport.send(request, endpoint)` overload already enables sending to an arbitrary endpoint path.

**Strategy:** Expose Kev-specific capabilities through a capability interface or a Kev-specific extension client, not through `DecisionModel`. The core contract is `decide(DecisionRequest)`; optional diagnostics belong in an optional extension:

```java
// not implemented -- illustrative only
interface SystemOneExtensions {
    PermuteResult permute(PermuteRequest request);
    DecisionResult separate(DecisionRequest request);
}
```

`JevDecisionModel` (or a future `KevDecisionModel`) could implement both `DecisionModel` and `SystemOneExtensions`. Callers that don't need the extension use `DecisionModel` only.

### 15.2 OpenRouter-specific extensions

OpenRouter adds `id`, `provider`, and `usage.cost` to responses. These are provider metadata, not domain-level decision data.

- **Never add them to the domain model** (`DecisionResult` must not have `cost()` or `providerId()`).
- **Current approach is correct:** `FAIL_ON_UNKNOWN_PROPERTIES = false` silently drops them.
- If cost/request-id tracking is needed, add `metadata()` to `JevDecisionModel`/`JevHttpTransport`, capturing the raw response -- not to `DecisionModel`.

### 15.3 The rule

`DecisionModel.decide()` returns `DecisionResult`. `DecisionResult` contains only typed answers -- no provider metadata, no cost, no request ID. Provider-specific metadata lives on the provider implementation class only.

---

## 16. Architecture Alternatives

### Alternative 1: Current (Architecture A with Jev names, no change)

Recognize that the implementation already supports Kev and OpenRouter via `baseUrl` configuration. Document this. Add a second provider only when needed, without renaming.

**Pros:** Zero migration cost. Zero risk. Already structurally correct.
**Cons:** Misleading names accumulate debt. When `KevDecisionModel` is added, the naming asymmetry becomes visible and confusing.

### Alternative 2: Rename transport/DTOs only (internal rename)

Rename `JevHttpTransport` and DTOs to `SystemOneXxx`. Keep `JevDecisionModel` and module name.

**Pros:** Cleans the most misleading names without touching the public API.
**Cons:** Still leaves `JevDecisionModel` as a misleading entry point. Half-measure.

### Alternative 3: Full rename now (Architecture B, complete migration)

Rename module to `argonaut-decision-systemone`, class to `SystemOneDecisionModel`, all DTOs/transport/exceptions to System One names.

**Pros:** Accurate naming from the start.
**Cons:** Non-trivial migration scope. Currently no second provider justifies the effort.

### Alternative 4: SystemOneProvider configuration record (recommended short-term)

Keep the current class names but add a `SystemOneProvider` record that encapsulates (baseUrl, apiKey, modelName). The builder accepts either individual fields or a `SystemOneProvider`. This documents the intent without renaming.

**Pros:** Zero migration cost. Makes the provider-configuration concept explicit. Enables easy switching.
**Cons:** Does not fix the naming debt. But makes the architecture discoverable.

---

## 17. Recommended Architecture

### Short-term (no rename, no refactor)

1. **Add Javadoc to `JevDecisionModel`** clarifying that it implements the System One protocol and that `baseUrl` can target any compatible endpoint (Kev, OpenRouter).

2. **Add `SystemOneProvider` as a configuration convenience record** (optional) -- a simple value record with `baseUrl`, `apiKey`, `modelName` fields and factory methods `TypeSafe.withKey(...)`, `openRouter(...)`, `kev(baseUrl, ...)`. This makes the multi-provider intent explicit without renaming.

3. **Fix the missing `model` field in `JevResponseDto`** -- add `private String model` so the response is fully deserialized (small, non-breaking).

### Medium-term (when adding a second provider)

4. **Rename module and public types**: `argonaut-decision-jev` -> `argonaut-decision-systemone`, `JevDecisionModel` -> `SystemOneDecisionModel`, exception types -> `SystemOneXxxException`.

5. **Keep the wire DTOs as `SystemOneXxx`** (internal; callers don't see them).

6. **Keep `argonaut-decision` unchanged.** It is already correct.

### What NOT to do

- Do not create `KevDecisionModel` as a separate class -- it would duplicate all System One logic. Kev is a provider configuration, not a different protocol.
- Do not create `OpenRouterDecisionModel` as a separate class -- same reason.
- Do not add packed/primitive API split -- single `decide()` is correct.
- Do not expose `id`, `provider`, `cost`, or `latency_ms` through `DecisionModel` or `DecisionResult`.
- Do not add per-type convenience methods (`choose`, `score`, `noul`) at the `DecisionModel` level.

---

## 18. Migration Impact

| Change | Scope | Risk |
|---|---|---|
| `argonaut-decision-jev` -> `argonaut-decision-systemone` module | Maven artifact ID change; all POMs referencing it | Low -- currently a leaf module with no Argonaut dependents |
| `JevDecisionModel` -> `SystemOneDecisionModel` | All callers of the class | Low -- currently no other Argonaut module calls it |
| `JevException` -> `SystemOneException` (and subclasses) | All callers catching these exceptions | Low -- currently only test code |
| Internal type renames (DTOs, transport, mappers) | Zero public impact | None |
| `argonaut-decision` | No change required | -- |
| `DecisionModel` | No change required | -- |
| `DecisionRequest` / `DecisionResult` | No change required | -- |
| `Choice`, `Score`, `Noul` (domain types) | No change required | -- |
| Wire protocol behavior | No change -- same HTTP calls | -- |

The existing test suite (23 tests in `argonaut-decision-jev`) covers all behavior. After renaming, re-running `mvn verify` confirms correctness with zero behavioral changes.

---

## 19. Unresolved Questions

### UQ-001: OpenRouter path ambiguity

The TypeSafe SDK route produces `https://openrouter.ai/api/v1/systemone`. OpenRouter's native Decisions path is `/api/alpha/decisions`. Are these served by identical infrastructure or different backend routing? **Live test required.**

### UQ-002: Score `legend` end-to-end

TypeSafe API docs show Score answers return `legend: map<string, string>`. Argonaut does not deserialize `legend` from the response -- it reconstructs from `score.scale()`. For Kev, the legend is returned as `{"0": text, "1": text}`. Verify that Argonaut's legend reconstruction from `scale()` matches the wire `legend` end-to-end.

### UQ-003: TypeSafe Noul confidence field

TypeSafe's API docs show Noul answer as `{type: "noul", noul: float}` with no `confidence` field. `JevAnswerDto` has a `confidence` field on all answer types. For Noul, `dto.getConfidence()` returns null from real providers, which `mapNoulAnswer()` ignores. Confirmed compatible by analysis; not verified live.

### UQ-004: Score criteria count constraint

TypeSafe's documentation says Score criteria accepts 2-10 levels. Kev accepts 1-255. Argonaut's `Score` domain type requires at least 2 entries. If someone passes 11+ levels targeting TypeSafe, TypeSafe will reject with 422. If targeting Kev, it will succeed. This constraint is not validated client-side by Argonaut, which is correct (it is a provider concern).

### UQ-005: OpenRouter `usage.cost` format

OpenRouter adds `usage.cost` to the response. `JevUsageDto` has `input_tokens` and `output_tokens` only. `FAIL_ON_UNKNOWN_PROPERTIES = false` silently drops `cost`. This is the correct behavior -- cost is provider metadata, not domain data. Verify that the Jackson silent-drop works for nested objects as well as simple fields.

---

## 20. Recommended Next Slice

### Slice 1: Kev compatibility smoke test (validate current implementation)

- Configure `JevDecisionModel` with `baseUrl("http://127.0.0.1:8009")`, `modelName("kev-latest")`, `apiKey("local")`.
- Run one heterogeneous request (Choice + Score + Noul) against a running Kev-0.8B server.
- Assert response schema: `answers` present, types correct, probabilities valid, `usage` present.
- This proves the System One claim without code changes.

### Slice 2: SystemOneProvider configuration record (no rename, no refactor)

- Add `SystemOneProvider` as a public record in `argonaut-decision-jev` with named factory methods: `typeSafe(String apiKey)`, `kev(String baseUrl, String apiKey)`, `openRouter(String apiKey)`.
- Add a `JevDecisionModel.Builder.provider(SystemOneProvider)` setter as a convenience constructor.
- Adds no renaming, no breaking changes, makes multi-provider intent discoverable.

### Slice 3: Full rename (when adding a real second provider)

- Triggered by concrete business need, not architecture preference.
- Precondition: at least one non-TypeSafe provider in active use or imminent use.

---

## 21. Findings F001-F011

### F001 -- System One Identity

**Evidence:** TypeSafe documentation defines System One as "a class of AI models built to make fast, structured decisions." Jev is the "first System One model." Kev's README: "The API matches TypeSafe's System One, so you can point their Python SDK at your local server." Kev's `api.py` defines `SystemOneRequest` with the same fields as TypeSafe's wire contract. `tests/test_api.py` uses the TypeSafe SDK against a Kev server.

**Conclusion:** System One is both a model architecture design philosophy (fast, calibrated, probabilistic, prefill-only) AND a wire protocol (`POST /v1/systemone`, `{model, state, questions}` -> `{model, answers, usage}`). The wire protocol has crystallized into a de facto standard, evidenced by independent implementation (Kev) and broad SDK support (TypeSafe SDK, OpenRouter).

**Confidence:** CONFIRMED

**Architectural consequence:** Argonaut's transport, DTOs, and mapper are System One protocol implementations. They should be named accordingly, not for Jev specifically.

---

### F002 -- Jev Identity

**Evidence:** TypeSafe docs: "Jev is TypeSafe's flagship model and the first System One model." `JevDecisionModel.DEFAULT_BASE_URL = "https://api.typesafe.ai"`, `DEFAULT_MODEL = "jev-latest"`. OpenRouter offers `typesafe/jev-1.13` at `/api/alpha/decisions` with the same wire format.

**Conclusion:** Jev is a trained model (a product), not the protocol. Jev is the default implementation of System One. Another model (Kev) implements the same protocol. Jev may be accessed via TypeSafe directly or via OpenRouter as a proxy.

**Confidence:** CONFIRMED

**Architectural consequence:** `JevDecisionModel` should ultimately be renamed `SystemOneDecisionModel`. The two default values (`typesafe.ai` URL, `jev-latest` model name) are the only genuinely Jev-specific elements.

---

### F003 -- Kev Compatibility

**Evidence:** `libs-code/kev/kev/api.py` defines `SystemOneRequest` with identical fields. `kev/serve.py` exposes `POST /v1/systemone`. Response fields match Argonaut's DTOs field-for-field. `FAIL_ON_UNKNOWN_PROPERTIES = false` handles Kev's `latency_ms` extension. `JevDecisionModel.Builder.baseUrl()` allows targeting Kev.

**Conclusion:** The current Argonaut `JevDecisionModel` can talk to Kev unchanged except for builder configuration (baseUrl, modelName, apiKey). No DTOs, no mappings, no domain types need to change.

**Confidence:** STRONG EVIDENCE (source analysis; live test not executed)

**Architectural consequence:** Kev proves `JevDecisionModel` is a System One provider, not a Jev-specific one. A `KevDecisionModel` is not needed -- it would be `JevDecisionModel` with different defaults.

---

### F004 -- OpenRouter Compatibility

**Evidence:** OpenRouter docs: "TypeSafe SDK can point to OpenRouter by setting base_url to `https://openrouter.ai/api`." Response adds `id`, `provider`, `usage.cost` -- all ignored by `FAIL_ON_UNKNOWN_PROPERTIES = false`. Wire request is identical.

**Conclusion:** OpenRouter is wire-compatible with the System One contract on the request side. On the response side it adds optional metadata fields that Argonaut silently drops. The current `JevDecisionModel` can target OpenRouter by setting `baseUrl("https://openrouter.ai/api")` and `modelName("typesafe/jev-1.13")`.

**Confidence:** STRONG EVIDENCE (confirmed by multiple sources; live test not executed)

**Architectural consequence:** OpenRouter does not require a separate provider class. It is a transport configuration variant of `JevDecisionModel`. Extra response fields must never enter `DecisionResult`.

---

### F005 -- Shared Wire Contract

**Evidence:** Cross-analysis of TypeSafe API docs, Kev `api.py`, Kev `serve.py`, Kev test suite, and Argonaut DTOs confirms identical core contract.

**Conclusion:** The shared wire contract is: request `{model, state, questions}`, response `{model, answers, usage}`, with three question types (noul, choice, score) and three answer shapes. All three implementations (TypeSafe, Kev, OpenRouter) implement this contract.

**Confidence:** CONFIRMED

**Architectural consequence:** Argonaut's DTOs (`JevRequestDto` etc.) are implementations of this shared contract. They can be renamed `SystemOneXxxDto` without any change to their fields.

---

### F006 -- Natural Invocation API

**Evidence:** API analysis in section 11, comparison of packed vs per-type semantics, review of System One's core value proposition.

**Conclusion:** The current `DecisionModel.decide(DecisionRequest)` is the correct and most natural API. It represents one semantic operation: "evaluate this state against these questions." Per-type methods would misrepresent the protocol as a series of independent calls and destroy the packed-batch semantic.

**Confidence:** CONFIRMED

**Architectural consequence:** No change to `DecisionModel` or `DecisionRequest`. The current API is already optimal.

---

### F007 -- Packed Heterogeneous Evaluation

**Evidence:** System One design explicitly supports heterogeneous questions in one request. Kev `test_packed_equals_separate` verifies packed and separate produce statistically identical results. TypeSafe documentation confirms questions share state but do not interact. Argonaut's `DecisionRequest` already supports heterogeneous questions.

**Conclusion:** Packed heterogeneous evaluation is a **fundamental capability**, not a convenience feature. It is semantically meaningful (shared state, atomic evaluation snapshot, question isolation) and architecturally guaranteed.

**Confidence:** CONFIRMED

**Architectural consequence:** Single-question calls are a degenerate case. No separate "single question" API is needed. The `DecisionRequest` builder works for both.

---

### F008 -- Provider / Protocol / Transport Boundaries

**Evidence:** Code analysis of `JevDecisionModel`, `JevHttpTransport`, DTOs, and mappings. Comparison with Kev source. Web evidence on OpenRouter compatibility.

**Conclusion:** The correct layering is: domain contract (`DecisionModel`) -> protocol/provider implementation (future `SystemOneDecisionModel`) -> transport (future `SystemOneHttpTransport`) -> wire types (future `SystemOneXxxDto`). The current codebase already implements this structure; it has the names wrong. No structural refactoring is needed.

**Confidence:** CONFIRMED

**Architectural consequence:** Architecture B is correct in concept. The rename from Architecture A to B is the only required change.

---

### F009 -- Naming Debt

**Evidence:** Full naming audit in section 14.

**Conclusion:** All `Jev*` names (except the two default values `"https://api.typesafe.ai"` and `"jev-latest"`) are technically incorrect: the implementation is a System One protocol implementation, not a Jev-specific one. The naming debt is real but currently low-cost because no second provider exists. The trigger for the full rename is adding a second provider.

**Confidence:** CONFIRMED

**Architectural consequence:** Document the intent in Javadoc now. Rename when adding a second provider. Migration impact is low -- leaf module, no current consumers outside the module itself.

---

### F010 -- Extension Model

**Evidence:** Kev exposes `/v1/systemone/permute` and `/v1/systemone/separate`. OpenRouter adds `id`, `provider`, `usage.cost`. TypeSafe has no extensions beyond the core contract.

**Conclusion:** Provider-specific extensions should be surfaced through capability interfaces or provider-specific extension APIs, never through `DecisionModel` or `DecisionResult`. The current `FAIL_ON_UNKNOWN_PROPERTIES = false` is the correct treatment for response-side extensions. Request-side extensions (Kev's separate/permute) should be accessible through a typed extension interface on the provider implementation only.

**Confidence:** CONFIRMED

**Architectural consequence:** `DecisionModel` remains clean. Extensions live on the provider class. No changes needed now.

---

### F011 -- Recommended Architecture

**Evidence:** Synthesis of F001-F010, naming audit, compatibility matrix, provider boundary analysis.

**Conclusion:**

> **We are implementing System One with Jev as one model/provider.**

The smallest coherent Argonaut API that can exploit the common capabilities of TypeSafe Jev, OpenRouter Jev, and local Kev without becoming a God Request is:

```
DecisionModel.decide(DecisionRequest) -> DecisionResult
```

with provider selected by configuration:
```java
JevDecisionModel.builder()
    .apiKey(...)
    .baseUrl(...)        // https://api.typesafe.ai | http://kev-host:port | https://openrouter.ai/api
    .modelName(...)      // jev-latest | kev-latest | typesafe/jev-1.13
    .build()
```

The domain types (`Choice`, `Score`, `Noul`, `ChoiceResult`, `ScoreResult`, `NoulResult`) are already named in protocol-neutral terms. The wire types are all correctly structured System One DTOs under temporary Jev names. No structural changes are needed to support Kev or OpenRouter.

**Immediate actions (no renaming):**
1. Confirm Kev compatibility with a live smoke test.
2. Add Javadoc to `JevDecisionModel` explaining it is a System One provider.
3. Add `SystemOneProvider` configuration record (optional convenience).

**Deferred actions (at second-provider pressure):**
4. Rename `argonaut-decision-jev` -> `argonaut-decision-systemone`.
5. Rename `JevDecisionModel` -> `SystemOneDecisionModel`.
6. Rename all internal types accordingly.

**Never:**
- Add provider-specific fields to `DecisionModel` or `DecisionResult`.
- Create `KevDecisionModel` or `OpenRouterDecisionModel` as separate classes.
- Add per-type primitive methods to the domain API.
- Expose `cost`, `latency_ms`, or `provider` through the domain contract.

**Confidence:** CONFIRMED

---

*End of report.*
