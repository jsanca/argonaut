# Argonaut

> Same mission. Same model. Same evidence. Different agentic frameworks.

Argonaut is a comparative laboratory that runs the same agentic mission through four distinct Java frameworks while keeping every external variable as controlled as practical.

The experiment asks: given identical inputs, identical controlled evidence, and identical evaluation expectations, how do different frameworks naturally express the same intelligent behavior?

## Frameworks Under Comparison

| Framework | Service Port |
| --- | --- |
| Spring AI | 8081 |
| LangChain4j | 8082 |
| LangGraph4j | 8083 |
| Embabel | 8084 |

A fifth implementation — **Hybrid** — is reserved for after the first four have produced evidence. Its design will be informed by observed strengths and weaknesses, not predetermined.

## What Stays Constant

Each implementation receives:

- the same mission
- the same input
- the same external model (via OpenRouter)
- the same controlled evidence corpus
- the same conceptual tools and capabilities
- the same observable HTTP API contract
- the same evaluation expectations

## What Is Intentionally Allowed to Vary

Each framework expresses internal orchestration using its own idiomatic abstractions. A capability such as knowledge search may appear as a tool in Spring AI and LangChain4j, a node-tool interaction in LangGraph4j, and an action or equivalent construct in Embabel.

The comparison is about how each framework naturally implements the same behavioral contract — not about forcing them into a shared orchestration model.

## Architecture

Argonaut is a Maven multi-module project targeting Java 25. Each framework runs as an independent service. A Vue.js experiment console provides a framework-agnostic view of all running implementations.

### Intended Module Layout

```
argonaut-core           — shared contracts, evidence structures, execution traces, fixtures, evaluation utilities
argonaut-spring-ai      — Spring AI implementation (port 8081)
argonaut-langchain4j    — LangChain4j implementation (port 8082)
argonaut-langgraph4j    — LangGraph4j implementation (port 8083)
argonaut-embabel        — Embabel implementation (port 8084)
argonaut-ui             — Vue.js experiment console
```

`argonaut-core` holds only portable experiment concerns: API request/response contracts, shared evidence structures, execution trace structures, basic metrics, common fixtures, and evaluation utilities. It is not a framework abstraction layer.

> **Rule:** Share contracts, tools, fixtures, and observability structures. Do not share orchestration semantics.

Concepts such as `Agent`, `Graph`, `Node`, `Planner`, and `Workflow` belong to the framework implementations, not to core.

## Common HTTP Contract

Every framework service exposes the same minimal HTTP API:

```
GET  /api/health
GET  /api/about
POST /api/experiment/run
```

An experiment run response includes enough information to compare executions: framework identity, final result, evidence used, execution trace, basic metrics, and status. The contract will be defined in `argonaut-core`.

## Experiment Console

The Vue.js UI is an observer and launcher, not a framework-specific client. It can:

- select a single Argonaut implementation and run the experiment against it
- run the same experiment against all available implementations simultaneously
- inspect the execution trace, evidence gathered, final result, and basic metrics for each run

The frontend consumes the same HTTP contract from every service. It may know endpoint locations but contains no framework-specific business logic.

## Agentic Mission

All implementations execute the same mission. The mission is rich enough to exercise multi-step execution, tool or capability invocation, state or execution context, evidence gathering, decision making, termination, and an observable execution trace — while remaining bounded enough that four implementations can be reasonably compared.

The detailed mission definition lives in [`docs/knowledge/`](docs/knowledge/).

## Controlled Evidence

The first experiment uses a controlled local evidence corpus rather than unrestricted web search. Same documents, same search capabilities, same question, same model — controlled conditions reduce experimental noise. Later iterations may introduce external RAG or web search.

## Observability

Observability is part of the experiment, not optional decoration. Each execution trace exposes:

- which capabilities were invoked and in what order
- what evidence was gathered
- how execution progressed through the mission
- what conclusion was reached
- basic comparable metrics: duration, model calls, tool calls, steps, retries, and final status

## Running Locally (intended)

The project will provide a Docker Compose environment that starts all services and the Vue UI together. Configuration lives in `.env` (never committed). A versioned template captures the expected structure:

```
# .env.example
OPENROUTER_API_KEY=
OPENROUTER_MODEL=
```

Additional runtime configuration per framework will be documented there as implementations are built.

## Project Documentation

| Area | Location | Purpose |
| --- | --- | --- |
| Durable knowledge | [`docs/knowledge/`](docs/knowledge/) | Experiment design, mission definition, framework concepts, shared contract semantics, evaluation model |
| Engineering history | [`docs/engineering/`](docs/engineering/) | Implementation reports, experiment runs, technical investigations, validation evidence |
| Architecture decisions | [`docs/adr/`](docs/adr/) | Long-lived architectural choices and their rationale |
| Roadmap | [`docs/roadmap/`](docs/roadmap/) | Committed direction and deferred future ideas |

## Results and Article

Experiment results, comparative conclusions, and the final article will appear here after all four implementations have been executed and evaluated against the same conditions.

No winner is predetermined. The Hybrid implementation, if it materializes, will be designed from observed evidence — not from assumptions made before the experiment runs.
