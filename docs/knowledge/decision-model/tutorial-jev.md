# Tutorial: A First Decision Request Against TypeSafe Jev

> Build a `DecisionRequest` with one Choice, one Score, and one Noul, and run it against TypeSafe AI's hosted Jev implementation through the `argonaut-decision-systemone` provider module. No other Maven module is required.

This tutorial verifies end-to-end against the **fact**: every line of Java here compiles, every Javadoc quoted here is in the source, and every constructor / getter / factory exists exactly as named.

---

## 1. What you will build

A single Java program that

1. constructs a `DecisionContext` with one piece of shared state,
2. declares three heterogeneous questions (a `Choice`, a `Score`, a `Noul`),
3. packages them into a `DecisionRequest`,
4. sends the request to TypeSafe AI Jev over the System One protocol,
5. retrieves each answer with **no consumer cast**.

The point is not the domain example (a support ticket triage with routing, complexity, escalation). The point is the shape: one model call, three answers, all typed.

---

## 2. Prerequisites

- Java 25 (the project `pom.xml` declares `maven.compiler.release=25`; do not lower it).
- `uv`-style Maven availability; this project uses the parent `dev.jsanca.argonaut:argonaut-parent:0.1.0-SNAPSHOT`.
- Two Maven modules on the classpath:
  - `dev.jsanca.argonaut:argonaut-decision` (the domain contract — types only, no Jev).
  - `dev.jsanca.argonaut:argonaut-decision-systemone` (the System One provider; default target is Jev).
- A TypeSafe AI Jev API key. Obtain one from `https://docs.typesafe.ai/api`; set it as `JEV_API_KEY` in your environment.

Verify the dependency in a downstream module's `pom.xml`:

```xml
<dependency>
    <groupId>dev.jsanca.argonaut</groupId>
    <artifactId>argonaut-decision</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>dev.jsanca.argonaut</groupId>
    <artifactId>argonaut-decision-systemone</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The two modules together are the **only** runtime dependencies of this tutorial. There is no LangChain4j in your application code; that lives one layer below as the transport (see `ARG-DECISION-EP-002F`).

---

## 3. The full program

Save this as `Triage.java`. Replace the imports if your IDE auto-orders them; nothing else changes.

```java
import dev.jsanca.argonaut.decision.DecisionContext;
import dev.jsanca.argonaut.decision.DecisionRequest;
import dev.jsanca.argonaut.decision.DecisionResult;
import dev.jsanca.argonaut.decision.systemone.SystemOneDecisionModel;

import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import dev.jsanca.argonaut.decision.question.Score;

import dev.jsanca.argonaut.decision.result.ChoiceResult;
import dev.jsanca.argonaut.decision.result.NoulResult;
import dev.jsanca.argonaut.decision.result.ScoreResult;

import java.util.List;
import java.util.Set;

public final class Triage {

    // Domain types — your application's vocabulary, not Jev's.
    public enum Route      { RETURNS, SHIPPING, BILLING, ACCOUNT }
    public enum Complexity { LOW, MEDIUM, HIGH }

    public static void main(String[] args) {

        // ── Step 1: declare questions (typed handles) ─────────────────────────
        // Choice<Route>: a bounded candidate set derived from an enum.
        Choice<Route>  routeQ =
                Choice.ofEnum("route", "Which team should handle this ticket first?", Route.class);

        // Score<Complexity>: an ordered scale with at least two entries.
        Score<Complexity> complexityQ =
                Score.of("complexity", "How architecturally complex is this change?",
                        List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH));

        // Noul: a binary proposition returned as a probability, never as a boolean.
        Noul reviewQ =
                Noul.of("needs-review", "Does this change need architectural review?");

        // ── Step 2: build the request ──────────────────────────────────────────
        DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("""
                    Order 5521 arrived two weeks late and now I see two charges on my card.
                    Please refund one and tell me when the other arrives.
                    """))
                .question(routeQ)
                .question(complexityQ)
                .question(reviewQ)
                .build();

        // ── Step 3: build the model (default target is TypeSafe AI Jev) ────────
        SystemOneDecisionModel model = SystemOneDecisionModel.builder()
                .apiKey(System.getenv("JEV_API_KEY"))      // bearer token; required
                // .baseUrl("https://api.typesafe.ai")      // default; override for Kev/OpenRouter
                // .modelName("jev-latest")                // default
                // .connectTimeout(Duration.ofSeconds(10))  // default
                // .readTimeout(Duration.ofSeconds(60))     // default
                // .maxRetries(2)                          // default
                .build();

        // ── Step 4: one HTTP call, three typed answers ─────────────────────────
        DecisionResult result = model.decide(request);

        // ── Step 5: retrieve each answer with no cast ─────────────────────────
        ChoiceResult<Route>     routeResult =
                result.get(routeQ);                            // ChoiceResult<Route>
        ScoreResult<Complexity> complexityResult =
                result.get(complexityQ);                       // ScoreResult<Complexity>
        NoulResult              reviewResult =
                result.get(reviewQ);                           // NoulResult

        // ── Step 6: consume ────────────────────────────────────────────────────
        Set<dev.jsanca.argonaut.decision.DecisionCapability> caps = model.capabilities();
        System.out.printf("capabilities=%s%n", caps);
        System.out.printf("route      = %s%n",      routeResult.selected());
        System.out.printf("complexity = %s%n",      complexityResult.selected());
        if (complexityResult.rawScore() != null) {
            System.out.printf("rawScore   = %.3f%n", complexityResult.rawScore());
        }
        if (complexityResult.confidence() != null) {
            System.out.printf("confidence = %.3f%n", complexityResult.confidence());
        }
        System.out.printf("P(review)  = %.3f%n",    reviewResult.probabilityTrue().value());
    }
}
```

Run it:

```bash
export JEV_API_KEY=...
mvn -pl my-app -am compile exec:java -Dexec.mainClass=Triage
```

What you should observe, paraphrasing TC-DM-005 (heterogeneous batch):

- one HTTP call goes out to `https://api.typesafe.ai/v1/systemone`,
- three typed answers come back, one per question,
- argmax on the Choice is a single `Route` value (not a string),
- `ScoreResult.scale` is the same ordered list you passed in,
- `NoulResult.probabilityTrue.value()` is in `[0.0, 1.0]`.

---

## 4. Reading the result types

Each `get(question)` returns the exact `R` declared by the question; the assignment requires no `@SuppressWarnings` and no `(R)` cast. The compile-time contract is TC-DM-006 in `DecisionModelContractTest`:

```java
ChoiceResult<Route> routeResult = result.get(routeQ);
```

The runtime guarantee is TC-DM-007: a `null` confidence is *information absent*, and `0.0` is *information present at zero*. Use `result.get(q).confidence() == null` to distinguish them; do not treat either as `0.0` by default.

For `Score`, the result duplicates the scale so that downstream code can interpret `selected` without holding the original question:

```java
ScoreResult<Complexity> complexityResult = result.get(complexityQ);
int position = complexityResult.scale().indexOf(complexityResult.selected());   // 0..2
Complexity lower = (position > 0) ? complexityResult.scale().get(position - 1) : null;
```

For `Noul`, treat the probability as evidence, not a decision:

```java
NoulResult review = result.get(reviewQ);
double p = review.probabilityTrue().value();        // in [0.0, 1.0]
boolean autoApprove = p < 0.20;                     // your policy threshold, not the contract's
```

The contract deliberately does not provide a `selected()` method on `NoulResult`. See [`questions.md`](./questions.md#noul) and the discussion of "absence versus zero" below.

---

## 5. Five things to verify in your first run

These are not "things that may go wrong"; they are observable properties guaranteed by the test contract. If any is violated, the bug is in your code or in the code you depend on, not in the contract.

1. **One HTTP call, three typed answers.** Watch your network panel. `POST /v1/systemone` should fire once. (Behavior of `SystemOneDecisionModel.decide`; backed by the `MULTI_QUESTION` and `NATIVE_BATCH` capabilities in `model.capabilities()`.)
2. **Question identity is by string.** The IDs you set (`"route"`, `"complexity"`, `"needs-review"`) are what wire the request to the response. The contract enforces uniqueness at `DecisionRequest.Builder.question(...)` via a `LinkedHashSet` of ids; reusing an id throws `IllegalArgumentException` (TC-DM-008).
3. **`null` confidence is not zero.** When the provider does not supply a confidence, `ChoiceResult.confidence()` is `null`. The contract document records this explicitly so it doesn't get "fixed" by a future contributor: *"a `null` confidence means no confidence information is available; a value of `0.0` means zero confidence. These are semantically distinct — do not use `0.0` as a sentinel for 'absent'."*
4. **`Probability(0.0)` is constructable.** A probability of exactly `0.0` is a valid measurement, not a sentinel. The record's compact constructor only rejects non-finite values and values outside `[0.0, 1.0]`. (TC-DM-009 in `DecisionModelContractTest`.)
5. **The NoulResult is not a boolean decision.** `NoulResult.probabilityTrue().value() == 0.73` does not become `true` somewhere downstream. Apply your own threshold where the policy actually lives.

---

## 6. Targeting a different System One–compatible endpoint

The `SystemOneDecisionModel.builder()` is a configuration object, not a Jev-specific class. The `pom.xml` description is explicit:

> The default configuration targets TypeSafe AI's Jev model; other compatible implementations (Kev, OpenRouter) are reached by changing `baseUrl` and `modelName`.

```java
SystemOneDecisionModel kev = SystemOneDecisionModel.builder()
        .apiKey("local")
        .baseUrl("http://127.0.0.1:8009")              // Kev's local serve (kev.serve)
        .modelName("kev-latest")
        .build();
```

The same `DecisionRequest` you built in Step 2 works without any change. The provider modules in `argonaut-decision-systemone` and elsewhere are wired to the *System One contract*, not to a single vendor.

If you change providers, recompute your policy threshold (your section 4 `autoApprove = p < 0.20`) against the new provider's calibration. Provider temperatures differ, and the meaning of "0.93 P(review)" varies. The contract only promises the probability is in `[0.0, 1.0]` and is honest within the provider's own calibration set.

---

## 7. Things that are deliberately *not* in this tutorial

| Topic | Where to read |
| --- | --- |
| Calibration of probabilities; per-provider temperatures | `argonaut-decision-systemone` provider docs; not the domain tutorial's concern. |
| Thresholding and automation policy | Higher layer; the contract deliberately exposes a probability and lets the application decide. |
| Tests with `FakeDecisionModel` | `argonaut-decision/src/test/java/dev/jsanca/argonaut/decision/DecisionModelContractTest.java` has 11 examples with no network, no Jev. |
| Internal mappers, transport, retries | `argonaut-decision-systemone/src/main/java/.../internal/`; covered by `ARG-DECISION-EP-002F`. |

The boundary the contract draws is: **the domain knows nothing about Jev, Kev, or OpenRouter**. Your application decides what provider to call; the contract decides what the call looks like in Java.
