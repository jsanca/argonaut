# ARGONAUT-UI-001: Experiment Console

**Date:** 2026-08-23
**Status:** Complete
**Task:** ARGONAUT-UI-001 — Vue 3 experiment console for comparative AI framework evaluation

---

## Summary

Implemented the Argonaut experiment console as a Vue 3 + Vite + TypeScript single-page application in `argonaut-ui/`. The console allows a user to submit UC-001 experiment questions to any or all five Spring Boot backend services simultaneously, compare results side-by-side in a metrics table, and inspect the full answer, evidence, execution trace, and metrics for each framework run.

CORS configuration was added to all five Spring Boot backend modules (`spring-ai`, `langchain4j`, `langgraph4j`, `embabel`, `koog`) to enable browser requests from `localhost:5173`.

---

## Technology Selection and Rationale

| Choice | Rationale |
| --- | --- |
| Vue 3 (Composition API, `<script setup>`) | Matches the OSK skill set; reactive model maps cleanly to multi-framework run state |
| Vite 6 | Fast dev server; standard Vue scaffold; no build configuration overhead |
| TypeScript 5.7 | Type-safe consumption of `argonaut-core` contracts; interfaces match Java records directly |
| Vitest 3 | First-class Vite integration; same `vi.stubGlobal` / `vi.resetModules` API needed for module-level composable state |
| No Pinia, no Vue Router | State is simple enough for `ref` / `reactive` composables; single page requires no navigation |
| Sibling directory (`argonaut-ui/`) | Excluded from Maven reactor per existing project design; `npm` lifecycle is independent |

---

## Framework Registry Contract

Frameworks are discovered at runtime from `public/config/frameworks.json` — a static JSON file served by Vite. The registry shape:

```json
{
  "frameworks": [
    { "id": "spring-ai", "name": "Spring AI", "baseUrl": "http://localhost:8081", "enabled": true },
    ...
  ]
}
```

`useFrameworkRegistry` loads this file once on mount, filters `enabled: false` entries, and exposes a readonly reactive list. Adding or removing a backend requires only editing `frameworks.json` — no TypeScript changes.

---

## API Contract Consumed

All five backends expose the same HTTP contract on their respective ports (8081–8085):

| Endpoint | Method | Use |
| --- | --- | --- |
| `/api/health` | GET | Service availability check; returns `{ status, framework }` |
| `/api/about` | GET | Returns `ArgonautInfo` with framework metadata |
| `/api/experiment/run` | POST | Accepts `ExperimentRequest`, returns `ExperimentResult` |

`ArgonautClient` (one class, parameterised by `baseUrl`) handles all three endpoints. A 5-second `AbortSignal.timeout` is applied to health and about calls; a 120-second timeout is applied to experiment runs.

---

## CORS Approach

`WebMvcConfigurer` beans were added to each module's existing `@Configuration` class. The pattern is identical across all Java modules:

```java
@Bean
WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST");
        }
    };
}
```

The Kotlin module (`KoogConfig.kt`) uses the equivalent `object : WebMvcConfigurer` expression. `allowedOrigins("*")` is intentionally permissive for the development context; this would be tightened to `http://localhost:5173` in a production deployment.

Files modified:
- `argonaut-spring-ai/src/main/java/dev/jsanca/argonaut/springai/config/SpringAiConfig.java`
- `argonaut-langchain4j/src/main/java/dev/jsanca/argonaut/langchain4j/config/LangChain4jConfig.java`
- `argonaut-langgraph4j/src/main/java/dev/jsanca/argonaut/langgraph4j/config/LangGraph4jConfig.java`
- `argonaut-embabel/src/main/java/dev/jsanca/argonaut/embabel/config/EmbabelConfig.java`
- `argonaut-koog/src/main/kotlin/dev/jsanca/argonaut/koog/config/KoogConfig.kt`

---

## Component Architecture

```
App.vue                     — root; orchestrates registry, health, experiment state
├── ExperimentPanel.vue     — question input + framework checkbox list + Run buttons
├── HealthStatus.vue        — compact dot indicators per framework + Refresh button
├── ComparisonTable.vue     — metrics comparison table (one row per run); row click selects detail
└── ResultDetail.vue        — tabbed detail for selected framework run
    ├── AnswerPanel.vue     — final answer text (pre-wrap)
    ├── EvidencePanel.vue   — evidence table (id, source, kind, score, excerpt, usedFor)
    ├── TracePanel.vue      — chronological event timeline + raw JSON toggle
    └── MetricsPanel.vue    — definition list of all ExecutionMetrics fields
```

Key composables:

- `useFrameworkRegistry` — loads `frameworks.json` once; cached; module-level reactive state
- `useExperiment` — tracks per-framework `FrameworkRun` state (`idle | running | done | error`); `runAll()` uses `Promise.allSettled()` for fault isolation

---

## Comparison Model and Metric Accuracy Caveat

The `ComparisonTable` displays `modelCalls` and `toolCalls` columns with a footnote:

> * modelCalls and toolCalls accuracy varies across frameworks; values should not be ranked.

This reflects documented implementation differences: Spring AI hardcodes `modelCalls = 1`; LangChain4j reports `0` (loop opaque); LangGraph4j and Koog are accurate via `AtomicInteger` / `handleEvents`; Embabel does not use an LLM tool loop. The UI does not highlight or sort by these columns.

---

## Verification

### Test files

| File | Tests | Coverage |
| --- | --- | --- |
| `src/__tests__/ArgonautClient.test.ts` | 8 | `health()` (200 OK, network error, non-OK); `about()` (success, non-OK); `runExperiment()` (headers/body, success, non-OK) |
| `src/__tests__/useFrameworkRegistry.test.ts` | 5 | Load populates; filters disabled; sets loaded; idempotent; error on failure |
| `src/__tests__/useExperiment.test.ts` | 6 | Initial state idle; runFramework done; runFramework error; runAll all run; runAll partial failure; reset clears |

Total: **19 Vitest tests**

`vi.resetModules()` in `beforeEach` resets module-level reactive state in composable tests. `vi.stubGlobal('fetch', ...)` is used in `ArgonautClient` tests; `vi.doMock` is used in composable tests so mocks apply to subsequent dynamic imports.

### `git diff --check`

Run prior to finalising — no whitespace errors.

---

## File Inventory

### New files in `argonaut-ui/`
- `package.json`, `tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json`
- `vite.config.ts`, `vitest.config.ts`, `index.html`
- `public/config/frameworks.json`
- `src/main.ts`, `src/style.css`, `src/App.vue`
- `src/types/argonaut.ts`
- `src/api/ArgonautClient.ts`
- `src/composables/useFrameworkRegistry.ts`, `src/composables/useExperiment.ts`
- `src/components/AnswerPanel.vue`, `EvidencePanel.vue`, `TracePanel.vue`, `MetricsPanel.vue`
- `src/components/HealthStatus.vue`, `ExperimentPanel.vue`, `ComparisonTable.vue`, `ResultDetail.vue`
- `src/__tests__/ArgonautClient.test.ts`, `useFrameworkRegistry.test.ts`, `useExperiment.test.ts`

### Modified files in backend modules
- `SpringAiConfig.java` — added `corsConfigurer()` bean
- `LangChain4jConfig.java` — added `corsConfigurer()` bean
- `LangGraph4jConfig.java` — added `corsConfigurer()` bean
- `EmbabelConfig.java` — added `corsConfigurer()` bean
- `KoogConfig.kt` — added `corsConfigurer()` bean

---

## Known Limitations

- **No `npm install` run** during this task — `node_modules/` is absent; run `npm install` in `argonaut-ui/` before `npm run dev` or `npm test`.
- **No Docker Compose** — each of the five Spring Boot services must be started manually with the correct OpenRouter env vars.
- **No persistent storage** — experiment results exist in Vue reactive state only; page refresh clears all runs.
- **`modelCalls` / `toolCalls` accuracy** — documented as a caveat in the UI; not resolved at the framework level.
- **No authentication** — all five CORS configurations use `allowedOrigins("*")`; acceptable for local development only.
- **Health check is one-shot** — health is checked on mount and on manual Refresh; no polling.
