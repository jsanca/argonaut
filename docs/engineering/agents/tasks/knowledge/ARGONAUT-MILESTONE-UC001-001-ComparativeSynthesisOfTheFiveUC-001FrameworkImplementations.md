# ARGONAUT-MILESTONE-UC001-001
## Comparative synthesis of the five UC-001 framework implementations

### Assignee
Elito

### Type
Engineering milestone / comparative architecture review

### Objective

Close Argonaut UC-001 by synthesizing what the five implementations taught us:

- Spring AI
- LangChain4j
- LangGraph4j
- Koog
- Embabel

This is NOT a framework popularity comparison and NOT a recommendation to
standardize Argonaut on one framework.

The purpose is to identify:

1. which semantics belong to Argonaut;
2. which mechanics belong to frameworks;
3. where each framework naturally owns control;
4. where UC-001 exercises or under-exercises each framework;
5. what questions UC-002 must answer.

---

## Authoritative evidence

Use the existing implementation reports, source code, tests, and UC-001
contract already present in the repository.

At minimum inspect the final implementation/report for each framework and the
shared:

- ControlledLocalEvidenceContract
- ExperimentRequest / ExperimentResult
- KnowledgeRepository
- Evidence
- ExecutionTrace
- ExecutionMetrics

Do not perform external research.

Do not modify framework implementations.

---

## Required comparison axes

For each framework analyze:

### Execution model
Who controls execution?

- framework
- LLM
- explicit graph
- strategy
- planner
- application code

### Retrieval ownership
Who decides when/how retrieval happens?

### State ownership
Where does per-run mutable state live?

### Tool semantics
How are tools exposed and invoked?
Is a tool abstraction even required?

### Control-flow visibility
How much of the execution topology is visible to application code?

### Model invocation
How explicit or hidden are model calls?

### Observability
Which events/counts are natively observable?
Which Argonaut events are synthetic?
Which metrics are approximate or hard-coded?

### Testability
What is the natural unit of testing?

- action
- tool
- prompt
- service
- graph
- agent
- platform

### Framework leakage
What framework-specific concepts leak into the implementation boundary?

### Boilerplate / ceremony
Separate:

- unavoidable framework semantics
- accidental adapter boilerplate
- repeated Argonaut experiment mechanics

### Developer ergonomics
What was straightforward or awkward in this specific implementation?

Do not infer general framework quality solely from UC-001 ergonomics.

### Framework power exercised
Explicitly distinguish:

- capabilities actually exercised by UC-001
- significant capabilities left unexercised

---

## Specific questions

Answer with evidence:

1. What concepts remained identical across all five implementations because
   they belong to Argonaut?

2. Which apparent common concepts should NOT yet become abstractions?

3. Is `Tool` actually a universal concept across these implementations?

4. Is an explicit agent loop universal?

5. What different forms of execution control appeared?

6. Which frameworks provide the best native observability for this use case?

7. Which provide the cleanest test isolation?

8. Which framework-specific mechanisms appear most useful for a more complex
   UC-002?

9. Where does UC-001 make a framework look more complicated than necessary?

10. Where does UC-001 hide potentially valuable framework capabilities?

---

## Framework characterization

Evaluate and refine, rather than blindly copy, this working characterization:

    Spring AI
        framework-native imperative AI integration

    LangChain4j
        declarative AI service / hidden agent loop

    LangGraph4j
        explicit state + explicit topology

    Koog
        strategy-based agent runtime

    Embabel
        goal/action/type-driven planning

Produce more precise descriptions if the evidence supports them.

---

## Framework selection guidance

Produce a section:

    "When would I reach for each framework?"

This MUST be based on observed architecture plus clearly labeled inference.

Do not declare an overall winner.

Include:

- favorable problem shape;
- trade-offs;
- warning signs that another abstraction may fit better;
- confidence level based on what UC-001 actually exercised.

---

## Abstraction findings

Identify confirmed framework-neutral concepts.

Likely candidates to evaluate include:

- KnowledgeRepository
- ExperimentRequest / Result
- Evidence
- trace / metrics semantics
- experiment lifecycle

Identify concepts that remain premature, including potentially:

- common Tool
- common Agent
- common Graph
- common planner
- universal model-call abstraction

Explain why.

Do not implement any abstraction.

---

## UC-002 implications

UC-001 under-exercises graph/planning frameworks.

Propose characteristics for UC-002 that would expose meaningful differences,
such as:

- multiple stages;
- evolving state;
- conditional branching;
- alternative valid plans;
- retry/revision;
- parallel work;
- validation;
- optional human intervention;
- multiple responsibilities.

Do NOT design UC-002 in detail.

Only specify what capabilities the next experiment must exercise.

---

## Deliverable

Create:

docs/engineering/agents/reports/
ARGONAUT-MILESTONE-UC001-001-five-framework-comparative-synthesis.md

Update the engineering log.

Do not commit.

---

## Non-goals

DO NOT:

- refactor code;
- create shared framework abstractions;
- select a winning framework;
- implement Jigsaw;
- design Codex;
- implement UC-002;
- modify UI;
- modify Docker/infra;
- perform external research.

This task closes the learning milestone only.

---

## Definition of done

The report should let a future reader understand:

- what each framework fundamentally contributed;
- why all five can satisfy the same contract differently;
- what belongs to Argonaut rather than frameworks;
- when each framework appears most appropriate;
- what we still cannot conclude from UC-001;
- what UC-002 needs to test next.