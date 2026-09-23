# Deterministic QA Evidence — ARGONAUT-DV-CONFIG-001

> Execution evidence, not a task/gate/product-acceptance decision. Consumers must assess scope, freshness, and limitations before relying on it.

## Task association

- Task: `ARGONAUT-DV-CONFIG-001`
- Objective: Confirm deterministic-verification capability is correctly configured for the Argonaut workspace
- Execution scope: `FULL`
- Targets/modules: all Maven modules (argonaut-core, argonaut-spring-ai, argonaut-langchain4j, argonaut-langgraph4j, argonaut-koog, argonaut-embabel, argonaut-vector)
- Executor: claude-sonnet-4-6
- Workspace: `<WORKSPACE>`

## Execution provenance

- Executed (UTC): 2026-09-01T21:12:44Z
- Branch: `main`
- Revision: `f9910fc6c600f24244cacd9a86855d5bd0ba3aea`
- Capability configuration: `.osk/deterministic-verification.yaml`
- Build command: `mvn compile`
- Test command: `mvn verify`
- Evidence redaction/normalization: workspace, current home, and known temporary/runner paths were normalized before persistence.
- Working-tree status: [raw snapshot](raw/working-tree-status.txt) (SHA-256 `2c13da222d98ed30ca96a1406818aaab3c02ba0c2564838984330b7f3ee2a659`)
- Dirty working tree: `true`
- Unstaged tracked delta: [binary-safe diff](raw/working-tree-unstaged.diff) (SHA-256 `95e1dffb6918e6aef6fee853babf717a65a7a961d110ed567c12f145cd680b80`)
- Staged tracked delta: [binary-safe diff](raw/working-tree-staged.diff) (SHA-256 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`)
- Untracked-file manifest: [paths](raw/untracked-files.txt) (SHA-256 `15c8db73771dbf3a32efaa504fef421c017e6ad4c8c4eb20fc560033afe39bc4`)

## Results

Overall execution result: **PARTIAL**.

| Check | Classification | Requested / executed | Result | Scope and command | Raw evidence |
| --- | --- | --- | --- | --- | --- |
| Build / compilation | DETERMINISTIC GATE | yes | PASS | project-owned; `mvn compile` | [log](raw/build.log) (SHA-256 `a9459b4c8b395313c7df24ad164a71acae4c6d22d1a856d77f10124ecd8685ea`) |
| Project tests | DETERMINISTIC GATE | yes | PASS | project-owned; `mvn verify` | [log](raw/test.log) (SHA-256 `5a1dea3f2617ef8d31c2798d688c048830913a282c3cdccbc186486bdb86c6c8`) |
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
.osk/capabilities/deterministic-verification/verify.sh --task 'ARGONAUT-DV-CONFIG-001' --objective 'Confirm deterministic-verification capability is correctly configured for the Argonaut workspace' --scope 'FULL' --targets 'all Maven modules (argonaut-core, argonaut-spring-ai, argonaut-langchain4j, argonaut-langgraph4j, argonaut-koog, argonaut-embabel, argonaut-vector)' --checks 'build,test' --executor 'claude-sonnet-4-6' --evidence-dir '<WORKSPACE>/docs/engineering/agents/reviews'
```

The command creates a new timestamped package; it does not overwrite this one.
