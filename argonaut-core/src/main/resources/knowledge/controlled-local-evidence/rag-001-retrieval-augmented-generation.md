---
id: rag-001
title: Retrieval-Augmented Generation: Core Concepts
topic: retrieval-augmented-generation
version: 1
---

# Retrieval-Augmented Generation: Core Concepts

## What RAG means in Argonaut

Retrieval-Augmented Generation describes a pattern in which an agentic system retrieves source material before synthesizing an answer. In Argonaut, retrieval and synthesis are explicitly observable steps recorded in the execution trace. The pattern is not a single model call — it is a sequence of discrete, traceable operations.

## The retrieval-synthesis sequence

A minimal RAG flow in Argonaut proceeds as follows:

1. **Search** — the implementation issues one or more queries to `KnowledgeRepository.search()`. Each search call is recorded as a `KNOWLEDGE_SEARCH_STARTED` / `KNOWLEDGE_SEARCH_COMPLETED` event pair.
2. **Read** — the implementation reads the full content of one or more documents identified by search. Each read is recorded as a `DOCUMENT_READ_STARTED` / `DOCUMENT_READ_COMPLETED` event pair.
3. **Evidence selection** — the implementation identifies which content items are relevant to the question. Candidate items are recorded as `EVIDENCE_RETRIEVED`; selected items as `EVIDENCE_SELECTED`.
4. **Answer synthesis** — the implementation constructs a final answer grounded in the selected evidence. This is recorded as `ANSWER_SYNTHESIZED`.

Search and document reading are separate observable steps. They must not be collapsed into a single opaque operation. The observable contract requires that both events appear in the trace so that framework behavior can be compared at the step level.

## Why evidence selection is separate from retrieval

Retrieval returns candidate documents ranked by relevance. Evidence selection is the act of deciding which content from those documents should inform the answer. This distinction matters for the experiment:

- A framework that retrieves many documents but selects evidence selectively differs from one that uses every retrieved document equally.
- Evidence selection is the step where the framework's reasoning is most visible.
- Recording `EVIDENCE_RETRIEVED` and `EVIDENCE_SELECTED` as separate events allows a human evaluator or test to verify that the framework did not include irrelevant material in the answer.

## Which sources influenced the answer

An Argonaut `ExperimentResult` includes an `evidence` list. Each `Evidence` item records the source document ID, a title, an excerpt, a relevance score, and a description of how the item was used. This list tells the human evaluator exactly which documents contributed to the final answer and with what confidence.

The evidence list must be derived from the corpus, not generated from the model's parametric knowledge. If the evidence list does not reference specific source IDs from the controlled corpus, the retrieval step was bypassed. Evidence grounding also reduces hallucination: a model constrained to cite retrieved material cannot fabricate facts that are absent from the controlled corpus.

## RAG in the controlled experiment

In Argonaut's first controlled experiment, the corpus is five local documents. The question is answerable entirely from these documents. The implementation must:

- issue at least one search query;
- read at least one document;
- select at least one evidence item with a stable source ID;
- produce a final answer grounded in that evidence.

The controlled corpus is small enough that exhaustive retrieval is acceptable. The experiment does not require sophisticated ranking or re-ranking. What it requires is that every retrieval and read step is observable and that the evidence list references the actual source documents.

## What this experiment does not require

The first controlled experiment does not require:

- semantic embedding similarity;
- BM25 or TF-IDF weighting;
- hybrid dense-sparse retrieval;
- re-ranking or cross-encoder scoring;
- chunking strategies.

The in-memory lexical search provided by `LocalKnowledgeRepository` is sufficient for the controlled corpus. Framework implementations are not evaluated on retrieval sophistication in the first experiment; they are evaluated on whether retrieval, evidence selection, and synthesis are correctly orchestrated and observable.
