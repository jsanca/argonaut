# ARGONAUT-KOOG-001
## Implement the minimal Koog equivalent of Argonaut UC-001

### Assignee

Clio

### Objective

Add Koog as the next framework implementation in Argonaut's comparative
AI-framework journey.

Implement the smallest useful Koog-based equivalent of the existing UC-001
behavior already implemented by the other framework modules.

The purpose is NOT to explore all Koog capabilities.

The purpose is to learn how Koog expresses the same existing Argonaut use
case while preserving the project's framework-comparison boundaries.

---

## First principle

Treat the existing Argonaut UC-001 contract and shared project knowledge as
authoritative.

Do not redesign the use case to fit Koog.

Learn the minimum Koog API surface necessary to implement the existing
behavior.

---

## Scope

1. Inspect the existing Argonaut project structure and UC-001 implementations.

2. Determine the appropriate module/package shape for a Koog implementation
   using existing repository conventions.

3. Identify the minimum Koog dependencies required.

4. Implement the Koog equivalent of UC-001.

5. Reuse existing framework-neutral Argonaut contracts and shared components
   where appropriate.

6. Keep Koog-specific implementation details inside the Koog boundary.

7. Add the minimum tests necessary to establish behavioral equivalence with
   the existing implementations.

8. Verify the resulting implementation using appropriate project mechanisms.

---

## Framework discovery

Koog is intentionally treated as an unfamiliar framework for this task.

Investigate only the API surface necessary to implement UC-001.

Do not attempt to learn or document the entire framework.

If uncertainty remains during implementation, obtain sufficient evidence to
resolve the current engineering question before continuing.

---

## Dependency constraint

Do not guess dependency coordinates or versions and silently proceed.

Determine them from an authoritative source available to you.

Keep dependency discovery bounded to what this implementation requires.

Do not upgrade unrelated project dependencies.

---

## Repository boundaries

Work inside the Argonaut repository unless another location is explicitly
authorized.

Do not search unrelated user directories or repositories for possible Koog
material.

Do not clone external repositories unless you first establish that doing so
is necessary and request authorization.

---

## Architecture constraints

Preserve the existing Argonaut principle:

    common behavior / contracts
             |
             +-- Spring AI implementation
             +-- LangChain4j implementation
             +-- LangGraph4j implementation
             +-- Koog implementation

Do not leak Koog types into framework-neutral contracts merely to make the
implementation easier.

Do not redesign the existing modules unless a genuine incompatibility is
discovered.

If such an incompatibility appears, document it rather than performing a
large refactor.

---

## Comparison notes

While implementing, record framework characteristics that materially affect
the existing comparison, including where observable:

- model abstraction;
- prompt/message representation;
- tool definition and execution;
- state handling;
- orchestration/workflow model;
- dependency on Kotlin-specific concepts from Java;
- hidden vs explicit framework behavior;
- integration friction;
- testability;
- framework-neutral abstraction opportunities.

Do not turn this task into a framework review.

Capture only findings encountered while implementing UC-001.

---

## Verification

At minimum establish evidence that:

1. the module builds;
2. relevant tests pass;
3. UC-001 behavior is preserved;
4. existing modules remain unaffected;
5. framework-specific types remain appropriately bounded;
6. `git diff --check` passes.

Use stronger project/engineering evidence where appropriate and available.

---

## Deliverable

Implement the Koog module/use-case integration.

Create an engineering report following existing Argonaut conventions.

The report should include:

- implementation summary;
- files changed;
- Koog API surface actually used;
- dependency/version selected and evidence for that selection;
- relevant architectural observations;
- verification evidence;
- unresolved questions;
- candidate topics for later Koog exploration.

Update the engineering log according to project conventions.

Do not commit unless explicitly authorized.

---

## Non-goals

Do NOT:

- explore all Koog capabilities;
- implement advanced agent workflows;
- implement memory unless UC-001 requires it;
- introduce multi-agent behavior;
- redesign Argonaut;
- redesign the shared framework abstraction;
- modify other framework implementations merely for symmetry;
- clone Koog source without authorization;
- begin Embabel work;
- perform comparative framework conclusions beyond evidence encountered here.

---

## Definition of done

The task is complete when Argonaut contains a minimal, verified Koog
implementation of UC-001 that can later be compared with the existing
framework implementations.

STOP THERE.