# ARGONAUT-EMBABEL-001 — Embabel Controlled Local Evidence Implementation

**Date:** 2026-08-23
**Status:** Complete
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), ARGONAUT-KOOG-001 Koog reference implementation
**Task:** `docs/engineering/agents/tasks/ARGONAUT-EMBABEL-001-ImplementTheMinimalEmbabelEquivalentOfArgonautUC-001.md`

---

## Summary

Implemented the fifth Argonaut framework service using Embabel 1.5.0 + Spring Boot 4.1.0. All five framework services — Spring AI, LangChain4j, LangGraph4j, Koog, and Embabel — now satisfy TC-UC-001 using the same corpus, the same experiment question, and the same validator.

The central finding: Embabel's GOAP planner naturally replaces the LLM tool loop. UC-001 becomes a single `@AchievesGoal @Action` method where retrieval is pure Java (planner-driven) and the LLM is invoked only for synthesis. This is structurally different from every prior framework implementation.

**Validation:** `mvn verify` — BUILD SUCCESS, 156 tests (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j + 12 Koog + 8 Embabel), 0 failures.

---

## Dependencies

| Artifact | Version | Role |
| --- | --- | --- |
| `com.embabel.agent:embabel-agent-starter-openai` | 1.5.0 (via BOM) | Agent platform autoconfiguration + OpenAI-compatible LLM (used for OpenRouter) |
| `com.embabel.agent:embabel-agent-test` | 1.5.0 (test) | `FakeOperationContext` for unit-level action testing |
| `org.springframework.boot:spring-boot-starter-web` | 4.1.0 | REST layer |

**BOM:** `com.embabel.agent:embabel-agent-dependencies:1.5.0` imported in `dependencyManagement`.

**Custom Maven repository:** `https://repo.embabel.com/artifactory/libs-release` — required; Embabel artifacts are not published to Maven Central.

**No `@EnableAgents` annotation** — Spring Boot autoconfiguration via `embabel-agent-starter-openai` discovers `@Agent`-annotated beans automatically.

**`-parameters` compiler flag:** Required in `maven-compiler-plugin`. Without it, Embabel's action parameter inspection fails silently. Flag set in `argonaut-embabel/pom.xml`.

---

## Module Structure

| File | Purpose |
| --- | --- |
| `argonaut-embabel/pom.xml` | Embabel 1.5.0 + Spring Boot 4.1.0; BOM import; custom repo; `-parameters` flag |
| `ArgonautEmbabelApplication` | Spring Boot entry point, port 8084 |
| `config/EmbabelConfig` | `KnowledgeRepository` bean via `LocalKnowledgeRepository.withDemoCorpus()` |
| `agent/EvidenceQuestion` | Input record: `(String runId, String question)` |
| `agent/AnswerText` | LLM-intermediate record: `@JsonPropertyDescription` answer field for structured LLM response |
| `agent/EvidenceAnswer` | Output record: `(String finalAnswer, List<Evidence>, ExecutionTrace, ExecutionMetrics)` |
| `agent/ControlledLocalEvidenceAgent` | `@Agent` with single `@AchievesGoal @Action answer(EvidenceQuestion, OperationContext)` |
| `api/HealthController` | `GET /api/health` |
| `api/AboutController` | `GET /api/about` |
| `api/ExperimentController` | `POST /api/experiment/run` via `AgentInvocation` |
| `TcUc001EmbabelTest` (test) | 8 tests including TC-UC-001 contract verification and action-shape assertions |

---

## Embabel API Surface Used

| API element | Usage |
| --- | --- |
| `@Agent(description)` | Marks `ControlledLocalEvidenceAgent` as an Embabel agent bean |
| `@Action` | Marks `answer()` as an invocable agent action |
| `@AchievesGoal(description)` | Declares `answer()` as the terminal goal-satisfying action |
| `OperationContext` | Passed to `answer()`; provides LLM access |
| `context.ai().withDefaultLlm().createObject(prompt, AnswerText.class)` | LLM synthesis — structured-output call returning a typed record |
| `AgentPlatform` | Spring-injected in `ExperimentController`; passed to `AgentInvocation.builder()` |
| `AgentInvocation.builder(agentPlatform).build(EvidenceAnswer.class).invoke(question)` | Production agent invocation from REST controller |
| `FakeOperationContext` (test) | `fakeContext.expectResponse(new AnswerText(...))` — presets structured LLM response |
| `fakeContext.getLlmInvocations()` (test) | Verifies prompt shape: question embedded, evidence embedded, no tool groups |

---

## Agent Topology

UC-001 reduces to a single goal → single action in Embabel:

```
AgentInvocation.invoke(EvidenceQuestion)
  └─ GOAP planner: EvidenceQuestion available, EvidenceAnswer required
       └─ answer() satisfies goal → execute once
            ├─ Retrieval: knowledgeRepository.search() + .read() × 3  [pure Java]
            └─ Synthesis: context.ai().withDefaultLlm().createObject() [LLM]
```

There is no ReAct loop. The planner observes the type constraint (input: `EvidenceQuestion`, output: `EvidenceAnswer`) and dispatches the single matching action. Retrieval is not a planner-visible subgoal — it happens inside the action method as regular Java code.

This is architecturally distinct from every prior framework implementation:

- **Spring AI / LangChain4j / Koog:** LLM drives retrieval via tool calls (hidden loop)
- **LangGraph4j:** Explicit graph: retrieval node → synthesis node
- **Embabel:** Planner drives a single Java action; LLM sees only the synthesis prompt

---

## Per-Run State

All per-run state (`InMemoryExecutionObserver`, document accumulator `reads`, `startMs`) is method-local inside `answer()`. `ControlledLocalEvidenceAgent` is a singleton Spring bean. No per-run object construction is needed — this is simpler than LangChain4j (per-run `AiServices.build()`) and Koog (per-run `AIAgent` factory).

---

## Knowledge Access

`KnowledgeRepository` is injected into `ControlledLocalEvidenceAgent` via constructor. It is called directly inside `answer()` — not registered as an LLM tool. There are no `@LlmTool` or `@Tool` annotations on knowledge access methods.

The implication: retrieval is fully deterministic and testable without any LLM mock. Only the synthesis step (`context.ai()...createObject(...)`) requires `FakeOperationContext` in tests.

---

## Observability

All trace events in the Embabel implementation are **synthetic Argonaut instrumentation** — there are no native Embabel lifecycle hooks at the action level for the events Argonaut tracks.

| Event | Source |
| --- | --- |
| `RUN_STARTED` | Synthetic, emitted at `answer()` entry |
| `KNOWLEDGE_SEARCH_STARTED/COMPLETED` | Synthetic, wrapping `knowledgeRepository.search()` |
| `DOCUMENT_READ_STARTED/COMPLETED` | Synthetic, wrapping `knowledgeRepository.read()` |
| `EVIDENCE_RETRIEVED` | Synthetic, after each `read()` |
| `EVIDENCE_SELECTED` | Synthetic, after evidence list built |
| `ANSWER_SYNTHESIZED` | Synthetic, after `createObject()` |
| `RUN_COMPLETED` | Synthetic, at `answer()` exit |

**Model call counting:** `MODEL_CALL_STARTED` and `MODEL_CALL_COMPLETED` are not emitted — there is no observable hook into `context.ai().createObject()` from inside the action. `modelCalls` is hard-coded to `1`, matching the single `createObject()` call. Same limitation as Spring AI and LangChain4j.

---

## Testing Strategy

`FakeOperationContext` from `embabel-agent-test` enables unit-level action testing without the Embabel platform:

```java
var fakeContext = new FakeOperationContext();
fakeContext.expectResponse(new AnswerText(FINAL_ANSWER));
EvidenceAnswer answer = agent.answer(question, fakeContext);
```

Real `LocalKnowledgeRepository.withDemoCorpus()` executes — retrieval, search events, and evidence traceability (SC4) reflect genuine repository calls.

**Tests (8 total):**

| Test | Asserts |
| --- | --- |
| `embabelAgent_satisfies_tc_uc_001` | SC1–SC9 via `ControlledLocalEvidenceContract.verify()` |
| `embabelAgent_result_is_non_null` | Result non-null |
| `embabelAgent_result_contains_exp001_evidence` | `exp-001` in evidence list |
| `embabelAgent_result_has_non_blank_final_answer` | Non-blank `finalAnswer` |
| `answer_action_calls_llm_for_synthesis` | `fakeContext.getLlmInvocations()` non-empty |
| `answer_action_does_not_attach_tool_groups_to_synthesis_prompt` | `getToolGroups().isEmpty()` |
| `answer_action_embeds_question_in_synthesis_prompt` | Question text present in prompt |
| `answer_action_embeds_evidence_in_synthesis_prompt` | `exp-001` content present in prompt |

**Surefire/JUnit5 quirk:** Surefire 3.5.2 console output reports outer `@Test` methods under the `$ActionShapeTests` nested class ("Tests run: 0" for outer, "Tests run: 8" for nested). Surefire XML confirms all 8 tests ran and passed. This is a JUnit 5 + Surefire reporting artifact, not a test failure.

No Spring context is loaded during tests — `ControlledLocalEvidenceAgent` is instantiated directly. This matches the Embabel-native testing pattern from `StarNewsFinderTest` and `UserGuideValidatorAgentTest`.

---

## Implementation Friction

**Dependency setup.** The initial `argonaut-embabel/pom.xml` included `embabel-agent-starter-observability` + Zipkin and lacked the web starter. The correct minimal starter for this use case is `embabel-agent-starter-openai` (provides platform autoconfiguration + OpenAI-compatible LLM client). Identified by comparing with the local examples POM.

**Package-private `FRAMEWORK_ID` access.** `ControlledLocalEvidenceAgent.FRAMEWORK_ID` has no access modifier (package-private). Initial `ExperimentController` in a different package (`api` vs `agent`) tried `import static` access — compiler error. Fixed by defining `private static final String FRAMEWORK_ID = "embabel"` directly in `ExperimentController`.

**No LLM model call hook.** Embabel provides no surface inside an `@Action` method to observe the number of LLM calls made by `context.ai()`. `modelCalls` cannot be accurately counted and is hard-coded to `1`. This is the same limitation as Spring AI and LangChain4j but is notable because Embabel's planner-driven model otherwise reduces hidden state.

**Local-evidence research discipline.** `libs-code/embabel-agent-examples/` was sufficient for the entire implementation. Three key files drove the design:
- `StarNewsFinder.java` — `@Agent`, `@Action`, `@AchievesGoal`, `OperationContext`, `context.ai()...createObject()`
- `StarNewsFinderTest.java` — `FakeOperationContext` pattern, `expectResponse()`
- `UserGuideValidatorAgent.java` — multi-step action composition and `OperationContext` depth

No remote framework discovery was necessary. No Embabel repository was cloned.

---

## Comparison Observations

These observations arise directly from implementing the same UC-001 in five frameworks.

### Retrieval ownership

| Framework | Who decides to retrieve? | How? |
| --- | --- | --- |
| Spring AI | LLM | Tool call to `searchKnowledge` / `readDocument` registered on `ChatClient` |
| LangChain4j | LLM | Tool call to `@Tool`-annotated methods on `KnowledgeTools`, per `AiServices` |
| LangGraph4j | Graph definition | Explicit `retrieve` node before `synthesize` node |
| Koog | LLM | Tool call via `ToolRegistry` inside `singleRunStrategy` loop |
| Embabel | Java code / planner action | `knowledgeRepository.search()` + `.read()` called directly inside `answer()` |

Embabel is the only framework where the LLM never decides whether to retrieve. Retrieval is unconditional Java code in the action method. This makes retrieval deterministic and independently testable.

### Tool registration

Spring AI, LangChain4j, and Koog all require registering knowledge-access methods as LLM tools. LangGraph4j uses explicit graph nodes. Embabel requires neither — knowledge access is plain Java.

### Testing the retrieval path

In all prior frameworks, testing the retrieval path requires either a mock LLM that returns tool-call responses or a live LLM. In Embabel, retrieval can be tested with a null (or absent) `OperationContext` — only synthesis requires `FakeOperationContext`. This is the cleanest test isolation model of any framework implementation.

### Single action vs multi-step planning

UC-001 in Embabel is a single `@AchievesGoal @Action`. The GOAP planner is not exercised beyond goal dispatch. Embabel's differentiated value — planning across multiple actions/goals, subagent composition, conditional branching — is not observed in UC-001. A more complex use case would exercise the planner more meaningfully.

---

## Framework-Neutral Abstraction Opportunities

The planner-driven retrieval pattern reveals a distinction not previously captured in Argonaut:

**Retrieval mode:** `DETERMINISTIC` (code-driven) vs `LLM_DRIVEN` (tool-call-driven). Embabel is the only current `DETERMINISTIC` implementation. LangGraph4j is close — its retrieval is graph-driven, not LLM-driven — but still relies on an LLM choosing to invoke nodes.

Adding a `retrievalMode` field to `ExperimentResult` or `ArgonautInfo` would let the experiment console surface this structural difference. This is deferred to a future task.

**`modelCalls` accuracy taxonomy:** The current `ExecutionMetrics.modelCalls` field is unreliable across frameworks. A future `modelCallAccuracy` enum (`ACCURATE`, `HARD_CODED`, `UNKNOWN`) could make this honest without changing the metric value.

---

## Limitations

- **GOAP planner not exercised.** UC-001 is one goal, one action. Embabel's multi-goal planning, subagent composition, and conditional action sequencing are not demonstrated.
- **LLM call count.** `modelCalls=1` is hard-coded. No native hook exists inside `@Action` to count `context.ai()` invocations.
- **Platform integration test absent.** Tests call `agent.answer()` directly. The `AgentInvocation` production path (platform, goal dispatch, type matching) is not tested. Integration would require a full Spring context with a configured `AgentPlatform`.

---

## Research Discipline Evidence

- Local framework evidence (`libs-code/embabel-agent-examples/`) was used exclusively — no remote discovery.
- Three example files sufficed: `StarNewsFinder.java`, `StarNewsFinderTest.java`, `UserGuideValidatorAgent.java`.
- Research-to-first-meaningful-implementation time: low — prepared local evidence was sufficient to answer all API questions within the first few reads.
- No Embabel repository was cloned. No external search was performed.
- Significant context/tool-output pressure was observable near the end of the session — the engineering report was deferred to a subsequent context window.

---

## Validation Evidence

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 — TcUc001EmbabelTest

Tests:
  embabelAgent_satisfies_tc_uc_001          ← SC1–SC9 via ControlledLocalEvidenceContract
  embabelAgent_result_is_non_null
  embabelAgent_result_contains_exp001_evidence
  embabelAgent_result_has_non_blank_final_answer
  answer_action_calls_llm_for_synthesis
  answer_action_does_not_attach_tool_groups_to_synthesis_prompt
  answer_action_embeds_question_in_synthesis_prompt
  answer_action_embeds_evidence_in_synthesis_prompt

[INFO] BUILD SUCCESS
[INFO] Tests run: 156, Failures: 0, Errors: 0, Skipped: 0
```

`git diff --check` — clean (no whitespace errors).

---

## Changed Files

| File | Change |
| --- | --- |
| `argonaut-embabel/pom.xml` | Replaced observability+Zipkin with `embabel-agent-starter-openai` + web starter; added `embabel-agent-test`; `maven-compiler-plugin -parameters`; `spring-boot-maven-plugin 4.1.0` |
| `argonaut-embabel/src/main/resources/application.properties` | OpenRouter config via `spring.ai.openai.*`; port 8084 |
| `argonaut-embabel/src/main/java/.../ArgonautEmbabelApplication.java` | Plain `@SpringBootApplication` |
| `argonaut-embabel/src/main/java/.../config/EmbabelConfig.java` | `KnowledgeRepository` bean |
| `argonaut-embabel/src/main/java/.../agent/EvidenceQuestion.java` | Input record |
| `argonaut-embabel/src/main/java/.../agent/AnswerText.java` | LLM-intermediate record |
| `argonaut-embabel/src/main/java/.../agent/EvidenceAnswer.java` | Output record |
| `argonaut-embabel/src/main/java/.../agent/ControlledLocalEvidenceAgent.java` | Core implementation |
| `argonaut-embabel/src/main/java/.../api/ExperimentController.java` | `POST /api/experiment/run` |
| `argonaut-embabel/src/main/java/.../api/HealthController.java` | `GET /api/health` |
| `argonaut-embabel/src/main/java/.../api/AboutController.java` | `GET /api/about` |
| `argonaut-embabel/src/test/java/.../agent/TcUc001EmbabelTest.java` | 8 TC-UC-001 tests |
| `CLAUDE.md` | Embabel status, test count, per-framework table, Embabel-specific notes |
| `docs/engineering/ENGINEERING_LOG.md` | 2026-08-23 ARGONAUT-EMBABEL-001 entry |
