# Agent Workspace Guide

Start with [docs/PROJECT.md](docs/PROJECT.md), then [docs/OSK.md](docs/OSK.md) before changing workspace docs. Canonical knowledge lives under `docs/`; do not put shared project truth only here.

<!-- OSK:BEGIN -->

## OSK Workspace

Read:

- `docs/PROJECT.md`
- `docs/OSK.md`

<!-- OSK:END -->

## Build and test

Requires **Java 25** (`maven.compiler.release=25`). Do not lower the release.

```bash
mvn verify                          # full build + tests (repo root)
mvn compile                         # fast compile-only check
mvn test -pl argonaut-core          # one module
mvn test -pl argonaut-core -Dtest=ExperimentRequestTest
mvn verify -DskipTests
```

`argonaut-ui` is **not** in the Maven reactor (placeholder only; no `package.json` yet).

No Docker Compose, CI workflows, or runnable framework services yet — framework modules depend on `argonaut-core` only.

## Architecture (do not violate)

Maven multi-module (`dev.jsanca.argonaut:argonaut-parent:0.1.0-SNAPSHOT`):

| Module | Role | Port |
| --- | --- | --- |
| `argonaut-core` | Shared contracts only | — |
| `argonaut-spring-ai` | Spring AI service (skeleton) | 8081 |
| `argonaut-langchain4j` | LangChain4j (skeleton) | 8082 |
| `argonaut-langgraph4j` | LangGraph4j (skeleton) | 8083 |
| `argonaut-embabel` | Embabel (skeleton) | 8084 |
| `argonaut-ui/` | Vue console (not a Maven module) | 3000 |

**Core boundary:** share contracts, fixtures, observability structures — **never** orchestration or infra in `argonaut-core`.

Forbidden in `argonaut-core`: Spring AI / LangChain4j / LangGraph4j / Embabel deps; Lucene, Qdrant, Langfuse, LangSmith, OpenRouter; types named `Agent`, `Graph`, `Node`, `Planner`, `Workflow`.

Base package: `dev.jsanca.argonaut.core.*` (`experiment`, `evidence`, `knowledge`, `knowledge.local`, `trace`, `observability`, `metrics`, `error`).

Intended HTTP contract (not implemented yet): `GET /api/health`, `GET /api/about`, `POST /api/experiment/run`.

Observability pattern: frameworks collect via `InMemoryExecutionObserver`, then `.toTrace()` into `ExperimentResult`. Prefer `CompositeExecutionObserver` for future exporters.

## Controlled evidence corpus

| Location | Purpose |
| --- | --- |
| `docs/knowledge/corpus/controlled-local-evidence/` | Canonical corpus (5 docs: exp-001, rag-001, obs-001, vt-001, sc-001) |
| `argonaut-core/src/test/resources/knowledge/controlled-local-evidence/` | Test fixture copy — keep in sync when corpus changes |

Loaders: `MarkdownCorpusLoader.fromClasspath(...)` / `fromDirectory(...)`. `LocalKnowledgeCorpus.demo()` is programmatic and independent of files.

Mission/use case: `docs/knowledge/use-cases/controlled-local-evidence-rag.md`. Contract rationale: `docs/knowledge/common-contract.md`.

## Env

Copy `.env.example` → `.env` (gitignored). Keys: `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, optional port overrides. Never commit secrets.

## Documentation placement

| Type | Path |
| --- | --- |
| Durable knowledge | `docs/knowledge/` |
| Engineering reports / evidence | `docs/engineering/` (agent artifacts under `docs/engineering/agents/`) |
| Active eng log | `docs/engineering/ENGINEERING_LOG.md` — add a dated entry after non-trivial work |
| ADRs | `docs/adr/` (actual path; not `docs/decisions/adr/`) |
| Roadmap / future | `docs/roadmap/` |

Skills: `.osk/skills/` is authoritative; tool copies under `.claude/skills/` and `.opencode/skills/` are adapters.

<!-- OSK:SKILLS:BEGIN -->

## OSK Installed Skills

- [Architecture Review](.osk/skills/osk-architecture-review/SKILL.md)
- [Architecture Boundary Review](.osk/skills/osk-boundary-review/SKILL.md)
- [Engineering Reporting](.osk/skills/osk-engineering-reporting/SKILL.md)
- [Execution Observability](.osk/skills/osk-execution-observability/SKILL.md)
- [Verification Engineering](.osk/skills/osk-verification-engineering/SKILL.md)
- [Knowledge Curator](.osk/skills/osk-knowledge-curator/SKILL.md)
- [Adversarial Analysis](.osk/skills/osk-adversarial-analysis/SKILL.md)
- [Agent Harness Guide](.osk/skills/osk-agent-harness-guide/SKILL.md)
<!-- OSK:SKILLS:END -->
