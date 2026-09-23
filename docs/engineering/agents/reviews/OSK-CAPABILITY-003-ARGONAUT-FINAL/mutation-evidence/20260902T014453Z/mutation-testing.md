# Mutation Testing Evidence — OSK-CAPABILITY-003-ARGONAUT-FINAL

> Capability execution evidence and mutation findings, not a mutation-quality, gate, release, or product-acceptance decision.

## Identity, execution, and provenance

- Capability: mutation-testing version 0.1.0
- Objective: Acquire scoped Java mutation-testing evidence through PIT
- Executor: unavailable
- Started (UTC): 2026-09-02T01:44:53Z
- Ended (UTC): 2026-09-02T01:45:01Z
- Aggregate execution: **PASS**
- Workspace: <WORKSPACE>
- Branch/revision: main / f9910fc6c600f24244cacd9a86855d5bd0ba3aea
- Binding: .osk/mutation-testing.yaml (osk.mutation-testing/v0alpha1)
- Evidence path normalization: workspace, current home, and known temporary/runner roots normalized before persistence.
- Working-tree status: [raw](raw/working-tree-status.txt) SHA-256 ae92d56812cb979e7147a59ae640d64a660051b9ebd64e55af4cfa84e4b7a53e

## Target provider evidence

| Target | Declared execution scope | Provider/tool | Execution | Duration | Normalized findings | Primary/raw report |
| --- | --- | --- | --- | --- | --- | --- |
| `core-metrics` | argonaut-core package dev.jsanca.argonaut.core.metrics and its matching tests; bounded experimental mutation analysis | `pitest/pitest-maven` | `PASS` | 7s | [normalized](raw/core-metrics/normalized-mutation.json) | [primary](raw/core-metrics/primary-mutations.xml) SHA-256 448157d6c3cdfeb04f53157f1b532e0846dd2b47fb7a53f1e341919512c93377 |

## Normalization basis

- pitest-xml: counts provider mutation statuses KILLED, SURVIVED, and TIMED_OUT; all other observed statuses remain in providerSpecificStates.
- stryker-json: counts Killed, Survived, and Timeout; all other observed statuses remain in providerSpecificStates.
- score is intentionally null in the common model: providers have differing denominator/policy treatment. Consumers must inspect the native artifact for provider score semantics.

## What this supports and does not support

PASS establishes that the declared provider completed, its primary report was retained, and supported findings were derived under this recorded scope. Surviving mutants, timeouts, or a low/zero score are findings, not Capability execution failure. This does not establish test adequacy, a quality threshold, release acceptance, or a semantic dependency on coverage-measurement or deterministic-verification.

## Limits

- Scope is declared by the project binding and must be read with every measurement.
- Provider-native status taxonomies and report semantics are intentionally retained rather than forced into false symmetry.
- This is bounded raw-evidence retention and known host-path normalization, not general DLP, secret scanning, provider discovery, or incremental orchestration.
