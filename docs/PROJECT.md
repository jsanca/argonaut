# Project Context

## Mission

Argonaut is a comparative laboratory for agentic AI frameworks. It runs the same mission through Spring AI, LangChain4j, LangGraph4j, and Embabel — keeping the external model, controlled evidence, conceptual tools, observable API contract, and evaluation conditions as consistent as practical — to produce honest, evidence-based comparison of how each framework naturally implements the same intelligent behavior.

## Scope

### In scope

- Four framework implementations executing the same agentic mission: Spring AI, LangChain4j, LangGraph4j, Embabel.
- A shared HTTP contract (`GET /api/health`, `GET /api/about`, `POST /api/experiment/run`) implemented identically by each service.
- A portable `argonaut-core` module containing API contracts, evidence structures, execution trace structures, basic metrics, common fixtures, and evaluation utilities.
- A Vue.js experiment console that can run one or all implementations and compare observable results.
- A Docker Compose development environment covering all services and the UI.
- OpenRouter as the shared model provider for all framework implementations.
- Controlled local evidence corpus for the initial experiment.
- Observability sufficient to compare executions: capability invocations, evidence gathered, execution trace, duration, model calls, tool calls, steps, retries, and status.

### Out of scope

- Shared orchestration abstractions across frameworks. Concepts such as `Agent`, `Graph`, `Node`, `Planner`, and `Workflow` belong to the respective implementations, not to core.
- Predetermining a winner before the experiment has run.
- A fifth Hybrid implementation until evidence from the first four exists.
- Unrestricted web research in the initial experiment (controlled evidence only).

## Current State

Maven multi-module skeleton in place (ARGONAUT-002). All five Java modules compile cleanly against Java 25. No framework dependencies or implementation logic yet. Vue UI directory exists as a placeholder. Framework implementations, common HTTP contract, Docker Compose, and OpenRouter integration are next.

## Architecture

Intended: Maven multi-module project targeting Java 25. Each framework runs as an independent service on its assigned port. See [`../README.md`](../README.md) for the intended module layout and port assignments.

Architecture decisions will be recorded in [`adr/`](adr/).

Current architecture knowledge: Unknown (pre-implementation).

## Technology

- **Language / Runtime:** Java 25
- **Build:** Maven 3.9.x multi-module (`mvn verify` from repo root)
- **Frameworks under comparison:** Spring AI, LangChain4j, LangGraph4j, Embabel
- **Frontend:** Vue.js
- **Model provider:** OpenRouter
- **Development environment:** Docker Compose (intended)
- **Build/test commands:** Unknown (pre-implementation)

## Repository Map

```
argonaut-core/          — shared contracts and experiment utilities (intended)
argonaut-spring-ai/     — Spring AI service (intended)
argonaut-langchain4j/   — LangChain4j service (intended)
argonaut-langgraph4j/   — LangGraph4j service (intended)
argonaut-embabel/       — Embabel service (intended)
argonaut-ui/            — Vue.js experiment console (intended)
docs/knowledge/         — durable experiment concepts and mission definition
docs/engineering/       — implementation reports and engineering history
docs/adr/               — architecture decision records
docs/roadmap/           — committed direction and future ideas
.osk/skills/            — installed OSK skills (authoritative; do not duplicate)
```

## Getting Started

Docker Compose startup (intended; not yet implemented):

```bash
cp .env.example .env
# populate OPENROUTER_API_KEY and OPENROUTER_MODEL
docker compose up
```

Individual services and the Vue UI will be documented in their respective module READMEs once created.

## Important References

- [Workspace operating guide](OSK.md)
- [Root README](../README.md) — project overview and quick orientation
- [Experiment knowledge](knowledge/) — mission definition, framework concepts, evaluation model
- [Engineering log](engineering/ENGINEERING_LOG.md)
- [Architecture decisions](adr/)
- [Roadmap](roadmap/ROADMAP.md)
