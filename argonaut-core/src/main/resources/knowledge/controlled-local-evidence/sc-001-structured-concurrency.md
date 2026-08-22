---
id: sc-001
title: Structured Concurrency in the JVM
topic: java-concurrency
version: 1
---

# Structured Concurrency in the JVM

## What structured concurrency is

Structured concurrency is a programming model in which concurrent subtasks have a well-defined lifetime bounded by their enclosing scope. A parent task forks one or more child tasks, waits for all of them to complete, and handles their results or failures in one place. No child task outlives its parent scope.

Java's `StructuredTaskScope` (preview in Java 19, evolved through subsequent releases) provides this model. The scope is opened, subtasks are submitted, the scope is joined (blocking until subtasks complete or one fails), and results are collected within the same try-with-resources block.

## Why failure propagation matters

In unstructured concurrency, a background thread that throws an exception may silently fail, leaving the parent thread unaware. Structured concurrency changes this: if a subtask fails, the `StructuredTaskScope` can cancel the remaining subtasks and propagate the failure to the parent.

Two common scope policies are:

- **ShutdownOnFailure** — the first subtask failure cancels all remaining subtasks; the join throws the failure to the caller. Useful when all subtasks must succeed.
- **ShutdownOnSuccess** — the first subtask success cancels remaining subtasks; the join returns the winning result. Useful for racing alternative approaches.

## Relevance to agentic framework implementations

An agentic RAG run may want to execute multiple operations concurrently:

- issue two search queries in parallel and merge the results;
- read several documents simultaneously after a search;
- make parallel model calls when summarizing multiple evidence items.

Structured concurrency is a natural fit for these patterns because it keeps concurrent subtasks within a defined scope and ensures that cancellation and failure propagation are handled cleanly. If a document read fails, the scope can cancel other in-flight reads and report a single coordinated failure rather than leaking threads.

Whether a framework implementation chooses structured concurrency, a thread pool, reactive streams, or sequential calls is an implementation decision. The observable contract records what happened (searches, reads, events) but does not require a specific concurrency model.

## What this means for the first experiment

The first controlled experiment uses only local in-memory operations. Parallel execution offers no benefit for a corpus of five documents. Structured concurrency is not required for the baseline.

This document is in the corpus because future experiments — parallel searches across multiple knowledge sources, concurrent model calls — are likely candidates for structured concurrency. Understanding the model and its failure-propagation semantics will be relevant when framework implementations begin executing concurrent steps.
