package dev.jsanca.argonaut.decision.result;

import dev.jsanca.argonaut.decision.probability.ProbabilityDistribution;

import java.util.List;
import java.util.Objects;

/**
 * The result of a {@link dev.jsanca.argonaut.decision.question.Score} evaluation.
 *
 * <p>The {@code scale} is carried in the result so callers can interpret where
 * {@code selected} falls without needing the original question. Order is semantically
 * significant: the first entry is the lowest and the last is the highest.</p>
 *
 * <p>{@code rawScore} carries a provider's probability-weighted fractional position across
 * the scale (e.g. {@code 1.04} meaning slightly above index 1 in a 3-level scale).
 * It is {@code null} when the provider does not supply a continuous score — this field
 * must not be used to gate decisions in place of the full probability distribution.</p>
 *
 * <p>{@code confidence} and {@code distribution} are nullable; see {@link ChoiceResult}
 * for the absence-vs-zero convention.</p>
 *
 * @param <T> the scale type
 */
public record ScoreResult<T>(
        T selected,
        List<T> scale,
        Double rawScore,
        Double confidence,
        ProbabilityDistribution<T> distribution
) implements AnswerResult<T> {

    public ScoreResult {
        Objects.requireNonNull(selected, "selected must not be null");
        scale = scale != null ? List.copyOf(scale) : List.of();
    }

    /** Creates a minimal result with no raw score, confidence, or distribution. */
    public static <T> ScoreResult<T> of(final T selected, final List<T> scale) {
        return new ScoreResult<>(selected, scale, null, null, null);
    }
}
