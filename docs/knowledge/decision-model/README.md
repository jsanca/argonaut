# Decision Model

> Provider-independent Java foundation for typed decision-model evaluation: a single shared context, one or more heterogeneous typed questions (Choice / Score / Noul), typed results with no consumer cast.

## What this folder covers

The Argonaut `argonaut-decision` module and the providers that back it. The module is a small, durable API surface — types, sealed interfaces, builders, and a single `DecisionModel` interface — that multiple provider implementations can target without leaking transport, provider, or framework details.

This folder is **durable knowledge**. It records the shape of the contract, why it is shaped that way, and how an application uses it. It is not an implementation report and not a research log; those live in `docs/engineering/`.

## Documents in this folder

| Document | Purpose |
| --- | --- |
| [README.md](./README.md) | This file — scope, what belongs here, and how the documents fit together. |
| [class-diagrams.md](./class-diagrams.md) | Mermaid class diagrams of the four hierarchies (`DecisionRequest`/`DecisionResult`, `Question`/`AnswerResult`, result records, probability types). |
| [tutorial-jev.md](./tutorial-jev.md) | Worked tutorial: build a `DecisionRequest` with Choice + Score + Noul and run it against TypeSafe AI Jev via the `argonaut-decision-systemone` module. |
| [questions.md](./questions.md) | Deeper reference for the three question kinds: what they mean, when to use each, validation rules, and worked examples. |

## Module anatomy (one paragraph)

`argonaut-decision` exposes one interface — `DecisionModel.decide(DecisionRequest) → DecisionResult` — plus the typed vocabulary that surrounds it: `DecisionContext` (the shared state), three sealed subtypes of `Question<T, R>` (`Choice<T>`, `Score<T>`, `Noul`), three sealed subtypes of `AnswerResult<T>` (`ChoiceResult<T>`, `ScoreResult<T>`, `NoulResult`), and a `Probability` family (`Probability`, `OutcomeProbability<T>`, `ProbabilityDistribution<T>`). A `DecisionRequest` carries one context and one or more questions with unique IDs; `DecisionResult` is retrieved by passing the question handle back into `result.get(question)`, yielding the exact `R` with **no consumer cast**. The sealed hierarchies and the typed handle are what make "no consumer cast" compile-time provable (TC-DM-006 in `DecisionModelContractTest`).

## Authority and source-of-truth hierarchy

If anything here disagrees with the code, **the code wins**. Verify against:

1. **Source** — `argonaut-decision/src/main/java/dev/jsanca/argonaut/decision/`. Every public type is a record, sealed interface, or builder; behaviour is documented in Javadoc on the type itself.
2. **Test contract** — `argonaut-decision/src/test/java/dev/jsanca/argonaut/decision/DecisionModelContractTest.java` defines the behavioural contract (TC-DM-001 through TC-DM-010). A claim about the API that no test exercises is a candidate for revision.
3. **Engineering reports and reviews** — `docs/engineering/agents/reports/ARG-DECISION-EP-001-decision-model-foundation.md` (skeleton, TC-DM-001..010), `ARG-DECISION-EP-002-jev-provider.md` (first provider, written when the module was still named `argonaut-decision-jev`), `ARG-DECISION-EP-002F-lc4j-infrastructure-reuse.md` (LangChain4j `HttpClient` / `RetryUtils` reuse in place of the local JDK transport), and the structural review at `docs/engineering/agents/reviews/ARG-DECISION-EP-002R/review.md`.
4. **Argonaut's common contract** — `docs/knowledge/common-contract.md`. The decision module is a dedicated sub-contract of the common experiment contract; it follows the common rules ("no framework coupling in core") and adds its own rule ("no implicit threshold on Noul").

> History note: the Jev provider module was originally named `argonaut-decision-jev` and contained `JevDecisionModel`. It was renamed to `argonaut-decision-systemone` with the class renamed to `SystemOneDecisionModel` because the same module now also targets Kev and any other System One-compatible provider (see `SystemOneDecisionModel` Javadoc and `argonaut-decision-systemone/pom.xml`). The implementation report at `ARG-DECISION-EP-002-jev-provider.md` still reflects the original name; the durable fact is the *contract*, not the module name.

## Boundaries — what this folder does not cover

- **Provider implementations.** The Jev / Kev / OpenRouter provider is documented at the level of *configuration* and *behaviour* (tutorial), not its internal mapper/transport/error classes. For internals, read `argonaut-decision-systemone/src/main/java/.../internal/`.
- **Knowledge correlation, retrieval, or RAG.** `DecisionContext` carries the state the model evaluates; how that state was constructed (search, retrieval, prior messages) is a higher-layer concern. `DecisionContext` Javadoc: *"This is not a RAG framework — higher layers are responsible for constructing the state from retrieved evidence, messages, or other sources."*
- **Chat models, planners, agents, graphs.** The contract explicitly excludes them: *"This interface does not represent a chat model, classifier, router, reranker, or policy engine. Provider implementations belong in separate modules and must never appear in this package"* (from `DecisionModel` Javadoc).
- **Calibration, thresholding, or automation policy.** A `NoulResult` exposes a probability; converting it to a boolean for automation is deliberately absent. Policy decisions belong to a higher layer.

## Quick reference — the API in 12 lines

```java
import dev.jsanca.argonaut.decision.*;
import dev.jsanca.argonaut.decision.question.*;
import dev.jsanca.argonaut.decision.result.*;

enum Route { FAST, NORMAL }
enum Complexity { LOW, MEDIUM, HIGH }

var route      = Choice.ofEnum("route",      "Which execution route fits?",       Route.class);
var complexity = Score.of(     "complexity", "How architecturally complex is this?",
                              List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH));
var review     = Noul.of(      "review",     "Does this require architectural review?");

DecisionResult result = model.decide(
    DecisionRequest.builder()
        .context(DecisionContext.of("shared state for all three questions"))
        .question(route)
        .question(complexity)
        .question(review)
        .build());

ChoiceResult<Route>     r1 = result.get(route);       // no cast
ScoreResult<Complexity> r2 = result.get(complexity); // no cast
NoulResult              r3 = result.get(review);     // no cast
```

See [class-diagrams.md](./class-diagrams.md), [tutorial-jev.md](./tutorial-jev.md), and [questions.md](./questions.md) for the full shape, a runnable end-to-end walkthrough, and per-question-type references.
