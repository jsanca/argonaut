# Task: Implement Argonaut Vector Golden Retrieval Evaluator v0.1

## Context

Argonaut Vector now supports runtime switching between:

* `IN_MEMORY`
* `INTEGRALLIS`
* `QDRANT`

All three providers can coexist in the same running application and the active provider can be selected through:

```http
PUT /api/vector/provider
```

Retrieval is performed through:

```http
POST /api/vector/search
```

We also now have a controlled synthetic corpus and a golden retrieval dataset.

The golden query file contains **25 queries** with stable IDs and retrieval-scenario metadata.

Examples include:

* semantic paraphrase
* exact lexical match
* technical identifiers
* strong lexical distractors
* acronym/expanded terminology
* short underspecified queries
* chunk-boundary cases
* near-duplicate documents
* noisy/typo queries
* verbose queries
* distributed evidence
* multiple relevance grades
* no-answer behavior

`Q025` is explicitly a no-answer query:

```text
How do I configure S3 cross-region replication for vector corpus backups?
```

No corpus document should be considered relevant for that query.

---

# Goal

Implement a reproducible retrieval-quality evaluator that:

1. Reads the existing `golden-queries.jsonl`.
2. Reads the existing graded relevance judgments from `relevance.tsv`.
3. Selects a vector provider through the runtime provider API.
4. Executes all 25 golden queries against `/api/vector/search`.
5. Captures ranked results and raw scores.
6. Compares results against qrels.
7. Calculates standard retrieval metrics.
8. Produces machine-readable and human-readable evaluation reports.
9. Supports comparing `IN_MEMORY`, `INTEGRALLIS`, and `QDRANT`.

The evaluator must measure **retrieval quality**, not generated-answer quality.

---

# Preferred Implementation

Prefer **Python** for the evaluator.

Karate remains appropriate for HTTP contract/integration testing, but Python is preferred here because this task requires:

* qrels parsing
* ranking metrics
* graded relevance
* grouping by query category
* result aggregation
* comparative reporting
* future score-distribution analysis

Do not turn Karate into an ad-hoc statistical framework.

A small Karate suite may be added separately if useful, but the primary deliverable should be a Python evaluation runner.

---

# Inputs

Use the existing controlled-corpus evaluation assets.

Expected logical structure:

```text
docs/knowledge/vector-synthetic-corpus/v0.1/
    queries/
        golden-queries.jsonl

    qrels/
        relevance.tsv
```

Do not duplicate or rewrite the golden dataset.

Treat the existing corpus/query/qrel files as versioned evaluation assets.

---

# Query Format

The golden query JSONL contains records conceptually like:

```json
{
  "id": "Q001",
  "query": "How does the HNSW algorithm navigate a vector index?",
  "categories": [
    "semantic-paraphrase",
    "acronym-expanded-terminology"
  ],
  "notes": "Conceptual algorithm query; should prefer the graph explanation over configuration notes."
}
```

Some queries belong to multiple categories.

The evaluator must preserve these categories in its raw results so aggregate metrics can be calculated per category.

---

# Provider Selection

The evaluator should accept a provider argument.

Example:

```bash
python3 evaluate_retrieval.py \
  --provider IN_MEMORY
```

Supported values:

```text
IN_MEMORY
INTEGRALLIS
QDRANT
```

Default:

```text
IN_MEMORY
```

Before running evaluation:

```text
PUT /api/vector/provider
```

Then verify:

```text
GET /api/vector/provider
```

The active provider must match the requested provider before any golden query is executed.

Fail fast otherwise.

---

# Search Execution

For every query:

```http
POST /api/vector/search
Content-Type: application/json
```

Request:

```json
{
  "query": "...",
  "limit": 5
}
```

Capture at minimum:

```text
query ID
query text
query categories
provider
rank
document ID
raw provider score
```

Keep raw scores exactly as returned by the provider.

Do not modify or normalize them in the raw result file.

---

# Important Score-Semantics Constraint

We have already experimentally observed that provider score scales differ.

InMemory and Qdrant currently return essentially the same cosine-similarity values.

Integrallis appears to expose the same similarity using a transformed scale equivalent to approximately:

```text
integrallisScore = (1 + cosineSimilarity) / 2
```

Therefore:

> Raw similarity scores from different providers must NOT be treated as directly comparable confidence values.

The evaluator's primary quality metrics must be based on **ranking and relevance judgments**, not absolute provider score.

Keep scores for later analysis, but do not use arbitrary global thresholds such as:

```text
score > 0.5 = relevant
```

unless a separate calibrated experiment establishes such a rule.

This finding is important and must be mentioned in the evaluator README/report.

---

# Qrels

Read the existing `relevance.tsv`.

Expected graded relevance semantics:

```text
3 = essential / primary expected result
2 = highly useful
1 = related
0 = irrelevant
```

Do not infer relevance from similarity scores.

The qrels are ground truth.

---

# Required Metrics

Calculate at minimum:

## Recall@1

Relevant evidence found in the first result.

## Recall@3

Relevant evidence found within the first three results.

## Recall@5

Relevant evidence found within the first five results.

Where multiple relevant documents exist, use standard recall semantics:

```text
number of relevant docs retrieved
/
number of relevant docs expected
```

Provide both per-query and aggregate results where useful.

---

## MRR

Calculate reciprocal rank based on the first relevant document.

Example:

```text
relevant at #1 -> 1.0
relevant at #2 -> 0.5
relevant at #3 -> 0.333...
```

Report aggregate Mean Reciprocal Rank.

---

## nDCG@5

Use the graded qrels.

Higher relevance grades should contribute more than lower grades.

Calculate standard normalized Discounted Cumulative Gain at 5.

This metric is particularly important for queries tagged:

```text
multiple-relevance-grades
```

---

# Definition of Relevance for Binary Metrics

For Recall and MRR:

```text
relevance > 0
```

counts as relevant.

For nDCG preserve the full graded value.

Document this explicitly.

---

# No-Answer Query

`Q025` requires special handling.

It has:

```text
expected_relevance = none
```

and no corpus document is relevant.

The vector search API currently returns nearest neighbors even when all neighbors are irrelevant.

Therefore do NOT mark the evaluator itself as broken merely because `/vector/search` returns results.

Instead record:

```text
Q025
expected relevant documents = none
returned documents = [...]
top score = ...
```

and report this separately as:

```text
No-answer / abstention behavior
```

For v0.1 this can be informational rather than included in Recall/MRR/nDCG aggregates.

Do not invent a score threshold to make Q025 pass.

A later experiment will investigate confidence/abstention calibration.

---

# Per-Query Report

Generate a detailed record for every query.

Example:

```text
Q001
Categories:
  semantic-paraphrase
  acronym-expanded-terminology

Expected:
  DOC-001 relevance=3
  DOC-004 relevance=1

Actual:
  #1 DOC-001 score=...
  #2 DOC-008 score=...
  #3 DOC-004 score=...
  #4 ...
  #5 ...

Metrics:
  Recall@1
  Recall@3
  Recall@5
  Reciprocal Rank
  nDCG@5
```

Do not dump full document bodies into the report unless explicitly requested.

IDs, ranks and scores are sufficient.

---

# Aggregate Report

Produce a summary such as:

```text
Argonaut Vector Golden Evaluation
Provider: IN_MEMORY
Corpus: synthetic-v0.1
Queries: 25
Answerable queries: 24
No-answer queries: 1

Recall@1: ...
Recall@3: ...
Recall@5: ...
MRR: ...
nDCG@5: ...
```

---

# Category Breakdown

This is a required feature.

Using the categories already present in `golden-queries.jsonl`, calculate metrics grouped by retrieval scenario.

Examples include:

```text
semantic-paraphrase
exact-lexical-match
technical-identifier
strong-lexical-distractor
short-underspecified
chunk-boundary
near-duplicate-documents
minor-typo-noisy-query
verbose-query
information-distributed-across-documents
shared-keywords-different-meanings
multiple-relevance-grades
no-answer
```

Output something conceptually like:

```text
Category                           Queries   R@1   R@5   MRR   nDCG@5
semantic-paraphrase                  11      ...   ...   ...    ...
exact-lexical-match                   4      ...   ...   ...    ...
technical-identifier                  4      ...   ...   ...    ...
chunk-boundary                        2      ...   ...   ...    ...
minor-typo-noisy-query                1      ...   ...   ...    ...
```

Queries may contribute to multiple category aggregates.

This is intentional.

---

# Output Files

For each provider generate both raw and human-readable results.

Suggested:

```text
evaluation/results/
    in-memory/
        golden-v0.1.json
        golden-v0.1.txt

    integrallis/
        golden-v0.1.json
        golden-v0.1.txt

    qdrant/
        golden-v0.1.json
        golden-v0.1.txt
```

The JSON should preserve enough raw information that we can recalculate metrics later without rerunning retrieval.

At minimum preserve:

```text
provider
dataset version
query ID
query text
categories
expected qrels
actual ranked results
raw scores
calculated per-query metrics
aggregate metrics
```

---

# Optional Multi-Provider Mode

If straightforward, support:

```bash
python3 evaluate_retrieval.py --provider ALL
```

which executes:

```text
IN_MEMORY
INTEGRALLIS
QDRANT
```

sequentially using the runtime provider-selection endpoint.

However, single-provider execution is mandatory and should remain available.

Do not make the initial implementation unnecessarily complex to support ALL.

---

# Corpus Precondition

Providers have independent stores.

The evaluator should verify that the selected provider appears to contain the controlled corpus before beginning the full run.

A lightweight preflight query is acceptable.

Prefer failing with a clear message such as:

```text
Selected provider IN_MEMORY does not appear to contain synthetic corpus v0.1.
Load the corpus before evaluation.
```

Do not automatically replicate or ingest the corpus unless there is a compelling reason.

Corpus loading and retrieval evaluation should remain separate responsibilities.

---

# Comparison Report

If results for all three providers are available, optionally generate:

```text
provider-comparison.txt
```

with:

```text
Provider      R@1    R@3    R@5    MRR    nDCG@5
IN_MEMORY     ...
INTEGRALLIS   ...
QDRANT        ...
```

Also identify whether ranked document sequences differ.

For example:

```text
Queries with identical Top-5 across all providers: 22/24
Queries with ranking divergence: Q008, Q017
```

This is especially valuable because our initial smoke tests produced identical rankings across all three providers despite different score scales.

---

# Validation

Add unit tests for metric calculations using small deterministic examples.

At minimum test:

* Recall@K
* MRR
* nDCG@5
* multiple relevant documents
* graded relevance
* missing expected documents
* empty search results
* no-answer query handling

Do not rely only on integration runs against the live API to validate metric math.

---

# Non-Goals

Do not implement yet:

* score normalization
* confidence thresholds
* abstention thresholds
* hybrid retrieval
* BM25
* reranking
* LLM-as-a-judge
* generation evaluation
* automatic provider score calibration

These belong to later experiments.

---

# Documentation

Document:

1. how to run the evaluator;
2. expected corpus preconditions;
3. metric definitions;
4. qrel semantics;
5. provider-selection behavior;
6. why raw provider scores are not directly comparable;
7. why Q025 is treated separately.

---

# Definition of Done

The following must work:

```bash
python3 evaluate_retrieval.py --provider IN_MEMORY

python3 evaluate_retrieval.py --provider INTEGRALLIS

python3 evaluate_retrieval.py --provider QDRANT
```

Each run must:

```text
select provider
verify provider
read 25 golden queries
read qrels
execute search Top-5
calculate metrics
calculate metrics by category
preserve raw scores
write JSON report
write human-readable report
```

The three resulting reports must allow us to objectively compare retrieval quality without using provider-specific similarity-score thresholds.
