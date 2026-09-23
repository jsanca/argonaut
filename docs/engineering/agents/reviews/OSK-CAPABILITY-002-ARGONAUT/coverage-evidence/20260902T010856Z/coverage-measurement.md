# Coverage Measurement Evidence — OSK-CAPABILITY-002-ARGONAUT

> Capability execution evidence and measured findings, not a coverage policy, quality score, gate, or product-acceptance decision.

## Capability identity and intent

- Capability: <code>coverage-measurement</code> version <code>0.1.0</code>
- Objective: Acquire Java coverage evidence through the project-selected provider
- Target selection: project-declared coverage targets
- Executor: unavailable
- Executed (UTC): 2026-09-02T01:08:56Z
- Execution result: **PASS**
- Evidence normalization: workspace, current home, and known temporary/runner paths were normalized before persistence.

## Provenance

- Workspace: <code>&lt;WORKSPACE&gt;</code>
- Branch: <code>main</code>
- Revision: <code>f9910fc6c600f24244cacd9a86855d5bd0ba3aea</code>
- Binding: <code>.osk/coverage-measurement.yaml</code> (<code>osk.coverage-measurement/v0alpha1</code>)
- Working-tree status: [raw snapshot](raw/working-tree-status.txt) (SHA-256 <code>6a23cc57181c09fa9b8db620fcae778ed7a0528b12a47ea572a935ff12a6500d</code>)
- Unstaged delta: [raw snapshot](raw/working-tree-unstaged.diff) (SHA-256 <code>39e00a8173f5d659652ac10c4f73909c79da7fd320cec36c25528c187a94f4f3</code>)
- Staged delta: [raw snapshot](raw/working-tree-staged.diff) (SHA-256 <code>e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855</code>)

## Provider execution and evidence

| Target | Provider / tool | Execution | Finding | Normalized measurement | Primary/raw provider evidence |
| --- | --- | --- | --- | --- | --- |
| `java-reactor` | `jacoco-maven` / `maven` | `PASS` | `MEASURED` | [normalized measurement](raw/java-reactor/normalized-measurement.json) (SHA-256 `b7ac6d001e400ed1d67dc2adc538377bd288e42956f4a70dcaf522c494b0ff4e`) | [primary artifact](raw/java-reactor/primary-jacoco.xml) (SHA-256 `3c8d20dbc28dc9a3f8484b1993ee29f11f3c766803b3a205be6f98baaca78cb1`) |

## Derivation trace

For each <code>MEASURED</code> target, the retained primary provider artifact was parsed by the declared format adapter:

- <code>jacoco-xml</code>: final report-level <code>LINE</code> and, when present, <code>BRANCH</code> counters.
- <code>istanbul-json-summary</code>: provider's <code>total.lines</code> and <code>total.branches</code> summary fields.

The provider command log remains at <code>raw/&lt;target&gt;/provider.log</code>; the primary artifact remains adjacent to it. A normalized value without both a successful execution and retained primary artifact is not represented as valid coverage evidence.

## Limits

- Measurement is not judgment: no threshold or quality decision was applied.
- Only the two declared provider artifact formats are supported in this vertical slice.
- Provider configuration, installation, and suitability remain project-owned adaptation decisions.
- Raw evidence is bounded and normalized for known host paths; this is not generic secret or PII scanning.
