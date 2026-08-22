# ARGONAUT-007 — Controlled Local Evidence RAG Baseline — Adversarial Review

## Status

Complete

## Reviewed Target

Claim under challenge: the controlled local evidence RAG baseline (contracts ARGONAUT-003, `LocalKnowledgeRepository` ARGONAUT-004, use case ARGONAUT-005, Markdown corpus/loader ARGONAUT-006) is strong enough to serve as a shared, fair foundation before the first framework implementation (Spring AI / LangChain4j / LangGraph4j / Embabel).

Scope: review only. No production code, corpus, or use-case rewrites. No framework work.

Authoritative inputs inspected:

- `README.md`, `docs/PROJECT.md`, `docs/OSK.md`
- `docs/knowledge/common-contract.md`
- `docs/knowledge/use-cases/controlled-local-evidence-rag.md`
- `docs/knowledge/corpus/controlled-local-evidence/` (5 Markdown docs)
- `docs/engineering/ENGINEERING_LOG.md` and reports ARGONAUT-004/005/006
- `argonaut-core` main + test sources under `knowledge`, `knowledge.local`, `experiment`, `evidence`, `trace`, `observability`, `metrics`
- Skills: `osk-adversarial-analysis`, `osk-boundary-review`, `osk-verification-engineering`, `osk-engineering-reporting`

## Executive Summary

The baseline is a credible **contract + fixture skeleton**, not yet a **comparison-ready experiment harness**. Lexical search is deterministic for a fixed query and corpus; IDs and titles are stable; core stays free of orchestration types. Those strengths do not yet prevent unfair or gamed framework comparison.

Three material risks dominate:

1. **Dual corpus drift (High, verified):** `LocalKnowledgeCorpus.demo()` and the Markdown corpus share IDs/titles but **not** body text. Frameworks using `withDemoCorpus()` vs `MarkdownCorpusLoader` do not share the same evidence surface.
2. **Success criteria are gameable (High, plausible):** A framework can satisfy SC 1–7 with a single hard-coded `exp-001` evidence item, synthetic trace events, and a canned answer—without meaningful retrieval or multi-step RAG.
3. **No executable use-case test (High, validation gap):** TC-UC-001 is documented but not implemented. Nothing asserts end-to-end fairness invariants before frameworks land.

Recommended next task: harden baseline invariants (single corpus authority, anti-gaming assertions, TC-UC-001 harness) **before** Spring AI.

## Assumptions Identified

| Assumption | Evidence | Why It Matters | Confidence |
| --- | --- | --- | --- |
| A1. All frameworks will use the same corpus content | Use case actors/preconditions name `LocalKnowledgeCorpus.demo()`; common-contract also documents Markdown corpus as “the same five documents” | Dual sources with different bodies break “same evidence” | High |
| A2. Lexical scorer ranks mission-relevant docs for reasonable queries | `LocalKnowledgeRepository` scoring; tests for selected queries; use-case expects `exp-001` primary | Wrong ranking → frameworks that follow search order look worse | High |
| A3. Success criteria force meaningful retrieval + synthesis | Use case SC 1–9 | If gameable, comparison is theater | High |
| A4. Trace event presence implies real capability invocation | Trace expectations table; `ExecutionEvent` is free-form metadata | Events can be emitted without calling `KnowledgeRepository` | High |
| A5. Metrics are comparable across frameworks | `ExecutionMetrics` fields; use case SC 5–7 | Self-reported counts need not match evidence/trace | High |
| A6. Determinism of search is enough for experiment determinism | `search_isDeterministic`, loader order tests | LLM steps, clocks, and answer text remain non-deterministic | High |
| A7. `demo()` and Markdown stay in sync via process/discipline | ARGONAUT-006 kept `demo()` unchanged; dual paths in README | Drift is already present | High (drift verified) |
| A8. Missing/malformed/duplicate corpus cases are safe | Loader throws on missing id/title/delimiter; corpus `of()` has no unique-id check | Duplicate IDs or empty directory edge cases | Medium |
| A9. Core contract does not constrain orchestration | Forbidden types list; `KnowledgeRepository` is capability-only | Over-specified event sequences or required tool shape could homogenize frameworks | Medium |
| A10. No-external-call (SC8) is enforceable from result alone | Use case SC8 | Not observable in `ExperimentResult` | High |
| A11. `finalAnswer` semantic points are evaluable | Expected output bullets | No automated semantic checker; LLM answers vary | High |
| A12. Classpath loader works in all runtime layouts | `fromClasspath` → `Path.of(resource.toURI())` | Fails for JAR-packaged resources (noted in ARGONAUT-006) | High |

## Counterexamples

| Counterexample | Target Assumption | Expected Failure | Verification Status |
| --- | --- | --- | --- |
| C1. Framework A uses `withDemoCorpus()`; Framework B loads Markdown | A1, A7 | Different bodies/token sets → different excerpts, scores, readable facts | **Verified** — body lengths differ (e.g. exp-001 demo ~1846 chars vs MD ~3717); bodies not equal after whitespace normalize |
| C2. Framework emits min trace events + one `Evidence(sourceId=exp-001)` without calling `search`/`read` | A3, A4 | Passes SC 1–7 while skipping retrieval | **Plausible but unverified** — no runner exists; contract allows constructing `ExperimentResult` freely |
| C3. Query = full experiment question as-is | A2 | Still ranks exp-001 first (score 0.48) but pulls vt/obs via common tokens | **Verified** (offline scorer matching Java tokenize/score on MD bodies) |
| C4. Query = `"agentic frameworks"` | A2 | **obs-001 ranks first** (0.75), exp-001 second (0.50) | **Verified** on MD corpus |
| C5. Query = stop-word soup `"the a and or of"` | A2 | All five docs score; ranking is non-discriminative noise | **Verified** on MD corpus |
| C6. Framework sets `metrics.knowledgeSearches=1` but empty/mismatched evidence | A5 | Metrics look healthy; evidence/trace disagree | **Plausible but unverified** — `ExecutionMetrics` has no cross-field validation |
| C7. `LocalKnowledgeCorpus.of(doc, doc)` same id twice | A8 | `findById` returns first; search may double-score path depends on iteration | **Plausible but unverified** — no uniqueness guard in `of()` |
| C8. COMPLETED result with blank/`null` finalAnswer | A3, A11 | SC2 requires non-blank; record allows null answer on completed factory | **Verified** — `ExperimentResult.completed` does not validate finalAnswer |
| C9. Trace has required types out of logical order (ANSWER before SEARCH) | A4 | Use case says “logical order”; nothing enforces order | **Plausible but unverified** — only insertion order stored |
| C10. Framework bypasses `KnowledgeRepository` with private docs | A1, A3 | Same mission, different evidence → unfair comparison | **Plausible but unverified** — no adapter policy/test |
| C11. Two frameworks both “deterministic” but different model temperatures | A6 | Evidence IDs match; answers/metrics diverge → misread as orchestration | Observation / design limit |
| C12. `fromClasspath` inside a fat JAR | A12 | `Path.of(jar:file:...)` fails | **Plausible but unverified** in this env; mechanism documented in ARGONAUT-006 |

## Failure Modes

| Failure Mode | Trigger | Impact | Existing Coverage | Gap |
| --- | --- | --- | --- | --- |
| FM1. Unfair comparison via corpus split | Some modules use demo(), others Markdown | Invalidates “same evidence” principle | Titles/IDs tested separately; no parity test | No demo↔MD content equivalence test |
| FM2. Hollow success (gamed baseline) | Hard-coded evidence + trace | Framework looks compliant without RAG | Unit tests on records only | No anti-gaming / integration assertions |
| FM3. Query-sensitive wrong top-doc | Agent chooses weak search query | Misses exp-001 or ranks distractors first | Happy-path queries only (`controlled evidence experiment`) | No golden query set from use-case question variants |
| FM4. Common-word pollution | Broad/natural-language queries | Scores inflated; weak discrimination | Documented as acceptable in ARGONAUT-004 | No stop-word policy; no regression on known noisy queries |
| FM5. Metrics/trace/evidence inconsistency | Manual metrics builder misuse | Vue comparison rows lie | Metrics builder unit tests | No invariant: metrics ↔ trace counts ↔ evidence.size |
| FM6. Silent duplicate IDs | Malformed custom/loaded corpus | Ambiguous read; unstable mental model | Loader does not check duplicates | Reject or fail-fast on duplicate id |
| FM7. Semantic answer drift | LLM synthesis without grounding check | SC2 only “non-blank + human judgment” | None automated | Need checklist or lightweight claim→source checks later |
| FM8. SC8 unenforceable | External calls not represented | “Local-only baseline” is honor system | Out of scope of result DTO | Policy/doc + future adapter flags |
| FM9. Loader JAR failure | Runtime packaging | Framework service can’t load MD corpus | Test classpath filesystem only | Document constraint or stream-based loader |
| FM10. Event order / pairing weak | Unpaired STARTED/COMPLETED | Trace looks complete under “type present” checks | None | Pairing + order assertions in TC-UC-001 |

## Boundary Conditions

| Boundary | Observed behavior | Risk |
| --- | --- | --- |
| Blank query | `KnowledgeSearchRequest` rejects blank | OK |
| Punctuation-only / numeric tokens | Repo returns empty or no-match without throw | Covered lightly |
| topK=1 | Honored | Tested |
| Unknown read id | `KnowledgeSourceException` | Tested |
| Missing frontmatter id/title/delimiter | `MarkdownParseException` | Tested |
| Empty directory | `MarkdownParseException` (“no .md files”) | Untested explicitly |
| Duplicate document ids | Allowed in `LocalKnowledgeCorpus.of` | Untested |
| Blank document content | Document allows empty content string | Untested for search quality |
| COMPLETED + null finalAnswer | Allowed | Conflicts with SC2 intent |
| Score non-finite | `Evidence` rejects NaN/Inf | Tested in Evidence |
| CRLF Markdown | `indexOf("\n---")` still finds closer; `stripLeading` drops leftover `\r` | Plausible OK; no explicit test |
| Concurrent observer writes | `CopyOnWriteArrayList` | Basic thread-safety test exists |
| Negative/zero metrics | Allowed (ints unconstrained) | Untested |

## Invariants Challenged

| Invariant (stated or implied) | Status |
| --- | --- |
| Same mission / same evidence / same model across frameworks | **Broken for evidence** if dual corpus used (C1) |
| exp-001 is primary and reliably retrieved | **Holds** for curated queries; **fails** for some natural queries (C4) |
| Success criteria imply agentic RAG occurred | **Weak** — structural only (C2) |
| Deterministic re-run → same evidence source IDs | **Holds** for fixed repo+query; **does not** cover LLM path (A6) |
| Trace types in logical order | **Unenforced** |
| Metrics reflect actual work | **Unenforced** |
| Core contains no orchestration | **Holds** on inspection (no Agent/Graph/Node/Planner/Workflow types) |
| demo() is the baseline fixture for frameworks | **Stated** in use case; **competes** with Markdown loader guidance |

## Validation Gaps

1. **No TC-UC-001** (or any framework-agnostic experiment runner test).
2. **No demo ↔ Markdown parity** (ids, titles, and especially body/token equivalence or explicit “intentionally divergent” ADR).
3. **No golden query suite** derived from the experiment question and expected rankings (exp-001 first for mission queries).
4. **No anti-gaming tests** (must call real `KnowledgeRepository`; evidence sourceIds ⊆ search/read results; metrics ↔ trace).
5. **No duplicate-id / empty-corpus / blank-body loader corpus tests.**
6. **No trace pairing/order helper** used by assertions.
7. **No finalAnswer non-blank enforcement** on `COMPLETED`.
8. **No SC8 observability** (external call flag or test profile without network).
9. **No fixture sync check** between `docs/knowledge/corpus/...` and `argonaut-core/src/test/resources/...` (currently identical by inspection; not gated in CI — and there is no CI yet).
10. **No stop-word / noise-query characterization test** (document expected weakness or harden tokenizer).

## Recommended Hardening

Priority-ordered; smallest useful moves first.

| ID | Action | Severity addressed |
| --- | --- | --- |
| H1 | Declare **one authoritative corpus** for the experiment (recommend Markdown-loaded corpus or make `demo()` load the same bytes). Document the other as legacy/test-only. | High |
| H2 | Add **parity test**: same ids, titles; either equal normalized bodies or fail build on drift. | High |
| H3 | Implement **TC-UC-001 harness** in `argonaut-core` (or test utility) that accepts a `Function<ExperimentRequest, ExperimentResult>` / port so each framework plugs in without shared orchestration. | High |
| H4 | Assertions beyond SC minimums: evidence must include `exp-001`; every evidence `sourceId` must appear in some search or read path recorded in trace metadata; `metrics.knowledgeSearches/documentReads/evidenceCount` match trace/evidence; STARTED/COMPLETED pairing; RUN_STARTED first / RUN_COMPLETED last. | High |
| H5 | Golden queries: at least (a) use-case suggested query, (b) full experiment question, (c) `"controlled local evidence"` — all must rank `exp-001` first on the authoritative corpus. | Medium |
| H6 | Reject duplicate ids in `LocalKnowledgeCorpus.of` / loader. | Medium |
| H7 | `ExperimentResult.completed` requires non-blank `finalAnswer`. | Medium |
| H8 | Document SC8 as **process constraint** (test profile, no API key, mock model) until result can carry `externalCalls=0`. | Medium |
| H9 | Optional: stop-word filter or min token length ≥ 3 to reduce C5 noise — only if still “intentionally simple lexical.” | Low |
| H10 | Document JAR classpath loader limitation in framework READMEs; prefer `fromDirectory` at runtime. | Low |

## Recommended Tests

| Test | Intent |
| --- | --- |
| `demoAndMarkdown_sameIdsAndTitles` | Catch ID/title drift |
| `demoAndMarkdown_bodyParity_or_explicitSkip` | Catch C1 |
| `goldenQuery_exp001_first` × N queries | Lock retrieval expectation |
| `noisyStopWords_documentedBehavior` | Characterize C5 |
| `corpus_rejectsDuplicateIds` | FM6 |
| `loader_emptyDirectory_fails` | Boundary |
| `completedResult_rejectsBlankAnswer` | Align with SC2 |
| `TC-UC-001_structural` on a **reference fake runner** that uses real `LocalKnowledgeRepository` | Prove harness; define bar for frameworks |
| `TC-UC-001_rejectsHollowResult` (constructed result without repo use) | Anti-gaming |
| `metricsConsistentWithTraceAndEvidence` | FM5 |
| `docsCorpus_matches_testResources` byte or hash compare | Fixture sync |

## Findings by Severity

### Critical

None verified. Experiment not yet runnable across frameworks.

### High

1. **Dual corpus content drift** — demo vs Markdown bodies differ; “same evidence” is false if both are used. (Verified, C1)
2. **Success criteria gameable** — structural SC 1–7 do not prove retrieval. (Plausible, C2)
3. **No TC-UC-001 / comparison harness** before framework work. (Validation gap)
4. **Query ranking fragility** — some natural queries promote obs-001 over exp-001. (Verified, C4)

### Medium

5. Metrics/trace/evidence can disagree without detection. (A5, FM5)
6. Trace logical order and STARTED/COMPLETED pairing unenforced. (C9, FM10)
7. Duplicate corpus IDs allowed. (C7)
8. COMPLETED allows null/blank finalAnswer. (C8)
9. SC8 (no external calls) not observable on result. (A10)
10. finalAnswer semantic criteria not machine-checkable. (A11)

### Low

11. No stop-word handling; noisy queries non-discriminative. (C5, accepted in ARGONAUT-004 notes)
12. Classpath loader JAR limitation. (A12)
13. Empty-directory / blank-body edges lightly covered.
14. No CI to enforce fixture sync or Java 25 verify on PR.

### Observation

15. Core boundary (no orchestration types / no framework deps) appears intact — good.
16. Lexical scorer is intentionally simple; that is fine if golden queries and single corpus are locked.
17. Use case still points frameworks at `demo()` while Markdown is the human-canonical expanded prose — messaging conflict.
18. Model non-determinism will dominate answer text even when evidence IDs match; comparison design should weight evidence/trace/metrics over prose equality.

## Limitations

- Review-only: no production code changes; counterexamples C2/C6/C7/C9/C10 not executed as new tests.
- Offline Python scorer mirrored Java tokenize/score for ranking checks; not a JVM assertion in-repo.
- No framework module behavior to review (skeletons only).
- `mvn verify` confirms current tests pass; it does not validate use-case satisfaction.
- Security/dynamic probing out of scope per adversarial skill policy.

## Validation

```text
git diff --check     → clean (exit 0)
mvn verify           → BUILD SUCCESS
mvn test -pl argonaut-core → Tests run: 69, Failures: 0, Errors: 0, Skipped: 0
```

Manual inspections: demo vs MD body inequality; ranking table for listed queries on MD corpus; source read of repository, loader, result, metrics, use case, contracts.

## Related Records

| Record | Path |
| --- | --- |
| Task | `docs/engineering/agents/tasks/review/ARGONAUT-007—AdversarialReviewOfTheControlledLocalEvidenceRAGBaseline.md` |
| Use case | `docs/knowledge/use-cases/controlled-local-evidence-rag.md` |
| Contract | `docs/knowledge/common-contract.md` |
| ARGONAUT-004 report | `docs/engineering/agents/reports/ARGONAUT-004-controlled-local-knowledge-repository.md` |
| ARGONAUT-005 report | `docs/engineering/agents/reports/ARGONAUT-005-controlled-local-evidence-rag-use-case.md` |
| ARGONAUT-006 report | `docs/engineering/agents/reports/ARGONAUT-006-controlled-markdown-knowledge-corpus.md` |
| Engineering log | `docs/engineering/ENGINEERING_LOG.md` |

## Recommended Next Hardening Task

**ARGONAUT-008 (proposed): Baseline fairness hardening**

1. Single authoritative corpus + demo/MD parity test (H1–H2).  
2. TC-UC-001 harness + anti-gaming assertions (H3–H4).  
3. Golden query ranking locks for exp-001 (H5).  
4. Duplicate-id rejection + completed-answer validation (H6–H7).  

Do **not** start Spring AI until H1–H4 land (or are explicitly accepted as known risks).
