

# ARGONAUT-009 — LangChain4j Controlled Local Evidence Implementation

## Mission

Implement the second real framework implementation of Argonaut's Controlled Local Evidence RAG experiment using **LangChain4j**.

The implementation must satisfy the same experiment contract already used by the Spring AI implementation:

```text
Same mission.
Same model.
Same evidence.
Different agentic framework.
```

The goal is **not** to reproduce the internal architecture of the Spring AI module.

Use LangChain4j idiomatically while preserving the common Argonaut experiment boundary.

This task should also remove one known experiment-variable leak: the Controlled Local Evidence system prompt currently lives privately inside the Spring AI implementation. Move that prompt to a framework-neutral location in `argonaut-core` and make both implementations consume exactly the same prompt text.

Do not implement LangGraph4j or Embabel in this task.

Do not introduce a framework-agnostic agent/tool abstraction unless the need is demonstrated by both implementations.

---

## Read First

Inspect at minimum:

```text
README.md
docs/OSK.md
docs/PROJECT.md

docs/knowledge/common-contract.md
docs/knowledge/use-cases/controlled-local-evidence-rag.md

docs/engineering/ENGINEERING_LOG.md

ARGONAUT-007 review
ARGONAUT-008A report
ARGONAUT-008B report

argonaut-core/
argonaut-spring-ai/
```

Pay particular attention to:

```text
ControlledLocalEvidenceAgent
KnowledgeTool
KnowledgeRepository
ExperimentRequest
ExperimentResult
ControlledLocalEvidenceContract / TC-UC-001
ExecutionTrace
ExecutionMetrics
TraceEventMetadata
```

Use the existing Spring AI implementation as **behavioral evidence**, not as an architectural template.

---

# Part 1 — Make the Mission Prompt Common

The Spring AI implementation currently contains a private framework-specific copy of the Controlled Local Evidence system prompt.

Move the prompt into `argonaut-core`.

Choose an appropriate framework-neutral representation, for example:

```text
ControlledLocalEvidencePrompt
ControlledLocalEvidenceExperiment
ControlledLocalEvidenceMission
```

Use existing naming conventions where possible.

The exact class name is secondary.

The important invariant is:

```text
Spring AI prompt bytes/text
==
LangChain4j prompt bytes/text
```

Both framework implementations must obtain the prompt from the same core source.

Do not move framework-specific prompt APIs into core.

Core owns only the experiment text/content.

Framework modules own how that content is supplied to their framework.

Add a test locking the common prompt where useful.

---

# Part 2 — Add the LangChain4j Module

Create a dedicated module following repository conventions, preferably:

```text
argonaut-langchain4j/
```

The module may depend on:

```text
argonaut-core
LangChain4j dependencies strictly required for this implementation
```

It must not depend on:

```text
argonaut-spring-ai
Spring AI
LangGraph4j
Embabel
```

Do not create cross-framework implementation dependencies.

---

# Core Architectural Boundary

Reuse the existing framework-neutral domain/experiment contracts:

```text
KnowledgeRepository
ExperimentRequest
ExperimentResult
Evidence
ExecutionTrace
ExecutionMetrics
TC-UC-001 validator
authoritative corpus
common experiment prompt
```

Do not move LangChain4j concepts into `argonaut-core`.

In particular, do not introduce framework-neutral abstractions named like:

```text
Agent
Tool
ToolContext
ToolCall
AgentLoop
ChatModel
Planner
Workflow
Graph
```

merely to make Spring AI and LangChain4j look alike.

If both implementations need similar adapters, allow duplication for now and document it.

---

# Research Rule — Tool Duplication

A specific research question for this task is:

> Is knowledge-tool duplication across frameworks merely adapter duplication, or does it reveal a useful framework-neutral capability abstraction?

For this task:

**prefer framework-native implementations.**

It is acceptable for LangChain4j to have its own equivalent of Spring AI's:

```text
KnowledgeTool
```

provided both implementations ultimately delegate domain knowledge access to the same:

```text
KnowledgeRepository
```

Do not prematurely introduce a common `Tool` abstraction.

At the end of the task, compare the two implementations and report:

```text
- duplicated behavior;
- framework-specific behavior;
- potentially framework-neutral behavior;
- whether any abstraction is already strongly justified.
```

Observation only.

Do not refactor based on the comparison in this task.

---

# LangChain4j Implementation

Implement the Controlled Local Evidence use case using LangChain4j's idiomatic tool/function mechanism.

The model must be responsible for deciding when to invoke the exposed knowledge operations.

Expose capabilities equivalent to:

```text
searchKnowledge(query)
readDocument(sourceId)
```

They must ultimately delegate to the same authoritative `KnowledgeRepository` used by the experiment.

Do not hard-code experiment evidence into the model response.

Do not bypass the repository by reading corpus files directly from the agent implementation.

---

# Agentic Behavior

The implementation should preserve the same conceptual interaction:

```text
ExperimentRequest
      ↓
model
      ↓
knowledge search tool
      ↓
KnowledgeRepository
      ↓
document read tool
      ↓
KnowledgeRepository
      ↓
model synthesis
      ↓
ExperimentResult
```

Use the exact common system prompt moved into `argonaut-core`.

Do not add memory, conversational history, planner logic, graph orchestration, or additional agents.

This experiment is intentionally:

```text
single request
agentic tool use
controlled evidence
single final response
```

---

# Observability

Produce the same framework-neutral observable contract required by TC-UC-001.

Record appropriate events including:

```text
RUN_STARTED
KNOWLEDGE_SEARCH_STARTED
KNOWLEDGE_SEARCH_COMPLETED
DOCUMENT_READ_STARTED
DOCUMENT_READ_COMPLETED
EVIDENCE_RETRIEVED
EVIDENCE_SELECTED
ANSWER_SYNTHESIZED
RUN_COMPLETED
```

Use the existing trace metadata convention.

The LangChain4j implementation may observe/tool these events differently internally.

Do not change the common event semantics merely to match LangChain4j.

---

# Evidence Semantics

Evidence must originate from documents actually accessed through the controlled knowledge capability.

Do not manufacture:

```text
Evidence(sourceId="exp-001")
```

merely to satisfy TC-UC-001.

Preserve traceability from:

```text
search/read behavior
        ↓
document
        ↓
Evidence
        ↓
ExperimentResult
```

If LangChain4j exposes better information than Spring AI about actual tool calls or selected tool results, preserve that observation in the report rather than changing the common contract immediately.

---

# Metrics

Use the existing `ExecutionMetrics`.

Inspect its current semantics carefully.

Do not simply copy Spring AI's implementation if the metric would be misleading.

In particular investigate:

```text
modelCalls
toolCalls
knowledgeSearches
documentReads
evidenceCount
errors
durationMs
```

The current Spring AI implementation hard-codes at least one model-call metric.

Determine what LangChain4j can measure reliably.

If model-call semantics differ between frameworks, report the ambiguity rather than inventing comparable numbers.

Do not redesign metrics in this task unless TC-UC-001 requires a correctness fix.

---

# Validation Against TC-UC-001

The LangChain4j implementation must be exercised through the same reusable contract created by ARGONAUT-008B.

It must not receive a framework-specific weaker validator.

Add tests proving an honest LangChain4j execution/result shape can satisfy:

```text
TC-UC-001
```

where tests can run deterministically without an external model.

For real-model integration tests, follow existing project policy around credentials and opt-in execution.

Do not make the normal Maven build depend on paid/external model availability.

---

# Model Configuration

Use the same model/provider configuration intended for the Spring AI comparison wherever practically possible.

Do not silently choose a different model because LangChain4j makes it easier.

External credentials must remain configuration/environment driven.

Never commit secrets.

If exact model parity cannot yet be established, record the limitation prominently.

---

# No Framework Homogenization

Do not make LangChain4j call Spring AI-shaped APIs merely for visual symmetry.

The implementations are allowed to look different.

For example, this is acceptable:

```text
Spring AI
  ChatClient
  KnowledgeTool
  toolContext

LangChain4j
  AiServices / ChatModel / @Tool / native mechanism
```

as long as both satisfy the same outer experiment contract.

The experiment exists partly to discover these differences.

---

# Documentation

Update relevant documentation to record:

```text
Spring AI     → first framework implementation
LangChain4j   → second framework implementation
```

Document that both share:

```text
same corpus
same mission prompt
same ExperimentRequest
same TC-UC-001
same ExperimentResult contract
```

while framework-native orchestration/tool APIs remain intentionally independent.

Add a research note/question:

> Which abstractions remain genuinely framework-agnostic after multiple framework implementations, and which should remain framework-native?

Do not answer the question conclusively yet.

---

# Tests

Add tests covering at minimum:

```text
common mission prompt is used;
knowledge search delegates to KnowledgeRepository;
document read delegates to KnowledgeRepository;
trace metadata links accessed documents to evidence;
ExperimentResult has frameworkId identifying LangChain4j;
TC-UC-001 validator accepts a valid LangChain4j result fixture/execution;
invalid/hollow result cannot bypass the shared validator.
```

Do not duplicate the entire common TC-UC-001 negative-test suite inside the LangChain4j module.

Reuse the common validator.

---

# Validation

Run at minimum:

```bash
mvn test -pl argonaut-core
mvn test -pl argonaut-langchain4j -am
mvn verify
git diff --check
```

Report exact results.

Do not claim real-model validation unless it was actually performed.

---

# Engineering Report

Create:

```text
docs/engineering/agents/reports/ARGONAUT-009-langchain4j-controlled-local-evidence.md
```

Update:

```text
docs/engineering/ENGINEERING_LOG.md
```

according to repository conventions.

The report must include:

```text
- module/classes added;
- LangChain4j mechanism selected;
- common prompt extraction;
- knowledge capability implementation;
- observability implementation;
- TC-UC-001 validation;
- metric semantics;
- external-model validation if performed;
- Spring AI vs LangChain4j structural comparison;
- duplicated code observed;
- potential common abstractions;
- abstractions deliberately NOT introduced;
- limitations;
- recommended next framework/task.
```

Do not commit changes.

---

# Non-Goals

Do not:

```text
- implement LangGraph4j;
- implement Embabel;
- refactor Spring AI into a generic agent framework;
- create a universal Tool API;
- create a universal agent loop;
- add memory;
- add conversation persistence;
- add planner/orchestrator behavior;
- add embeddings/vector search;
- add web search;
- change the authoritative corpus;
- weaken TC-UC-001;
- compare framework winners;
- optimize prompts separately by framework.
```

---

# Success Criteria

The task succeeds if:

1. Spring AI and LangChain4j consume exactly the same experiment prompt;
2. LangChain4j uses the same controlled evidence repository;
3. LangChain4j uses its own idiomatic framework mechanisms;
4. it produces the same framework-neutral `ExperimentResult`;
5. TC-UC-001 remains the shared executable acceptance contract;
6. no Spring-specific concepts leak into core;
7. no premature universal Tool abstraction is introduced;
8. duplicated adapter behavior is explicitly documented for later analysis;
9. metrics/evidence semantics are reported honestly;
10. the implementation gives Argonaut meaningful evidence about what is actually common across frameworks.

---

Y respecto a tu preocupación: **si terminamos con cinco `KnowledgeTool` pequeñitos que solo adaptan `search/read` al framework, no me preocuparía mucho**.

Eso puede terminar siendo justamente el patrón correcto:

```text
common capability
    KnowledgeRepository

framework adapters
    SpringAiKnowledgeTool
    LangChain4jKnowledgeTools
    LangGraphKnowledgeNode
    EmbabelKnowledgeAction
```

Son cinco adapters, pero **una sola lógica de conocimiento**.

Me preocuparía si empezamos a duplicar en los cinco:

* scoring;
* corpus loading;
* authority logic;
* evidence construction;
* retry policy;
* filtering;
* business rules.

Ahí sí significaría que nos falta una abstracción común.

Por ahora, dejar que aparezca un poco de duplicación es incluso deseable: **necesitamos verla repetirse antes de saber qué merece subir al core**. Después de LangChain4j ya tendremos la primera comparación real para empezar a responder esa pregunta.
