# Question Types — Choice, Score, Noul

> Deeper reference for the three sealed subtypes of `Question<T, R>` in `argonaut-decision`: what each one means, when to use it, validation rules, and worked examples drawn from the test contract.

Each section ends with **Test contract** pointers. The full behaviour is verified by the tests in `argonaut-decision/src/test/java/dev/jsanca/argonaut/decision/` (see `DecisionModelContractTest`, `QuestionValidationTest`, `ProbabilityTest`).

---

## Choice — selection from a bounded set

A `Choice<T>` asks the decision engine to pick one element from a finite, typed candidate set. The result is a `ChoiceResult<T>` whose `selected` is one of the candidates. With `T = String` or an enum constant, the picked value is a real Java value, not a string-shaped bag of attributes.

### When to use

- A closed-set routing decision: `Route = {BILLING, RETURNS, SHIPPING, …}`.
- A classification with a known vocabulary: `category ∈ {billing, support, sales}`.
- Anything where "the answer is one of these N specific things" is part of the question's *meaning*. If open-vocabulary is plausible, you probably want a Noul or a different upstream abstraction.

### Constructors

```java
Choice.ofEnum(id, instructions, type)        // candidates = List.of(type.getEnumConstants())
Choice.ofStrings(id, instructions, list)     // candidates = any String list
new Choice<>(id, instructions, list)         // raw constructor; the record's compact form validates
```

### Validation

| Field | Rule | Source |
| --- | --- | --- |
| `id` | non-null, non-blank | `Choice` compact constructor |
| `instructions` | non-null, non-blank | `Choice` compact constructor |
| `candidates` | non-null, non-empty, returned as an immutable copy | `Choice` compact constructor |

The contract documents this as: *"Use `ofEnum` for enum-typed candidates (the full enum constants are the candidate set) and `ofStrings` for string-typed candidates."*

### Worked example — typed enum routing

```java
enum Route { RETURNS, SHIPPING, BILLING, ACCOUNT }

Choice<Route> routeQ = Choice.ofEnum(
        "route",
        "Which team should handle this ticket first?",
        Route.class);

var request = DecisionRequest.builder()
        .context(DecisionContext.of("""
            Order 5521 arrived two weeks late and now I see two charges on my card.
            Please refund one and tell me when the other arrives.
            """))
        .question(routeQ)
        .build();
```

```java
ChoiceResult<Route> result = model.decide(request).get(routeQ);
switch (result.selected()) {
    case RETURNS  -> /* route to returns inbox */;
    case SHIPPING -> /* route to shipping inbox */;
    case BILLING  -> /* route to billing inbox */;
    case ACCOUNT  -> /* route to account inbox */;
}
```

The compiler enforces exhaustiveness and the candidate type. There is no string-key lookup and no chance of the model returning `"None"` or `"I don't know"` — the picked value is one of the enum constants. (See the discussion of "absent probability" in [the Noul section](#absence-versus-zero) for how to handle uncertainty.)

### Worked example — string-typed category

```java
Choice<String> categoryQ = Choice.ofStrings(
        "category",
        "Which category best fits this request?",
        List.of("billing", "support", "sales"));

ChoiceResult<String> result = model.decide(request).get(categoryQ);
String category = result.selected();      // one of "billing", "support", "sales"
```

Use `Choice<String>` only when the candidate set is truly string-shaped and you want the picked value as a string. Enums are usually better — they catch typos at compile time.

### Worked example — distribution alongside selected

When a provider returns a full probability distribution (the Jev-compatible providers do when the `PROBABILITY_DISTRIBUTION` capability is advertised), the result carries it:

```java
ChoiceResult<Route> result = ChoiceResult.withDistribution(
        Route.BILLING,
        ProbabilityDistribution.of(List.of(
                OutcomeProbability.of(Route.BILLING,  0.62),
                OutcomeProbability.of(Route.SHIPPING, 0.21),
                OutcomeProbability.of(Route.RETURNS,  0.12),
                OutcomeProbability.of(Route.ACCOUNT,  0.05))));
```

Callers that only need the picked option read `result.selected()`; callers that need the full shape read `result.distribution().entries()`. Both come from the same answer; `selected` is `argmax(distribution)` by construction in well-calibrated providers.

### Confidence: nullable, not optional

```java
ChoiceResult<Route> noConf    = ChoiceResult.of(Route.BILLING);              // selected only
ChoiceResult<Route> withConf  = ChoiceResult.of(Route.BILLING, 0.87);        // selected + confidence
ChoiceResult<Route> withDist  = ChoiceResult.withDistribution(Route.BILLING, dist);  // selected + full distribution
```

| Value | Meaning |
| --- | --- |
| `confidence() == null` | Provider did not report confidence. |
| `confidence() == 0.0` | Provider reported zero confidence. |

The two are distinct (TC-DM-007). Do not use either as a sentinel for the other.

### Test contract

- **TC-DM-001** — typed enum Choice → typed `ChoiceResult<Route>`.
- **TC-DM-002** — string Choice returns a selected String.
- **TC-DM-007** — `null` confidence vs `0.0` confidence.
- **Validation** — `QuestionValidationTest` covers blank id, blank instructions, empty candidate list.

---

## Score — placement on an ordered categorical scale

A `Score<T>` asks the decision engine to place an evaluation on an **ordered** categorical scale. The result is a `ScoreResult<T>` whose `selected` is one of the scale entries and whose `scale` is the same ordered list passed in at construction. Order is semantically meaningful — `Score` is not just `Choice` with a different name.

### When to use

- An ordered judgment that has *meaningful direction*: `Complexity ∈ {LOW < MEDIUM < HIGH}`.
- Severity, urgency, confidence, priority, etc.
- Anything where "higher" / "lower" / "between" is the question. If the levels are not ordered, use `Choice` or a `Noul` per pair.

### Constructors

```java
Score.of(id, instructions, scale)
new Score<>(id, instructions, scale)     // raw constructor; the compact form validates
```

### Validation

| Field | Rule | Source |
| --- | --- | --- |
| `id` | non-null, non-blank | `Score` compact constructor |
| `instructions` | non-null, non-blank | `Score` compact constructor |
| `scale` | non-null, size ≥ 2, returned as an immutable copy | `Score` compact constructor |

The contract documents this as: *"the first entry in `scale` is the lowest position and the last is the highest. A `Score` is not merely a `Choice` with a different name — the ordered semantics must be preserved by the result."*

The size-2 minimum is what gives `Score` its ordering semantics; with one entry there is no order.

### Worked example — three-level complexity

```java
enum Complexity { LOW, MEDIUM, HIGH }

Score<Complexity> complexityQ = Score.of(
        "complexity",
        "How architecturally complex is this change?",
        List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH));
```

```java
ScoreResult<Complexity> result = model.decide(request).get(complexityQ);
Complexity c = result.selected();                          // LOW, MEDIUM, or HIGH
int position = result.scale().indexOf(c);                  // 0, 1, or 2

// Order is observable via index — TC-DM-003
Complexity higher = (position < result.scale().size() - 1)
        ? result.scale().get(position + 1) : null;
Complexity lower = (position > 0)
        ? result.scale().get(position - 1) : null;
```

### `rawScore` and what it is not

`ScoreResult.rawScore` is the provider's **fractional** position across the scale. For a 3-level scale with `selected = MEDIUM`, `rawScore = 1.04` means "slightly above level 1". This is provider-supplied and may be absent (`null`).

```java
if (result.rawScore() != null) {
    double p = result.rawScore();
    // p is in [0, scale.size() - 1], continuous.
    // The integer argmax of the full distribution gives scale.indexOf(selected);
    // rawScore is the fractional position, biased toward the modal index.
}
```

The Javadoc is unambiguous on what `rawScore` is **not**:

> It is `null` when the provider does not supply a continuous score — this field must not be used to gate decisions in place of the full probability distribution.

Use `result.distribution()` (when present) for gating; `rawScore` is informational.

### Confidence and distribution — same rules as Choice

`ScoreResult.confidence` and `ScoreResult.distribution` follow the same nullable-not-optional convention as `ChoiceResult`. A `null` confidence is "information absent"; a `0.0` confidence is "information present at zero".

When the provider returns a distribution, the distribution's outcomes are the scale entries in the same order:

```java
ProbabilityDistribution<Complexity> dist = result.distribution();
List<Complexity> entries = dist.entries().stream()
        .map(OutcomeProbability::outcome).toList();
// entries.equals(result.scale()) by contract.
```

### Test contract

- **TC-DM-003** — score scale preserved in result; order observable via index.
- **Validation** — `QuestionValidationTest` covers scale size < 2.
- **TC-DM-007** — `null` vs `0.0` confidence also applies to Score.

---

## Noul — a binary proposition, returned as a probability

A `Noul` is a yes/no question whose answer is **a probability that the proposition is true**, not a boolean. It is the answer-with-uncertainty type for the contract; if you ask "is this safe?" you do not receive `true` or `false`, you receive `P(safe = true) ∈ [0.0, 1.0]`.

### When to use

- Binary decisions you intend to gate or threshold at the application layer: *"does this need human review?"*, *"is this PII?"*, *"should this be escalated?"*.
- Risks you'd accept confidently as boolean in theory but in practice need calibration data on.
- Anything that has a "yes/no" surface but where the "no" is rarely absolute (almost nothing is).

### Constructors

```java
Noul.of(id, instructions)
new Noul(id, instructions)         // raw constructor; compact form validates
```

### Validation

| Field | Rule | Source |
| --- | --- | --- |
| `id` | non-null, non-blank | `Noul` compact constructor |
| `instructions` | non-null, non-blank | `Noul` compact constructor |

There is no candidate set on `Noul`: the domain type parameter is fixed at `Void`, and the result is `NoulResult` (no domain value, just a probability).

### What NoulResult does and does not have

```java
NoulResult review = model.decide(request).get(reviewQ);
double p = review.probabilityTrue().value();      // 0.73 (any double in [0.0, 1.0])
```

What it has:

- `probabilityTrue()` — a `Probability` whose `.value()` is in `[0.0, 1.0]`.

What it deliberately does **not** have:

- `selected()`,
- `isTrue()`, `isFalse()`, `asBoolean()`,
- any method that returns a `boolean` or `Optional<Boolean>`,
- any policy threshold baked into the contract.

The Javadoc on `NoulResult` is precise:

> There is intentionally no `selected()` or boolean conversion method. Applying a threshold to derive a boolean is a policy concern that belongs to a higher layer, not to this result type.

The same rule is repeated on `Noul`:

> Converting the probability to a boolean requires a policy threshold. That conversion is intentionally absent from both this question and its result type.

### Absence versus zero

The contract's "no sentinel" rule is sharpest here:

| Expression | Meaning |
| --- | --- |
| `result.probabilityTrue() == null` | Should not happen for Noul (the field is non-null), but `Probability` itself can be absent in a wider distribution context. |
| `result.probabilityTrue().value() == 0.0` | A measurement of zero probability. The provider is fully confident the proposition is false. |

`Probability(0.0)` is a valid, constructable value. The compact constructor only rejects `NaN`, `±∞`, and values outside `[0.0, 1.0]`. (TC-DM-009.) The semantic difference between "0.0" and "absent" exists at the level of `ChoiceResult.confidence` and `ScoreResult.confidence` (where `null` means "the provider didn't tell us"), not at the level of `NoulResult.probabilityTrue` (where the field itself is non-null).

### Worked example — escalation policy held in the application

```java
Noul reviewQ = Noul.of("needs-review", "Does this change need senior reviewer sign-off?");

NoulResult review = model.decide(request).get(reviewQ);
double p = review.probabilityTrue().value();

// Policy lives in the application. Not in the contract.
boolean autoApprove = p < 0.20;                              // your threshold
String disposition = autoApprove ? "auto" : "manual review";
```

If your policy depends on confidence, use the prior over the answer — not the answer — to set the threshold:

```java
// For example: only auto-approve when both signals agree
double priorRate  = 0.05;                                   // % of changes that historically need review
double p          = review.probabilityTrue().value();
double ratio      = p / priorRate;
boolean escalate  = ratio < 5.0;                            // ratio, not raw p, is your policy knob
```

The contract only constrains that `p ∈ [0.0, 1.0]` and is finite; the calibration story (whether `p = 0.20` is "20% of the time escalates" or "20% confident it escalates") is provider-dependent.

### Worked example — a heterogeneous batch mixing all three

```java
var route      = Choice.ofEnum("route",      "Which team?", Route.class);
var complexity = Score.of(     "complexity", "Complexity?",
                              List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH));
var review     = Noul.of(      "review",     "Needs review?");

DecisionResult result = model.decide(
    DecisionRequest.builder()
        .context(DecisionContext.of("change description"))
        .question(route)
        .question(complexity)
        .question(review)
        .build());

ChoiceResult<Route>     r1 = result.get(route);       // no cast
ScoreResult<Complexity> r2 = result.get(complexity); // no cast
NoulResult              r3 = result.get(review);     // no cast
```

This is one HTTP call. The contract enforces the order the questions appear in the request is preserved in `request.questions()`, and that no question id appears twice (TC-DM-008). The provider receives the same shape; the answer maps each id to its typed result by the `DecisionResult.get(question)` call. (See [`tutorial-jev.md`](./tutorial-jev.md) for the full provider-side example and [`class-diagrams.md`](./class-diagrams.md) §5 for the retrieval sequence.)

### Test contract

- **TC-DM-004** — `NoulResult.probabilityTrue.value()` is `0.73` (or whatever the model returned); it is not converted to `true`/`false` anywhere on the contract path.
- **TC-DM-009** — invalid probabilities (`-0.001`, `1.001`, `NaN`, `±∞`) rejected; boundary values `0.0` and `1.0` accepted.
- **Validation** — `QuestionValidationTest` covers blank id and blank instructions.

---

## Choosing among the three

| Decision shape | Use | Reason |
| --- | --- | --- |
| Pick one of N named candidates | `Choice<T>` | Argmax is part of the question's meaning. |
| Place on an ordered scale | `Score<T>` | Direction is part of the question's meaning; the result carries the scale. |
| Binary proposition, want a probability | `Noul` | Argmax is *your* policy decision; the contract returns evidence. |
| Open-vocabulary answer (free text) | None of the three — out of scope | Use an upstream generative abstraction and build `DecisionContext` from it. |
| Multiple yes/no at once | Multiple `Noul` questions in one request | Native batch (TC-DM-005); one HTTP call, several probabilities. |
| Mix of the above | Several questions in one request | The whole point of the contract. |

Three patterns to avoid:

1. **`Choice<String>` with `List.of("yes", "no")` to fake a Noul.** Use `Noul`. You get a real `Probability` and the option to inspect calibration later.
2. **`Score` with a one-element scale.** Use a one-element set only when the answer is an enumeration with a single valid output; otherwise `Choice` with one candidate.
3. **`Noul` with a boolean method on the result type.** Not available, by design. Add a `selected()` at your application layer if you must — but treat it as your policy, not the model's.
