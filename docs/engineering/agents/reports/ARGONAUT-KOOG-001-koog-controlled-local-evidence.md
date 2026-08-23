# ARGONAUT-KOOG-001 — Koog Controlled Local Evidence Implementation

**Date:** 2026-08-22
**Status:** Complete
**Input:** ARGONAUT-008B (`ControlledLocalEvidenceContract`), ARGONAUT-008A (`LocalKnowledgeRepository.withDemoCorpus()`), ARGONAUT-010 LangGraph4j reference implementation

---

## Summary

Implemented the fourth Argonaut framework service using JetBrains' Koog 1.1.1 + Spring Boot 4.1.0. All four framework services — Spring AI, LangChain4j, LangGraph4j, Koog — now satisfy TC-UC-001 using the same corpus, the same experiment question, and the same validator.

**Validation:** `mvn verify` — BUILD SUCCESS, 148 tests (111 core + 4 Spring AI + 9 LangChain4j + 12 LangGraph4j + 12 Koog), 0 failures.

---

## Dependencies

| Artifact | Version | Role |
| --- | --- | --- |
| `ai.koog:koog-agents-jvm` | 1.1.1 | Agent framework: `AIAgent`, `ToolRegistry`, `handleEvents`, graph engine |
| `ai.koog:prompt-executor-openrouter-client-jvm` | 1.1.1 | OpenRouter LLM client for production use |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.3.21 | Kotlin runtime (pinned to match transitive `kotlin-reflect:2.3.21`) |
| `org.springframework.boot:spring-boot-starter-web` | 4.1.0 | REST layer |

**Version conflict note:** `koog-agents-jvm:1.1.1` pulls `kotlin-reflect:2.3.21` transitively via `agents-tools-jvm`. Koog's release notes say "Kotlin 2.3.10" but the published artifacts resolve the reflect library at 2.3.21. `kotlin-reflect:2.3.21` references `KotlinGenericDeclaration` (added in `kotlin-stdlib:2.3.21`). Pinning `kotlin.version=2.3.21` in the koog module aligns stdlib and reflect and eliminates `NoClassDefFoundError: kotlin/jvm/internal/KotlinGenericDeclaration` at test runtime.

**OpenRouter factory function:** `OpenRouterLLMClient(apiKey)` is a JVM-platform Kotlin top-level function compiled with `@file:JvmName("OpenRouterClientFactory")`. In Kotlin it is called directly as a constructor-style factory function — `OpenRouterLLMClient(apiKey = apiKey)` — not as `OpenRouterClientFactory.openRouterClient(apiKey)`.

---

## Module Structure

| File | Purpose |
| --- | --- |
| `argonaut-koog/pom.xml` | Koog 1.1.1 + Spring Boot 4.1.0; Kotlin 2.3.21; allopen/spring plugin; `-java-parameters` |
| `ArgonautKoogApplication` | Spring Boot entry point, port 8085 |
| `config/KoogConfig` | `KnowledgeRepository` + `PromptExecutor` + `LLModel` beans |
| `knowledge/KnowledgeTools` | `ToolSet` with `@Tool`/`@LLMDescription`-annotated methods, per-run constructor injection |
| `agent/ControlledLocalEvidenceAgent` | `AIAgent`-based orchestration; `handleEvents` for `MODEL_CALL_STARTED/COMPLETED` |
| `api/HealthController` | `GET /api/health` |
| `api/AboutController` | `GET /api/about` |
| `api/ExperimentController` | `POST /api/experiment/run` |
| `MockPromptExecutor` (test) | 3-stage state machine: searchKnowledge → readDocument → final answer |
| `TcUc001KoogTest` (test) | 12 tests including TC-UC-001 contract verification |

---

## Agent Topology

Koog uses `AIAgent` (a factory function returning `GraphAIAgent<String, String>`) with the implicit `singleRunStrategy()` (default ReAct loop):

```
START → LLM call → tool dispatch loop → LLM call → ... → final text → END
```

The agent receives a plain `String` input (the question) and returns a plain `String` output (the final answer). There is no explicit graph definition — the loop is encapsulated inside `singleRunStrategy`.

**`handleEvents` block** (installed in the trailing lambda of `AIAgent { ... }`):
- `onLLMCallStarting { }` → emits `MODEL_CALL_STARTED`, increments `modelCallCounter`
- `onLLMCallCompleted { }` → emits `MODEL_CALL_COMPLETED`

This makes Koog the third framework (after LangGraph4j) to accurately observe and count model calls.

---

## Per-Run State Pattern

Koog does not have a `ToolContext` side-channel (Spring AI) or per-run `AiServices.build()` (LangChain4j). A fresh `KnowledgeTools` instance (and fresh `InMemoryExecutionObserver`, `reads` accumulator, `modelCallCounter`, `ToolRegistry`, and `AIAgent`) is created per `ControlledLocalEvidenceAgent.run()` invocation. This mirrors the LangChain4j constructor-injection pattern.

---

## Coroutine Bridge

`AIAgent.run()` and `AIAgent.close()` are Kotlin suspend functions. The Java bytecode exposes non-suspend wrappers via `@JvmSynthetic` but these are invisible to the Kotlin compiler. A `runBlocking { }` bridge is used from synchronous Spring MVC handler threads.

---

## Key Differences from Prior Frameworks

| Concern | Spring AI | LangChain4j | LangGraph4j | Koog |
| --- | --- | --- | --- | --- |
| Tool loop | Hidden in `ChatClient` | Hidden in `AiServices` | Explicit graph edges | Hidden in `singleRunStrategy` |
| Per-run state | `ToolContext` side-channel | Constructor + per-run `AiServices.build()` | Node closure capture | Constructor + per-run `AIAgent` |
| `modelCalls` accuracy | Hard-coded `1` | `0` (loop opaque) | Accurate (`AtomicInteger`) | Accurate (`AtomicInteger` via `handleEvents`) |
| `MODEL_CALL_STARTED/COMPLETED` | Not observable | Not observable | First framework to emit | Also emits via `handleEvents` |
| Language | Java | Java | Java | Kotlin |
| Async | Sync (Spring MVC) | Sync (Spring MVC) | Sync (Spring MVC) | Kotlin suspend → `runBlocking` bridge |

---

## MockPromptExecutor Design

The mock extends `MultiLLMPromptExecutor(emptyMap())` but must override two methods to bypass the client-map lookup:

1. `resolveModel(model, operation)` → returns `ResolvedModel(effectiveModel = model)` (bypasses `No client found for provider` check)
2. `execute(prompt, resolvedModel, tools)` → the `ResolvedModel`-based overload recommended for custom subclasses; implements the 3-stage state machine

`ContextualPromptExecutor` (the agent's internal executor wrapper) calls `executor.resolveModel()` first, then `executor.execute(prompt, resolvedModel, tools)` — so both overloads must be present.

---

## Validation Evidence

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
  koogAgent_satisfies_tc_uc_001                   ← SC1–SC9 via ControlledLocalEvidenceContract
  koogAgent_result_is_non_null
  koogAgent_frameworkId_is_koog
  koogAgent_result_contains_exp001_evidence
  koogAgent_result_has_non_blank_final_answer
  koogAgent_uses_common_system_prompt
  koogAgent_model_calls_are_counted_accurately
  knowledge_search_delegates_to_repository
  document_read_delegates_to_repository
  document_read_emits_trace_metadata_for_sc4
  hollow_result_cannot_bypass_tc_uc_001
  independent_runs_do_not_share_state
```

Full reactor: `mvn verify` — BUILD SUCCESS, all 148 tests, 0 failures.
