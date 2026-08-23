# ARGONAUT-010

## LangGraph4j Controlled Local Evidence Implementation

### Assignee

Clio

### Mission

Implement the third framework variant of Argonaut's **Controlled Local Evidence RAG / UC-001**, using **LangGraph4j**.

The existing implementations are:

```text
argonaut-spring-ai/
argonaut-langchain4j/
```

Add:

```text
argonaut-langgraph4j/
```

The experiment invariant remains:

```text
Same mission.
Same model.
Same evidence.
Same acceptance contract.
Different agentic framework.
```

The objective is NOT to reproduce the architecture of Spring AI or LangChain4j using LangGraph4j APIs.

Use LangGraph4j **idiomatically**.

This implementation is especially important because the first two frameworks use relatively similar model-driven tool interaction styles:

```text
Spring AI
  ChatClient + tools

LangChain4j
  AiServices + tools
```

LangGraph4j introduces graph/state-oriented orchestration.

We want to observe what remains genuinely common when the orchestration model changes.

---

# Read First

Inspect at minimum:

```text
README.md
docs/OSK.md
docs/PROJECT.md

docs/knowledge/common-contract.md
docs/knowledge/use-cases/controlled-local-evidence-rag.md

docs/engineering/ENGINEERING_LOG.md

ARGONAUT-007
ARGONAUT-008A
ARGONAUT-008B
ARGONAUT-009 report

argonaut-core/
argonaut-spring-ai/
argonaut-langchain4j/
```

Pay particular attention to:

```text
ControlledLocalEvidencePrompt
ExperimentRequest
ExperimentResult
KnowledgeRepository
Evidence
ExecutionTrace
ExecutionEvent
ExecutionMetrics
ControlledLocalEvidenceContract / TC-UC-001
```

And compare:

```text
Spring AI:
  ControlledLocalEvidenceAgent
  KnowledgeTool

LangChain4j:
  ControlledLocalEvidenceAgent
  KnowledgeTools
```

Treat those implementations as behavioral evidence, NOT templates that LangGraph4j must imitate.

---

# Core Constraint

Do not change the experiment.

LangGraph4j must use:

```text
same ControlledLocalEvidencePrompt.SYSTEM_PROMPT
same KnowledgeRepository
same authoritative corpus
same ExperimentRequest
same ExperimentResult
same TC-UC-001 validator
```

Do not create a LangGraph4j-specific acceptance contract.

---

# Module

Create:

```text
argonaut-langgraph4j/
```

following existing Maven/module conventions.

It may depend on:

```text
argonaut-core
LangGraph4j dependencies required for UC-001
```

It must NOT depend on:

```text
argonaut-spring-ai
argonaut-langchain4j
Spring AI
LangChain4j-specific implementation code
Embabel
```

No cross-framework implementation dependencies.

---

# Use LangGraph4j Idiomatically

Do not implement:

```text
SpringAiAgentButWithLangGraphNames
```

or:

```text
LangChain4jAiServicesWrappedInGraph
```

solely to achieve structural symmetry.

Determine the idiomatic LangGraph4j representation for UC-001.

Expected conceptual ingredients may include:

```text
graph
state
nodes
edges
conditional transitions
model invocation
tool execution
```

but use the actual framework abstractions appropriate to the version selected.

Document why the chosen graph structure is idiomatic.

---

# Preserve the Agentic Nature of UC-001

The model must still participate in deciding how controlled knowledge is used.

Do not replace the experiment with a deterministic pipeline such as:

```text
search
  ↓
read
  ↓
LLM
```

where the application unconditionally chooses all retrieval actions.

UC-001 is intended to exercise agentic retrieval.

The conceptual behavior remains:

```text
question
   ↓
model reasoning / decision
   ↓
knowledge search
   ↓
model/state
   ↓
document read
   ↓
model/state
   ↓
possibly more retrieval
   ↓
final synthesis
```

The exact graph topology is LangGraph4j's concern.

---

# Graph State

Use LangGraph4j's state model naturally.

Determine what per-run information belongs in graph state.

Potential state includes:

```text
question
messages
retrieved/search results
documents read
final answer
```

Do not automatically place Argonaut observability infrastructure into graph state if a cleaner mechanism exists.

In particular investigate how LangGraph4j handles the problem solved by:

```text
Spring AI
  ToolContext(observer, reads)

LangChain4j
  KnowledgeTools(repository, observer, reads)
```

Record the LangGraph4j solution explicitly.

This is an important experiment observation.

---

# Controlled Knowledge Capability

The framework must expose equivalent capabilities to:

```text
searchKnowledge(query)
readDocument(sourceId)
```

Both must ultimately delegate to:

```text
KnowledgeRepository
```

Do not duplicate:

```text
corpus loading
ranking
document authority
search semantics
document lookup
```

inside the LangGraph4j module.

Those remain core responsibilities.

---

# Tool / Node Design

Do NOT introduce a common framework-neutral `Tool` abstraction as part of this task.

Allow LangGraph4j to represent knowledge access however is idiomatic:

```text
tool
node
action
graph transition
other native mechanism
```

The experiment should help determine whether the apparent duplication across frameworks is:

```text
necessary adapter duplication
```

or:

```text
evidence of a missing common abstraction
```

Observe first.

Refactor later.

---

# Important Research Question

After implementation, compare the three approaches:

```text
Spring AI
LangChain4j
LangGraph4j
```

Specifically classify duplicated concepts into:

### Clearly common domain capability

Example candidate:

```text
KnowledgeRepository
```

### Common experiment infrastructure

Possible candidates already observed:

```text
buildEvidence(...)
buildMetrics(...)
ExecutionEvent construction
tool result formatting
tool descriptions
```

### Framework-native adapter behavior

Examples currently include:

```text
Spring AI ToolContext
LangChain4j per-run tool object
LangGraph4j state/node mechanism
```

### Premature abstraction

Identify anything that looks similar in two frameworks but stops being natural in the third.

Do NOT refactor based on this classification in ARGONAUT-010.

The report is the deliverable for this analysis.

---

# Prompt Parity

Consume exactly:

```java
ControlledLocalEvidencePrompt.SYSTEM_PROMPT
```

from `argonaut-core`.

Do not create another prompt variant.

Do not optimize the prompt for LangGraph4j.

Prompt differences would contaminate UC-001.

---

# Observability

Produce the same framework-neutral observable event contract.

The resulting trace must support the existing TC-UC-001 assertions.

Required semantics include:

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

and:

```text
RUN_FAILED
```

where appropriate.

How these events are produced internally may differ substantially from the other frameworks.

That difference is valuable experimental evidence.

---

# Evidence

Evidence must be grounded in documents actually accessed through the controlled knowledge capability.

Preserve:

```text
search/read
    ↓
DocumentContent
    ↓
Evidence
    ↓
ExperimentResult
```

Do not fabricate evidence simply to satisfy TC-UC-001.

If LangGraph4j naturally distinguishes:

```text
retrieved
read
used
selected
```

better than the current implementations, record that observation.

Do NOT change the common evidence contract in this task unless required for correctness.

---

# Framework-Native Observability

The previous comparison revealed that both existing implementations currently discard useful native framework metadata:

Spring AI currently terminates at approximately:

```text
.call().content()
```

while LangChain4j currently exposes only:

```text
String answer(...)
```

despite their frameworks offering richer result/response models.

For LangGraph4j:

**do not intentionally throw away useful native execution information merely to match those limitations.**

Investigate what LangGraph4j exposes naturally regarding:

```text
node executions
state transitions
model interactions
tool invocations
intermediate messages/responses
token usage
execution timing
errors
```

Use information that maps cleanly to the existing Argonaut contract.

For richer information that does not map cleanly:

```text
document it
do not force it into core
```

This will inform a later observability-normalization task.

---

# Metrics

Use the existing:

```text
ExecutionMetrics
```

where semantics are defensible.

Current known issue:

```text
Spring AI:
  modelCalls currently approximated/hardcoded

LangChain4j:
  modelCalls currently reported as unknown
```

Do not manufacture a comparable `modelCalls` value for LangGraph4j.

If LangGraph4j can reliably expose actual model invocation count, report it accurately and document why it is trustworthy.

If not:

```text
unknown
```

is preferable to false precision.

Record any native metric capabilities that could later help normalize:

```text
model calls
tool calls
tokens
duration
state transitions
```

across frameworks.

---

# TC-UC-001

LangGraph4j must produce an:

```text
ExperimentResult
```

accepted by the exact same reusable:

```text
ControlledLocalEvidenceContract
```

created by ARGONAUT-008B.

Do not weaken validation.

Do not duplicate the validator.

Do not add framework exceptions.

---

# Failure Semantics

Map LangGraph4j failures into the existing:

```text
ExperimentResult.failed(...)
```

contract.

Preserve useful trace evidence accumulated before failure.

Do not expose framework exceptions directly through the experiment boundary unless existing Argonaut conventions explicitly require it.

---

# Tests

Add tests covering at minimum:

1. common prompt usage;
2. KnowledgeRepository delegation;
3. search trace production;
4. document-read trace production;
5. evidence traceability;
6. successful ExperimentResult;
7. failed ExperimentResult;
8. framework ID = LangGraph4j;
9. valid result passes shared TC-UC-001;
10. hollow/invalid result cannot bypass TC-UC-001;
11. graph state does not leak between independent runs.

Do not require paid/external model access for the normal build.

Use deterministic test doubles where appropriate.

---

# Real Model

If existing project infrastructure permits a real-model smoke test using the same provider/model configuration as the other implementations, perform it.

Otherwise document:

```text
NOT RUN
```

and why.

Never commit credentials.

Exact model parity remains an experiment requirement for the eventual comparative run, not a reason to make normal Maven builds depend on external services.

---

# Documentation

Update relevant project documentation to show UC-001 now has three framework implementations:

```text
Controlled Local Evidence RAG

  Spring AI
  LangChain4j
  LangGraph4j
```

Preserve the experiment framing:

```text
same mission
same model
same evidence
different framework
```

Do not claim framework winners.

---

# Three-Framework Comparison

The engineering report must include a concise comparison such as:

```text
Concern              Spring AI       LangChain4j       LangGraph4j
--------------------------------------------------------------------
entry abstraction    ChatClient      AiServices        ?
per-run state        ToolContext     tool instance      ?
knowledge exposure   @Tool           @Tool              ?
agent loop           framework       framework          ?
state visibility     ...             ...                ...
native metadata      ...             ...                ...
```

Fill it from actual implementation evidence.

Do not force symmetry where none exists.

The differences are the point.

---

# Engineering Report

Create:

```text
docs/engineering/agents/reports/
ARGONAUT-010-langgraph4j-controlled-local-evidence.md
```

Update:

```text
docs/engineering/ENGINEERING_LOG.md
```

according to project conventions.

The report must include:

* LangGraph4j version/dependencies;
* graph topology;
* state model;
* model invocation mechanism;
* knowledge-access mechanism;
* per-run state handling;
* common prompt usage;
* observability;
* native execution metadata discovered;
* evidence semantics;
* metric semantics;
* TC-UC-001 validation;
* test results;
* real-model validation status;
* three-framework comparison;
* duplicated behavior across all three;
* framework-native differences;
* candidate common abstractions;
* abstractions deliberately deferred;
* limitations;
* recommendation for what should happen before implementing framework #4.

---

# Validation

Run at minimum:

```bash
mvn test -pl argonaut-core
mvn test -pl argonaut-langgraph4j -am
mvn verify
git diff --check
```

Use the repository's actual module names if they differ.

Report exact results.

Do not claim real-model validation unless actually performed.

---

# Non-Goals

Do not:

* implement Embabel;
* implement another use case;
* add memory;
* add human-in-the-loop;
* add multiple agents;
* add web search;
* add embeddings/vector search;
* redesign KnowledgeRepository;
* create a universal Tool API;
* create a universal Agent API;
* create a universal Graph API;
* refactor Spring AI or LangChain4j for symmetry;
* normalize every native framework metric;
* declare a framework winner;
* change UC-001 mission semantics.

---

# Success Criteria

ARGONAUT-010 succeeds if:

1. LangGraph4j implements the same UC-001;
2. it consumes the exact shared mission prompt;
3. it uses the same authoritative KnowledgeRepository;
4. it remains idiomatic to LangGraph4j;
5. it produces the same ExperimentResult boundary;
6. the same TC-UC-001 validator accepts honest executions;
7. framework-specific graph/state concepts do not leak into core;
8. useful native observability is preserved/documented;
9. no premature common Tool/Agent abstraction is introduced;
10. the implementation gives us a third independent data point for deciding what is genuinely framework-agnostic.

Do not commit.
