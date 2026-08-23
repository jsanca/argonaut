# Engineering Reporting

## Mission

Create, review, reconcile, and recover non-trivial engineering work with durable implementation, review, fix, checkpoint, and documentation-audit records grounded in actual repository evidence.

## Scope

### Covers

- Completion reporting for non-trivial implementation, review, recovery, architecture, security, and documentation tasks.
- Implementation, review, fix, recovery checkpoint, and documentation-audit report structures.
- Evidence rules, validation reporting, limitations, unresolved issues, and preservation of historical records.

### Does not cover

- Inventing evidence, completion, ownership, behavior, or decisions.
- Treating a checkpoint as an implementation report or silently rewriting historical delivery evidence.
- Defining the specialized methodology of architecture, security, or other review disciplines.

## Responsibilities

- Locate the task and read any open checkpoint before reporting completion.
- Select the matching report type and include the required sections.
- Record only commands and validation results actually observed.
- Link related durable records and update canonical documentation only when repository evidence supports current-state claims.
- State skipped/unavailable validation, unresolved issues, and future behavior explicitly.

## Boundaries / Constraints

- Do not report DONE while an OPEN checkpoint exists; resolve or supersede it first.
- Never claim tests pass, files exist, or work is complete without evidence.
- Keep active canonical documentation about present behavior; put future behavior in planned work or open questions.
- Preserve historical reports through annotation or supersession rather than silent rewriting.
- A recovery checkpoint is temporary operational memory, not an implementation report.

## Required Inputs

- Active task identifier/objective and the work or artifact being reported.
- Actual changed files, validation commands/results, evidence, limitations, and unresolved issues.
- Existing relevant reports and any open recovery checkpoint.

## Expected Outputs

- A correctly typed, structured engineering record with evidence, validation, limitations, follow-ups, and references.
- An engineering-log/index entry where the repository convention requires it.
- Explicit checkpoint status when recovery state exists.

## Workflow

1. Locate the active task and inspect the relevant prior reports and OPEN checkpoint.
2. Select the report type: implementation, review, fix, checkpoint, or documentation audit.
3. Collect repository evidence, files changed, commands run, validation outcomes, and limitations.
4. Populate the required report sections for that type.
5. Link related durable records and update current-state documentation only when the evidence warrants it.
6. Verify that the report does not overclaim success, validation, or scope.
7. Record checkpoint status and any required log/index entry.

## Questions to Ask

- What task and report type is being recorded?
- What commands, tests, or inspections actually ran, and what were their results?
- Is there an OPEN checkpoint that prevents completion reporting?
- Which claims are current-state facts, historical evidence, future plans, or unresolved questions?

## Escalation Rules

- Missing task identity, unavailable evidence, or an unresolved OPEN checkpoint → do not claim completion; request the missing record or resolve the checkpoint.
- Conflicting current-state evidence → report the conflict and escalate to the authority; do not select a winner.
- Hard/early stop before completion → create the recovery checkpoint under osk-execution-timebox.

## Quality Checklist

- [ ] The report type matches the work performed.
- [ ] All mandatory sections for that report type are present.
- [ ] Every validation claim is supported by an actual command or inspection result.
- [ ] Limitations, skipped validation, and unresolved issues are explicit.
- [ ] Historical records are preserved and current-state claims are evidence-backed.
- [ ] No OPEN checkpoint is contradicted by a completion claim.

## Anti-Patterns

- **Claiming tests pass without running them** — record only actual results or state validation was skipped.
- **Using an implementation report as a checkpoint** — use the recovery checkpoint structure and status.
- **Marking work done with an OPEN checkpoint** — resolve or supersede it first.
- **Writing planned behavior as present fact** — place it in follow-ups or open questions.

## Dependencies

| Skill ID | Relationship | Required before | Rationale |
| --- | --- | --- | --- |
| osk-execution-timebox | requires | Reporting interrupted or bounded execution | Defines recovery checkpoint and hard-stop behavior. |

## Activation Conditions

Apply when non-trivial engineering work needs an implementation, review, fix, checkpoint, or documentation-audit record. Do not use a progress update as a substitute for this durable report, and do not apply to a trivial status message with no engineering record to preserve.

## Evidence and Validation

Use observable artifacts, command results, references, and honest limitations as evidence. Validate that each report type satisfies its required sections and that no completion claim exceeds the recorded evidence.

## Supporting Resources

None.
