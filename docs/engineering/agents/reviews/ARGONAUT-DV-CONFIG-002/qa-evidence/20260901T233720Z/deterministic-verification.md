# Deterministic QA Evidence — ARGONAUT-DV-CONFIG-002

> Execution evidence, not a task/gate/product-acceptance decision. Consumers must assess scope, freshness, and limitations before relying on it.

## Task association

- Task: `ARGONAUT-DV-CONFIG-002`
- Objective: Confirm v0alpha2 multi-target deterministic-verification is correctly configured for backend and ui targets
- Execution scope: `FULL`
- Targets/modules: backend (Maven multi-module, repo root), ui (Vue 3 + Vite, argonaut-ui/)
- Executor: claude-sonnet-4-6
- Workspace: `<WORKSPACE>`

## Execution provenance

- Executed (UTC): 2026-09-01T23:37:20Z
- Branch: `main`
- Revision: `f9910fc6c600f24244cacd9a86855d5bd0ba3aea`
- Capability configuration: `.osk/deterministic-verification.yaml`
- Configuration schema: `osk.deterministic-verification/v0alpha2`
- Evidence redaction/normalization: workspace, current home, and known temporary/runner paths were normalized before persistence.
- Working-tree status: [raw snapshot](raw/working-tree-status.txt) (SHA-256 `280bcc7c61436bbd4ee4e221ef4f0e25541b3c83c6b7d5adb8d1ee33923eda0d`)
- Dirty working tree: `true`
- Unstaged tracked delta: [binary-safe diff](raw/working-tree-unstaged.diff) (SHA-256 `e3faa033e878c7b829b90c16f31292ed8191ba79f949861170c95d9e2fbd1d1d`)
- Staged tracked delta: [binary-safe diff](raw/working-tree-staged.diff) (SHA-256 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`)
- Untracked-file manifest: [paths](raw/untracked-files.txt) (SHA-256 `3309439d7cce3d1fcc438e4a90a0a35f99268ce4be71c190fee094b7b0ea19b8`)

## Results

Overall execution result: **PARTIAL**.

| Target | Working directory | Check | Result | Project-owned command | Raw evidence |
| --- | --- | --- | --- | --- | --- |
| `backend` | `.` | build | PASS | `mvn compile` | [log](raw/backend/build.log) (SHA-256 `577921c154594b20251399095b78de5714de7f1f541d3c0c96fc341545c8b841`) |
| `backend` | `.` | test | PASS | `mvn verify` | [log](raw/backend/test.log) (SHA-256 `f47095e3637dd4aed6efb45c73c8f2d464e0051a072d59692a823ccb0d126bef`) |
| `ui` | `argonaut-ui` | build | PASS | `npm run build` | [log](raw/ui/build.log) (SHA-256 `cbc73ef42336cf3eaff9c18cdcaa57070fb6cd6f1c1d4e30427b322f93bd9b2e`) |
| `ui` | `argonaut-ui` | test | PASS | `npm run test` | [log](raw/ui/test.log) (SHA-256 `af3a4e0eb1f64a03e700e28ef9173c9d619f0e84c94100ade9b4395c0113b54c`) |
| Coverage | POLICY THRESHOLD | no | NOT EXECUTED | No repository coverage command/policy was selected. | — |
| Mutation | EVIDENCE SIGNAL | no | NOT EXECUTED | No Go mutation tool/configuration was selected. | — |
| Change risk / CRAP | EVIDENCE SIGNAL | no | NOT EXECUTED | No Go CRAP analyzer/configuration was selected. | — |
| Static analysis | POLICY THRESHOLD | no | NOT EXECUTED | No additional analyzer/policy was selected. | — |
| Dependency / structural checks | POLICY THRESHOLD | no | NOT EXECUTED | Not selected for this execution. | — |
| Security | EVIDENCE SIGNAL | no | NOT EXECUTED | No scanner or security policy was selected. | — |

## What this evidence supports

The executed deterministic checks produced the recorded result against the revision and working-tree snapshot above. A passing build/test check establishes only that its recorded command exited successfully in this execution. It does not establish architecture correctness, security, complete coverage, mutation adequacy, or task/gate acceptance.

## Not executed and limitations

- This is a FULL execution. Unlisted categories and all listed `NOT EXECUTED` checks are outside its claim.
- The working tree may be dirty; compare the recorded revision and working-tree snapshot to the code under review before treating this evidence as fresh.
- When dirty, this package preserves staged and unstaged *tracked* deltas in binary-safe Git patch form. It preserves untracked paths only, not their contents; an untracked source file can therefore influence execution without being reproducible from this package. Consumers must treat that case as incomplete source provenance and request a bounded task-specific snapshot when material.
- Raw logs are bounded command output for this execution. They are evidence references, not durable project knowledge and may require redaction before external sharing.
- Review Skills may consume this package without rerunning checks when its scope and freshness are sufficient; they retain responsibility for judgment and may request more evidence.

## Reproduction

From the recorded workspace revision where practical:

```sh
.osk/capabilities/deterministic-verification/verify.sh --task 'ARGONAUT-DV-CONFIG-002' --objective 'Confirm v0alpha2 multi-target deterministic-verification is correctly configured for backend and ui targets' --scope 'FULL' --targets 'backend (Maven multi-module, repo root), ui (Vue 3 + Vite, argonaut-ui/)' --checks 'build,test' --executor 'claude-sonnet-4-6' --evidence-dir '<WORKSPACE>/docs/engineering/agents/reviews'
```

The command creates a new timestamped package; it does not overwrite this one.
