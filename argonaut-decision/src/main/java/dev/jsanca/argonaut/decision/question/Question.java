package dev.jsanca.argonaut.decision.question;

import dev.jsanca.argonaut.decision.result.AnswerResult;

/**
 * A typed question submitted to a {@link dev.jsanca.argonaut.decision.DecisionModel}.
 *
 * <p>The type parameter {@code T} is the domain type (e.g., a Route enum or String).
 * The type parameter {@code R} is the result type produced when this question is answered.
 * The question instance itself serves as a typed handle for retrieving the result from
 * a {@link dev.jsanca.argonaut.decision.DecisionResult}.</p>
 *
 * <p>Subtypes:</p>
 * <ul>
 *   <li>{@link Choice} — selection from a bounded candidate set</li>
 *   <li>{@link Score} — evaluation over an ordered categorical scale</li>
 *   <li>{@link Noul} — binary proposition evaluated probabilistically</li>
 * </ul>
 *
 * @param <T> the domain type of the question
 * @param <R> the result type produced by evaluating this question
 */
public sealed interface Question<T, R extends AnswerResult<T>>
        permits Choice, Score, Noul {

    /** Unique identifier for this question within a request. */
    String id();

    /** Instructions or prompt text describing the question for the decision engine. */
    String instructions();
}
