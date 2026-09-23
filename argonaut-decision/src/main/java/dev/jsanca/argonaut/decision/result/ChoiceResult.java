package dev.jsanca.argonaut.decision.result;

import dev.jsanca.argonaut.decision.probability.ProbabilityDistribution;

import java.util.Objects;

/**
 * The result of a {@link dev.jsanca.argonaut.decision.question.Choice} evaluation.
 *
 * <p>{@code confidence} is nullable. A {@code null} confidence means no confidence
 * information is available; a value of {@code 0.0} means zero confidence. These are
 * semantically distinct — do not use {@code 0.0} as a sentinel for "absent".</p>
 *
 * <p>{@code distribution} is nullable when the provider does not supply a full
 * probability distribution.</p>
 *
 * @param <T> the candidate type
 */
public record ChoiceResult<T>(
        T selected,
        Double confidence,
        ProbabilityDistribution<T> distribution
) implements AnswerResult<T> {

    public ChoiceResult {
        Objects.requireNonNull(selected, "selected must not be null");
    }

    public static <T> ChoiceResult<T> of(final T selected) {
        return new ChoiceResult<>(selected, null, null);
    }

    public static <T> ChoiceResult<T> of(final T selected, final double confidence) {
        return new ChoiceResult<>(selected, confidence, null);
    }

    public static <T> ChoiceResult<T> withDistribution(final T selected, final ProbabilityDistribution<T> distribution) {

        Objects.requireNonNull(distribution, "distribution must not be null");
        return new ChoiceResult<>(selected, null, distribution);
    }
}
