package dev.jsanca.argonaut.decision;

import dev.jsanca.argonaut.decision.question.Question;
import dev.jsanca.argonaut.decision.result.AnswerResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable heterogeneous result container returned by {@link DecisionModel#decide}.
 *
 * <p>Results are retrieved via typed question handles:
 * <pre>{@code
 *   ChoiceResult<Route> r = result.get(routeQuestion);
 * }</pre>
 * No consumer cast is required. The compile-time type is derived from the question's
 * result type parameter.</p>
 *
 * <h2>Unchecked cast boundary</h2>
 * <p>The internal map stores {@code AnswerResult<?>} values keyed by question ID.
 * The cast in {@link #get} is safe by construction: the only path that writes to the
 * map is {@link Builder#answer(Question, AnswerResult)}, which requires the caller to
 * supply a matching {@code Question<T,R>} and {@code R} pair. The cast never escapes
 * this class. It must not be replicated by consumers.</p>
 */
public final class DecisionResult {

    private final Map<String, AnswerResult<?>> answers;

    private DecisionResult(final Map<String, AnswerResult<?>> answers) {
        this.answers = Map.copyOf(answers);
    }

    /**
     * Retrieves the typed result for a question.
     *
     * @throws IllegalArgumentException if no result exists for the given question id
     */
    @SuppressWarnings("unchecked")
    public <T, R extends AnswerResult<T>> R get(final Question<T, R> question) {

        Objects.requireNonNull(question, "question must not be null");
        AnswerResult<?> raw = answers.get(question.id());

        if (raw == null) {
            throw new IllegalArgumentException(
                    "No result for question id: '" + question.id() + "'");
        }
        return (R) raw; // safe: builder.answer() enforces Question<T,R> ↔ R pairing
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Provider SPI factory: constructs a {@code DecisionResult} from a raw answer map.
     *
     * <p>Intended for {@link DecisionModel} implementations that build their answer map
     * programmatically (e.g. after deserializing a provider HTTP response). Normal consumers
     * should use {@link #builder()} with the typed {@link Builder#answer} method instead.</p>
     *
     * <p>Callers must ensure each value's type is consistent with the question whose id
     * was used as the key. The unchecked cast in {@link #get} is safe only when this
     * invariant holds.</p>
     */
    public static DecisionResult forProvider(final Map<String, AnswerResult<?>> rawAnswers) {
        Objects.requireNonNull(rawAnswers, "rawAnswers must not be null");
        if (rawAnswers.isEmpty()) {
            throw new IllegalArgumentException("provider result map must not be empty");
        }
        return new DecisionResult(rawAnswers);
    }

    /**
     * Package-private alias retained for {@code FakeDecisionModel} (same package, test scope).
     */
    static DecisionResult assemble(final Map<String, AnswerResult<?>> rawAnswers) {
        return forProvider(rawAnswers);
    }

    public static final class Builder {

        private final Map<String, AnswerResult<?>> answers = new LinkedHashMap<>();

        public <T, R extends AnswerResult<T>> Builder answer(final Question<T, R> question, final R result) {
            Objects.requireNonNull(question, "question must not be null");
            Objects.requireNonNull(result, "result must not be null");
            answers.put(question.id(), result);
            return this;
        }

        public DecisionResult build() {
            if (answers.isEmpty()) {
                throw new IllegalStateException("DecisionResult must contain at least one answer");
            }
            return new DecisionResult(answers);
        }
    }
}
