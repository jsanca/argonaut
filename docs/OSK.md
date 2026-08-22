# OSK Workspace Guide

## Start Here

Read [PROJECT.md](PROJECT.md) for project context. Use this guide to decide where new information belongs. Keep facts traceable to their evidence and do not treat tool-specific instruction files as canonical knowledge.

## Information Model

| Concern | Meaning | Canonical location |
| --- | --- | --- |
| Engineering | What happened | `docs/engineering/` |
| Knowledge | What is true | `docs/knowledge/` |
| Decisions | Why we chose it | `docs/decisions/adr/` |
| Roadmap | Where we intend to go | `docs/roadmap/ROADMAP.md` |
| Future | Where we might go | `docs/roadmap/future/` |

## Engineering

Record task evidence, reports, reviews, checkpoints, and reproducible validation under `docs/engineering/`. Keep [ENGINEERING_LOG.md](engineering/ENGINEERING_LOG.md) as the stable active log. Future rotation may archive completed entries, but must retain this active path. Do not claim results that evidence does not support.

## Knowledge

Place durable current understanding under `docs/knowledge/`: domain concepts, actors, entities, flows, terminology, and current architecture. Put the conclusion here and retain the discovery or validation history in engineering records. Create a subdirectory only when it aids navigation.

## Decisions

Place significant decision records in `docs/decisions/adr/`. An ADR explains why a choice was made and remains historical evidence. Describe the system as it works now in `docs/knowledge/architecture/`, not by rewriting historical decisions.

## Roadmap and Future

Use [ROADMAP.md](roadmap/ROADMAP.md) for intended, committed direction. Preserve non-committed ideas in `docs/roadmap/future/`; they do not become roadmap work unless explicitly adopted.

## OSK-Managed Artifacts

`.osk/` is reserved for OSK-managed state. Installed canonical Skills belong under `.osk/skills/`; do not duplicate their package contracts in project `docs/`.

Future agent integrations should reference this canonical Skill source through small adapters. If a tool requires a generated copy, treat it as derived and disposable, with `.osk/skills/` remaining authoritative. Do not use symlinks as the canonical integration mechanism.

## Contribution Checks

- [ ] Put the artifact in the location matching its information type.
- [ ] Link durable claims to evidence or an authority.
- [ ] Keep current knowledge separate from historical decisions and engineering records.
- [ ] Keep future ideas separate from committed roadmap work.
- [ ] Update `PROJECT.md` when project context materially changes.
