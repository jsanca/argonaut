# Argonaut Vector Golden Retrieval Evaluator v0.1

Reproducible retrieval-quality evaluator for the controlled synthetic corpus.

Measures **retrieval quality** (ranking), not generated-answer quality.

---

## Prerequisites

### Python dependencies

```bash
pip install requests
```

No other third-party libraries are required.

### Running service

The Argonaut Vector service must be running at `http://localhost:8086`.

```bash
# From the argonaut-vector directory
docker compose up
```

### Corpus loaded into the target provider

Each provider maintains an independent store. You must load the synthetic corpus
into whichever provider you intend to evaluate before running the evaluator.

Use the corpus loader (store endpoint) or smoke test scripts to populate each
provider. The evaluator will detect an empty/missing corpus and fail with a
clear message:

```
ERROR: Selected provider IN_MEMORY does not appear to contain synthetic corpus v0.1.
       Load the corpus before evaluation.
```

---

## Running

```bash
cd argonaut-vector/evaluation

# Evaluate IN_MEMORY (default)
python3 evaluate_retrieval.py

# Evaluate a specific provider
python3 evaluate_retrieval.py --provider IN_MEMORY
python3 evaluate_retrieval.py --provider INTEGRALLIS
python3 evaluate_retrieval.py --provider QDRANT

# Evaluate all three providers sequentially
python3 evaluate_retrieval.py --provider ALL

# Custom base URL
python3 evaluate_retrieval.py --provider QDRANT --base-url http://localhost:8086
```

Results are written to:

```
evaluation/results/
    in-memory/
        golden-v0.1.json     ← raw data for later reanalysis
        golden-v0.1.txt      ← human-readable report
    integrallis/
        golden-v0.1.json
        golden-v0.1.txt
    qdrant/
        golden-v0.1.json
        golden-v0.1.txt
    provider-comparison.txt  ← generated when ≥2 providers evaluated together
```

---

## Running unit tests

```bash
cd argonaut-vector/evaluation
python3 -m pytest test_metrics.py -v
# or
python3 -m unittest test_metrics -v
```

---

## Metric definitions

All metrics are calculated over answerable queries only. `Q025` is excluded (see below).

### Binary relevance threshold

For Recall@K and MRR, a document is considered **relevant** when its qrel grade is `> 0`.
The full graded value is used for nDCG@5.

### Recall@K

```
Recall@K = |relevant ∩ top-K retrieved| / |relevant|
```

Reported at K = 1, 3, 5.

### MRR (Mean Reciprocal Rank)

Reciprocal rank of the first relevant document across answerable queries:

```
RR = 1 / rank_of_first_relevant_doc   (0 if none found)
MRR = mean(RR across queries)
```

### nDCG@5

Normalized Discounted Cumulative Gain using graded relevance:

```
DCG@K  = Σ grade(i) / log2(i + 1)   for i = 1..K
IDCG@K = DCG@K of ideal ranking (grades sorted descending)
nDCG@K = DCG@K / IDCG@K
```

Particularly meaningful for queries tagged `multiple-relevance-grades`.

---

## Qrel semantics

```
3 = essential / primary expected result
2 = highly useful
1 = related
0 = irrelevant (implicit — absence from the file means grade 0)
```

Source: `docs/knowledge/vector-synthetic-corpus/v0.1/qrels/relevance.tsv`

Do not modify the qrel file. It is the ground truth.

---

## Why raw provider scores are not directly comparable

InMemory and Qdrant return raw cosine similarity values in approximately `[-1, 1]`.

Integrallis exposes a transformed value:

```
integrallis_score ≈ (1 + cosine_similarity) / 2
```

This maps the same semantic similarity to a different numeric range (`[0, 1]`).

Therefore, **you must not compare absolute scores across providers**, and the
evaluator does not apply score thresholds to determine relevance. The qrels
are the sole ground truth for relevance.

This finding is documented here so that future experiments that add
confidence-threshold or abstention logic start from a calibrated baseline
rather than an arbitrary global threshold.

---

## Q025 — No-answer query

`Q025` asks:

> How do I configure S3 cross-region replication for vector corpus backups?

No corpus document is relevant to this query. It is included to test whether
the system can withhold an answer rather than force a topical match.

The vector search API currently returns nearest neighbors regardless of
relevance. The evaluator records Q025 results in the "No-Answer / Abstention
Behavior" section of the report and **excludes** them from aggregate metrics.

A later experiment will investigate confidence/abstention calibration using
score distributions and the results captured here.

---

## Provider selection protocol

Before any golden query is executed, the evaluator:

1. Sends `PUT /api/vector/provider` with the requested provider.
2. Sends `GET /api/vector/provider` and verifies the active provider matches.
3. Fails fast if there is a mismatch.

This ensures no query is evaluated against the wrong provider due to a
rejected provider switch.

---

## JSON output format

The `golden-v0.1.json` file preserves enough information to recalculate all
metrics without re-running retrieval:

```json
{
  "provider": "IN_MEMORY",
  "corpus_version": "v0.1",
  "evaluated_at": "...",
  "query_count": 25,
  "answerable_query_count": 24,
  "no_answer_query_count": 1,
  "aggregate_metrics": { "recall_at_1": ..., "mrr": ..., "ndcg_at_5": ... },
  "category_metrics": { "semantic-paraphrase": { ... }, ... },
  "query_results": [
    {
      "query_id": "Q001",
      "query": "...",
      "categories": [...],
      "expected_qrels": { "DOC-001": 3, "DOC-003": 2 },
      "actual_results": [
        { "rank": 1, "id": "DOC-001", "score": 0.923456 },
        ...
      ],
      "metrics": { "recall_at_1": 1.0, "mrr": 1.0, "ndcg_at_5": 0.972 }
    }
  ],
  "no_answer_results": [ ... ]
}
```

Raw scores are preserved exactly as returned by the provider.
