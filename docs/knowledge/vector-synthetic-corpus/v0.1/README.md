# Argonaut Vector Synthetic Corpus v0.1

This is a small, deterministic, backend-independent corpus for evaluating ranked retrieval. It is intentionally not a generation benchmark: a run is evaluated only by its retrieved document identifiers and ranks against the supplied graded relevance judgments.

## Layout

```text
v0.1/
  corpus/controlled/DOC-001.md ... DOC-030.md
  queries/golden-queries.jsonl
  qrels/relevance.tsv
  scenario-matrix.md
  design-report.md
```

Document IDs (`DOC-001` through `DOC-030`) and query IDs (`Q001` through `Q025`) are stable corpus API. Do not change their text, identifiers, relevance judgments, or query wording in place. A changed challenge belongs in a later corpus version.

## Evaluation contract

1. Index the 30 Markdown documents unchanged, or record a distinct chunking policy as part of the run.
2. Submit each query verbatim to the retrieval implementation.
3. Retain document IDs and ranks through the chosen cutoff. For chunked implementations, reduce chunks to their parent document ID before using these qrels, and record the reduction policy.
4. Join the ranked IDs with `qrels/relevance.tsv`; absent pairs have relevance `0`.
5. Calculate metrics such as Recall@K, MRR, and nDCG from the fixed grades. Grade `3` is essential/primary, `2` highly useful, `1` related but insufficient, and `0` irrelevant.

`Q025` is a no-answer query. It intentionally has no qrels rows; every corpus document is relevance `0`. A system that returns no result or applies an explicit abstention threshold may be compared, but a forced top-K list is still scored using zero relevance.

Scores produced by dense, lexical, hybrid, or database-specific systems are not comparable inputs to the qrels. Only rank and document identifier are used. The corpus does not prescribe an embedding model, analyzer, vector database, similarity threshold, candidate pool, or arbitrary expected score.

## Design principles

The challenges were designed before the documents. Each query starts with a retrieval phenomenon and an expected primary result; supporting and distractor documents then make a failure diagnosable. For example, `Q001` asks about the HNSW algorithm, while `Q002` includes an exact Argonaut property. A dense system may give both HNSW discussions high rank, but an exact lexical system should strongly prefer `DOC-002` for `Q002`.

The corpus is synthetic. It uses technically coherent examples for controlled comparison, not as production configuration guidance. It has no runtime network dependency and no LLM-created relevance judgments.

See the [scenario matrix](scenario-matrix.md) for query-level phenomena and the [design report](design-report.md) for deliberately embedded traps.
