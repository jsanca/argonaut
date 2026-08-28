# Task: Design and Create Argonaut Vector Synthetic Corpus v0.1

## Context

Argonaut Vector is a retrieval laboratory intended to compare multiple retrieval strategies under controlled conditions.

Current and planned strategies include:

* Dense vector retrieval
* In-memory cosine similarity
* Integrallis FLAT/HNSW and related indexes
* Qdrant
* Different chunking strategies
* Lexical/BM25 retrieval
* Hybrid retrieval
* Multi-vector retrieval
* Reranking/rank fusion

The purpose of this task is **not to test generation quality**.

The corpus must allow us to evaluate **retrieval quality independently from the LLM**.

## Objective

Create a small, deterministic synthetic corpus that exposes meaningful differences between retrieval strategies.

Target:

* ~30 Markdown documents
* ~25 golden queries
* Explicit relevance judgments (qrels)
* Technical/software-engineering domain
* Human-readable and easy to audit

The corpus should be deliberately designed rather than randomly generated.

## Required Retrieval Scenarios

The dataset must contain examples covering at least:

1. Exact lexical matching
2. Semantic paraphrases
3. Technical identifiers and configuration properties
4. Acronyms and expanded terminology
5. Documents sharing keywords but different meanings
6. Near-duplicate documents
7. Strong lexical distractors
8. Information near potential chunk boundaries
9. Queries whose answer requires identifying one highly specific document
10. Multiple documents with different degrees of relevance
11. Short/underspecified queries
12. Verbose queries
13. Minor typo/noisy-query cases
14. Information distributed across related documents
15. No-answer queries where no corpus document should be considered relevant

Pay particular attention to scenarios where dense and lexical retrieval could reasonably behave differently.

Example:

A conceptual query about HNSW should favor a document explaining the HNSW algorithm.

A query containing an exact Argonaut configuration property should favor the document containing that property, even if another document is semantically richer about HNSW.

## Domain

Use technical material familiar to the Argonaut project, including topics such as:

* Java
* Spring/Spring Boot
* Kubernetes
* HTTP/REST
* Observability
* Vector search
* Embeddings
* RAG
* HNSW
* Qdrant
* Application health/readiness
* Software architecture

The documents may be synthetic, but technical claims should be coherent and plausible.

Do not depend on external network resources at runtime.

## Proposed Structure

```text
evaluation/
  corpus/
    controlled/
      DOC-001.md
      DOC-002.md
      ...
  queries/
    golden-queries.jsonl
  qrels/
    relevance.tsv
  README.md
```

Each document must have a stable identifier.

Example:

```text
DOC-001
DOC-002
...
```

Each query must also have a stable identifier:

```text
Q001
Q002
...
```

## Relevance Scale

Use graded relevance:

```text
3 = essential / expected primary result
2 = highly useful
1 = related but insufficient
0 = irrelevant
```

The qrels must make it possible to calculate metrics such as:

* Recall@K
* MRR
* nDCG

Do not evaluate generated answers.

## Query Metadata

For every golden query, include metadata identifying the scenario being tested.

Suggested shape:

```json
{
  "id": "Q001",
  "query": "How should Kubernetes handle an application that takes a long time to initialize?",
  "category": "semantic-paraphrase",
  "notes": "Should retrieve the startup probe document without requiring exact lexical overlap."
}
```

Multiple categories may be used when appropriate.

## Design Requirement

Do not create 30 independent documents and then invent questions afterward.

Design the retrieval challenges first.

For each challenge:

1. Define what behavior is being tested.
2. Define the expected relevant document(s).
3. Create distractor documents where useful.
4. Write the query.
5. Assign relevance judgments.

The resulting corpus should make retrieval failures diagnosable.

We should be able to inspect a failed query and understand **why one retrieval strategy succeeded while another failed**.

## Deliverables

Produce:

* The complete controlled corpus
* Golden queries
* Relevance judgments
* README explaining corpus design
* A scenario matrix mapping queries to retrieval phenomena
* A short design report describing important traps intentionally embedded in the corpus

## Constraints

Keep Corpus v0.1 small.

Prefer deliberate experimental design over corpus size.

Do not optimize the corpus for any specific vector database or embedding model.

Do not make expected results depend on arbitrary similarity scores.

Do not use an LLM as the ground-truth judge.

The dataset itself must remain backend-independent.

## Definition of Done

Corpus v0.1 is complete when the same dataset can be fed unchanged into different Argonaut Vector retrieval implementations and produce objectively comparable ranked retrieval results.
