# ARGONAUT-009 — LangChain4j Controlled Local Evidence Implementation

**Date:** 2026-08-21
**Status:** Complete
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), Spring AI reference implementation

---

## Summary

Implemented the second Argonaut framework service using LangChain4j 0.36.2 + Spring Boot 4.1.0. Both Spring AI and LangChain4j now satisfy TC-UC-001 using the same corpus, the same experiment question, and the same validator. The system prompt was extracted from the Spring AI module into `argonaut-core` and both implementations now consume the same bytes.

**Validation:** `mvn verify` — BUILD SUCCESS, 124 tests (111 core, 4 Spring AI, 9 LangChain4j), 0 failures.

---

## Part 1 — Common Mission Prompt

### What changed

`ControlledLocalEvidencePrompt` (in `dev.jsanca.argonaut.core.experiment`) was created to hold the canonical system prompt text as a `public static final String`.

The Spring AI agent's private `SYSTEM_PROMPT` constant was removed. Both agents now reference `ControlledLocalEvidencePrompt.SYSTEM_PROMPT`.

### How each framework consumes it

| Framework | Mechanism |
| --- | --- |
| Spring AI | `chatClient.prompt().system(ControlledLocalEvidencePrompt.SYSTEM_PROMPT)` |
| LangChain4j | `@SystemMessage(ControlledLocalEvidencePrompt.SYSTEM_PROMPT)` on the `KnowledgeAssistant` interface method |

The LangChain4j `@SystemMessage` annotation accepts a compile-time constant string, and Java text blocks with `static final` ARE compile-time constants (JLS §15.29). The value is inlined at compile time, guaranteeing byte-identical prompts between frameworks.

### Tests added

`ControlledLocalEvidencePromptTest` (4 tests in `argonaut-core`): prompt is non-blank; references both tool names; instructs at least two searches; constrains evidence to corpus only.

---

## Part 2 — LangChain4j Module

### Module / classes added

| File | Purpose |
| --- | --- |
| `argonaut-langchain4j/pom.xml` | Spring Boot 4.1.0 + LangChain4j 0.36.2 + `<parameters>true</parameters>` |
| `ArgonautLangChain4jApplication` | Spring Boot entry point, port 8082 |
| `config/LangChain4jConfig` | Provides `KnowledgeRepository` and `ChatLanguageModel` beans |
| `knowledge/KnowledgeTools` | `@Tool`-annotated POJO wrapping `KnowledgeRepository`, per-run instantiation |
| `agent/ControlledLocalEvidenceAgent` | Orchestrates `AiServices`, emits trace events, builds `ExperimentResult` |
| `api/HealthController` | `GET /api/health` |
| `api/AboutController` | `GET /api/about` |
| `api/ExperimentController` | `POST /api/experiment/run` |
| `MockChatLanguageModel` (test) | 3-stage state machine: search → read → answer |
| `TcUc001LangChain4jTest` (test) | 9 tests including TC-UC-001 contract verification |

### LangChain4j mechanism selected

`AiServices` with a `KnowledgeAssistant` interface:

```java
interface KnowledgeAssistant {
    @SystemMessage(ControlledLocalEvidencePrompt.SYSTEM_PROMPT)
    String answer(String question);
}

var assistant = AiServices.builder(KnowledgeAssistant.class)
    .chatLanguageModel(chatModel)
    .tools(tools)
    .build();
```

`AiServices` handles the internal tool-calling loop: model → search tool(s) → read tool(s) → synthesis. This is the idiomatic high-level LangChain4j API for agentic tool use.

### Knowledge capability implementation

`KnowledgeTools` is a plain Java object (not a Spring bean) with `@Tool`-annotated methods. It is constructed per run with the `ExecutionObserver` and reads accumulator as constructor arguments. The tool methods mirror Spring AI's capability names (`searchKnowledge`, `readDocument`) and delegate to the same `KnowledgeRepository`.

Both tools populate the `TraceEventMetadata` keys required for SC4 (anti-gaming assertion):
- `searchKnowledge` → `KNOWLEDGE_SEARCH_STARTED`, `KNOWLEDGE_SEARCH_COMPLETED` (with `sourceIds`, `query`)
- `readDocument` → `DOCUMENT_READ_STARTED`, `DOCUMENT_READ_COMPLETED`, `EVIDENCE_RETRIEVED` (with `sourceId`)

### Observability implementation

All nine required events are emitted:
- `KnowledgeTools`: `KNOWLEDGE_SEARCH_STARTED/COMPLETED`, `DOCUMENT_READ_STARTED/COMPLETED`, `EVIDENCE_RETRIEVED`
- `ControlledLocalEvidenceAgent`: `RUN_STARTED`, `EVIDENCE_SELECTED`, `ANSWER_SYNTHESIZED`, `RUN_COMPLETED`

`InMemoryExecutionObserver.toTrace()` is called at the end of each run to produce the `ExecutionTrace` embedded in `ExperimentResult`.

### TC-UC-001 validation

The LangChain4j implementation is verified with `ControlledLocalEvidenceContract.verify(executor)` — the same shared contract used by Spring AI. No framework-specific weaker validator was added.

Mock test: `MockChatLanguageModel` implements `ChatLanguageModel.generate(messages, toolSpecifications)`. It returns tool-call `AiMessage`s (with `ToolExecutionRequest` objects) for calls 1–2 and a final text answer for call 3. `AiServices` drives the loop automatically.

---

## Metric Semantics

### `modelCalls` — known ambiguity

Spring AI reports `modelCalls = 1` (hard-coded). `AiServices` in LangChain4j hides the internal loop count. For a 3-stage run (search, read, answer) the actual model call count is 3, but this is not directly observable without wrapping the model.

**Decision:** LangChain4j reports `modelCalls = 0` (not reliably measurable) rather than inventing a comparable number. This is an honest difference to carry into the framework comparison.

**Recommendation:** A future task could add a `CountingChatLanguageModel` decorator to count real model calls, making the metric comparable. Alternatively, `modelCalls` could be redefined as the number of `AiServices.answer()` invocations (1 per run).

### Other metrics (accurate for both frameworks)

| Metric | Source |
| --- | --- |
| `knowledgeSearches` | Count of `KNOWLEDGE_SEARCH_COMPLETED` events in trace |
| `documentReads` | Count of `DOCUMENT_READ_COMPLETED` events in trace |
| `evidenceCount` | `result.evidence().size()` — enforced by `ExperimentResult.completed()` |
| `errors` | `result.errors().size()` — 0 for successful baseline run |
| `durationMs` | Wall clock from run start to `ExperimentResult` construction |

---

## External Model Validation

Not performed. `mvn verify` runs only mock-backed tests; no real model or network call is made. Real-model validation would require `OPENROUTER_API_KEY` and `OPENROUTER_MODEL` environment variables and is out of scope for the normal Maven build.

---

## Spring AI vs LangChain4j Structural Comparison

### Duplicated behavior

| Behavior | Spring AI | LangChain4j |
| --- | --- | --- |
| Tool names | `searchKnowledge`, `readDocument` | same |
| Tool descriptions | copy of same text | copy of same text |
| TraceEventMetadata keys emitted | same keys | same keys |
| Evidence construction | `"ev-" + sourceId`, SUPPORTING, score 1.0 | same |
| Trace event structure | same 9 required events | same 9 required events |
| `buildEvidence(reads)` logic | 200-char excerpt, same pattern | identical |
| `buildMetrics(trace, …)` logic | count KNOWLEDGE_SEARCH_COMPLETED, DOCUMENT_READ_COMPLETED | identical |

### Framework-specific behavior

| Aspect | Spring AI | LangChain4j |
| --- | --- | --- |
| Tool mechanism | `@Tool` + `ToolContext` (side-channel state injection) | `@Tool` + `@P` + constructor state |
| Per-run tool state | Passed at call time via `toolContext(Map.of(...))` | Injected at `KnowledgeTools` construction |
| Orchestration API | `ChatClient.prompt().tools(…).call()` | `AiServices.builder(…).tools(…).build()` |
| System prompt supply | `chatClient.prompt().system(...)` | `@SystemMessage(...)` on interface method |
| Tool annotation package | `org.springframework.ai.tool.annotation.Tool` | `dev.langchain4j.agent.tool.Tool` |
| Param annotation | `@ToolParam` (Spring AI) | `@P` (LangChain4j) |
| `KnowledgeTool` bean type | Spring `@Component` singleton | plain Java POJO, per-run |
| Model calls observable | 1 (hard-coded) | 0 (not reliably measurable via AiServices) |

### Potentially framework-neutral behavior

The following are candidates for `argonaut-core` promotion after additional frameworks confirm the pattern:

1. **`buildEvidence(reads)`** — identical logic in both agents: `"ev-" + sourceId`, SUPPORTING, 200-char excerpt, score 1.0, provenance string. If LangGraph4j and Embabel replicate this, a shared static helper belongs in core.
2. **`buildMetrics(trace, evidence, durationMs)`** — identical event-counting pattern. Would be a pure function on `ExecutionTrace`.
3. **Tool output format** — both tools format search results as `source_id=... | title=... | score=...\n...` and read results as `source_id=...\n\ncontent`. Shared formatting in `KnowledgeRepository` output adapters could make evidence assembly more uniform.
4. **`agentEvent(type, metadata)`** — the per-framework `ExecutionEvent` factory creates events with framework ID as source. The pattern is the same; only the framework ID string differs.

### Abstractions deliberately NOT introduced

- No `ArgonautKnowledgeTool` interface or base class — the tool *adapter* pattern is framework-specific.
- No shared `AgentLoop`, `Tool`, or `ToolContext` in core.
- No `AbstractControlledEvidenceAgent` sharing the run loop.
- No unified evidence builder utility — duplication documented here, not refactored.

---

## Research Question: Tool Duplication Analysis

> Is knowledge-tool duplication across frameworks merely adapter duplication, or does it reveal a useful framework-neutral capability abstraction?

**Observation after two frameworks:**

The duplication is in two distinct layers:

1. **Adapter layer** (framework-specific, expected): `@Tool` annotation package, param annotation, state injection mechanism, tool bean lifecycle. This is genuine adapter duplication — it exists because each framework has its own tool protocol. It should NOT be abstracted away.

2. **Logic layer** (potentially common): Tool descriptions, output formatting, evidence construction, metrics counting, event factory. This logic is identical in both frameworks and does NOT depend on framework-specific types. If LangGraph4j and Embabel also replicate it, these are justified candidates for core utilities.

**Conclusion at this stage:** Duplication is present in both layers. The adapter layer duplication is correct and desirable (the experiment intends to compare framework-native mechanisms). The logic layer duplication should be tracked but not refactored until at least one more framework confirms the pattern.

**Research question remains open.** A definitive answer requires LangGraph4j and Embabel.

---

## Limitations

1. `modelCalls` metric is 0 (not reliably measurable through `AiServices`) — documented above.
2. LangChain4j BOM version `0.36.2` — verify against Maven Central for the current stable release; the implementation code is API-stable across recent 0.3x versions.
3. Real-model validation not performed.
4. No `GET /api/about` `modelName` at startup — the model name is not injected into `AboutController` (same limitation as Spring AI).

---

## Recommended Next Task

Implement LangGraph4j (`argonaut-langgraph4j`, port 8083) as the third framework. LangGraph4j's graph/node model is structurally the most different from Spring AI and LangChain4j, making it the highest-value next comparison point. After LangGraph4j, the logic-layer duplication comparison will have three data points and a decision on whether to promote shared helpers to core will be well-supported by evidence.
