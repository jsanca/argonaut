# ARGONAUT-008A — Single Authoritative Corpus and Corpus Invariants

**Date:** 2026-08-14
**Task:** ARGONAUT-008A — Single Authoritative Corpus and Corpus Invariants
**Type:** Hardening / Invariant enforcement
**Status:** Complete

---

## Objective

Harden the controlled local evidence baseline by:

1. Declaring the Markdown files as the single authoritative corpus source.
2. Making `LocalKnowledgeCorpus.demo()` load from that source (Option A).
3. Rejecting duplicate document IDs.
4. Rejecting blank document bodies in the loader.
5. Adding golden query tests that lock retrieval behavior for `exp-001`.
6. Adding corpus-level invariant tests.
7. Eliminating documentation ambiguity between programmatic and Markdown corpus.

This task addresses the highest-priority finding from ARGONAUT-007 (dual corpus drift, verified counterexample C1).

---

## Authoritative Corpus Decision: Option A

`LocalKnowledgeCorpus.demo()` now loads from the Markdown corpus.

**Before (ARGONAUT-006):**
- `demo()` was programmatic — inline string literals in Java source.
- Markdown files existed under `docs/knowledge/corpus/controlled-local-evidence/` and were copied to `src/test/resources` for loader tests.
- `demo()` and the Markdown corpus had the same IDs and titles but **different body text** (bodies differed in length, phrasing, and token set).
- ARGONAUT-007 C1 verified: body lengths differed (e.g. `exp-001` demo ~1846 chars vs Markdown ~3717 chars); bodies not equal after whitespace normalization.

**After (this task):**
- `demo()` calls `MarkdownCorpusLoader.fromClasspath(CONTROLLED_CORPUS_CLASSPATH, LocalKnowledgeCorpus.class.getClassLoader())`.
- Markdown files now live in **both** `docs/knowledge/corpus/controlled-local-evidence/` (human-editable source) and `argonaut-core/src/main/resources/knowledge/controlled-local-evidence/` (classpath copy for production and tests).
- The `src/test/resources` copy was removed; the main resources copy serves both production and test classpaths.
- `CorpusInvariantsTest.demo_returns_the_markdown_backed_corpus_with_same_body_content` asserts normalized body equality — corpus drift fails the build.

**Why Option A over Option B (parity test only):**
- Option A makes the constraint structural: `demo()` cannot diverge from Markdown because it IS the Markdown corpus.
- Option B (parity test) would require keeping two copies of body text in sync.
- The JAR classpath limitation (`resource.toURI()` fails inside JARs) is noted and deferred (H10 from ARGONAUT-007); the Maven exploded layout used for all Argonaut module tests is unaffected.

---

## Files Changed

### Corpus files — moved to main resources

| Old location | New location | Change |
| --- | --- | --- |
| `argonaut-core/src/test/resources/knowledge/controlled-local-evidence/*.md` | Removed | No longer needed |
| `argonaut-core/src/main/resources/knowledge/controlled-local-evidence/*.md` | **Created** | Canonical classpath location |
| `docs/knowledge/corpus/controlled-local-evidence/rag-001-retrieval-augmented-generation.md` | Updated | Added "hallucination" sentence (existing test `search_findsByBodyTerm` required it; legitimate RAG concept) |

### Java source changes

| Class | Change |
| --- | --- |
| `LocalKnowledgeCorpus` | Added `CONTROLLED_CORPUS_CLASSPATH` constant; `of(List)` now rejects duplicate IDs; `demo()` now loads from Markdown via `MarkdownCorpusLoader.fromClasspath` |
| `MarkdownCorpusLoader` | Added blank body check in `parseFile()` (throws `MarkdownParseException`); updated `fromClasspath` javadoc |

### New test class

`CorpusInvariantsTest` — 17 tests:

| Test | Coverage |
| --- | --- |
| `authoritative_corpus_has_exactly_five_documents` | Corpus size invariant |
| `authoritative_corpus_has_exactly_the_expected_stable_ids` | Exact ID set: {exp-001, rag-001, obs-001, vt-001, sc-001} |
| `authoritative_corpus_titles_match_use_case_expectations` | Five stable titles matched to use-case document |
| `authoritative_corpus_is_ordered_ascending_by_id` | Deterministic ordering |
| `demo_returns_the_markdown_backed_corpus_with_same_ids` | demo() parity — IDs |
| `demo_returns_the_markdown_backed_corpus_with_same_titles` | demo() parity — titles |
| `demo_returns_the_markdown_backed_corpus_with_same_body_content` | demo() parity — normalized body content |
| `corpus_rejects_duplicate_ids_via_of_list` | Duplicate ID via `LocalKnowledgeCorpus.of(List)` |
| `corpus_rejects_duplicate_ids_via_of_varargs` | Duplicate ID via `LocalKnowledgeCorpus.of(T...)` |
| `markdown_loader_rejects_duplicate_ids_in_directory` | Duplicate ID via `MarkdownCorpusLoader.fromDirectory` |
| `markdown_loader_rejects_empty_directory` | Empty directory edge case |
| `markdown_loader_rejects_blank_body` | Blank body content edge case |
| `golden_query_controlled_local_evidence_ranks_exp001_first` | H5 golden query 1 |
| `golden_query_controlled_evidence_framework_comparison_ranks_exp001_first` | H5 golden query 2 |
| `golden_query_why_controlled_evidence_before_web_search_includes_exp001` | H5 golden query 3 |
| `golden_query_same_evidence_different_frameworks_includes_exp001` | H5 golden query 4 |
| `noisy_stop_word_query_returns_all_documents_as_non_discriminative_noise` | C5 characterization — stop words |

### Documentation updated

| File | Change |
| --- | --- |
| `docs/knowledge/common-contract.md` | Replaced dual-corpus paragraph with authoritative-corpus section; added corpus authority rules, usage patterns, drift-is-failure statement |
| `docs/knowledge/use-cases/controlled-local-evidence-rag.md` | Updated preconditions and corpus fixture to reflect Markdown-backed `demo()`; removed "or equivalent constructed corpus" language |
| `argonaut-core/README.md` | Rewrote "Markdown corpus loader" section to "Authoritative corpus and loader"; updated test count (69→86); clarified that `demo()` is no longer programmatic |

---

## Invariant Behavior

### Duplicate ID rejection

`LocalKnowledgeCorpus.of(List)` and `of(T...)` now iterate the document list and throw `IllegalArgumentException("duplicate document id in corpus: <id>")` on the first duplicate found. This prevents `findById()` from silently returning the first duplicate. `MarkdownCorpusLoader.fromDirectory` calls `of()` after parsing, so the duplicate check applies to Markdown-loaded corpora as well.

### Blank body rejection

`MarkdownCorpusLoader.parseFile()` now checks whether the parsed body (after stripping leading whitespace from after the closing `---` delimiter) is blank. If so, it throws `MarkdownParseException("blank body content in: <filename> (id: <id>)")`. This catches corpus files committed with empty bodies.

### Golden query behavior (Markdown corpus)

| Query | exp-001 position | Assertion |
| --- | --- | --- |
| `controlled local evidence` | First | Asserted first |
| `controlled evidence framework comparison` | First | Asserted first |
| `why controlled evidence before web search` | Present | Asserted present |
| `same evidence different frameworks` | Present | Asserted present |

The first two queries use exact terms from the `exp-001` title and body, producing the highest normalized lexical score. The last two include stop words and phrase variants that may give partial credit to other documents; presence is asserted but top-1 ranking is not required.

### Noisy query characterization

Query `"the a and or of"` returns all five documents because the tokenizer has no stop-word filter and all English prose contains these tokens. This is documented as known, acceptable behavior for the intentionally simple lexical scorer. The test asserts `size == 5` with an explanatory comment; it does not assert any ranking.

---

## Corpus Synchronization

Two copies of the Markdown corpus files now exist:

```text
docs/knowledge/corpus/controlled-local-evidence/   ← human-editable authoritative source
argonaut-core/src/main/resources/knowledge/controlled-local-evidence/   ← classpath copy
```

There is no automated synchronization between them. When editing a corpus document, both locations must be updated. This is documented in `argonaut-core/README.md`. Fixture sync validation (comparing file hashes) is deferred to a future task.

The `CorpusInvariantsTest.demo_returns_the_markdown_backed_corpus_with_same_body_content` test gates against body drift within the classpath copy: if the classpath copy diverges from itself (e.g. `demo()` returns different content than `MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR)`), the test fails. It does not catch drift between the `docs/` source and the `src/main/resources` copy — that remains a process/discipline constraint.

---

## Validation

**`git diff --check`:** clean — no trailing whitespace errors.

**`mvn verify`:** BUILD SUCCESS — 86 tests, 0 failures, 0 errors. (17 new tests in `CorpusInvariantsTest`; prior count was 69.)

```
Tests run: 86, Failures: 0, Errors: 0, Skipped: 0
```

---

## Limitations

- **Two-location corpus synchronization** is enforced by process, not automated tooling. The `docs/` source and the `src/main/resources` copy must be kept in sync manually.
- **`demo()` does I/O on each call.** No caching is applied. The corpus is small (5 files, ~3K chars each) so the cost is negligible for the experiment's scale, but this is worth noting if hot-path usage patterns emerge.
- **JAR classpath limitation** (H10, deferred): `MarkdownCorpusLoader.fromClasspath` fails for resources inside JAR files. Documented in code and README. All current usage is in Maven exploded layouts where this does not apply.
- **`demo()` parity test scope**: `demo_returns_the_markdown_backed_corpus_with_same_body_content` asserts equivalence within the classpath copy, not between `docs/` and `src/main/resources`. Cross-location hash comparison is deferred.
- **Anti-gaming assertions** (H3–H4 from ARGONAUT-007) and **TC-UC-001 harness** are deferred to ARGONAUT-008B.
- **`ExperimentResult.completed` non-blank `finalAnswer` enforcement** (H7) is deferred — not strictly required for corpus/retrieval invariants.
- Vector search, BM25, Lucene, Qdrant, OpenSearch remain deferred.

---

## Recommended Next Task

**ARGONAUT-008B — TC-UC-001 Harness and Anti-Gaming Assertions**

With the single authoritative corpus established and invariants in place, the next hardening step is the TC-UC-001 executable test harness and anti-gaming assertions that prove frameworks cannot satisfy the success criteria without performing real retrieval.
