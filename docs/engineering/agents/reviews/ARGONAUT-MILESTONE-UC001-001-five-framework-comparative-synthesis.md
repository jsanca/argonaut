# ARGONAUT-MILESTONE-UC001-001 — Five-framework comparative synthesis

**Date:** 2026-08-23  
**Status:** Complete  
**Type:** Comparative architecture review / engineering milestone  
**Scope:** UC-001 only; local repository evidence only

## Purpose and evidence boundary

This report closes the UC-001 learning milestone. It compares the five implementations
of the same controlled-local-evidence question; it is neither a framework popularity
comparison nor a recommendation to standardize on one framework.

The review inspected the UC-001 contract, shared `argonaut-core` contracts, each
framework's production agent and test, and the four available framework implementation
reports. No external research was performed. Spring AI's implementation evidence is its
production code, test, and the 2026-08-16 engineering-log entry; it has no separate
framework report in `docs/engineering/agents/reports/`.

All five implementations use the same:

- `ControlledLocalEvidenceContract` request and SC1–SC9 assertions;
- `ExperimentRequest` and `ExperimentResult` boundary;
- `KnowledgeRepository` over the controlled local corpus;
- `Evidence`, `ExecutionTrace`, and `ExecutionMetrics` result vocabulary;
- canonical prompt and requirement that `exp-001` be traceable to actual document reads.

The common validator is important: it proves comparable externally observable behavior,
not equivalent internal topology, model-call count, or production-model quality.

## Executive synthesis

UC-001 establishes that the experiment contract is portable while orchestration is not.
Every implementation can answer from the same local corpus and return the same
evidence/trace/result shape, but the actor controlling retrieval and continuation differs:

| Framework | Observed control model | Retrieval owner in UC-001 | Model-call visibility |
| --- | --- | --- | --- |
| Spring AI | Imperative `ChatClient` integration with an internal tool loop | LLM | Opaque; metric is hard-coded `1` |
| LangChain4j | Declarative `AiServices` interface with an internal tool loop | LLM | Opaque; metric is `0` meaning unknown |
| LangGraph4j | Explicit ReAct state graph and conditional edges | LLM requests tools, while graph owns transitions | Accurate, directly wrapped calls |
| Koog | `AIAgent` strategy runtime with an implicit ReAct loop | LLM | Accurate through lifecycle event handlers |
| Embabel | Goal/action invocation; one planner-selected Java action | Application action deterministically | One synthesis call assumed (`1`), not hook-observed |

The most useful conclusion is therefore not “all frameworks need tools” or “all
frameworks are agents.” The stable Argonaut concepts are experiment inputs/outputs,
controlled knowledge access, evidence provenance, and normalized observable lifecycle
semantics. A tool loop, graph, planner, tool object, and model-call mechanism are
implementation choices that must remain framework-local until a later experiment proves
a narrower neutral contract.

## Evidence-backed comparison

### Execution, retrieval, state, and tool semantics

| Axis | Spring AI | LangChain4j | LangGraph4j | Koog | Embabel |
| --- | --- | --- | --- | --- | --- |
| Execution owner | `ChatClient` plus framework's internal tool loop | `AiServices` plus framework's internal tool loop | Application-defined graph and conditional edge | `AIAgent` default `singleRunStrategy()` | `AgentInvocation`/GOAP planner selects one action; action code executes it |
| Retrieval decision | LLM invokes registered methods | LLM invokes registered methods | Model asks for tools; explicit graph decides tool-node transition | LLM invokes tools in strategy loop | Java action always searches then reads top three results |
| Per-run mutable state | `ToolContext` map passes observer and reads to singleton tool | Fresh tool POJO and fresh `AiServices` hold observer/reads | Fresh graph/tool service; node closures capture observer, reads, counter | Fresh tool set, registry, agent, observer, reads, counter | Action-local observer, reads, and elapsed time |
| Tool abstraction | Spring `@Tool` methods with framework context | LangChain4j `@Tool` methods with constructor-injected state | LangChain4j-compatible tools executed by `LC4jToolService` node | Koog `ToolSet` registered in `ToolRegistry` | No LLM tool: direct `KnowledgeRepository` calls |
| Control-flow topology | Hidden | Hidden | Visible: `START → agent → tools → agent → … → END` | Hidden in strategy | Visible only as planner goal/action dispatch; internal retrieval is ordinary code |
| Model invocation | `.prompt().tools(...).call()` | interface method `assistant.answer()` | explicit `chatModel.chat(ChatRequest)` in agent node | agent runtime invokes executor | explicit `context.ai()...createObject()` only for synthesis |

The working characterizations can now be made more precise:

- **Spring AI** is framework-native imperative AI integration: application code configures
  a `ChatClient`, supplies a prompt/tools/context, and delegates the ReAct-like continuation
  to the client.
- **LangChain4j** is declarative AI-service integration: an annotated Java interface and
  tool object describe the interaction, while `AiServices` owns the hidden loop.
- **LangGraph4j** is explicit stateful topology: application code names state, nodes,
  transitions, tool execution, and direct model calls.
- **Koog** is a strategy-based agent runtime: application code configures `AIAgent`, tools,
  a maximum iteration count, and event handling, while a runtime strategy owns the loop.
- **Embabel** is goal/action/type-driven planning: the platform chooses an action that
  satisfies the requested typed goal; UC-001 puts retrieval inside that action and uses the
  LLM as a structured synthesis dependency.

These are observed descriptions of these implementations, not claims that they exhaust
each framework's API or best fit for every problem.

### Observability and metrics

Every implementation emits the contract-required run, search, document-read, evidence,
answer, and completion events through `InMemoryExecutionObserver`. Consequently, those
events are **normalized Argonaut instrumentation**, not evidence that each framework
natively exposes the same lifecycle. The anti-gaming assertion verifies that returned
evidence source IDs appear in read/retrieval event metadata, which makes repository reads
observable across all five.

| Observation | Spring AI | LangChain4j | LangGraph4j | Koog | Embabel |
| --- | --- | --- | --- | --- | --- |
| Required UC-001 trace | Synthetic around agent and tool adapter | Synthetic around agent and tool adapter | Synthetic for Argonaut events; model calls wrapped directly | Synthetic Argonaut events; model lifecycle comes from `handleEvents` | Synthetic around action and repository calls |
| Native/model lifecycle surface exercised | No loop hook used | `AiServices` hides loop | Direct model-call boundary in graph node | `onLLMCallStarting` / `onLLMCallCompleted` | No action-local hook for `context.ai()` call |
| `modelCalls` result | `1`, hard-coded | `0`, explicitly unknown | Accurate counter | Accurate counter | `1`, hard-coded from action shape |
| Other UC-001 counts | searches/reads derived from trace; evidence count from result | Same | Same | Same | Same |

For this use case, LangGraph4j and Koog provide the best **native model-call
observability actually exercised**: both report model lifecycle events at a real boundary.
LangGraph4j additionally exposes graph state, finish reasons, response metadata, and
node-level hooks, although UC-001 did not normalize them. This does not make the other
frameworks unobservable; it means their high-level APIs conceal the exact loop boundary
used by the current adapters. The `modelCalls` field is therefore not currently comparable
without an accuracy/provenance field or framework-specific instrumentation.

`toolCalls` is `0` in the completed metrics construction across these implementations,
despite tool usage in four of them. It is not a meaningful comparative measure in UC-001.
That is a contract/adapter limitation, not a statement that no tool executions occurred.

### Testability

All five tests run the shared contract with deterministic model doubles, so all prove the
same external UC-001 behavior. Their natural isolated seam differs:

| Framework | Natural test unit demonstrated | Isolation observation |
| --- | --- | --- |
| Spring AI | Service plus mock chat model | Model double must drive the expected tool-call sequence |
| LangChain4j | Agent/AI service plus mock chat model | Model double must drive `AiServices`' hidden loop |
| LangGraph4j | Compiled graph plus mock chat model | Graph topology and direct model calls can be asserted, but test setup includes state/tool service plumbing |
| Koog | Agent runtime plus mock prompt executor | Strategy behavior is covered; coroutine bridge and executor internals remain part of harness setup |
| Embabel | Plain `@Action` method plus `FakeOperationContext` | Retrieval runs against the real repository; only structured synthesis is faked |

Embabel provides the cleanest test isolation **for this specific UC-001 shape**, because
retrieval is deterministic Java in the action and `FakeOperationContext` only substitutes
the LLM. That conclusion does not assess Embabel planner integration: the production
`AgentInvocation`/platform selection path is not integration-tested. LangGraph4j provides
the clearest isolated test of topology when topology itself is the behavior under test.

### Ceremony, leakage, and UC-001 ergonomics

Some repeated code is Argonaut experiment mechanics, not framework ceremony: constructing
evidence from actually read documents, emitting the normalized trace, deriving searches and
reads from trace events, producing the common result, and handling failures. It is repeated
to keep the experiment adapters independently readable; it must not be mistaken for a
universal agent-loop abstraction.

Framework-required semantics and framework leakage are distinct:

| Framework | Unavoidable semantics exercised | Adapter/accidental ceremony or leakage |
| --- | --- | --- |
| Spring AI | `ChatClient`, Spring `@Tool`, `ToolContext` | Context-map keys and singleton-tool lifecycle are Spring-specific |
| LangChain4j | `AiServices`, annotated interface/methods, tool POJO | Rebuilding `AiServices` to carry per-run state; annotations and version-specific model API |
| LangGraph4j | State schema, nodes, edges, conditional routing, serializer | `LC4jStateSerializer`, `LC4jToolService`, message-state and command update plumbing |
| Koog | Agent strategy, registry, event feature, coroutine execution | Kotlin/Java bridge and mock executor overloads are integration mechanics, not UC-001 semantics |
| Embabel | typed input/output, `@Agent`, `@Action`, `@AchievesGoal`, operation context | Request/result translation around platform invocation; direct retrieval is application policy, not a planner primitive |

UC-001 makes LangGraph4j look more elaborate because its main differentiated asset—explicit
topology—is used to represent a tiny ReAct cycle that high-level integrations hide. It makes
Embabel look simpler because one goal maps to one action, thereby under-exercising planning.
It also makes Spring AI, LangChain4j, and Koog look deceptively similar because each can
hide a short tool loop behind a friendly entry point.

## Answers to the specific questions

1. **What remained identical because it belongs to Argonaut?** The controlled corpus and
   repository contract, request/result payloads, evidence provenance, normalized trace
   semantics, core success criteria, canonical prompt, framework IDs, and the experiment
   lifecycle boundary. They are comparative conditions rather than framework mechanisms.

2. **Which apparent common concepts should not become abstractions yet?** Tool, agent,
   graph, planner, model call, tool context, agent loop, and a common framework executor.
   Each maps to materially different ownership and state semantics, and Embabel uses no
   LLM tool abstraction at all.

3. **Is `Tool` universal?** No. It is a valid adapter vocabulary for Spring AI,
   LangChain4j, LangGraph4j's tool node, and Koog. In Embabel UC-001, repository access is
   an ordinary deterministic call inside a goal-achieving action. `KnowledgeRepository` is
   the neutral capability; “tool” describes only one exposure mechanism.

4. **Is an explicit agent loop universal?** No. Spring AI, LangChain4j, and Koog have
   loops but hide them; LangGraph4j encodes one explicitly; Embabel's UC-001 path is a
   planner dispatch to one action with no ReAct loop.

5. **What forms of execution control appeared?** Imperative high-level client control,
   declarative AI-service control, explicit graph state-machine control, strategy-runtime
   control, and goal/action planner dispatch plus application-controlled retrieval.

6. **Which have the best native observability here?** LangGraph4j and Koog for model-call
   lifecycle/counting exercised by UC-001. LangGraph4j has the richest unexercised state and
   response metadata surface. Required evidence traces are consistently Argonaut-synthetic.

7. **Which has the cleanest test isolation?** Embabel for the currently single-action,
   deterministic-retrieval case. LangGraph4j is strongest when testing visible routing is
   the objective. Both conclusions are bounded to the demonstrated tests.

8. **Which mechanisms look useful for a more complex UC-002?** LangGraph4j's named state,
   conditional edges, state history, and hooks; Embabel's multi-action typed planning;
   Koog's strategies and event feature; and the high-level tool-loop integrations where a
   bounded single responsibility does not need application-visible routing. This is an
   inference from observed architecture, not a capability benchmark.

9. **Where does UC-001 overstate complexity?** Its deterministic search/read/synthesize
   path forces graph and planning frameworks to expose setup whose payoff requires branching,
   evolving state, or multiple goals. Graph serialization and planner annotations are not
   evidence that the use case itself needs that complexity.

10. **Where does UC-001 hide valuable capability?** It does not exercise LangGraph4j
    checkpoints/history or complex routing; Embabel multi-action planning/subagents; Koog
    custom strategies beyond one run; or richer error/retry, streaming, memory, and
    multi-turn facilities of the high-level integrations. No conclusion about those
    capabilities is warranted.

## Confirmed and premature abstractions

### Confirmed framework-neutral concepts

These concepts are already justified by five independently shaped adapters and should
remain the center of Argonaut's common contract:

- `KnowledgeRepository` / search and read, as framework-independent controlled-evidence
  capabilities;
- `ExperimentRequest`, `ExperimentResult`, run status, and framework ID as the external
  experiment boundary;
- `Evidence` and provenance traceability as a comparison result, not as a framework object;
- `ExecutionTrace` event semantics and `ExecutionMetrics` as normalized comparison output;
- experiment lifecycle expectations: start, controlled retrieval, evidence selection,
  synthesis, completion/failure;
- `ControlledLocalEvidenceContract` as a framework-neutral behavioral baseline.

There is also repeated *mechanical* evidence building and trace-derived metrics counting.
Those are candidates for a small pure core helper only if doing so preserves each
implementation's independent control flow and provenance. This milestone does not add one.

### Premature abstractions

Do not introduce a common `Tool`, `Agent`, `Graph`, `Planner`, `Workflow`, model-call
abstraction, or shared agent loop. Such types would either reduce `KnowledgeRepository` to
only LLM-invocable access (incorrect for Embabel), hide the very topology Argonaut is
comparing (incorrect for LangGraph4j), or leak one framework's lifecycle/state conventions
into all others. A common model-call metric is similarly premature until its accuracy and
counting boundary are explicit.

## When would I reach for each framework?

These are selection hypotheses inferred from the implementations, not universal quality
claims. Confidence labels reflect that UC-001 is one small, mock-backed local-evidence
experiment and did not run production providers.

| Framework | Favorable problem shape | Warning signs | Confidence |
| --- | --- | --- | --- |
| Spring AI | A Spring service needs a compact, imperative prompt-and-tool interaction whose routing need not be application-visible | Precise loop-level metrics, replayable topology, or complex routing become first-class requirements | Moderate for simple tool use; low beyond UC-001 |
| LangChain4j | Java code benefits from an annotated AI-service interface and concise tool exposure | You must observe or customize every intermediate model/tool transition, or carry complex mutable per-run state | Moderate for declarative tool-driven interaction; low for custom control flow |
| LangGraph4j | State, branches, loops, transition visibility, and execution inspection are domain requirements | The workflow is a single straight-line prompt/tool exchange and graph setup obscures rather than clarifies intent | High that explicit topology is its differentiator; low on broader operational behavior |
| Koog | A Kotlin application wants an agent runtime, bounded strategy loop, tools, and model lifecycle events | Java/Spring synchronous boundaries or a fully application-authored graph dominate the design | Moderate; UC-001 exercised only the default strategy |
| Embabel | Typed goals/actions and deterministic business/retrieval steps should frame LLM work, especially when action-level testing matters | The problem is chiefly a free-form iterative tool loop, or planner behavior is not needed | Moderate for one deterministic action; low for multi-action planning because it was unexercised |

No row is an overall winner. The primary selection criterion should be the required control
model and observability boundary of the next use case, not the amount of UC-001 adapter code.

## UC-002 implications

UC-002 should remain a comparison experiment, not a feature grab bag. To expose meaningful
differences, it should require several of the following characteristics while keeping input,
evidence conditions, output contract, and evaluation criteria controlled:

- multiple named stages with state that evolves between stages;
- conditional branching based on retrieved evidence or validation results;
- more than one valid plan or ordering of responsibilities;
- retry, revision, and bounded failure paths that must be observable;
- a validation step capable of rejecting or revising a draft;
- parallelizable independent work with a defined join;
- optional human intervention/approval at a visible boundary;
- multiple responsibilities or typed outputs such that planner/action composition is
  meaningfully exercised;
- requirements for accurate model/tool-call provenance, step topology, and perhaps
  resumable/replayable state.

This would exercise what UC-001 deliberately does not: LangGraph4j routing/state history,
Embabel multi-action planning, Koog strategy alternatives, and the limits of high-level
hidden loops. It must not presume that any of those mechanisms is the desired universal
Argonaut abstraction.

## Limitations and unresolved questions

- Validation is mock-backed; none of these results compare real-model answer quality,
  latency, cost, provider compatibility, streaming, resilience, or production operations.
- The five modules satisfy the same contract, but their mocks make different numbers of
  model/tool calls. UC-001 is not a controlled benchmark of call efficiency.
- `ExecutionMetrics.modelCalls` and `toolCalls` do not yet carry accuracy/provenance; values
  should not be ranked across frameworks.
- The Embabel platform path is used in production code but action-level rather than full
  platform integration tests provide the milestone evidence.
- The reports describe implemented versions and APIs in this repository only. They do not
  establish broad ecosystem or future-version behavior.

## References

- `docs/engineering/agents/tasks/knowledge/ARGONAUT-MILESTONE-UC001-001-ComparativeSynthesisOfTheFiveUC-001FrameworkImplementations.md`
- `argonaut-core/src/main/java/dev/jsanca/argonaut/core/testing/ControlledLocalEvidenceContract.java`
- `argonaut-core/src/main/java/dev/jsanca/argonaut/core/experiment/ExperimentRequest.java`
- `argonaut-core/src/main/java/dev/jsanca/argonaut/core/experiment/ExperimentResult.java`
- `argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/KnowledgeRepository.java`
- `argonaut-core/src/main/java/dev/jsanca/argonaut/core/evidence/Evidence.java`
- `argonaut-core/src/main/java/dev/jsanca/argonaut/core/trace/ExecutionTrace.java`
- `argonaut-core/src/main/java/dev/jsanca/argonaut/core/metrics/ExecutionMetrics.java`
- `docs/engineering/agents/reports/ARGONAUT-009-langchain4j-controlled-local-evidence.md`
- `docs/engineering/agents/reports/ARGONAUT-010-langgraph4j-controlled-local-evidence.md`
- `docs/engineering/agents/reports/ARGONAUT-KOOG-001-koog-controlled-local-evidence.md`
- `docs/engineering/agents/reports/ARGONAUT-EMBABEL-001-embabel-controlled-local-evidence.md`
- `docs/engineering/ENGINEERING_LOG.md` entries dated 2026-08-16, 2026-08-21, 2026-08-22,
  and 2026-08-23.
