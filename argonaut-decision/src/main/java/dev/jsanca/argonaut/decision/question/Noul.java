package dev.jsanca.argonaut.decision.question;

import dev.jsanca.argonaut.decision.result.NoulResult;

/**
 * A question that asks a decision engine to evaluate a binary proposition probabilistically.
 *
 * <p>The result is {@link NoulResult}, which carries {@code P(proposition = true)} as a
 * {@link dev.jsanca.argonaut.decision.probability.Probability}. There is no domain type
 * parameter ({@code T = Void}) because a Noul question has no candidate set.</p>
 *
 * <p>Converting the probability to a boolean requires a policy threshold. That conversion
 * is intentionally absent from both this question and its result type.</p>
 */
public record Noul(String id, String instructions)
        implements Question<Void, NoulResult> {

    public Noul {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException("instructions must not be blank");
        }
    }

    public static Noul of(final String id, final String instructions) {

        return new Noul(id, instructions);
    }
}
