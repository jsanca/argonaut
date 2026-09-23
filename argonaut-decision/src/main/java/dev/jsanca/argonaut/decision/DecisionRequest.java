package dev.jsanca.argonaut.decision;

import dev.jsanca.argonaut.decision.question.Question;
import dev.jsanca.argonaut.decision.result.AnswerResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable request submitted to a {@link DecisionModel}.
 *
 * <p>A request carries a shared {@link DecisionContext} and one or more heterogeneous
 * typed {@link Question}s. Question identities must be unique within a request; duplicate
 * IDs are rejected at build time.</p>
 */
public final class DecisionRequest {

    private final DecisionContext context;
    private final List<Question<?, ?>> questions;

    private DecisionRequest(DecisionContext context, List<Question<?, ?>> questions) {
        this.context = context;
        this.questions = List.copyOf(questions);
    }

    public DecisionContext context() {
        return context;
    }

    public List<Question<?, ?>> questions() {
        return questions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private DecisionContext context;
        private final List<Question<?, ?>> questions = new ArrayList<>();
        private final Set<String> ids = new LinkedHashSet<>();

        public Builder context(DecisionContext context) {
            this.context = Objects.requireNonNull(context, "context must not be null");
            return this;
        }

        public <T, R extends AnswerResult<T>> Builder question(Question<T, R> question) {
            Objects.requireNonNull(question, "question must not be null");
            if (!ids.add(question.id())) {
                throw new IllegalArgumentException(
                        "Duplicate question id in DecisionRequest: '" + question.id() + "'");
            }
            questions.add(question);
            return this;
        }

        public DecisionRequest build() {
            Objects.requireNonNull(context, "context must not be null");
            if (questions.isEmpty()) {
                throw new IllegalStateException("DecisionRequest must contain at least one question");
            }
            return new DecisionRequest(context, questions);
        }
    }
}
