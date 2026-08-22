---
id: exp-001
title: Controlled Evidence in AI Framework Experiments
topic: experiment-design
version: 1
---

# Controlled Evidence in AI Framework Experiments

## Why controlled evidence comes first

Argonaut compares four agentic AI frameworks executing the same mission. A fair comparison requires that each framework receives the same information. If retrieval quality varies — because one framework uses a different search engine, a different embedding model, or a different index — then differences in the final answer may reflect retrieval differences, not orchestration differences.

Controlled local evidence eliminates retrieval variability. Every framework reads from the same in-memory corpus, using the same search interface, before any model call is made. When answers differ, the difference can be attributed to how each framework orchestrates the retrieval-synthesis loop, not to what each framework retrieved.

## What controlled evidence means

Controlled evidence is source material that is:

- fixed: the corpus does not change between runs;
- local: no external service, vector database, or web request is involved;
- shared: all framework implementations use the same corpus and the same retrieval contract;
- deterministic: the same query returns the same ranked results on every run.

These properties ensure that the experiment measures framework orchestration behavior rather than infrastructure variability.

## Why external retrieval is deferred

Web search, vector databases (Qdrant, OpenSearch, pgvector), and external observability tools each introduce sources of variability:

- Web search results change over time and differ by region or session.
- Vector search results depend on the embedding model, the index parameters, and chunking strategy.
- External observability exporters introduce latency and require credentials.

Introducing any of these before the baseline is stable would make it impossible to determine whether a difference in results came from the framework or from the infrastructure.

The correct sequence is: establish a stable baseline with controlled local evidence first, then introduce external systems one at a time with the ability to compare against the baseline.

## Why reproducibility matters for framework comparison

Reproducible evidence means the same question produces the same retrievable documents on every run, in the same order. This has two benefits:

First, automated tests can assert specific evidence source IDs. A test that asserts `exp-001` appears in the evidence list is stable only if retrieval is deterministic. Non-deterministic retrieval makes such assertions flaky.

Second, human evaluators can compare results across frameworks without worrying that differing answers stem from differing retrievals. When controlled evidence is in place, two frameworks that produce different answers received the same input material. The orchestration is the variable under study.

## How this applies to Argonaut

Argonaut's first experiment uses a five-document controlled corpus loaded entirely from local memory. All framework implementations access this corpus through the `KnowledgeRepository` interface using identical search and read calls. The corpus does not change between runs. The search algorithm is deterministic.

This controlled baseline makes it possible to:

- verify that each framework can perform multi-step retrieval and synthesis;
- confirm that the observable trace contract captures search, read, evidence selection, and answer synthesis events;
- establish a reference result that future experiments with external retrieval can be compared against.

External retrieval, embedding similarity, vector databases, and live web search are deferred until this controlled baseline is verified across all four framework implementations.
