package dev.jsanca.argonaut.decision.probability;

import java.util.Objects;

/**
 * A single outcome and its associated probability in a distribution.
 *
 * @param <T> the type of the outcome value
 */
public record OutcomeProbability<T>(T outcome, Probability probability) {

    public OutcomeProbability {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(probability, "probability must not be null");
    }

    public static <T> OutcomeProbability<T> of(final T outcome, final double probability) {
        return new OutcomeProbability<>(outcome, Probability.of(probability));
    }
}
