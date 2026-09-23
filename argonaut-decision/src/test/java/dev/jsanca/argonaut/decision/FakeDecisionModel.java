package dev.jsanca.argonaut.decision;

import dev.jsanca.argonaut.decision.question.Question;
import dev.jsanca.argonaut.decision.result.AnswerResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A provider-independent {@link DecisionModel} for use in tests and contract verification.
 *
 * <p>Callers configure preset answers per question before calling {@link #decide}. No Jev,
 * ChatModel, or any other provider dependency is involved. This satisfies TC-DM-010.</p>
 *
 * <p>Lives in test scope so that provider integration modules (future) can use it without
 * pulling test infrastructure into production. Resides in the same package as
 * {@link DecisionResult} to access {@code DecisionResult.assemble()}.</p>
 */
public final class FakeDecisionModel implements DecisionModel {

    private final Map<String, AnswerResult<?>> presets;
    private final Set<DecisionCapability> capabilities;

    private FakeDecisionModel(Map<String, AnswerResult<?>> presets, Set<DecisionCapability> capabilities) {
        this.presets = Map.copyOf(presets);
        this.capabilities = Set.copyOf(capabilities);
    }

    @Override
    public DecisionResult decide(DecisionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Map<String, AnswerResult<?>> answers = new LinkedHashMap<>();
        for (Question<?, ?> question : request.questions()) {
            AnswerResult<?> answer = presets.get(question.id());
            if (answer == null) {
                throw new IllegalStateException(
                        "FakeDecisionModel has no preset for question id: '" + question.id() + "'");
            }
            answers.put(question.id(), answer);
        }
        return DecisionResult.assemble(answers);
    }

    @Override
    public Set<DecisionCapability> capabilities() {
        return capabilities;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, AnswerResult<?>> presets = new LinkedHashMap<>();
        private final Set<DecisionCapability> capabilities = java.util.EnumSet.noneOf(DecisionCapability.class);

        public <T, R extends AnswerResult<T>> Builder preset(Question<T, R> question, R answer) {
            Objects.requireNonNull(question, "question must not be null");
            Objects.requireNonNull(answer, "answer must not be null");
            presets.put(question.id(), answer);
            return this;
        }

        public Builder capability(DecisionCapability cap) {
            Objects.requireNonNull(cap, "capability must not be null");
            capabilities.add(cap);
            return this;
        }

        public FakeDecisionModel build() {
            return new FakeDecisionModel(presets, capabilities);
        }
    }
}
