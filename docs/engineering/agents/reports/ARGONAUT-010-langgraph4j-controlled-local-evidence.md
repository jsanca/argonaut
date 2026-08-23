# ARGONAUT-010 — LangGraph4j Controlled Local Evidence Implementation

**Date:** 2026-08-22
**Status:** Complete
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), ARGONAUT-009 LangChain4j reference implementation

---

## Summary

Implemented the third Argonaut framework service using LangGraph4j 1.8.24 + LangChain4j 1.18.1 (transitively) + Spring Boot 4.1.0. All three framework services — Spring AI, LangChain4j, LangGraph4j — now satisfy TC-UC-001 using the same corpus, the same experiment question, and the same validator.

**Validation:** `mvn verify` — BUILD SUCCESS, 136 tests (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j), 0 failures.

---

## Dependencies

| Artifact | Version | Role |
| --- | --- | --- |
| `org.bsc.langgraph4j:langgraph4j-core` | 1.8.24 | Graph engine (StateGraph, CompiledGraph, MessagesState) |
| `org.bsc.langgraph4j:langgraph4j-langchain4j` | 1.8.24 | LangChain4j integration (LC4jToolService, LC4jStateSerializer) |
| `dev.langchain4j:langchain4j-core` | 1.18.1 (forced) | Chat types, message types, InvocationContext |
| `dev.langchain4j:langchain4j-open-ai` | 1.0.0 | OpenAiChatModel (production config) |
| `org.springframework.boot:spring-boot-starter-web` | 4.1.0 | REST layer |

**Version conflict note:** `langchain4j-open-ai:1.0.0` pulls `langchain4j-core:1.0.0` transitively. Maven's nearest-wins rule would have downgraded the core to 1.0.0, losing `InvocationContext` (added in 1.18.x). Fixed by declaring `langchain4j-core:1.18.1` as an explicit direct dependency to override the transitive resolution.

---

## Module Structure

| File | Purpose |
| --- | --- |
| `argonaut-langgraph4j/pom.xml` | LangGraph4j + LangChain4j 1.18.1 + Spring Boot 4.1.0 |
| `ArgonautLangGraph4jApplication` | Spring Boot entry point, port 8083 |
| `config/LangGraph4jConfig` | `KnowledgeRepository` + `ChatModel` beans |
| `knowledge/KnowledgeTools` | `@Tool`-annotated POJO, per-run, constructor-injected state |
| `agent/ControlledLocalEvidenceAgent` | Graph-based orchestration, explicit model calls, trace emission |
| `api/HealthController` | `GET /api/health` |
| `api/AboutController` | `GET /api/about` |
| `api/ExperimentController` | `POST /api/experiment/run` |
| `MockChatModel` (test) | 4-stage state machine: 2 searches → reads → answer |
| `TcUc001LangGraph4jTest` (test) | 12 tests including TC-UC-001 contract verification |

---

## Graph Topology

LangGraph4j is used idiomatically via its **ReAct agent pattern**:

```
START → agent → (hasToolCalls?) → tools → agent → ... → END
```

The agent node calls the model explicitly. The conditional edge checks whether the last message has tool execution requests. The tools node executes all pending tool calls, appending results to the message state. The loop repeats until the model produces a plain text answer.

**Why this is idiomatic:** LangGraph4j's core design is an explicit state machine where each node is a pure function from state to state update. The ReAct pattern (Reason + Act loop) maps directly to two nodes and a conditional edge. This differs fundamentally from Spring AI's `ChatClient` and LangChain4j's `AiServices`, both of which hide the agent loop internally.

---

## State Model

`MessagesState<ChatMessage>` is the pre-built LangGraph4j state with an appender channel for messages. Each node returns `Map.of("messages", msg)` or `Map.of("messages", List.of(msgs...))` — the appender merges new messages into the growing list.

**Serializer requirement:** `MessagesState` uses `ObjectStreamStateSerializer` by default, which requires Java `Serializable`. LangChain4j message types (`UserMessage`, `AiMessage`, etc.) are NOT serializable. The fix is `LC4jStateSerializer` (from `langgraph4j-langchain4j`), which registers custom serializers for all LangChain4j message types. Used as:

```java
new MessagesStateGraph<ChatMessage>(new LC4jStateSerializer<>(MessagesState::new))
```

This is a necessary adapter detail — LangGraph4j requires state serialization even without explicit checkpointing (used internally for state cloning between node transitions).

---

## Model Invocation Mechanism

The agent node calls the model directly:

```java
var chatRequest = ChatRequest.builder()
    .messages(systemMessage + history)
    .parameters(ChatRequestParameters.builder()
        .toolSpecifications(toolSpecs)
        .build())
    .build();
var response = chatModel.chat(chatRequest);
```

This is the key difference from both previous implementations:
- **Spring AI**: `chatClient.prompt().tools().call().content()` — model calls are opaque
- **LangChain4j 0.x**: `aiServices.answer(question)` — model calls are opaque  
- **LangGraph4j**: explicit `chatModel.chat(request)` — model calls are counted with `AtomicInteger`

`MODEL_CALL_STARTED` and `MODEL_CALL_COMPLETED` events are emitted around each call, and the actual model call count is reported accurately in `ExecutionMetrics.modelCalls()`.

---

## Knowledge Access Mechanism

`LC4jToolService` (from `langgraph4j-langchain4j`) wraps `@Tool`-annotated objects using `DefaultToolExecutor` (LangChain4j 1.x reflection-based tool invocation).

```java
var toolService = LC4jToolService.builder()
    .toolsFromObject(knowledgeTools)
    .build();
```

Tool execution in the tools node:
```java
var command = toolService.execute(
    lastMsg.toolExecutionRequests(),
    InvocationContext.builder().build(),
    "messages"  // state property to update with tool results
).get();
return command.update();
```

`LC4jToolService.execute()` returns a `CompletableFuture<Command>`. The `Command.update()` map contains `"messages" → List<ToolExecutionResultMessage>`, which the appender channel merges into state.

**LangGraph4j solution for per-run state:** Per-run state (observer, reads accumulator, model-call counter) is captured via Java closures in the node lambdas. The `KnowledgeTools` instance is created per run and captured in the lambda. There is no side-channel context (Spring AI's `ToolContext`) and no per-invocation tool reconstruction (LangChain4j's per-`AiServices` tool build).

---

## Per-Run State Handling

All per-run mutable state is captured in the graph-building closure:

```java
final var observer = new InMemoryExecutionObserver();
final List<DocumentContent> reads = new CopyOnWriteArrayList<>();
final AtomicInteger modelCallCounter = new AtomicInteger(0);
final var knowledgeTools = new KnowledgeTools(repository, observer, reads);
final var toolService = LC4jToolService.builder().toolsFromObject(knowledgeTools).build();
// ... build graph with closures capturing these variables
```

The graph is compiled per run. This is consistent with LangChain4j (`AiServices` rebuilt per run) but avoids the LangChain4j model rebuild cost since we only rebuild the `StateGraph` wrapping (the `ChatModel` itself is shared).

---

## Observability

All required trace events are emitted:

| Event | Where |
| --- | --- |
| `RUN_STARTED` | agent (`run()` entry) |
| `MODEL_CALL_STARTED` | agent node (before `chatModel.chat()`) |
| `MODEL_CALL_COMPLETED` | agent node (after `chatModel.chat()`) |
| `KNOWLEDGE_SEARCH_STARTED` | KnowledgeTools.searchKnowledge() |
| `KNOWLEDGE_SEARCH_COMPLETED` | KnowledgeTools.searchKnowledge() |
| `DOCUMENT_READ_STARTED` | KnowledgeTools.readDocument() |
| `DOCUMENT_READ_COMPLETED` | KnowledgeTools.readDocument() |
| `EVIDENCE_RETRIEVED` | KnowledgeTools.readDocument() |
| `EVIDENCE_SELECTED` | agent (`run()` post-graph) |
| `ANSWER_SYNTHESIZED` | agent (`run()` post-graph) |
| `RUN_COMPLETED` | agent (`run()` post-graph) |
| `RUN_FAILED` | agent (exception catch) |

`MODEL_CALL_STARTED/COMPLETED` are newly observable in this framework. Spring AI and LangChain4j 0.x hide their internal model call loops; LangGraph4j's explicit node design naturally exposes this.

---

## Native Execution Metadata Discovered

LangGraph4j via LangChain4j 1.x exposes the following that the other implementations do not:

1. **`ChatResponse.metadata()`** — includes `ChatResponseMetadata` with token usage (`inputTokenCount`, `outputTokenCount`) and model name. Not mapped to `ExecutionMetrics` in this task (no field for tokens); documented for future normalization.
2. **`ChatResponse.finishReason()`** — `STOP` vs `TOOL_EXECUTION` vs other reasons. Useful for distinguishing model exit conditions; not captured in the current trace schema.
3. **State snapshots** — `CompiledGraph` supports `stateOf(RunnableConfig)` and `getStateHistory()` for debugging. Not used here; mentioned for completeness.
4. **Node-level hooks** — `addBeforeCallNodeHook`, `addAfterCallNodeHook`, `addWrapCallNodeHook` allow injecting behavior around any node execution. Could be used for trace emission without modifying node closures; deferred for future exploration.

---

## Evidence Semantics

Evidence is built from documents actually read through `KnowledgeRepository.read()`, accumulated in `reads` list. The flow is:

```
readDocument(sourceId)
  → repository.read(DocumentReference)
  → DocumentContent added to reads list
  → after graph completes: buildEvidence(reads)
  → Evidence(id="ev-sourceId", kind=SUPPORTING, score=1.0)
```

This is identical to the Spring AI and LangChain4j implementations — a confirmed common pattern.

---

## Metric Semantics

| Metric | Source | Accuracy |
| --- | --- | --- |
| `durationMs` | wall clock | accurate |
| `modelCalls` | `AtomicInteger` incremented per `chatModel.chat()` call | **accurate** (first time in Argonaut) |
| `knowledgeSearches` | count of `KNOWLEDGE_SEARCH_COMPLETED` events | accurate |
| `documentReads` | count of `DOCUMENT_READ_COMPLETED` events | accurate |
| `evidenceCount` | `evidence.size()` | accurate |
| `errors` | `List.of()` on success, populated on failure | accurate |

**`modelCalls` comparison across frameworks:**
- Spring AI: `1` (hardcoded)
- LangChain4j 0.x: `0` (AiServices hides loop count)
- LangGraph4j: accurate count (e.g., 4 for the mock's 2-search + 2-read + answer run)

---

## TC-UC-001 Validation

The same shared `ControlledLocalEvidenceContract` is used. No framework-specific weaker validator added.

**Mock:** `MockChatModel` implements `ChatModel.doChat(ChatRequest)` (the abstract method in LangChain4j 1.x, with `chat()` as the listener-wrapping default). 4-stage state machine:
1. Call 1 → `searchKnowledge("controlled local evidence")`
2. Call 2 → `searchKnowledge("agentic framework comparison")` + `readDocument("exp-001")`
3. Call 3 → `readDocument("rag-001")` + `readDocument("obs-001")`
4. Call 4 → final text answer

This satisfies all 9 SC assertions:
- SC1 (COMPLETED status), SC2 (non-blank answer), SC3 (required trace events), SC4 (evidence traceable to reads), SC5 (STARTED/COMPLETED pairing), SC6 (metrics non-negative), SC7 (no errors), SC8 (exp-001 in evidence), SC9 (evidence traceability).

**Test results:** 12 tests, 0 failures.

---

## Three-Framework Comparison

| Concern | Spring AI | LangChain4j 0.x | LangGraph4j 1.x |
| --- | --- | --- | --- |
| Entry abstraction | `ChatClient` | `AiServices` | `StateGraph` / `CompiledGraph` |
| Per-run state | `ToolContext` (side-channel map) | constructor injection into per-run tool POJO | Java closure in node lambda |
| Knowledge exposure | `@Tool` + `ToolContext` | `@Tool` + `@P` + constructor state | `@Tool` + `@P` via `LC4jToolService` |
| Agent loop | opaque (inside `ChatClient`) | opaque (inside `AiServices`) | explicit graph (nodes + conditional edge) |
| Model calling | `chatClient.prompt().call()` | `aiServices.answer()` | `chatModel.chat(ChatRequest)` |
| Model calls observable | ✗ (hardcoded 1) | ✗ (0/unknown) | ✓ (counted accurately) |
| Tool execution | framework-driven internally | framework-driven internally | explicit: `LC4jToolService.execute()` |
| System prompt supply | `.system(SYSTEM_PROMPT)` | `@SystemMessage(SYSTEM_PROMPT)` | prepended in agent node lambda |
| State visibility | hidden in chat history | hidden in AiServices session | explicit `MessagesState<ChatMessage>` |
| Native serializer | n/a (Spring manages) | n/a (AiServices manages) | `LC4jStateSerializer` required |
| Intermediate state access | not available | not available | available via `stateOf(config)` |
| Token usage accessible | via `ChatResponse` (not exposed) | via `AiMessage` (not exposed) | via `ChatResponse.metadata()` (not exposed but accessible) |

---

## Duplicated Behavior Across All Three Frameworks

After three implementations, the following behavior is identical and a strong candidate for `argonaut-core` promotion:

1. **`buildEvidence(reads)`** — `"ev-" + sourceId`, `EvidenceKind.SUPPORTING`, 200-char excerpt, `score=1.0`, provenance string. All three implementations are byte-identical. Confirmed common utility.

2. **`buildMetrics(trace, evidence, durationMs, modelCalls)`** — event-count logic using `countEvents(trace, KNOWLEDGE_SEARCH_COMPLETED)` and `countEvents(trace, DOCUMENT_READ_COMPLETED)` is the same in all three (LangGraph4j adds `modelCalls` parameter).

3. **Tool output format** — `"source_id=... | title=... | score=..."` for search, `"source_id=...\n\ncontent"` for read. All three agents produce the same format; the prompt instructs the same capability names.

4. **`agentEvent(type, metadata)`** factory — per-framework `ExecutionEvent` construction differs only by framework ID string. The pattern is identical.

5. **`KnowledgeTools` capability names** — `searchKnowledge` and `readDocument` with identical `@Tool` descriptions across all three. The tool descriptions are a shared experiment constant and should live in `argonaut-core`.

6. **`TraceEventMetadata` key usage** — all three use `SOURCE_ID`, `SOURCE_IDS`, `QUERY`, `EVIDENCE_ID` with the same semantics.

---

## Framework-Native Differences

| Difference | Spring AI | LangChain4j 0.x | LangGraph4j 1.x |
| --- | --- | --- | --- |
| Tool state mechanism | `ToolContext.getContext(K)` | Constructor injection | Closure capture |
| How per-run tool instance reaches model | `toolContext(Map.of(...))` on `ChatClient` | `.tools(tools)` on `AiServices.builder()` | `LC4jToolService.builder().toolsFromObject(knowledgeTools).build()` |
| Loop hidden vs explicit | hidden | hidden | explicit nodes + conditional edge |
| State serialization | framework-managed | framework-managed | must use `LC4jStateSerializer` |
| Intermediate state access | n/a | n/a | `graph.getStateHistory()` |

---

## Candidate Common Abstractions

Based on three data points:

### Should move to `argonaut-core`
- `buildEvidence(reads)` — identical in all three; no framework types
- Tool descriptions (both `searchKnowledge` and `readDocument`) — experiment constants

### Should move to `argonaut-core` after one more framework confirms
- `buildMetrics(trace, evidence, durationMs, modelCalls)` — logic is the same; only parameter signature differs (modelCalls was absent in two frameworks)
- `agentEvent(type, metadata)` — same pattern, differs only in framework ID string

### Deliberately deferred
- No common `Tool` abstraction — the three tool mechanisms (`ToolContext`, constructor injection, `LC4jToolService`) are genuinely different and the experiment is designed to observe these differences
- No common `AgentLoop` abstraction — the loop structures are intentionally framework-specific
- No normalization of `modelCalls` metric across frameworks — the implementations now have genuinely different accuracy; a later task should define a common metric contract rather than leveling down

---

## Limitations

1. `langchain4j-open-ai:1.0.0` + `langchain4j-core:1.18.1` version mismatch — pinned with explicit `langchain4j-core:1.18.1` override. Should migrate to a consistent LangChain4j version when a `langchain4j-open-ai:1.18.x` release is used.
2. Real-model validation not performed (see section below).
3. `ChatResponse.metadata()` (token usage) discovered but not mapped to `ExecutionMetrics` — no token fields exist in the current contract.
4. Graph is compiled per run (includes `StateGraph.compile()`). Acceptable for correctness; could be optimized to compile once per agent instance if the per-run closures are refactored.

---

## Real-Model Validation

**NOT RUN.**

`mvn verify` runs only mock-backed tests. Real-model validation requires:
- `OPENROUTER_API_KEY` environment variable
- `OPENROUTER_MODEL` environment variable  
- `OPENROUTER_BASE_URL` (defaults to `https://openrouter.ai/api/v1`)

Spring to the service via `mvn spring-boot:run -pl argonaut-langgraph4j` and POST to `/api/experiment/run`. Credentials must not be committed.

---

## Recommendation Before Framework #4 (Embabel)

Before implementing Embabel, two cleanup tasks are well-supported by three-framework evidence:

1. **Extract shared evidence builder** — Move `buildEvidence(reads)` to `argonaut-core` as a static utility (`EvidenceBuilder.fromReads(reads, provenance)`). All three implementations are byte-identical. Risk: low.

2. **Extract shared tool descriptions** — Move the `searchKnowledge` and `readDocument` `@Tool` description strings to `argonaut-core` constants. Ensures all frameworks present identical capability descriptions to the model. Risk: low.

These are NOT blocking for Embabel, but would reduce copy-paste risk in the fourth implementation.

A third optional task:

3. **Normalize `modelCalls` metric** — Define what "model calls" means across frameworks (invocations of the LLM, not internal framework bookkeeping). LangGraph4j is the first accurate implementation. Before Embabel, agree on whether the metric should be: (a) invocations per question, (b) total over run lifetime, or (c) mark as `unknown` unless natively observable.
