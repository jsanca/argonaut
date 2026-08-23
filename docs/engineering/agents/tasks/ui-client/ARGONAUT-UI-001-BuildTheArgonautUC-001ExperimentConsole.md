# ARGONAUT-UI-001
## Build the Argonaut UC-001 Experiment Console

### Assignee

Clio

### Type

Frontend / experiment visualization

---

# Objective

Create the first Argonaut UI.

The UI is not a general AI chat application.

It is an experiment console that allows a user to execute the same Argonaut
use case against multiple framework implementations and compare their
observable behavior.

Current framework services:

- Spring AI
- LangChain4j
- LangGraph4j
- Embabel
- Koog

All services expose the same Argonaut HTTP contract but run independently on
different endpoints.

The UI must remain unaware of framework-specific implementation details.

---

# Architectural principle

The frontend communicates only with the shared Argonaut service contract.

Conceptually:

                         Argonaut UI
                              |
                     Framework Registry
                              |
          +-------------------+-------------------+
          |                   |                   |
      Spring AI           LangChain4j         LangGraph4j
          |                   |                   |
          +-------------------+-------------------+
                              |
                       Embabel / Koog

Each framework implementation is an independent service.

The UI selects one or more service endpoints and sends the same experiment
request to each.

---

# Runtime framework registry

Introduce a minimal runtime configuration describing available framework
services.

Do NOT hard-code framework ports throughout application components.

Preferred shape:

    public/config/frameworks.json

Example:

    {
      "frameworks": [
        {
          "id": "spring-ai",
          "name": "Spring AI",
          "baseUrl": "http://localhost:8081",
          "enabled": true
        },
        {
          "id": "langchain4j",
          "name": "LangChain4j",
          "baseUrl": "http://localhost:8082",
          "enabled": true
        },
        {
          "id": "langgraph4j",
          "name": "LangGraph4j",
          "baseUrl": "http://localhost:8083",
          "enabled": true
        },
        {
          "id": "embabel",
          "name": "Embabel",
          "baseUrl": "http://localhost:8084",
          "enabled": true
        },
        {
          "id": "koog",
          "name": "Koog",
          "baseUrl": "http://localhost:8085",
          "enabled": true
        }
      ]
    }

Before finalizing exact ports, verify them against repository configuration.

The registry must be loaded at runtime so Docker/local environments can supply
different service addresses without rebuilding the frontend.

Do not build a dynamic service-discovery mechanism.

Do not introduce Consul, Kubernetes discovery, API Gateway, etc.

---

# Shared API contract

Inspect the existing service controllers and core request/result contracts.

At minimum the UI should understand:

    GET  /api/health
    GET  /api/about
    POST /api/experiment/run

Do not create framework-specific frontend APIs.

Create one shared client abstraction such as conceptually:

    ArgonautFrameworkClient

that receives:

    framework endpoint

and speaks:

    ExperimentRequest
    ExperimentResult
    health/about contracts

Do not create:

    SpringAiClient
    LangChain4jClient
    KoogClient
    ...

unless repository evidence shows a genuine protocol difference.

---

# Primary user experience

Create an experiment-console workflow.

## Experiment input

Provide:

- UC identifier / title;
- question input;
- framework selector;
- Run button;
- Run All capability.

The initial use case is:

    UC-001 — Controlled Local Evidence

The UI does not need dynamic use-case discovery in v0.1.

---

# Single-framework execution

The user can select one framework and run UC-001.

Show at minimum:

- framework;
- status;
- final answer;
- evidence;
- execution trace;
- execution metrics.

---

# Run All

Provide a prominent:

    Run All

action.

It sends the SAME request/question to all currently enabled framework
services.

Execute requests independently.

A failure from one framework must not prevent results from the others from
being displayed.

The UI should make comparative results easy to inspect.

---

# Comparison view

The important product is comparison, not chat.

Design a compact comparison summary with approximately:

| Framework | Status | Duration | Model calls | Searches | Reads | Evidence |
|-----------|--------|----------|-------------|----------|-------|----------|

Use actual `ExecutionMetrics` fields from Argonaut.

IMPORTANT:

The UC-001 comparative milestone established that some current metrics are not
equally trustworthy across frameworks.

In particular:

    modelCalls
    toolCalls

may be exact, inferred, unknown, or currently incomplete.

Do not visually imply false precision.

If the API does not currently expose metric provenance, display the values
without ranking frameworks by those values and document the limitation.

Do NOT redesign the core metric contract in this task.

---

# Result detail

Selecting a framework result should expose three primary views:

## Answer

Render the final answer clearly.

Markdown rendering is acceptable if the existing output requires it.

## Evidence

Show each evidence item with useful fields such as:

- evidence ID;
- source ID;
- kind;
- excerpt;
- relevance/score when present.

Evidence provenance should be visually easy to understand.

## Trace

Render the execution trace chronologically.

Prefer a readable timeline over raw JSON.

Conceptually:

    RUN_STARTED
        |
    KNOWLEDGE_SEARCH_STARTED
        |
    KNOWLEDGE_SEARCH_COMPLETED
        |
    DOCUMENT_READ
        |
    EVIDENCE_RETRIEVED
        |
    ANSWER_SYNTHESIZED
        |
    RUN_COMPLETED

Use actual event data rather than assuming every framework has exactly this
sequence.

Provide raw JSON only as an optional/debug view if useful.

---

# Health state

Use `/api/health` to determine whether configured framework services are
reachable.

Show a compact service state:

    Spring AI       available
    LangChain4j     available
    LangGraph4j     unavailable
    Embabel         available
    Koog            available

The UI should remain usable when only some frameworks are running.

Do not continuously poll aggressively.

A check on application load plus explicit refresh is sufficient for v0.1.

---

# About metadata

Inspect `/api/about`.

Where useful, use returned metadata rather than duplicating framework
information in frontend code.

The registry owns connection information.

The service owns framework/runtime information.

Keep those responsibilities distinct.

---

# Visual direction

Argonaut is an engineering laboratory.

The UI should feel like:

    experiment console
    engineering workbench
    observability interface

not:

    consumer chatbot
    marketing landing page

Favor:

- information density;
- readable typography;
- clear hierarchy;
- code/trace readability;
- restrained visual design;
- useful tables;
- side-by-side comparison.

Avoid excessive gradients, decorative cards, animation, or AI-chat clichés.

---

# Suggested layout

A possible structure:

    ---------------------------------------------------------
    ARGONAUT
    AI Framework Experiment Lab

    UC-001 Controlled Local Evidence

    Question
    [......................................................]

    Frameworks
    [x] Spring AI
    [x] LangChain4j
    [x] LangGraph4j
    [x] Koog
    [x] Embabel

                     [ Run Selected ] [ Run All ]
    ---------------------------------------------------------

    COMPARISON

    Framework | Status | Duration | Calls | Search | Reads | Evidence

    ---------------------------------------------------------

    RESULT DETAIL

       Answer | Evidence | Trace | Metrics

    ---------------------------------------------------------

This is guidance, not a mandated pixel layout.

Use good frontend judgment.

---

# Technology selection

Inspect the repository first.

If no frontend stack already exists, prefer a small, maintainable web
application compatible with the existing project development workflow.

Do not introduce a heavy frontend architecture merely for this console.

Document the technology choice and rationale before substantial
implementation if one must be selected.

---

# CORS

Because the development UI will call services running on different localhost
ports, verify existing CORS behavior.

If CORS configuration is required:

- make the smallest development-appropriate backend change;
- keep it consistent across framework services;
- avoid duplicating arbitrary framework-specific policies.

If changing all backend modules would create significant scope, document the
issue before proceeding rather than performing a broad refactor.

---

# Failure handling

Handle independently:

- framework offline;
- request timeout;
- HTTP failure;
- malformed response;
- completed experiment;
- failed experiment result.

One failing implementation must not break the comparison screen.

---

# State

Keep frontend state simple.

The application needs roughly:

    framework registry
    framework availability
    current experiment input
    execution state per framework
    results per framework
    selected result

Do not introduce a global state framework unless complexity actually
requires it.

---

# Testing

At minimum provide tests for:

- framework registry loading;
- shared API client;
- single-framework result handling;
- partial framework failure;
- Run All aggregation;
- comparison rendering for representative results.

Do not pursue exhaustive browser automation in v0.1 unless the selected
frontend stack makes it trivial.

---

# Documentation

Create an engineering report:

    docs/engineering/agents/reports/
    ARGONAUT-UI-001-experiment-console.md

Document:

- UI architecture;
- selected frontend technology;
- framework registry contract;
- API contract consumed;
- comparison model;
- screenshots or visual description;
- verification performed;
- known limitations;
- Docker/runtime configuration implications.

Update the engineering log.

---

# Explicit non-goals

DO NOT:

- implement Docker Compose;
- introduce Kubernetes;
- introduce service discovery infrastructure;
- introduce API Gateway/BFF unless an unavoidable blocker is proven;
- modify framework AI implementations;
- redesign ExperimentResult;
- redesign ExecutionMetrics;
- implement UC-002;
- add authentication;
- add persistent experiment storage;
- add user accounts;
- create a general chat UI;
- redesign Argonaut architecture.

---

# Definition of done

The task is complete when:

1. Argonaut has a usable experiment-console UI.
2. Framework endpoints come from runtime configuration.
3. The same UC-001 request can be sent to any configured implementation.
4. Run All can execute against all available implementations.
5. Partial framework failure is handled cleanly.
6. Answers, evidence, traces and metrics can be inspected.
7. Comparison does not falsely imply that currently approximate metrics are
   exact.
8. Relevant tests pass.
9. The engineering report records the result.
10. `git diff --check` passes.

Do not commit unless explicitly authorized.

STOP THERE.