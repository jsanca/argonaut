# Project Knowledge

## Purpose

Store durable understanding of what the project is, how its domain works, and the concepts needed to change it safely.

## What belongs here

Domain definitions, actors, entities, workflows, business rules, terminology, conceptual architecture, external-system relationships, and other enduring project concepts.

**Example:** Put “an Order has three terminal states” in `order-lifecycle.md`, even when that fact was discovered during an implementation task.

## What does not belong here

Task reports, implementation logs, review results, temporary investigation notes, Architecture Decision Records, future plans, or unverified conclusions. Keep execution history in `../engineering/`.

## Vector backends

- [ONNX Local Embedding Pipeline](vector-backends/onnx-embedding-pipeline.md) — how to produce MiniLM embeddings locally without a remote provider; pipeline details, dependencies, long-input handling.
- [Integrallis Vectors](vector-backends/integrallis-vectors.md) — embedded persistent vector engine for Java 25; VectorCollection API, indexes, persistence, metadata filtering, VCR distinction.
- [Candidate Assessment](vector-backends/candidate-assessment.md) — updated matrix for all Argonaut 0.2 vector backend candidates with confidence, evidence, and architectural distinctiveness.

## Vector retrieval evaluation

- [Argonaut Vector Synthetic Corpus v0.1](vector-synthetic-corpus/v0.1/README.md) — a deterministic 30-document controlled corpus with 25 golden queries, graded qrels, scenario matrix, and retrieval-design traps. It evaluates ranked retrieval independently of generation.

## Articles

- [Five Ways to Build the Same AI Capability](articles/UC-ARTICLE-001-five-ways-to-build-the-same-ai-capability.md) - an evidence-bounded explanation of UC-001's five JVM execution models for senior engineers and architects.
