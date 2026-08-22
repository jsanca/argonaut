# ARGONAUT-006 — Materialize the Controlled Local Knowledge Corpus as Markdown

**Date:** 2026-08-13
**Task:** ARGONAUT-006 — Materialize the Controlled Local Knowledge Corpus as Markdown
**Type:** Implementation / Knowledge materialization
**Status:** Complete

---

## Objective

Move Argonaut's first controlled knowledge corpus from programmatic demo documents into local Markdown files, then add a deterministic loader (`MarkdownCorpusLoader`) that converts those Markdown files into `LocalKnowledgeDocument` instances and returns a `LocalKnowledgeCorpus`.

---

## Mode

Authorized implementation. Scope: new Markdown corpus files, new loader class, new exception class, new tests, documentation updates, engineering report and log entry. `LocalKnowledgeCorpus.demo()` was not modified.

---

## Files Changed

### Created — Corpus documents

| File | Source ID | Title |
| --- | --- | --- |
| `docs/knowledge/corpus/controlled-local-evidence/exp-001-controlled-evidence.md` | `exp-001` | Controlled Evidence in AI Framework Experiments |
| `docs/knowledge/corpus/controlled-local-evidence/rag-001-retrieval-augmented-generation.md` | `rag-001` | Retrieval-Augmented Generation: Core Concepts |
| `docs/knowledge/corpus/controlled-local-evidence/obs-001-agentic-observability.md` | `obs-001` | Observability Fundamentals for Agentic Systems |
| `docs/knowledge/corpus/controlled-local-evidence/vt-001-virtual-threads-blocking-io.md` | `vt-001` | Virtual Threads and Blocking I/O in Java |
| `docs/knowledge/corpus/controlled-local-evidence/sc-001-structured-concurrency.md` | `sc-001` | Structured Concurrency in the JVM |

Each document uses the required frontmatter format:

```markdown
---
id: <source-id>
title: <title>
topic: <topic>
version: 1
---
```

### Created — Test fixture copies

Identical copies of the five Markdown files under:

```text
argonaut-core/src/test/resources/knowledge/controlled-local-evidence/
```

These allow `MarkdownCorpusLoader.fromClasspath("knowledge/controlled-local-evidence")` to be called from JUnit tests without path-resolution overhead or reliance on the working directory.

### Created — Implementation classes

| Class | Package | Purpose |
| --- | --- | --- |
| `MarkdownCorpusLoader` | `dev.jsanca.argonaut.core.knowledge.local` | Loads `.md` files from a directory or classpath path, parses frontmatter, returns `LocalKnowledgeCorpus` |
| `MarkdownParseException` | `dev.jsanca.argonaut.core.knowledge.local` | Unchecked exception for malformed or missing frontmatter fields |

### Created — Test class

`MarkdownCorpusLoaderTest` — 13 tests in `dev.jsanca.argonaut.core.knowledge.local`:

| Test | Coverage |
| --- | --- |
| `loadsAllFiveDocumentsFromClasspath` | Corpus contains exactly 5 documents |
| `documentIdsMatchExpectedControlledCorpus` | All five stable IDs present |
| `titlesMatchUseCase` | Titles match use-case document expectations |
| `metadataIncludesTopicAndVersion` | Frontmatter extras preserved as metadata |
| `bodyContentIsNotBlankForAnyDocument` | No document has empty content |
| `documentsAreReturnedInDeterministicIdOrder` | Ascending source ID ordering |
| `loadedCorpusCanBeSearchedThroughLocalKnowledgeRepository` | Integration: repository wraps loaded corpus |
| `controlledEvidenceQueryRanksExpOneFirst` | `exp-001` tops search for controlled-evidence query |
| `observabilityQueryRetrievesObsOne` | `obs-001` appears for observability query |
| `missingIdFieldThrowsMarkdownParseException` | Error handling: missing `id` |
| `missingTitleFieldThrowsMarkdownParseException` | Error handling: missing `title` |
| `missingFrontmatterDelimiterThrowsMarkdownParseException` | Error handling: no `---` |
| `repeatedLoadsProduceIdenticalOrdering` | Determinism: two loads produce same order |

### Updated — Documentation

| File | Change |
| --- | --- |
| `docs/knowledge/common-contract.md` | Added Markdown corpus paragraph and `MarkdownCorpusLoader` description alongside `LocalKnowledgeCorpus.demo()` section |
| `docs/knowledge/use-cases/controlled-local-evidence-rag.md` | Updated future test case note to reflect corpus is now materialized; points to canonical Markdown location |
| `argonaut-core/README.md` | Added `MarkdownCorpusLoader` and `MarkdownParseException` to package table; updated test count (56 → 69); added "Markdown corpus loader" section with canonical path, classpath loading, and directory loading examples |

---

## Parser Behavior

`MarkdownCorpusLoader` implements a minimal line-by-line parser. No external library is required.

**Frontmatter detection:** The file must begin with `---`. The parser scans for the next `\n---` occurrence to find the closing delimiter. Everything between the delimiters is the frontmatter block.

**Frontmatter parsing:** Each non-blank line is split on the first `:`. Left side is the key; right side (trimmed) is the value. A line with no `:` or with an empty key throws `MarkdownParseException`.

**Required fields:** After parsing, `id` and `title` are extracted from the map (and removed). If either is absent or blank, `MarkdownParseException` is thrown with a message naming the missing field.

**Metadata:** All remaining frontmatter fields (e.g. `topic`, `version`) are preserved as the document's metadata map.

**Body:** Everything after the closing `---\n` delimiter (leading whitespace stripped) becomes `LocalKnowledgeDocument.content`.

**Ordering:** Documents are sorted by ascending `id` before being passed to `LocalKnowledgeCorpus.of(List)`. This makes the corpus ordering deterministic regardless of filesystem iteration order.

**Error propagation:** `MarkdownParseException` is unchecked. `UncheckedIOException` wraps file read failures. Both propagate to the caller without swallowing the cause.

---

## `LocalKnowledgeCorpus.demo()` Stability

`LocalKnowledgeCorpus.demo()` was not changed. It remains a fully programmatic corpus backed by string literals. It has no dependency on classpath resources and requires no file I/O. Framework modules using `LocalKnowledgeRepository.withDemoCorpus()` are unaffected.

The Markdown loader is an additive factory (`MarkdownCorpusLoader.fromDirectory(Path)`, `fromClasspath(String)`) alongside `demo()`. The canonical Markdown documents are the human-editable versions of the same five knowledge items, with expanded prose bodies suited for human review and version-controlled diffs.

---

## Validation

**`git diff --check`:** clean — no trailing whitespace errors.

**`mvn verify`:** BUILD SUCCESS — 69 tests, 0 failures, 0 errors. (13 new tests added; prior count was 56.)

```
Tests run: 69, Failures: 0, Errors: 0, Skipped: 0
```

---

## Limitations

- The Markdown corpus files and the test fixture copies under `src/test/resources/` are maintained as separate copies. There is no automated synchronization between them. If a corpus document is updated, the test fixture must be updated manually. This is documented in `argonaut-core/README.md`.
- `MarkdownCorpusLoader.fromClasspath()` resolves the classpath directory to a `Path` via `resource.toURI()`. This works correctly when the resources are on the local filesystem (standard Maven test resource layout) but will not work for resources inside JAR files. This is sufficient for the current use case (tests and development) and is not a limitation in practice until a packaged-JAR loading scenario is needed.
- The parser does not support multi-line frontmatter values. This is intentional: the controlled corpus format is simple enough that multi-line values are not needed.
- Vector search, BM25, Lucene, Qdrant, OpenSearch, and embedding-based retrieval remain deferred.

---

## Deferred Work

- Synchronization tooling between `docs/knowledge/corpus/` and `src/test/resources/` (not required for this task).
- Making `LocalKnowledgeCorpus.demo()` file-backed was evaluated but deferred: the programmatic corpus requires no classpath setup and keeps the demo factory dependency-free. Defer until there is a clear use case.
- JAR-safe classpath loading (using `getResourceAsStream` instead of `toURI()`) — deferred until a packaged deployment scenario requires it.
