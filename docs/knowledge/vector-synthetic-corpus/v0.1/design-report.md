# Design Report — Vector Synthetic Corpus v0.1

## Purpose and scope

Corpus v0.1 is a 30-document, 25-query retrieval laboratory for technical software-engineering material. It is deliberately small enough for manual audit and dense enough to expose strategy differences. The corpus makes no claim about generation quality, answer correctness, or a winning vector database.

## Challenges designed first

The initial design inventory contained: HNSW algorithm versus HNSW property lookup; probe concepts versus probe configuration; cosine normalization versus dimension mismatch; HTTP duplicate prevention versus message duplicate delivery; and a rollout incident split across explanation and troubleshooting documents. Documents and qrels were then written to make these distinctions inspectable.

## Intentional traps

- **Exact property against semantic richness:** `Q002` should rank `DOC-002` first even though `DOC-001`, `DOC-003`, and `DOC-020` are richer HNSW discussions.
- **Near duplicates with different scope:** `DOC-001`, `DOC-003`, and `DOC-020` share HNSW terminology but respectively explain the algorithm, compare index choices, and tune query breadth. `Q001` and `Q017` separate those intents.
- **Same words, incompatible meanings:** `DOC-011` and `DOC-019` both concern duplicate effects, but HTTP idempotency and at-least-once messaging are not interchangeable. `DOC-027` uses navigation language that should not displace REST pagination with HNSW material.
- **Chunk sensitivity:** `DOC-010` is written so a heading, condition, warning, and remediation should remain retrievable as one unit. `Q008` and `Q019` make an unsuitable chunk policy visible.
- **Distributed diagnosis:** `Q020` has two essential documents because a useful retrieval set contains both the readiness/index-restore failure mechanism and the troubleshooting sequence.
- **No-answer behavior:** `Q025` has no relevant document; a retrieved topical document is a false positive, not weak relevance.

## Relevance policy

The qrels were assigned by corpus design, not by an LLM or observed similarity scores. Grade 3 identifies the evidence expected to satisfy the retrieval intent. Grade 2 is highly useful supporting evidence. Grade 1 shares meaningful context but is insufficient on its own. The absence of a qrels pair means relevance 0.

## Limits

This corpus is a controlled diagnostic fixture, not a representative documentation collection or a benchmark for broad web search. It does not set thresholds, mandate chunk size, or evaluate result snippets. A retrieval implementation must record its own model, analyzer, chunking, index, candidate-pool, and parent-document reduction settings so results remain interpretable.
