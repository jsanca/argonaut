package dev.jsanca.argonaut.decision.probability;

/**
 * A formal probability value in [0.0, 1.0].
 *
 * <p>A value of {@code 0.0} is a valid probability (zero probability of the outcome).
 * It is NOT a sentinel for "probability unavailable". Absent probability is represented
 * by a {@code null} reference, not by any special Probability value.</p>
 */
public record Probability(double value) {

    public Probability {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "Probability value must be in [0.0, 1.0], got: " + value);
        }
    }

    public static Probability of(final double value) {
        return new Probability(value);
    }
}
