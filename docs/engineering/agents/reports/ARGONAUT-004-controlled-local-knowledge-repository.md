# ARGONAUT-004 — Controlled Local Knowledge Repository

**Date:** 2026-08-13
**Task:** ARGONAUT-004 — Implement Controlled Local Knowledge Repository
**Type:** Implementation
**Module:** `argonaut-core`
**Status:** Complete

---

## Objective

Implement the first concrete `KnowledgeRepository` in `argonaut-core` using controlled local documents. The repository must be deterministic, require no external services, support the core knowledge contract from ARGONAUT-003, and serve as the shared evidence source for all four Argonaut framework implementations.

---

## Files Changed

### Created

| File | Purpose |
| --- | --- |
| `argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/KnowledgeSourceException.java` | Unchecked exception for failed read operations; maps to `ArgonautErrorCode.KNOWLEDGE_SOURCE_ERROR` |
| `argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/local/LocalKnowledgeDocument.java` | Immutable record: id, title, content, metadata. Compact constructor validates non-blank id and makes defensive copy of metadata |
| `argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/local/LocalKnowledgeCorpus.java` | Immutable document collection. Factory methods `of(List)`, `of(T...)`, and `demo()` |
| `argonaut-core/src/main/java/dev/jsanca/argonaut/core/knowledge/local/LocalKnowledgeRepository.java` | Implements `KnowledgeRepository`. Lexical search with title-weighted scoring; read by sourceId |
| `argonaut-core/src/test/java/dev/jsanca/argonaut/core/knowledge/local/LocalKnowledgeRepositoryTest.java` | 21 unit tests |

### Updated

| File | Change |
| --- | --- |
| `argonaut-core/README.md` | Added `knowledge.local` row to package table; updated test count |
| `docs/knowledge/common-contract.md` | Added section documenting `LocalKnowledgeRepository` behavior, demo corpus, and framework usage pattern |
| `docs/engineering/ENGINEERING_LOG.md` | Compact ARGONAUT-004 index entry |

---

## Implementation Detail

### `KnowledgeSourceException`

Unchecked exception in `dev.jsanca.argonaut.core.knowledge`. Thrown by `LocalKnowledgeRepository.read()` when a `DocumentReference.sourceId()` is not found in the corpus. Framework callers should catch this and map it to an `ArgonautError` with code `KNOWLEDGE_SOURCE_ERROR`.

### `LocalKnowledgeDocument`

Plain Java record with four fields: `id` (non-blank), `title`, `content`, `metadata`. The compact constructor validates `id` and makes an unmodifiable copy of `metadata`. A three-argument `of()` factory omits metadata.

### `LocalKnowledgeCorpus`

Wraps an immutable `List<LocalKnowledgeDocument>`. Provides `findById(String)` returning `Optional<LocalKnowledgeDocument>`. The `demo()` factory returns the standard five-document controlled corpus.

### `LocalKnowledgeRepository`

Implements `KnowledgeRepository`. No external dependencies.

**Tokenizer:** Lowercases input, splits on `[^a-z0-9]+`, filters empty strings, returns `Set<String>` (unique terms only).

**Scoring per document:**
- For each unique query term: +2.0 if found in title tokens, +1.0 if found in content tokens only (not title).
- Raw score divided by `queryTermCount × 2.0`, clamped to [0.0, 1.0].
- Documents with score = 0.0 are omitted from results.

**Result ordering:** Descending score, then ascending `sourceId` for stable tie-breaking.

**Excerpt:** Finds the character position of the first matching query term in the document content. Returns a 250-character window starting 80 characters before that position. Prepends "…" when the window does not start at the beginning. Falls back to the content start when no term is found.

**Read:** Looks up `DocumentReference.sourceId()` in the corpus via `findById()`. Returns `DocumentContent(reference, doc.content())` on success. Throws `KnowledgeSourceException` on miss; the exception message includes the sourceId.

### Demo corpus (`LocalKnowledgeCorpus.demo()`)

Five synthetic, original documents:

| id | Title |
| --- | --- |
| `vt-001` | Virtual Threads and Blocking I/O in Java |
| `sc-001` | Structured Concurrency in the JVM |
| `obs-001` | Observability Fundamentals for Agentic Systems |
| `rag-001` | Retrieval-Augmented Generation: Core Concepts |
| `exp-001` | Controlled Evidence in AI Framework Experiments |

The corpus is sufficient to drive a multi-step agentic mission that exercises knowledge search, document read, and evidence selection across multiple steps. `exp-001` carries a `metadata` map (`topic=experiment-design, version=1`) to demonstrate the metadata field.

---

## Search Behavior Notes

The lexical scorer does not apply IDF weighting, stemming, stop-word removal, or fuzzy matching. Common English words (e.g., "the", "in", "is") that appear in every document inflate scores equally and do not meaningfully discriminate between documents — this is acceptable for an experimental baseline. The scorer is optimized for explainability and determinism, not retrieval quality.

Numeric-only tokens (e.g., "123") are valid query terms but will not match prose documents, producing empty results. This is expected behavior.

---

## Boundary Validation

Verified no forbidden dependencies in `argonaut-core`:

```
argonaut-core runtime dependencies: none
argonaut-core test dependencies: JUnit 5.11.4 (junit-jupiter, test scope only)
```

No types named `Agent`, `Graph`, `Node`, `Planner`, `Workflow`, or `UniversalOrchestrator` introduced.

No dependencies on Spring AI, LangChain4j, LangGraph4j, Embabel, OpenRouter, Lucene, Qdrant, OpenSearch, Langfuse, LangSmith, or OpenTelemetry.

---

## Validation

**Command:** `mvn verify` (from repository root)

**Result:** BUILD SUCCESS

```
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
  ExecutionMetricsTest        3 tests
  EvidenceTest                5 tests
  CompositeExecutionObserverTest  4 tests
  InMemoryExecutionObserverTest   4 tests
  NoopExecutionObserverTest       2 tests
  KnowledgeSearchRequestTest      5 tests
  LocalKnowledgeRepositoryTest   21 tests
  ExperimentResultTest            5 tests
  ExperimentRequestTest           7 tests
```

All 21 new `LocalKnowledgeRepositoryTest` cases pass, including corpus loading, title-match ranking, body-only match, topK limiting, determinism, zero-score omission, missing-document exception, score normalization, and custom corpus construction.

---

## Limitations

1. **No IDF or stop-word filtering.** Common words score equally across documents. Relevance quality is intentionally minimal.
2. **No stemming.** "thread" and "threads" are different tokens. Queries must match the exact root form present in documents.
3. **Set-based title/content matching.** Term frequency within a document is not counted; a term appearing 10 times in content scores the same as one appearing once.
4. **Corpus is programmatic, not file-based.** The demo corpus is defined in code. Loading documents from classpath resources is deferred (see below).
5. **No concurrent modification protection on corpus itself.** `LocalKnowledgeCorpus` is immutable; concurrent reads are safe, but the corpus cannot be updated at runtime.

---

## Deferred Work

- **File-based corpus loader:** Load `LocalKnowledgeDocument` instances from `src/main/resources/knowledge/` for richer runtime corpora. Requires a simple line-based or JSON format; deferred to avoid adding Jackson to core.
- **IDF weighting:** Score rare terms higher than common ones. Meaningful once the corpus grows beyond ~20 documents.
- **Stop-word filtering:** Remove high-frequency English words that carry no discriminating signal.
- **Stemming:** Normalize "threads" → "thread" etc. Adds a stem library dependency or a simple suffix-stripping approach.
- **Vector/hybrid search:** Lucene, Qdrant, pgvector — deferred per experiment design; the `KnowledgeRepository` interface supports future implementations without contract changes.
