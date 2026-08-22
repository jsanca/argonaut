---
id: vt-001
title: Virtual Threads and Blocking I/O in Java
topic: java-concurrency
version: 1
---

# Virtual Threads and Blocking I/O in Java

## What virtual threads are

Virtual threads, introduced as a preview in Java 19 and finalized in Java 21, are lightweight threads managed by the JVM rather than mapped one-to-one to OS threads. The JVM scheduler mounts virtual threads onto a small pool of OS threads (carrier threads) and unmounts them when they block.

This means a Java application can have millions of virtual threads in flight simultaneously, far more than a typical OS-thread-per-request model allows.

## When virtual threads help

Virtual threads are most useful for workloads that spend time waiting on blocking I/O: network calls, database queries, file reads, or HTTP requests to external services. When a virtual thread blocks on I/O, the JVM unmounts it from its carrier thread and parks it. The carrier thread is then free to run another virtual thread. No thread pool starvation occurs even when many requests block simultaneously.

For CPU-bound workloads, virtual threads offer no advantage over platform threads. The benefit is specifically about I/O wait.

## Relevance to Argonaut framework implementations

Agentic RAG frameworks make I/O-heavy calls: LLM API requests, knowledge store queries, embedding model invocations, and potentially parallel tool calls. These are exactly the workloads where virtual threads can reduce latency and increase throughput.

Each framework implementation in Argonaut may handle concurrency differently:

- Spring AI may configure a virtual-thread executor for its reactive or imperative model calls;
- LangChain4j may use virtual threads for parallel tool execution;
- LangGraph4j may run graph node transitions on virtual threads;
- Embabel may dispatch actions on a virtual-thread-backed pool.

Whether a framework uses virtual threads for I/O operations is an implementation detail that may affect observed execution duration in `ExecutionMetrics.durationMs`. It is not the primary variable under study in the first experiment but may be relevant context when interpreting timing differences between framework runs.

## What this means for the first experiment

The first controlled experiment uses only local in-memory operations. There are no blocking I/O calls to external services, no LLM API calls, and no database queries. Virtual threads provide no measurable benefit for the initial baseline.

This document is included in the controlled corpus because future experiments will introduce real I/O: OpenRouter LLM calls, external knowledge stores, and Langfuse/LangSmith exporters. When that happens, the choice of executor and threading model becomes a relevant variable in execution duration metrics.
