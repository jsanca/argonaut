package dev.jsanca.argonaut.decision;

import java.util.Map;
import java.util.Objects;

/**
 * The shared state against which all questions in a {@link DecisionRequest} are evaluated.
 *
 * <p>{@code state} is the primary content (e.g., a text passage, a JSON payload, or a
 * structured summary). {@code metadata} carries optional key-value annotations.
 * This is not a RAG framework — higher layers are responsible for constructing the state
 * from retrieved evidence, messages, or other sources.</p>
 */
public record DecisionContext(String state, Map<String, Object> metadata) {

    public DecisionContext {
        Objects.requireNonNull(state, "state must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static DecisionContext of(String state) {
        return new DecisionContext(state, Map.of());
    }
}
