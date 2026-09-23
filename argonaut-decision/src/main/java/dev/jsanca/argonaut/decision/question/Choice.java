package dev.jsanca.argonaut.decision.question;

import dev.jsanca.argonaut.decision.result.ChoiceResult;

import java.util.List;
import java.util.Objects;

/**
 * A question that asks a decision engine to select one candidate from a bounded set.
 *
 * <p>Use {@link #ofEnum} for enum-typed candidates (the full enum constants are the candidate set)
 * and {@link #ofStrings} for string-typed candidates.</p>
 *
 * @param <T> the candidate type
 */
public record Choice<T>(String id, String instructions, List<T> candidates)
        implements Question<T, ChoiceResult<T>> {

    public Choice {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException("instructions must not be blank");
        }

        Objects.requireNonNull(candidates, "candidates must not be null");

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }

        candidates = List.copyOf(candidates);
    }

    public static <T extends Enum<T>> Choice<T> ofEnum(final String id,
                                                       final String instructions,
                                                       final Class<T> type) {

        Objects.requireNonNull(type, "type must not be null");
        return new Choice<>(id, instructions, List.of(type.getEnumConstants()));
    }

    public static Choice<String> ofStrings(final String id,
                                           final String instructions,
                                           final List<String> candidates) {

        return new Choice<>(id, instructions, candidates);
    }
}
