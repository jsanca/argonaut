package dev.jsanca.argonaut.decision.question;

import dev.jsanca.argonaut.decision.result.ScoreResult;

import java.util.List;
import java.util.Objects;

/**
 * A question that asks a decision engine to place an evaluation on an ordered categorical scale.
 *
 * <p>Order is semantically significant: the first entry in {@code scale} is the lowest position
 * and the last is the highest. A {@link Score} is not merely a {@link Choice} with a different
 * name — the ordered semantics must be preserved by the result.</p>
 *
 * <p>The scale must contain at least two entries to have ordering semantics.</p>
 *
 * @param <T> the scale type
 */
public record Score<T>(String id, String instructions, List<T> scale)
        implements Question<T, ScoreResult<T>> {

    public Score {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException("instructions must not be blank");
        }

        Objects.requireNonNull(scale, "scale must not be null");

        if (scale.size() < 2) {
            throw new IllegalArgumentException("scale must have at least 2 entries");
        }

        scale = List.copyOf(scale);
    }

    public static <T> Score<T> of(final  String id,
                                  final String instructions,
                                  final List<T> scale) {

        return new Score<>(id, instructions, scale);
    }
}
