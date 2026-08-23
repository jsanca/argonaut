# ARGONAUT-EMBABEL-001
## Implement the minimal Embabel equivalent of Argonaut UC-001

### Assignee
Clio

### Purpose

Implement the Embabel equivalent of Argonaut UC-001 using Embabel 1.5.0.

This task is also an experiment in **architect-prepared framework evidence**.

Unlike prior framework work, the relevant Embabel source and examples have
already been provided locally.

Use those sources before performing external framework discovery.

---

# Authoritative local framework evidence

Embabel example source is available at:

    ./libs-code/embabel-agent-examples/

Start from these files/areas:

    ./libs-code/embabel-agent-examples/README.md

    ./libs-code/embabel-agent-examples/pom.xml

    ./libs-code/embabel-agent-examples/examples-java/pom.xml

    ./libs-code/embabel-agent-examples/examples-java/README.md

For the Embabel agent programming model, inspect:

    examples-java/src/main/java/com/embabel/example/horoscope/StarNewsFinder.java

Relevant concepts already identified there include:

    @Agent
    @Action
    @AchievesGoal
    @Export
    Ai
    OperationContext
    typed action inputs and outputs

If useful, the examples also contain handoff/subagent examples demonstrating:

    Subagent.ofClass(...)
    Subagent.byName(...)

Do not broadly inspect the repository.

Follow references only when they answer a concrete implementation question.

---

# Framework version

Use:

    Embabel Agent 1.5.0

The upstream examples parent uses:

    com.embabel.agent:embabel-agent-dependencies:1.5.0

as its BOM.

Do not perform version discovery unless local Maven evidence contradicts this.

---

# Dependency preparation

The `argonaut-embabel` module already contains initial Embabel dependency
configuration.

Before implementing code:

1. inspect the current module POM;
2. compare it with the provided local Embabel example POMs;
3. make only the minimum dependency corrections necessary.

Prefer importing the Embabel BOM rather than independently versioning Embabel
modules.

Determine from local source whether:

    embabel-agent-platform-autoconfigure

or:

    embabel-agent-starter

is the appropriate minimal dependency for this Argonaut service.

Do not add shell, MCP server, A2A server, observability infrastructure, or
unrelated starters merely because the examples contain them.

---

# Repository boundary

Work inside:

    /Users/jonathan/code/argonaut

The explicitly authorized external reference tree for this task is:

    ./libs-code/embabel-agent-examples/

You may read that tree as local framework evidence.

Do not search elsewhere on the machine.

Do not clone Embabel or another repository.

Do not perform broad remote framework research while relevant local evidence
remains available.

If a framework question cannot reasonably be answered from the authorized
local source, state the missing evidence before changing evidence source.

---

# Objective

Implement the smallest Embabel-native equivalent of the existing controlled
local evidence UC-001.

Use the existing:

    ControlledLocalEvidenceContract
    ExperimentRequest
    ExperimentResult
    KnowledgeRepository
    execution/evidence contracts
    common system prompt

Do not redesign UC-001 to fit Embabel.

---

# Embabel-specific objective

Use Embabel according to its native programming model.

Do not reproduce the LangGraph4j or Koog implementation mechanically.

In particular investigate whether UC-001 should naturally be expressed using:

    typed state
    @Action transformations
    @AchievesGoal
    Embabel Ai / OperationContext
    planner-driven execution

rather than manually recreating a ReAct loop.

The experiment should reveal what Embabel actually contributes.

---

# Architecture boundary

Preserve:

    argonaut-core
        framework-neutral experiment semantics

    argonaut-embabel
        Embabel-specific implementation

Do not leak Embabel classes or annotations into `argonaut-core`.

Do not modify existing framework implementations merely for symmetry.

---

# Knowledge capability

UC-001 still requires the same semantic operations:

    searchKnowledge
    readDocument

The actual knowledge implementation remains:

    KnowledgeRepository

How those capabilities are exposed to Embabel should be Embabel-native.

Do not create a new common Tool framework abstraction in this task.

One purpose of Argonaut is to observe how each framework naturally exposes
tools/actions.

---

# Planning question

A major objective of this implementation is to determine:

> Does Embabel naturally model UC-001 as a goal over typed Actions rather than
> as an explicit agent/tool loop?

Record evidence from the implementation.

Do not force the answer either way.

---

# Observability

Preserve the existing Argonaut trace contract where reasonably observable.

Record which lifecycle/model/tool events Embabel exposes natively and which
would require synthetic instrumentation.

Do not invent fake precision merely to match another framework.

---

# Testing

Prefer Embabel's native testing support where useful.

The local examples demonstrate `FakeOperationContext` for isolated Action/LLM
interaction testing.

Determine whether Embabel provides an appropriate mechanism for testing the
complete planned agent flow.

Do not perform broad framework archaeology solely to find a test abstraction;
use existing Argonaut contract tests if they provide sufficient evidence.

---

# Verification

At minimum:

1. `argonaut-embabel` compiles.
2. Its relevant tests pass.
3. TC-UC-001 passes using the same shared contract.
4. Existing modules remain green.
5. Embabel types remain inside the Embabel module.
6. `git diff --check` passes.

Run the appropriate Maven reactor verification when implementation is ready.

---

# Research discipline

This task intentionally provides local framework evidence.

Observe this rule:

> Spend reasoning on understanding and implementing the framework, not on
> rediscovering evidence already prepared for the task.

Prefer:

    local example
        →
    relevant source/API
        →
    compilation/test feedback

over broad remote discovery.

Do not read the entire Embabel source tree.

---

# Report

Create the appropriate Argonaut engineering report.

Include:

- Embabel API surface actually used;
- final dependency setup;
- how Actions/Goals/state were modeled;
- how the agent is invoked;
- how knowledge access is exposed;
- planning behavior;
- per-run state;
- observability;
- testing strategy;
- implementation friction;
- comparison observations arising directly from UC-001;
- framework-neutral abstraction opportunities;
- limitations.

Also record:

    approximate research time before first meaningful implementation;
    significant context/tool-output pressure if observable;
    whether remote framework discovery became necessary.

This will be compared qualitatively with the Koog implementation.

---

# Non-goals

Do NOT:

- explore all Embabel functionality;
- implement MCP;
- implement A2A;
- implement secured agents;
- implement subagents unless UC-001 actually requires them;
- implement HITL;
- implement multi-model workflows;
- redesign Argonaut;
- create Jigsaw abstractions;
- create canonical Tool abstractions;
- refactor LangGraph4j;
- refactor Koog;
- begin UC-002;
- clone repositories;
- perform broad dependency research.

---

# Definition of done

The task is complete when:

1. Argonaut contains a minimal Embabel-native implementation of UC-001.
2. It satisfies the existing controlled-local-evidence contract.
3. The implementation preserves Embabel's native semantics rather than
   mimicking another framework.
4. Local prepared framework evidence was used as the primary source.
5. Verification evidence is recorded.
6. The engineering report explains what Embabel taught us.

STOP THERE.