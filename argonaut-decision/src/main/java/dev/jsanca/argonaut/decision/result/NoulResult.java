package dev.jsanca.argonaut.decision.result;

import dev.jsanca.argonaut.decision.probability.Probability;

import java.util.Objects;

/**
 * The result of a {@link dev.jsanca.argonaut.decision.question.Noul} evaluation.
 *
 * <p>The fundamental result is {@code probabilityTrue}: the probability that the
 * proposition evaluated to {@code true}. This is evidence, not a decision.</p>
 *
 * <p>There is intentionally no {@code selected()} or boolean conversion method.
 * Applying a threshold to derive a boolean is a policy concern that belongs to a
 * higher layer, not to this result type.</p>
 */
public record NoulResult(Probability probabilityTrue) implements AnswerResult<Void> {

    public NoulResult {
        Objects.requireNonNull(probabilityTrue, "probabilityTrue must not be null");
    }

    public static NoulResult of(final double probability) {

        return new NoulResult(Probability.of(probability));
    }
}
