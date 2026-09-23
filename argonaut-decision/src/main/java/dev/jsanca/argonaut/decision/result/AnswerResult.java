package dev.jsanca.argonaut.decision.result;

/**
 * Sealed supertype for all typed decision results.
 *
 * <p>The type parameter {@code T} is the domain type associated with the question.
 * For {@link NoulResult}, which has no candidate domain type, {@code T} is {@code Void}.</p>
 *
 * <p>This hierarchy deliberately contains no {@code selected()} method. Adding it
 * would force a boolean threshold onto {@link NoulResult}, violating the architecture.</p>
 *
 * @param <T> the domain type of the question that produced this result
 */
public sealed interface AnswerResult<T>
        permits ChoiceResult, ScoreResult, NoulResult {
}
