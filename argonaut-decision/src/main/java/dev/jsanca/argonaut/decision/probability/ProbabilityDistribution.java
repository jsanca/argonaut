package dev.jsanca.argonaut.decision.probability;

import java.util.List;
import java.util.Objects;

/**
 * A probability distribution over a set of typed outcomes.
 *
 * @param <T> the type of the outcome values
 */
public record ProbabilityDistribution<T>(List<OutcomeProbability<T>> entries) {

    public ProbabilityDistribution {
        Objects.requireNonNull(entries, "entries must not be null");
        entries = List.copyOf(entries);
    }

    public static <T> ProbabilityDistribution<T> of(final List<OutcomeProbability<T>> entries) {
        return new ProbabilityDistribution<>(entries);
    }
}
