---
id: obs-001
title: Observability Fundamentals for Agentic Systems
topic: observability
version: 1
---

# Observability Fundamentals for Agentic Systems

## Why observability matters in an agentic experiment

An agentic system executes a sequence of decisions and tool calls that are not directly visible from inputs and outputs alone. When comparing four frameworks executing the same mission, the final answer is insufficient evidence that the frameworks behaved comparably. A human evaluator needs to see what happened during execution: which searches were issued, which documents were read, which evidence was selected, and in what order.

Argonaut records normalized execution traces so that this internal behavior is visible and comparable across frameworks without requiring access to each framework's internal state.

## Trace events make steps visible

Every significant step in an Argonaut run is recorded as an `ExecutionEvent`. An event carries:

- a type from `ExecutionEventType` (e.g. `KNOWLEDGE_SEARCH_STARTED`, `DOCUMENT_READ_COMPLETED`, `EVIDENCE_SELECTED`);
- a timestamp;
- an actor (the component that emitted the event);
- a summary description;
- optional metadata and duration.

The event type is a fixed vocabulary shared across all framework implementations. A search event in Spring AI has the same type as a search event in LangChain4j. This uniformity is what makes the traces comparable.

## Comparable traces enable cross-framework evaluation

The goal of Argonaut's experiment is to compare framework orchestration behavior. Comparable traces mean that an evaluator can answer questions such as:

- Did both frameworks issue the same number of searches?
- Did one framework read more documents than another?
- Did both frameworks select `exp-001` as a primary evidence source?
- Which framework's trace shows a longer document-read phase?

These questions are answerable only if all four implementations emit events using the same type vocabulary and the same event boundaries. The `ExecutionObserver` contract enforces this: frameworks call `observer.record(event)` with events typed from the shared enum.

## External exporters are optional adapters

Argonaut captures execution events through `ExecutionObserver`. The in-memory implementation (`InMemoryExecutionObserver`) is sufficient for the baseline experiment. It accumulates events and converts them to an `ExecutionTrace` at the end of the run.

External observability tools — Langfuse, LangSmith, OpenTelemetry collectors — are future adapters. They receive events from the observer and export them to an external platform. Adding them does not change the observable contract; it adds a forwarding layer.

External exporters are not required for the baseline verification. Requiring them would:

- introduce external service dependencies that make the experiment harder to run locally;
- conflate the experiment's observable contract with a specific telemetry vendor;
- require credentials that may not be available in every environment.

The controlled baseline must be verifiable without any external exporter. External exporters are an opt-in enhancement once the baseline is stable.

## The source of truth for observability

The `ExecutionTrace` attached to the `ExperimentResult` is the source of truth for what happened during a run. It is not derived from Langfuse, LangSmith, or any other external tool. It is the canonical record produced by the framework implementation itself.

This means:
- the experiment can be run and verified in a fully offline environment;
- test assertions can inspect the trace directly without querying an external service;
- if an external exporter fails or is misconfigured, the experiment result is unaffected.

## What the trace must contain for the baseline

The minimum trace required for the first controlled experiment includes, in logical order:

- `RUN_STARTED` — marks the beginning of the run;
- `KNOWLEDGE_SEARCH_STARTED` / `KNOWLEDGE_SEARCH_COMPLETED` — at least one search pair;
- `DOCUMENT_READ_STARTED` / `DOCUMENT_READ_COMPLETED` — at least one read pair;
- `EVIDENCE_RETRIEVED` — at least one candidate evidence item identified;
- `EVIDENCE_SELECTED` — at least one evidence item selected for the answer;
- `ANSWER_SYNTHESIZED` — the final answer has been produced;
- `RUN_COMPLETED` — marks the end of the run.

A framework implementation that returns an answer without emitting these events has not satisfied the observable contract, regardless of answer quality.
