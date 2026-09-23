# Agent Workspace Guide

Start with [docs/PROJECT.md](docs/PROJECT.md) for project context and [docs/OSK.md](docs/OSK.md) for the information-placement model. `docs/PROJECT.md` is partially stale (pre-implementation labels still present) — for current state use [docs/engineering/ENGINEERING_LOG.md](docs/engineering/ENGINEERING_LOG.md) and the root [README.md](README.md). Canonical knowledge lives under `docs/`; do not put shared project truth only here.

<!-- OSK:BEGIN -->

## OSK Workspace

Read:

- `docs/PROJECT.md`
- `docs/OSK.md`

<!-- OSK:END -->

## Build and test

Requires **Java 25** (`maven.compiler.release=25`, `java.version=25`). Do not lower the release. Build will fail explicitly on JDK < 25.

```bash
mvn verify                          # full build + tests (repo root)
mvn compile                         # fast compile-only check
mvn test -pl argonaut-core          # one module
mvn test -pl argonaut-core -Dtest=ExperimentRequestTest
mvn verify -DskipTests
```

`argonaut-ui/` is **not** in the Maven reactor — it has its own `package.json` (Vue 3.5 + Vite 6 + Vitest 3):

```bash
cd argonaut-ui
npm install
npm run dev                         # Vite dev server with HMR
npm run test                        # Vitest single run
npm run build                       # vue-tsc typecheck + Vite production build
```

`libs-code/` is gitignored — locally-cloned framework sources for reading, never build or import from it. `org/` is a scratch directory.

## Architecture (do not violate)

Maven multi-module (`dev.jsanca.argonaut:argonaut-parent:0.1.0-SNAPSHOT`):

| Module | Role | Port |
| --- | --- | --- |
| `argonaut-core` | Shared contracts only | — |
| `argonaut-spring-ai` | Spring AI service | 8081 |
| `argonaut-langchain4j` | LangChain4j service | 8082 |
| `argonaut-langgraph4j` | LangGraph4j service | 8083 |
| `argonaut-embabel` | Embabel service | 8084 |
| `argonaut-koog` | Koog service (Kotlin) | 8085 |
| `argonaut-vector` | Vector embedding/retrieval service (no core dep) | 8086 |
| `argonaut-decision` | Provider-independent decision-model SPI | — |
| `argonaut-decision-jev` | Jev (TypeSafe AI System One) decision provider | — |
| `argonaut-ui/` | Vue console (not a Maven module) | 3000 dev / 80 in Docker |

Common HTTP contract (implemented by all five framework services): `GET /api/health`, `GET /api/about`, `POST /api/experiment/run`. `argonaut-vector` exposes `/api/vector/store` and `/api/vector/search` instead.

**Core boundary:** share contracts, fixtures, observability structures — **never** orchestration or infra in `argonaut-core`.

Forbidden in `argonaut-core`: Spring AI / LangChain4j / LangGraph4j / Embabel deps; Lucene, Qdrant, Langfuse, LangSmith, OpenRouter; types named `Agent`, `Graph`, `Node`, `Planner`, `Workflow`.

Base package: `dev.jsanca.argonaut.core.*` (`experiment`, `evidence`, `knowledge`, `knowledge.local`, `trace`, `observability`, `metrics`, `error`, `testing`).

Observability pattern: frameworks collect via `InMemoryExecutionObserver`, then `.toTrace()` into `ExperimentResult`. Use `CompositeExecutionObserver` for future exporters (Langfuse, LangSmith, OpenTelemetry).

## Controlled evidence corpus

Corpus documents live at `argonaut-core/src/main/resources/knowledge/controlled-local-evidence/`. Each `.md` file requires a YAML frontmatter block with at least `id` and `title`:

```
---
id: exp-001
title: Some Title
topic: optional-topic
version: 1
---
```

`LocalKnowledgeCorpus.demo()` / `LocalKnowledgeRepository.withDemoCorpus()` loads these from the classpath. Framework config classes wire the demo corpus via `LocalKnowledgeRepository.withDemoCorpus()` to expose a `KnowledgeRepository` bean. The primary required evidence document is `exp-001`. A canonical mirror lives under `docs/knowledge/corpus/controlled-local-evidence/` (kept in sync, used as the source of truth for documentation).

Mission/use case: `docs/knowledge/use-cases/controlled-local-evidence-rag.md`. Contract rationale: `docs/knowledge/common-contract.md`.

## Docker Compose

Root `compose.yaml` builds and runs all 5 framework services + Qdrant + `argonaut-vector` + the nginx-hosted UI:

```bash
cp .env.example .env        # populate OPENROUTER_API_KEY and OPENROUTER_MODEL
docker compose up --build
# UI on http://localhost:8080
```

`Dockerfile.backend` is one reusable multi-stage build (Maven build → JRE runtime). Browser hits nginx; nginx routes `/api/frameworks/<id>/...` to the right backend. Provider credentials live only in backend containers.

`argonaut-vector/` ships its own `compose.yaml` (vector + Qdrant only) for working on vector features in isolation. On an Intel Mac, ONNX tokenizer tests require `linux-x86_64` — use Docker (linux/amd64 containers) instead of running on the host.

## Env

Copy `.env.example` → `.env` (gitignored). Keys: `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, `OPENROUTER_BASE_URL`, optional `*_PORT` overrides, `ARGONAUT_UI_PORT`. Never commit secrets.

Unit tests in framework modules use mock chat models — no real LLM call or network is required. To run a framework service locally against a real model, export the OpenRouter env vars before starting it.

## Documentation placement

| Type | Path |
| --- | --- |
| Durable knowledge | `docs/knowledge/` |
| Engineering reports / evidence | `docs/engineering/` (agent artifacts under `docs/engineering/agents/`) |
| Active eng log | `docs/engineering/ENGINEERING_LOG.md` — add a dated entry after non-trivial work |
| ADRs | `docs/adr/` (actual path; `docs/OSK.md` mentions `docs/decisions/adr/` but that does not exist) |
| Roadmap / future | `docs/roadmap/` |

Skills: `.osk/skills/` is authoritative; tool copies under `.claude/skills/` and `.opencode/skills/` are adapters.

<!-- OSK:SKILLS:BEGIN -->

## OSK Installed Skills

- [Architecture Review](.osk/skills/osk-architecture-review/SKILL.md)
- [Architecture Boundary Review](.osk/skills/osk-boundary-review/SKILL.md)
- [Engineering Reporting](.osk/skills/osk-engineering-reporting/SKILL.md)
- [Execution Observability](.osk/skills/osk-execution-observability/SKILL.md)
- [Execution Timebox](.osk/skills/osk-execution-timebox/SKILL.md)
- [Verification Engineering](.osk/skills/osk-verification-engineering/SKILL.md)
- [Knowledge Curator](.osk/skills/osk-knowledge-curator/SKILL.md)
- [Knowledge Integrity Review](.osk/skills/osk-knowledge-integrity-review/SKILL.md)
- [Adversarial Analysis](.osk/skills/osk-adversarial-analysis/SKILL.md)
- [Agent Harness Guide](.osk/skills/osk-agent-harness-guide/SKILL.md)
- [Code Docs](.osk/skills/osk-code-docs/SKILL.md)
<!-- OSK:SKILLS:END -->
