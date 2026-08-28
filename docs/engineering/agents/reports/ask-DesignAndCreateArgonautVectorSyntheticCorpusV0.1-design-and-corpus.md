# Design and Create Argonaut Vector Synthetic Corpus v0.1 — Report

## Status

Complete

## Objective

Create a small, deterministic, human-auditable technical corpus that compares ranked retrieval strategies independently of LLM generation. The requested deliverables were approximately 30 Markdown documents, approximately 25 golden queries, explicit graded qrels, a scenario matrix, and a design report.

## Summary

Created Argonaut Vector Synthetic Corpus v0.1 under `docs/knowledge/vector-synthetic-corpus/v0.1/`. It contains 30 stable controlled documents (`DOC-001`–`DOC-030`), 25 stable JSONL query records (`Q001`–`Q025`), and 59 positive graded qrels. Qrels use the requested 1–3 scale; absent pairs are relevance 0.

The corpus deliberately separates closely related retrieval intents: an HNSW algorithm explanation from an exact HNSW property lookup, HTTP idempotency from at-least-once message delivery, Qdrant-specific HNSW settings from generic HNSW material, and Service routing from Kubernetes probe behavior. It also includes a deliberately no-answer query (`Q025`) with no qrels rows.

## Files Changed

- `docs/knowledge/vector-synthetic-corpus/v0.1/corpus/controlled/` — 30 source Markdown documents.
- `docs/knowledge/vector-synthetic-corpus/v0.1/queries/golden-queries.jsonl` — query text, stable IDs, categories, notes, and no-answer metadata.
- `docs/knowledge/vector-synthetic-corpus/v0.1/qrels/relevance.tsv` — graded document-level relevance judgments.
- `docs/knowledge/vector-synthetic-corpus/v0.1/README.md` — dataset contract, metric interpretation, and versioning guidance.
- `docs/knowledge/vector-synthetic-corpus/v0.1/scenario-matrix.md` — query-to-phenomenon mapping.
- `docs/knowledge/vector-synthetic-corpus/v0.1/design-report.md` — challenge-first design and embedded retrieval traps.
- `docs/knowledge/README.md` and `docs/PROJECT.md` — durable knowledge index and project-context reference.

## Evidence

The authoritative task is [Task-DesignAndCreateArgonautVectorSyntheticCorpusV0.1.md](../tasks/ite-02/knowledge/Task-DesignAndCreateArgonautVectorSyntheticCorpusV0.1.md). The corpus README defines the evaluation contract: submit queries verbatim, reduce chunk results to stable parent document IDs when needed, and compute document-ranked metrics using fixed qrels rather than generated answers or similarity-score thresholds.

## Validation

- A repository-local Node validation parsed all JSONL query lines and confirmed 30 unique document IDs, 25 unique query IDs, 59 qrels, valid document/query references, valid grades, no duplicate query/document pairs, and the `Q025` no-answer invariant.
- `git diff --check` was run. It reported pre-existing trailing whitespace in `.osk/workspace.yaml`; no corpus-file whitespace issue was reported.

## Limitations

No retrieval implementation was run against this dataset in this task. Therefore no recall, MRR, nDCG, dense-versus-lexical outcome, or database-specific behavior is claimed. Chunking, embedding models, analyzers, candidate pools, result cutoffs, and parent-document reduction policies remain run-level controls to record by each retrieval implementation.

## Open Follow-Up

Feed this unchanged v0.1 corpus into Argonaut Vector retrieval implementations and record ranked document IDs plus run configuration. Add a later corpus version rather than modifying these stable IDs or qrels when new retrieval phenomena are needed.

## Related Records

- [Vector Synthetic Corpus v0.1](../../../knowledge/vector-synthetic-corpus/v0.1/README.md)
- [Scenario matrix](../../../knowledge/vector-synthetic-corpus/v0.1/scenario-matrix.md)
- [Design report](../../../knowledge/vector-synthetic-corpus/v0.1/design-report.md)
- [ARGONAUT-0.2-003 vector backend wiring](ARGONAUT-0.2-003-vector-ports-and-backend-wiring.md)
